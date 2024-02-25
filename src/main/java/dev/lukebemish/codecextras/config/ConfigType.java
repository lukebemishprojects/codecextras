package dev.lukebemish.codecextras.config;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.repair.FillMissingLogOps;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface ConfigType<O> {
    Codec<O> codec();

    default String versionKey() {
        return "config_version";
    }

    default int currentVersion() {
        return 0;
    }

    default @Nullable DataFixer fixer() { return null; }

    String name();

    O defaultConfig();

    default <T> DataResult<O> decode(DynamicOps<T> ops, T input, Logger logger) {
        ops = FillMissingLogOps.of((field, original) -> logger.warn("Could not parse entry "+original+" for field "+field+" in config "+name()+"; replacing with default."), ops);

        Dynamic<T> dynamic = new Dynamic<>(ops, input);
        DataFixer fixer = fixer();
        if (fixer != null) {
            DataResult<Integer> version = dynamic.getElement(versionKey()).flatMap(ops::getNumberValue).map(Number::intValue);
            if (version.result().isPresent()) {
                int versionValue = version.result().get();
                dynamic = fixer.update(CONFIG, dynamic, versionValue, currentVersion());
            } else if (version.error().isPresent()) {
                logger.error("Could not parse config version for config " + name() + "; any datafixers will not be applied!");
            }
        }
        return codec().parse(dynamic);
    }

    default <T> DataResult<T> encode(DynamicOps<T> ops, O config) {
        var out = codec().encodeStart(ops, config);
        if (fixer() != null) {
            out = out.flatMap(t -> ops.mergeToMap(t, ops.createString(versionKey()), ops.createInt(currentVersion())));
        }
        return out;
    }

    default <T> O load(Path location, OpsIo<T> opsIo, Logger logger) {
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

    default <T> void save(Path location, OpsIo<T> opsIo, Logger logger, O config) {
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

    DSL.TypeReference CONFIG = () -> "CONFIG";
}
