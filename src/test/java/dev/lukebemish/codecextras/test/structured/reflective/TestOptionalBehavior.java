package dev.lukebemish.codecextras.test.structured.reflective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.IdentityInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import dev.lukebemish.codecextras.structured.reflective.annotations.Default;
import dev.lukebemish.codecextras.structured.reflective.annotations.Structured;
import dev.lukebemish.codecextras.structured.reflective.annotations.Value;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestOptionalBehavior {
    public record TestRecord(
        @Default(value = @Value(intValue = 1)) int a,
        @Default(value = @Value(intValue = 1)) OptionalInt b,
        @Default(value = @Value(longValue = 1)) OptionalLong c,
        @Default(value = @Value(doubleValue = 1)) OptionalDouble d,
        @Default(value = @Value(stringValue = "string")) String e,
        @Default(value = @Value(stringValue = "string")) Optional<String> f,
        @Default(value = @Value(location = TestOptionalBehavior.class, field = "OPTIONAL_VALUE")) @Structured(value = @Value(location = TestOptionalBehavior.class, field = "OPTIONAL_STRUCTURE"), directOptional = true) Optional<String> g
    ) {}

    public static Structure<Optional<String>> OPTIONAL_STRUCTURE = Structure.STRING.flatComapMap(Optional::of, o -> o.map(DataResult::success).orElseGet(() -> DataResult.error(() -> "No value present")));
    public static Optional<String> OPTIONAL_VALUE = Optional.of("string");

    private static final Structure<TestRecord> STRUCTURE = ReflectiveStructureCreator.create(TestRecord.class);
    private static final Codec<TestRecord> CODEC = CodecInterpreter.create().interpret(STRUCTURE).getOrThrow();

    private final TestRecord defaultValue = new TestRecord(
        1, OptionalInt.of(1), OptionalLong.of(1),
        OptionalDouble.of(1), "string", Optional.of("string"),
        Optional.of("string")
    );

    private final String json = "{}";

    @Test
    void testDecode() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, json, defaultValue, CODEC);
    }

    @Test
    void testIdentity() {
        var instance = IdentityInterpreter.INSTANCE.interpret(STRUCTURE).getOrThrow();
        Assertions.assertEquals(defaultValue, instance);
    }
}
