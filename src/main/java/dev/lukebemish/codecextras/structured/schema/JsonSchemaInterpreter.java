package dev.lukebemish.codecextras.structured.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.StringRepresentation;
import dev.lukebemish.codecextras.structured.Annotation;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Interpreter;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.KeyStoringInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Keys2;
import dev.lukebemish.codecextras.structured.ParametricKeyedValue;
import dev.lukebemish.codecextras.structured.RecordStructure;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.types.Identity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Creates a JSON schema for a given {@link Structure}. Note that interpreting a structure with this interpreter will
 * require resolving any lazy aspects of the structure, such as bounds, so should not be done until that is safe
 * @see #interpret(Structure)
 */
public class JsonSchemaInterpreter extends KeyStoringInterpreter<JsonSchemaInterpreter.Holder.Mu, JsonSchemaInterpreter> {
    private final CodecInterpreter codecInterpreter;
    private final DynamicOps<JsonElement> ops;

    public JsonSchemaInterpreter(
        Keys<Holder.Mu, Object> keys,
        Keys2<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1> parametricKeys,
        CodecInterpreter codecInterpreter,
        DynamicOps<JsonElement> ops
    ) {
        super(keys.join(Keys.<Holder.Mu, Object>builder()
            .add(Interpreter.UNIT, new Holder<>(OBJECT.get()))
            .add(Interpreter.BOOL, new Holder<>(BOOLEAN.get()))
            .add(Interpreter.BYTE, new Holder<>(INTEGER.get()))
            .add(Interpreter.SHORT, new Holder<>(INTEGER.get()))
            .add(Interpreter.INT, new Holder<>(INTEGER.get()))
            .add(Interpreter.LONG, new Holder<>(INTEGER.get()))
            .add(Interpreter.FLOAT, new Holder<>(NUMBER.get()))
            .add(Interpreter.DOUBLE, new Holder<>(NUMBER.get()))
            .add(Interpreter.STRING, new Holder<>(STRING.get()))
            .add(Interpreter.EMPTY_MAP, new Holder<>(() -> {
                var json = OBJECT.get();
                json.add("additionalProperties", new JsonPrimitive(false));
                return json;
            }))
            .add(Interpreter.EMPTY_LIST, new Holder<>(() -> {
                var json = ARRAY.get();
                json.add("prefixItems", new JsonArray());
                json.add("items", new JsonPrimitive(false));
                return json;
            }))
            .build()
        ), parametricKeys.join(Keys2.<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1>builder()
            .add(Interpreter.STRING_REPRESENTABLE, new ParametricKeyedValue<>() {
                @Override
                public <T> App<Holder.Mu, App<Identity.Mu, T>> convert(App<StringRepresentation.Mu, T> parameter) {
                    var representation = StringRepresentation.unbox(parameter);
                    JsonArray oneOf = new JsonArray();
                    for (var value : representation.values().get()) {
                        JsonObject object = new JsonObject();
                        object.addProperty("const", representation.representation().apply(value));
                        oneOf.add(object);
                    }
                    JsonObject schema = new JsonObject();
                    schema.add("oneOf", oneOf);
                    return new Holder<>(schema);
                }
            })
            .build()
        ));
        this.codecInterpreter = codecInterpreter;
        this.ops = ops;
    }

    @Override
    public JsonSchemaInterpreter with(Keys<Holder.Mu, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1> parametricKeys) {
        return new JsonSchemaInterpreter(
            keys().join(keys),
            parametricKeys().join(parametricKeys),
            this.codecInterpreter, this.ops
        );
    }

    public JsonSchemaInterpreter() {
        this(
            Keys.<Holder.Mu, Object>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1>builder().build(),
            CodecInterpreter.create(),
            JsonOps.INSTANCE
        );
    }

    @Override
    public <A> DataResult<App<Holder.Mu, List<A>>> list(App<Holder.Mu, A> single) {
        var object = ARRAY.get();
        object.add("items", schemaValue(single));
        return DataResult.success(new Holder<>(object, definitions(single)));
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
        var object = OBJECT.get();
        var properties = new JsonObject();
        var required = new JsonArray();
        Map<String, Structure<?>> definitions = new HashMap<>();
        for (RecordStructure.Field<A, ?> field : fields) {
            Supplier<String> error = singleField(field, properties, required, definitions);
            if (error != null) {
                return DataResult.error(error);
            }
        }
        object.add("properties", properties);
        object.add("required", required);
        return DataResult.success(new Holder<>(object, definitions));
    }

