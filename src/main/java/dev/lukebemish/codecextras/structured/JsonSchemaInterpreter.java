package dev.lukebemish.codecextras.structured;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.types.Identity;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class JsonSchemaInterpreter extends KeyStoringInterpreter<JsonSchemaInterpreter.Holder.Mu> {
    public JsonSchemaInterpreter(Keys<Holder.Mu, Object> keys, Keys2<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1> parametricKeys) {
        super(keys.join(Keys.<Holder.Mu, Object>builder()
            .add(Interpreter.UNIT, new Holder<>(OBJECT))
            .add(Interpreter.BOOL, new Holder<>(BOOLEAN))
            .add(Interpreter.BYTE, new Holder<>(INTEGER))
            .add(Interpreter.SHORT, new Holder<>(INTEGER))
            .add(Interpreter.INT, new Holder<>(INTEGER))
            .add(Interpreter.LONG, new Holder<>(INTEGER))
            .add(Interpreter.FLOAT, new Holder<>(NUMBER))
            .add(Interpreter.DOUBLE, new Holder<>(NUMBER))
            .add(Interpreter.STRING, new Holder<>(STRING))
            .build()
        ), parametricKeys);
    }

    public JsonSchemaInterpreter() {
        this(
            Keys.<Holder.Mu, Object>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<Holder.Mu>, K1, K1>builder().build()
        );
    }

    @Override
    public <A> DataResult<App<Holder.Mu, List<A>>> list(App<Holder.Mu, A> single) {
        var object = copy(ARRAY);
        object.add("items", unbox(single));
        return DataResult.success(new Holder<>(object));
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
        var object = copy(OBJECT);
        var properties = new JsonObject();
        var required = new JsonArray();
        for (RecordStructure.Field<A, ?> field : fields) {
            Supplier<String> error = singleField(field, properties, required);
            if (error != null) {
                return DataResult.error(error);
            }
        }
        object.add("properties", properties);
        object.add("required", required);
        return DataResult.success(new Holder<>(object));
    }

    private <A, F> @Nullable Supplier<String> singleField(RecordStructure.Field<A, F> field, JsonObject properties, JsonArray required) {
        var partialResolt = field.structure().interpret(this);
        if (partialResolt.isError()) {
            return partialResolt.error().orElseThrow().messageSupplier();
        }
        var fieldObject = copy(unbox(partialResolt.result().orElseThrow()));

        field.missingBehavior().ifPresentOrElse(missingBehavior -> {}, () -> required.add(field.name()));

        properties.add(field.name(), fieldObject);
        return null;
    }

    @Override
    public <A, B> DataResult<App<Holder.Mu, B>> flatXmap(App<Holder.Mu, A> input, Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer) {
        return DataResult.success(new Holder<>(unbox(input)));
    }

    @Override
    public <A> DataResult<App<Holder.Mu, A>> annotate(App<Holder.Mu, A> input, Keys<Identity.Mu, Object> annotations) {
        var schema = copy(unbox(input));
        Annotation.get(annotations, Annotation.DESCRIPTION).or(() -> Annotation.get(annotations, Annotation.COMMENT)).ifPresent(comment -> {
            schema.addProperty("description", comment);
        });
        Annotation.get(annotations, Annotation.TITLE).ifPresent(comment -> {
            schema.addProperty("title", comment);
        });
        return DataResult.success(new Holder<>(schema));
    }

    public static JsonObject unbox(App<Holder.Mu, ?> box) {
        return Holder.unbox(box).jsonObject;
    }

    public <T> DataResult<JsonObject> interpret(Structure<T> structure) {
        return structure.interpret(this).map(JsonSchemaInterpreter::unbox);
    }

    public record Holder<T>(JsonObject jsonObject) implements App<Holder.Mu, T> {
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

    private static final JsonObject OBJECT = new JsonObject();
    private static final JsonObject NUMBER = new JsonObject();
    private static final JsonObject STRING = new JsonObject();
    private static final JsonObject BOOLEAN = new JsonObject();
    private static final JsonObject INTEGER = new JsonObject();
    private static final JsonObject ARRAY = new JsonObject();

    static {
        OBJECT.addProperty("type", "object");
        NUMBER.addProperty("type", "number");
        STRING.addProperty("type", "string");
        BOOLEAN.addProperty("type", "boolean");
        INTEGER.addProperty("type", "integer");
        ARRAY.addProperty("type", "array");
    }
}
