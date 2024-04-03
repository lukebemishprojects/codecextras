package dev.lukebemish.codecextras.config;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.repair.FillMissingLogOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ConfigType<O> {
	public abstract Codec<O> codec();

	public String versionKey() {
		return "config_version";
	}

	public int currentVersion() {
		return 0;
	}

	private final Supplier<@Nullable DataFixer> fixer;

	public ConfigType() {
		this.fixer = Suppliers.memoize(() -> {
			DataFixerBuilder dataFixerBuilder = new DataFixerBuilder(currentVersion());
			boolean[] touched = new boolean[] {false};
			addFixers(() -> {
				touched[0] = true;
				return dataFixerBuilder;
			});
			if (!touched[0]) {
				return null;
			}
			return dataFixerBuilder.buildUnoptimized();
		});
	}

	public <T> ConfigHandle<O> handle(Path path, OpsIo<T> opsIo, Logger logger) {
		OpsIo<T> withLogging = opsIo.accompanied(FillMissingLogOps.TOKEN, (FillMissingLogOps<T>) (field, original) -> {
			if (original.equals(opsIo.ops().empty())) {
				logger.info("Missing key {} in config {}; filling with default value", field, name());
			} else {
				logger.info("Unreadable key {} in config {}; filling with default value", field, name());
			}
		});
		return new ConfigHandle<O>() {
			@Override
			public O load() {
				return ConfigType.this.load(path, withLogging, logger);
			}

			@Override
			public void save(O config) {
				ConfigType.this.save(path, withLogging, logger, config);
			}
		};
	}

	public interface ConfigHandle<O> {
		O load();
		void save(O config);
	}

	public void addFixers(Supplier<DataFixerBuilder> builder) {}

	public abstract String name();

	public abstract O defaultConfig();

	public Optional<Integer> defaultVersion() {
		return Optional.empty();
	}

	public <T> DataResult<O> decode(DynamicOps<T> ops, T input, Logger logger) {
		ops = FillMissingLogOps.of((field, original) -> logger.warn("Could not parse entry "+original+" for field "+field+" in config "+name()+"; replacing with default."), ops);

		Dynamic<T> dynamic = new Dynamic<>(ops, input);
		DataFixer fixer = this.fixer.get();
		if (fixer != null) {
			Optional<Integer> version = dynamic
				.getElement(versionKey())
				.flatMap(ops::getNumberValue)
				.map(Number::intValue)
				.result().or(this::defaultVersion);
			if (version.isPresent()) {
				int versionValue = version.get();
				dynamic = fixer.update(CONFIG, dynamic, versionValue, currentVersion());
			} else {
				logger.error("Could not parse config version for config " + name() + "; any datafixers will not be applied!");
			}
		}
		return codec().parse(dynamic);
	}

	public <T> DataResult<T> encode(DynamicOps<T> ops, O config) {
		var out = codec().encodeStart(ops, config);
		if (fixer.get() != null) {
			out = out.flatMap(t -> ops.mergeToMap(t, ops.createString(versionKey()), ops.createInt(currentVersion())));
		}
		return out;
	}

	public <T> O load(Path location, OpsIo<T> opsIo, Logger logger) {
		if (!Files.exists(location)) {
			logger.info("Config {} does not exist; creating default config", name());
			save(location, opsIo, logger, defaultConfig());
			return defaultConfig();
		} else {
			try (var is = Files.newInputStream(location)) {
				var out = decode(opsIo.ops(), opsIo.read(is), logger);
				if (out.error().isPresent()) {
					logger.error("Could not load config {}; attempting to fix by writing default config. Error was {}", name(), out.error().get().message());
					save(location, opsIo, logger, defaultConfig());
					return defaultConfig();
				} else {
					//noinspection OptionalGetWithoutIsPresent
					var config = out.result().get();
					save(location, opsIo, logger, config);
					return config;
				}
			} catch (IOException e) {
				logger.error("Could not load config {}; attempting to fix by writing default config ", name(), e);
				save(location, opsIo, logger, defaultConfig());
				return defaultConfig();
			}
		}
	}

	public <T> void save(Path location, OpsIo<T> opsIo, Logger logger, O config) {
		DataResult<T> result = encode(opsIo.ops(), config);
		if (result.error().isPresent()) {
			logger.error("Could not encode config {} to save it: {}", name(), result.error().get().message());
		} else {
			//noinspection OptionalGetWithoutIsPresent
			T encoded = result.result().get();
			try {
				Files.createDirectories(location.getParent());
				try (var os = Files.newOutputStream(location)) {
					opsIo.write(encoded, os);
				}
			} catch (IOException e) {
				logger.error("Could not save config {}: ", name(), e);
			}
		}
	}

	public static final DSL.TypeReference CONFIG = () -> "config";
}