    private <A, F> @Nullable Supplier<String> singleField(RecordStructure.Field<A, F> field, JsonObject properties, JsonArray required, Map<String, Structure<?>> definitions) {
        var partialResolt = field.structure().interpret(this);
        if (partialResolt.isError()) {
            return partialResolt.error().orElseThrow().messageSupplier();
        }
        var fieldObject = copy(schemaValue(partialResolt.result().orElseThrow()));
        definitions.putAll(definitions(partialResolt.result().orElseThrow()));

        var error = new Object() {
            @Nullable Supplier<String> value = null;
        };

        field.missingBehavior().ifPresentOrElse(missingBehavior -> {
            var codec = codecInterpreter.interpret(field.structure());
            if (codec.error().isPresent()) {
                error.value = codec.error().get().messageSupplier();
                return;
            }
            var defaultValue = missingBehavior.missing().get();
            var defaultValueResult = codec.result().orElseThrow().encodeStart(ops, defaultValue);
            if (defaultValueResult.error().isPresent()) {
                // If it cannot serialize the default value, we just don't report it -- it could be something like an Optional where the default value does not exist.
                return;
            }
            fieldObject.add("default", defaultValueResult.result().orElseThrow());
        }, () -> required.add(field.name()));

        if (error.value != null) {
            return error.value;
        }

        properties.add(field.name(), fieldObject);
        return null;
    }

