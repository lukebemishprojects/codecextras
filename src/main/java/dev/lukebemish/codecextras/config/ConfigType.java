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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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

	public <T> ConfigHandle<O> handle(Path location, OpsIo<T> opsIo) {
		return handle(location, opsIo, LoggerFactory.getLogger(ConfigType.class));
	}

	public <T> ConfigHandle<O> handle(Path location, OpsIo<T> opsIo, Logger logger) {
		List<String> missedFields = new ArrayList<>();
		List<String> unreadableFields = new ArrayList<>();
		OpsIo<T> withLogging = opsIo.accompanied(FillMissingLogOps.TOKEN, (FillMissingLogOps<T>) (field, original) -> {
			if (original.equals(opsIo.ops().empty())) {
				missedFields.add(field);
			} else {
				unreadableFields.add(field);
			}
		});
		if (!missedFields.isEmpty() || !unreadableFields.isEmpty()) {
			logger.info("Replacing missing or unreadable fields in config {}", location);
			for (String missedField : missedFields) {
				logger.info("Missing key {}; filling with default value", missedField);
			}
			for (String unreadableField : unreadableFields) {
				logger.warn("Unreadable key {}; filling with default value", unreadableField);
			}
		}
		return new ConfigHandle<>() {
            @Override
            public O load() {
                return ConfigType.this.load(location, withLogging, logger);
            }

            @Override
            public void save(O config) {
                ConfigType.this.save(location, withLogging, logger, config);
            }
        };
	}

	public interface ConfigHandle<O> {
		O load();
		void save(O config);
	}

	public void addFixers(Supplier<DataFixerBuilder> builder) {}

	public abstract O defaultConfig();

	public Optional<Integer> defaultVersion() {
		return Optional.empty();
	}

	public <T> DataResult<O> decode(String name, DynamicOps<T> ops, T input, Logger logger) {
		DynamicOps<T> withLogging = FillMissingLogOps.of((field, original) -> {
			if (original.equals(ops.empty())) {
				logger.info("In config {}, missing key {}; filling with default value", name, field);
			} else {
				logger.warn("In config {}, unreadable value for key {}; filling with default value", name, field);
			}
		}, ops);

		Dynamic<T> dynamic = new Dynamic<>(withLogging, input);
		DataFixer fixer = this.fixer.get();
		if (fixer != null) {
			Optional<Integer> version = dynamic
				.getElement(versionKey())
				.flatMap(withLogging::getNumberValue)
				.map(Number::intValue)
				.result().or(this::defaultVersion);
			if (version.isPresent()) {
				int versionValue = version.get();
				dynamic = fixer.update(CONFIG, dynamic, versionValue, currentVersion());
			} else {
				logger.error("Could not parse config version for config {}; any datafixers will not be applied!", name);
			}
		}
		return codec().parse(dynamic);
	}

	public <T> DataResult<T> encode(@SuppressWarnings("unused") String name, DynamicOps<T> ops, @SuppressWarnings("unused") Logger logger, O config) {
		var out = codec().encodeStart(ops, config);
		if (fixer.get() != null) {
			out = out.flatMap(t -> ops.mergeToMap(t, ops.createString(versionKey()), ops.createInt(currentVersion())));
		}
		return out;
	}

	public <T> O load(Path location, OpsIo<T> opsIo, Logger logger) {
		if (!Files.exists(location)) {
			logger.info("Config {} does not exist; creating default config", location);
			save(location, opsIo, logger, defaultConfig());
			return defaultConfig();
		} else {
			try (var is = Files.newInputStream(location)) {
				var out = decode(location.toString(), opsIo.ops(), opsIo.read(is), logger);
				if (out.error().isPresent()) {
					logger.error("Could not load config {}; attempting to fix by writing default config. Error was {}", location, out.error().get().message());
					save(location, opsIo, logger, defaultConfig());
					return defaultConfig();
				} else {
					var config = out.result().orElseThrow();
					save(location, opsIo, logger, config);
					return config;
				}
			} catch (IOException e) {
				logger.error("Could not load config {}; attempting to fix by writing default config ", location, e);
				save(location, opsIo, logger, defaultConfig());
				return defaultConfig();
			}
		}
	}

	public <T> void save(Path location, OpsIo<T> opsIo, Logger logger, O config) {
		DataResult<T> result = encode(location.toString(), opsIo.ops(), logger, config);
		if (result.error().isPresent()) {
			logger.error("Could not encode config {} to save it: {}", location, result.error().get().message());
		} else {
			//noinspection OptionalGetWithoutIsPresent
			T encoded = result.result().get();
			try {
				Files.createDirectories(location.getParent());
				try (var os = Files.newOutputStream(location)) {
					opsIo.write(encoded, os);
				}
			} catch (IOException e) {
				logger.error("Could not save config {}: ", location, e);
			}
		}
	}

	public static final DSL.TypeReference CONFIG = () -> "config";
}
