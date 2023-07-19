package dev.lukebemish.codecextras.compat.jankson;

import blue.endless.jankson.*;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import dev.lukebemish.codecextras.comments.CommentOps;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class JanksonOps implements CommentOps<JsonElement> {
    public static final JanksonOps INSTANCE = new JanksonOps() {

        @Override
        protected boolean isUncommented() {
            return true;
        }

        @Override
        public DynamicOps<JsonElement> withoutComments() {
            return INSTANCE;
        }
    };
    public static final JanksonOps COMMENTED = new JanksonOps() {

        @Override
        protected boolean isUncommented() {
            return false;
        }

        @Override
        public DynamicOps<JsonElement> withoutComments() {
            return INSTANCE;
        }
    };

    protected abstract boolean isUncommented();

    @Override
    public JsonElement empty() {
        return JsonNull.INSTANCE;
    }

    @Override
    public JsonElement emptyList() {
        return new JsonArray();
    }

    @Override
    public JsonElement emptyMap() {
        return new JsonObject();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, JsonElement input) {
        if (input instanceof JsonNull) {
            return outOps.empty();
        } else if (input instanceof JsonObject) {
            return convertMap(outOps, input);
        } else if (input instanceof JsonArray) {
            return convertList(outOps, input);
        } else if (input instanceof JsonPrimitive primitive) {
            var value = primitive.getValue();
            if (value instanceof String string) {
                return outOps.createString(string);
            } else if (value instanceof Boolean bool) {
                return outOps.createBoolean(bool);
            } else if (value instanceof Number number) {
                return outOps.createNumeric(number);
            }
        }
        throw new UnsupportedOperationException("Object "+input+" could not be converted");
    }

    @Override
    public DataResult<Number> getNumberValue(JsonElement input) {
        if (input instanceof JsonPrimitive primitive) {
            var value = primitive.getValue();
            if (value instanceof Number number) {
                return DataResult.success(number);
            } else if (value instanceof Boolean bool) {
                return DataResult.success(bool ? 1 : 0);
            }
        }
        return DataResult.error(() -> "Not a number: "+input);
    }

    @Override
    public JsonElement createNumeric(Number i) {
        return new JsonPrimitive(i);
    }

    @Override
    public DataResult<String> getStringValue(JsonElement input) {
        if (input instanceof JsonPrimitive primitive) {
            if (primitive.getValue() instanceof String string) {
                return DataResult.success(string);
            }
        }
        return DataResult.error(() -> "Not a string: "+input);
    }

    @Override
    public JsonElement createString(String value) {
        return JsonPrimitive.of(value);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(JsonElement input) {
        if (input instanceof JsonPrimitive primitive) {
            var value = primitive.getValue();
            if (value instanceof Boolean bool) {
                return DataResult.success(bool);
            } else if (value instanceof Number number) {
                return DataResult.success(number.byteValue() != 0);
            }
        }
        return DataResult.error(() -> "Not a boolean: " + input);
    }

    @Override
    public JsonElement createBoolean(boolean value) {
        return JsonPrimitive.of(value);
    }

    @Override
    public DataResult<JsonElement> mergeToList(JsonElement list, JsonElement value) {
        if (!(list instanceof JsonArray) && list != empty()) {
            return DataResult.error(() -> "Not a list: "+list);
        }

        final JsonArray result = new JsonArray();
        if (list instanceof JsonArray jsonArray) {
            result.addAll(jsonArray);
        }
        result.add(value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<JsonElement> mergeToList(JsonElement list, List<JsonElement> values) {
        if (!(list instanceof JsonArray) && list != empty()) {
            return DataResult.error(() -> "Not a list: "+list);
        }

        final JsonArray result = new JsonArray();
        if (list instanceof JsonArray jsonArray) {
            result.addAll(jsonArray);
        }
        result.addAll(values);
        return DataResult.success(result);
    }

    @Override
    public DataResult<JsonElement> mergeToMap(JsonElement map, JsonElement key, JsonElement value) {
        if (!(map instanceof JsonObject) && map != empty()) {
            return DataResult.error(() -> "Not a map: "+map);
        }
        if (!(key instanceof JsonPrimitive jsonPrimitive) || !(jsonPrimitive.getValue() instanceof String string)) {
            return DataResult.error(() -> "Expected string key: "+key);
        }

        final JsonObject result = new JsonObject();
        if (map instanceof JsonObject jsonObject) {
            result.putAll(jsonObject);
        }
        result.put(string, value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<JsonElement> mergeToMap(JsonElement map, Map<JsonElement, JsonElement> values) {
        if (!(map instanceof JsonObject) && map != empty()) {
            return DataResult.error(() -> "Not a map: "+map);
        }

        final JsonObject result = new JsonObject();
        if (map instanceof JsonObject jsonObject) {
            result.putAll(jsonObject);
        }

        final List<JsonElement> missed = Lists.newArrayList();

        values.forEach((key, value) -> {
            if (!(key instanceof JsonPrimitive jsonPrimitive) || !(jsonPrimitive.getValue() instanceof String string)) {
                missed.add(key);
                return;
            }
            result.put(string, value);
        });

        if (!missed.isEmpty()) {
            return DataResult.error(() -> "Found non-string keys: "+missed);
        }

        return DataResult.success(result);
    }

    @Override
    public DataResult<JsonElement> mergeToMap(JsonElement map, MapLike<JsonElement> values) {
        if (!(map instanceof JsonObject) && map != empty()) {
            return DataResult.error(() -> "Not a map: "+map);
        }

        final JsonObject result = new JsonObject();
        if (map instanceof JsonObject jsonObject) {
            result.putAll(jsonObject);
        }

        final List<JsonElement> missed = Lists.newArrayList();

        values.entries().forEach(pair -> {
            var key = pair.getFirst();
            var value = pair.getSecond();
            if (!(key instanceof JsonPrimitive jsonPrimitive) || !(jsonPrimitive.getValue() instanceof String string)) {
                missed.add(key);
                return;
            }
            result.put(string, value);
        });

        if (!missed.isEmpty()) {
            return DataResult.error(() -> "Found non-string keys: "+missed);
        }

        return DataResult.success(result);
    }

    @Override
    public DataResult<Stream<Pair<JsonElement, JsonElement>>> getMapValues(JsonElement input) {
        if (input instanceof JsonObject jsonObject) {
            return DataResult.success(jsonObject.entrySet().stream().map(entry -> Pair.of(JsonPrimitive.of(entry.getKey()), entry.getValue())));
        }
        return DataResult.error(() -> "Not a map: "+input);
    }

    @Override
    public DataResult<Consumer<BiConsumer<JsonElement, JsonElement>>> getMapEntries(JsonElement input) {
        if (input instanceof JsonObject jsonObject) {
            return DataResult.success(c -> {
                for (var entry : jsonObject.entrySet()) {
                    c.accept(JsonPrimitive.of(entry.getKey()), entry.getValue());
                }
            });
        }
        return DataResult.error(() -> "Not a map: "+input);
    }

    @Override
    public JsonElement createMap(Stream<Pair<JsonElement, JsonElement>> map) {
        JsonObject result = new JsonObject();

        map.forEach(pair -> {
            var key = pair.getFirst();
            var value = pair.getSecond();
            if (!(key instanceof JsonPrimitive jsonPrimitive) || !(jsonPrimitive.getValue() instanceof String string)) {
                throw new UnsupportedOperationException("Attempted to treat a non-string primitive as a string");
            }
            result.put(string, value);
        });

        return result;
    }

    @Override
    public DataResult<Stream<JsonElement>> getStream(JsonElement input) {
        if (input instanceof JsonArray jsonArray) {
            return DataResult.success(jsonArray.stream());
        }
        return DataResult.error(() -> "Not a list: "+input);
    }

    @Override
    public DataResult<Consumer<Consumer<JsonElement>>> getList(JsonElement input) {
        if (input instanceof JsonArray jsonArray) {
            return DataResult.success(c -> {
                for (var element : jsonArray) {
                    c.accept(element);
                }
            });
        }
        return DataResult.error(() -> "Not a list: "+input);
    }

    @Override
    public JsonElement createList(Stream<JsonElement> input) {
        var result = new JsonArray();
        input.forEach(result::add);
        return result;
    }

    @Override
    public JsonElement remove(JsonElement input, String key) {
        if (input instanceof JsonObject jsonObject) {
            final JsonObject result = new JsonObject();
            jsonObject.entrySet().stream().filter(entry -> !Objects.equals(entry.getKey(), key)).forEach(entry -> result.put(entry.getKey(), entry.getValue()));
            return result;
        }
        return input;
    }

    @Override
    public String toString() {
        return "Jankson";
    }

    @Override
    public DataResult<JsonElement> commentToMap(JsonElement map, JsonElement key, JsonElement comment) {
        if (isUncommented()) {
            return DataResult.success(map);
        }

        if (!(map instanceof JsonObject jsonObject)) {
            return DataResult.error(() -> "Not a map: "+map);
        }
        if (!(key instanceof JsonPrimitive primitive) || !(primitive.getValue() instanceof String string)) {
            return DataResult.error(() -> "Not a string: "+key);
        }
        if (!(comment instanceof JsonPrimitive commentPrimitive) || !(commentPrimitive.getValue() instanceof String commentString)) {
            return DataResult.error(() -> "Not a string: "+comment);
        }
        var result = new JsonObject();
        result.putAll(jsonObject);
        result.setComment(string, commentString);
        return DataResult.success(result);
    }

    @Override
    public DataResult<JsonElement> commentToMap(JsonElement map, Map<JsonElement, JsonElement> comments) {
        if (isUncommented()) {
            return DataResult.success(map);
        }

        if (!(map instanceof JsonObject jsonObject)) {
            return DataResult.error(() -> "Not a map: "+map);
        }

        var result = new JsonObject();
        result.putAll(jsonObject);
        List<JsonElement> missed = Lists.newArrayList();
        for (var entry : comments.entrySet()) {
            if (!(entry.getKey() instanceof JsonPrimitive primitive) || !(primitive.getValue() instanceof String string)) {
                missed.add(entry.getKey());
                continue;
            }
            if (!(entry.getValue() instanceof JsonPrimitive commentPrimitive) || !(commentPrimitive.getValue() instanceof String commentString)) {
                missed.add(entry.getValue());
                continue;
            }
            result.setComment(string, commentString);
        }
        if (!missed.isEmpty()) {
            return DataResult.error(() -> "Found non-string keys or values: "+missed);
        }
        return DataResult.success(result);
    }

    @Override
    public DataResult<JsonElement> commentToMap(JsonElement map, MapLike<JsonElement> comments) {
        if (isUncommented()) {
            return DataResult.success(map);
        }

        if (!(map instanceof JsonObject jsonObject)) {
            return DataResult.error(() -> "Not a map: "+map);
        }

        var result = new JsonObject();
        result.putAll(jsonObject);
        List<JsonElement> missed = Lists.newArrayList();
        comments.entries().forEach(entry -> {
            if (!(entry.getFirst() instanceof JsonPrimitive primitive) || !(primitive.getValue() instanceof String string)) {
                missed.add(entry.getFirst());
                return;
            }
            if (!(entry.getSecond() instanceof JsonPrimitive commentPrimitive) || !(commentPrimitive.getValue() instanceof String commentString)) {
                missed.add(entry.getSecond());
                return;
            }
            result.setComment(string, commentString);
        });
        if (!missed.isEmpty()) {
            return DataResult.error(() -> "Found non-string keys or values: "+missed);
        }
        return DataResult.success(result);
    }
}
