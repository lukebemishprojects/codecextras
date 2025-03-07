package dev.lukebemish.codecextras.test.structured.reflective;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.compat.nightconfig.TomlConfigOps;
import dev.lukebemish.codecextras.structured.Annotation;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import dev.lukebemish.codecextras.structured.reflective.annotations.Annotated;
import dev.lukebemish.codecextras.structured.reflective.annotations.Comment;
import dev.lukebemish.codecextras.structured.reflective.annotations.Structured;
import dev.lukebemish.codecextras.structured.reflective.annotations.Value;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class TestReflectiveStructureAnnotations {
    public static final Structure<Integer> INT_AS_LIST = Structure.INT.listOf().comapFlatMap(l -> {
        if (l.size() == 1) {
            return DataResult.success(l.getFirst());
        } else {
            return DataResult.error(() -> "Expected exactly one element in list");
        }
    }, List::of);

    public record TestRecordAnnotated(
        @Annotated(
            key = @Value(location = Annotation.class, field = "COMMENT"),
            stringValue = "Commented field with @Annotated"
        ) int a,
        @Comment("Commented field with @Comment") int b,
        @Structured(@Value(location = TestReflectiveStructureAnnotations.class, field = "INT_AS_LIST")) int c
    ) {}

    public static class TestFieldAnnotated {
        @Annotated(
            key = @Value(location = Annotation.class, field = "COMMENT"),
            stringValue = "Commented field with @Annotated"
        )
        public int a;

        @Comment("Commented field with @Comment")
        public int b;

        @Structured(@Value(location = TestReflectiveStructureAnnotations.class, field = "INT_AS_LIST"))
        public int c;

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof TestFieldAnnotated that)) return false;
            return a == that.a && b == that.b && c == that.c;
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b, c);
        }
    }

    public static class TestMethodAnnotated {
        private int a;
        private int b;
        private int c;

        public void setA(int a) {
            this.a = a;
        }
        public void setB(int b) {
            this.b = b;
        }
        public void setC(int c) {
            this.c = c;
        }

        @Annotated(
            key = @Value(location = Annotation.class, field = "COMMENT"),
            stringValue = "Commented field with @Annotated"
        )
        public int getA() {
            return this.a;
        }

        @Comment("Commented field with @Comment")
        public int getB() {
            return this.b;
        }

        @Structured(@Value(location = TestReflectiveStructureAnnotations.class, field = "INT_AS_LIST"))
        public int getC() {
            return this.c;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof TestMethodAnnotated that)) return false;
            return a == that.a && b == that.b && c == that.c;
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b, c);
        }
    }

    private static final Codec<TestRecordAnnotated> RECORD_ANNOTATED_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestRecordAnnotated.class)).getOrThrow();
    private static final Codec<TestFieldAnnotated> FIELD_ANNOTATED_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestFieldAnnotated.class)).getOrThrow();
    private static final Codec<TestMethodAnnotated> METHOD_ANNOTATED_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestMethodAnnotated.class)).getOrThrow();

    private final TestRecordAnnotated testRecordAnnotated = new TestRecordAnnotated(1, 2, 3);
    private final TestFieldAnnotated testFieldAnnotated = new TestFieldAnnotated();
    private final TestMethodAnnotated testMethodAnnotated = new TestMethodAnnotated();
    {
        testFieldAnnotated.a = 1;
        testFieldAnnotated.b = 2;
        testFieldAnnotated.c = 3;
        testMethodAnnotated.setA(1);
        testMethodAnnotated.setB(2);
        testMethodAnnotated.setC(3);
    }

    private final String toml = """
            #Commented field with @Annotated
            a = 1
            #Commented field with @Comment
            b = 2
            c = [3]

            """;

    private final Config tomlParsed = new TomlParser().parse(toml);

    private static final Function<Object, String> TOML_TO_STRING = toml -> {
        if (toml instanceof Config config) {
            return new TomlWriter().writeToString(config);
        } else {
            return toml.toString();
        }
    };

    @Test
    void testEncodingRecordAnnotated() {
        CodecAssertions.assertEncodesString(TomlConfigOps.COMMENTED, testRecordAnnotated, toml, TOML_TO_STRING, RECORD_ANNOTATED_CODEC);
    }

    @Test
    void testDecodingRecordAnnotated() {
        CodecAssertions.assertDecodes(TomlConfigOps.COMMENTED, tomlParsed, testRecordAnnotated, RECORD_ANNOTATED_CODEC);
    }

    @Test
    void testEncodingFieldAnnotated() {
        CodecAssertions.assertEncodesString(TomlConfigOps.COMMENTED, testFieldAnnotated, toml, TOML_TO_STRING, FIELD_ANNOTATED_CODEC);
    }

    @Test
    void testDecodingFieldAnnotated() {
        CodecAssertions.assertDecodes(TomlConfigOps.COMMENTED, tomlParsed, testFieldAnnotated, FIELD_ANNOTATED_CODEC);
    }

    @Test
    void testEncodingMethodAnnotated() {
        CodecAssertions.assertEncodesString(TomlConfigOps.COMMENTED, testMethodAnnotated, toml, TOML_TO_STRING, METHOD_ANNOTATED_CODEC);
    }

    @Test
    void testDecodingMethodAnnotated() {
        CodecAssertions.assertDecodes(TomlConfigOps.COMMENTED, tomlParsed, testMethodAnnotated, METHOD_ANNOTATED_CODEC);
    }
}