    @Override
    public <A, B> DataResult<App<Holder.Mu, B>> flatXmap(App<Holder.Mu, A> input, Function<A, DataResult<B>> to, Function<B, DataResult<A>> from) {
        return DataResult.success(new Holder<>(schemaValue(input), definitions(input)));
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> annotate(Structure<A> input, Keys<Identity.Mu, Object> annotations) {
        JsonObject schema;
        Map<String, Structure<?>> definitions;
        var refName = Annotation.get(annotations, SchemaAnnotations.REUSE_KEY);
        if (refName.isPresent()) {
            schema = new JsonObject();
            var ref = refName.get();
            schema.addProperty("$ref", "#/$defs/"+ref);
            definitions = new HashMap<>();
            definitions.put(ref, input);
        } else {
            var result = input.interpret(this);
            if (result.error().isPresent()) {
                return DataResult.error(result.error().get().messageSupplier());
            }
            schema = schemaValue(result.result().orElseThrow());
            definitions = new HashMap<>(definitions(result.result().orElseThrow()));
        }

        Annotation.get(annotations, Annotation.PATTERN).ifPresent(pattern -> {
            schema.addProperty("pattern", pattern);
        });
        Annotation.get(annotations, Annotation.DESCRIPTION).or(() -> Annotation.get(annotations, Annotation.COMMENT)).ifPresent(comment -> {
            schema.addProperty("description", comment);
        });
        Annotation.get(annotations, Annotation.TITLE).ifPresent(comment -> {
            schema.addProperty("title", comment);
        });
        return DataResult.success(new Holder<>(schema, definitions));
    }

    @Override
    public <E, A> DataResult<App<Holder.Mu, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Supplier<Set<A>> keys, Function<A, DataResult<Structure<? extends E>>> structures) {
        return keyStructure.interpret(this).flatMap(keySchemaApp -> {
            var definitions = new HashMap<>(definitions(keySchemaApp));
            var keySchema = schemaValue(keySchemaApp);
            JsonObject out = new JsonObject();
            JsonObject properties = new JsonObject();
            JsonArray required = new JsonArray();

            required.add(key);
            properties.add(key, keySchema);

            JsonArray allOf = new JsonArray();
            var keyCodecResult = codecInterpreter.interpret(keyStructure);
            if (keyCodecResult.error().isPresent()) {
                return DataResult.error(keyCodecResult.error().get().messageSupplier());
            }
            var keyCodec = keyCodecResult.result().orElseThrow();
            for (A entryKey : keys.get()) {
                var result = structures.apply(entryKey).flatMap(it -> it.interpret(this));
                if (result.error().isPresent()) {
                    return DataResult.error(result.error().get().messageSupplier());
                }
                var entrySchema = schemaValue(result.result().orElseThrow());
                definitions.putAll(definitions(result.result().orElseThrow()));
                var entryValueResult = keyCodec.encodeStart(JsonOps.INSTANCE, entryKey);
                if (entryValueResult.error().isPresent()) {
                    return DataResult.error(entryValueResult.error().get().messageSupplier());
                }
                var entryValue = entryValueResult.result().orElseThrow();
                var ifObj = new JsonObject();
                var ifProperties = new JsonObject();
                var keyProperty = new JsonObject();
                keyProperty.add("const", entryValue);
                ifProperties.add(key, keyProperty);
                ifObj.add("properties", ifProperties);
                var obj = new JsonObject();
                obj.add("if", ifObj);
                obj.add("then", entrySchema);
                allOf.add(obj);
            }

            out.add("properties", properties);
            out.add("required", required);
            out.add("allOf", allOf);
            return DataResult.success(new Holder<>(out, definitions));
        });
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> bounded(Structure<A> input, Supplier<Set<A>> values) {
        return codecInterpreter.interpret(input).flatMap(codec -> {
            var types = new JsonArray();
            for (var value : values.get()) {
                var result = codec.encodeStart(ops, value);
                if (result.error().isPresent()) {
                    return DataResult.error(result.error().get().messageSupplier());
                }
                types.add(result.result().orElseThrow());
            }
            return input.interpret(this).flatMap(outer -> {
                var schema = copy(schemaValue(outer));
                schema.add("enum", types);
                return DataResult.success(new Holder<>(schema, definitions(outer)));
            });
        });
    }

    @Override
    public <K, V> DataResult<App<Holder.Mu, Map<K, V>>> unboundedMap(App<Holder.Mu, K> key, App<Holder.Mu, V> value) {
        var schema = OBJECT.get();
        var definitions = new HashMap<String, Structure<?>>();
        var keyJson = schemaValue(key);
        if (keyJson.has("pattern") && keyJson.get("pattern").isJsonPrimitive()) {
            // if the key has a pattern, we use "patternProperties"
            var patternProperties = new JsonObject();
            patternProperties.add(keyJson.get("pattern").getAsString(), schemaValue(value));
            schema.add("patternProperties", patternProperties);
        } else {
            schema.add("additionalProperties", schemaValue(value));
        }
        definitions.putAll(definitions(value));
        definitions.putAll(definitions(key));
        return DataResult.success(new Holder<>(schema, definitions));
    }

    @Override
    public <L, R> DataResult<App<Holder.Mu, Either<L, R>>> either(App<Holder.Mu, L> left, App<Holder.Mu, R> right) {
        var schema = new JsonObject();
        var anyOf = new JsonArray();
        var definitions = new HashMap<String, Structure<?>>();
        var leftSchema = schemaValue(left);
        var rightSchema = schemaValue(right);
        definitions.putAll(definitions(left));
        definitions.putAll(definitions(right));
        anyOf.add(leftSchema);
        anyOf.add(rightSchema);
        schema.add("anyOf", anyOf);
        return DataResult.success(new Holder<>(schema, definitions));
    }

    @Override
    public <L, R> DataResult<App<Holder.Mu, Either<L, R>>> xor(App<Holder.Mu, L> left, App<Holder.Mu, R> right) {
        var schema = new JsonObject();
        var oneOf = new JsonArray();
        var definitions = new HashMap<String, Structure<?>>();
        var leftSchema = schemaValue(left);
        var rightSchema = schemaValue(right);
        definitions.putAll(definitions(left));
        definitions.putAll(definitions(right));
        oneOf.add(leftSchema);
        oneOf.add(rightSchema);
        schema.add("oneOf", oneOf);
        return DataResult.success(new Holder<>(schema, definitions));
    }

    @Override
    public <K, V> DataResult<App<Holder.Mu, Map<K, V>>> dispatchedMap(Structure<K> keyStructure, Supplier<Set<K>> keys, Function<K, DataResult<Structure<? extends V>>> valueStructures) {
        var definitions = new HashMap<String, Structure<?>>();
        return codecInterpreter.interpret(keyStructure).flatMap(keyCodec -> {
            var schema = OBJECT.get();
            for (var key : keys.get()) {
                var keyValue = keyCodec.encodeStart(ops, key).flatMap(ops::getStringValue);
                if (keyValue.error().isPresent()) {
                    return DataResult.error(keyValue.error().get().messageSupplier());
                }
                var valueSchema = valueStructures.apply(key).flatMap(it -> it.interpret(this));
                if (valueSchema.error().isPresent()) {
                    return DataResult.error(valueSchema.error().get().messageSupplier());
                }
                schema.add(keyValue.result().orElseThrow(), schemaValue(valueSchema.result().orElseThrow()));
                definitions.putAll(definitions(valueSchema.result().orElseThrow()));
            }
            return DataResult.success(schema);
        }).map(schema -> new Holder<>(schema, definitions));
    }

    private static JsonObject schemaValue(App<Holder.Mu, ?> box) {
        return Holder.unbox(box).jsonObject;
    }

    private static Map<String, Structure<?>> definitions(App<Holder.Mu, ?> box) {
        return Holder.unbox(box).definition;
    }

    public <T> DataResult<JsonObject> interpret(Structure<T> structure) {
        return structure.interpret(this).flatMap(holder -> {
            var object = copy(schemaValue(holder));
            var definitions = definitions(holder);
            var defsObject = new JsonObject();
            while (!definitions.isEmpty()) {
                var newDefs = new HashMap<String, Structure<?>>();
                for (Map.Entry<String, Structure<?>> entry : definitions.entrySet()) {
                    if (defsObject.has(entry.getKey())) {
                        continue;
                    }
                    var result = entry.getValue().interpret(this);
                    if (result.error().isPresent()) {
                        return DataResult.error(result.error().get().messageSupplier());
                    }
                    var schema = schemaValue(result.result().orElseThrow());
                    defsObject.add(entry.getKey(), schema);
                    newDefs.putAll(definitions(result.result().orElseThrow()));
                }
                definitions = newDefs;
            }
            if (!defsObject.isEmpty()) {
                object.add("$defs", defsObject);
            }
            return DataResult.success(object);
        });
    }

    public static final Key<Holder.Mu> KEY = Key.create("JsonSchemaInterpreter");

    @Override
    public Stream<KeyConsumer<?, Holder.Mu>> keyConsumers() {
        return Stream.of(
            new KeyConsumer<Holder.Mu, Holder.Mu>() {
                @Override
                public Key<Holder.Mu> key() {
                    return KEY;
                }

                @Override
                public <T> App<Holder.Mu, T> convert(App<Holder.Mu, T> input) {
                    return input;
                }
            }
        );
    }

    public record Holder<T>(JsonObject jsonObject, Map<String, Structure<?>> definition) implements App<Holder.Mu, T> {
        public Holder(JsonObject object) {
            this(object, Map.of());
        }

        public Holder(Supplier<JsonObject> objectCreator) {
            this(objectCreator.get());
        }

        public static final class Mu implements K1 { private Mu() {} }

        static <T> Holder<T> unbox(App<Holder.Mu, T> box) {
            return (Holder<T>) box;
        }
    }

    private JsonObject copy(JsonObject object) {
        JsonObject copy = new JsonObject();
        for (String key : object.keySet()) {
            copy.add(key, object.get(key));
        }
        return copy;
    }

    public static final Supplier<JsonObject> OBJECT = () -> {
        JsonObject object = new JsonObject();
        object.addProperty("type", "object");
        return object;
    };
    public static final Supplier<JsonObject> NUMBER = () -> {
        JsonObject object = new JsonObject();
        object.addProperty("type", "number");
        return object;
    };
    public static final Supplier<JsonObject> STRING = () -> {
        JsonObject object = new JsonObject();
        object.addProperty("type", "string");
        return object;
    };
    public static final Supplier<JsonObject> BOOLEAN = () -> {
        JsonObject object = new JsonObject();
        object.addProperty("type", "boolean");
        return object;
    };
    public static final Supplier<JsonObject> INTEGER = () -> {
        JsonObject object = new JsonObject();
        object.addProperty("type", "integer");
        return object;
    };
    public static final Supplier<JsonObject> ARRAY = () -> {
        JsonObject object = new JsonObject();
        object.addProperty("type", "array");
        return object;
    };
}
