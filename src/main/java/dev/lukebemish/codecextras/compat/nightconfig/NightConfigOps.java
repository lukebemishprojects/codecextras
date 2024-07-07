package dev.lukebemish.codecextras.compat.nightconfig;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.NullObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class NightConfigOps<T extends Config> implements DynamicOps<Object> {

    protected abstract T newConfig();

    @Override
    public Object empty() {
        return NullObject.NULL_OBJECT;
    }

    public T copyConfig(Config config) {
        T out = newConfig();
        out.addAll(config);
        return out;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, Object input) {
        if (input instanceof Config) {
            return convertMap(outOps, input);
        } else if (input instanceof NullObject) {
            return outOps.empty();
        } else if (input instanceof Number number) {
            return outOps.createNumeric(number);
        } else if (input instanceof String string) {
            return outOps.createString(string);
        } else if (input instanceof Boolean bool) {
            return outOps.createBoolean(bool);
        } else if (input instanceof List) {
            return convertList(outOps, input);
        }
        throw new UnsupportedOperationException("Object "+input+" could not be converted");
    }

    @Override
    public DataResult<Number> getNumberValue(Object input) {
        if (input instanceof Number number) {
            return DataResult.success(number);
        } else if (input instanceof Boolean bool) {
            return DataResult.success(bool ? 1 : 0);
        }
        return DataResult.error(() -> "Not a number: " + input);
    }

    @Override
    public Object createNumeric(Number i) {
        return i;
    }

    @Override
    public DataResult<String> getStringValue(Object input) {
        if (input instanceof String string) {
            return DataResult.success(string);
        }
        return DataResult.error(() -> "Not a string: " + input);
    }

    @Override
    public Object createString(String value) {
        return value;
    }

    @Override
    public DataResult<Boolean> getBooleanValue(Object input) {
        if (input instanceof Boolean bool) {
            return DataResult.success(bool);
        } else if (input instanceof Number number) {
            return DataResult.success(number.byteValue() != 0);
        }
        return DataResult.error(() -> "Not a boolean: " + input);
    }

    @Override
    public Object createBoolean(boolean value) {
        return value;
    }

    @Override
    public DataResult<Object> mergeToList(Object list, Object value) {
        if (list instanceof List<?> list1) {
            List<Object> out = new ArrayList<>(list1);
            out.add(value);
            return DataResult.success(out);
        }
        return DataResult.error(() -> "Not a list: " + list);
    }

    @Override
    public DataResult<Object> mergeToList(Object list, List<Object> values) {
        if (list instanceof List<?> list1) {
            List<Object> out = new ArrayList<>(list1);
            out.addAll(values);
            return DataResult.success(out);
        }
        return DataResult.error(() -> "Not a list: " + list);
    }

    @Override
    public Object emptyList() {
        return List.of();
    }

    @Override
    public Object emptyMap() {
        return newConfig();
    }

    @Override
    public DataResult<Object> mergeToMap(Object map, Object key, Object value) {
        if (map instanceof Config config) {
            Config newConfig = copyConfig(config);
            if (key instanceof String string) {
                newConfig.set(string, value);
                return DataResult.success(newConfig);
            }
            return DataResult.error(() -> "Not a string: " + key);
        }
        return DataResult.error(() -> "Not a map: " + map);
    }

    @Override
    public DataResult<Object> mergeToMap(Object map, MapLike<Object> values) {
        if (!(map instanceof Config config)) {
            return DataResult.error(() -> "Not a map: " + map);
        }
        Config newConfig = copyConfig(config);
        List<Object> missed = new ArrayList<>();
        values.entries().forEach(entry -> {
            if (entry.getFirst() instanceof String string) {
                newConfig.set(string, entry.getSecond());
            } else {
                missed.add(entry);
            }
        });
        if (missed.isEmpty()) {
            return DataResult.success(newConfig);
        }
        return DataResult.error(() -> "Some keys were not strings: " + missed);
    }

    @Override
    public DataResult<Object> mergeToMap(Object map, Map<Object, Object> values) {
        if (!(map instanceof Config config)) {
            return DataResult.error(() -> "Not a map: " + map);
        }
        Config newConfig = copyConfig(config);
        List<Object> missed = new ArrayList<>();
        values.forEach((key, value) -> {
            if (key instanceof String string) {
                newConfig.set(string, value);
            } else {
                missed.add(key);
            }
        });
        if (missed.isEmpty()) {
            return DataResult.success(newConfig);
        }
        return DataResult.error(() -> "Some keys were not strings: " + missed);
    }

    @Override
    public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object input) {
        if (input instanceof Config config) {
            return DataResult.success(config.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())));
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public Object createMap(Stream<Pair<Object, Object>> map) {
        Config config = newConfig();
        map.forEach(pair -> {
            if (pair.getFirst() instanceof String string) {
                config.set(string, pair.getSecond());
            } else {
                throw new UnsupportedOperationException("Key "+pair.getFirst()+" is not a string");
            }
        });
        return config;
    }

    @Override
    public DataResult<Stream<Object>> getStream(Object input) {
        if (input instanceof List<?> list) {
            return DataResult.success(list.stream().map(Function.identity()));
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    public Object createList(Stream<Object> input) {
        return input.collect(Collectors.toList());
    }

    @Override
    public Object remove(Object input, String key) {
        if (input instanceof Config config) {
            Config newConfig = copyConfig(config);
            newConfig.remove(key);
            return newConfig;
        }
        return input;
    }
}
