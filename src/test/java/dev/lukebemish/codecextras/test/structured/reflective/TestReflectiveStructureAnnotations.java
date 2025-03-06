package dev.lukebemish.codecextras.test.structured.reflective;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.serialization.Codec;
import dev.lukebemish.codecextras.compat.nightconfig.TomlConfigOps;
import dev.lukebemish.codecextras.structured.Annotation;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import dev.lukebemish.codecextras.structured.reflective.annotations.Annotated;
import dev.lukebemish.codecextras.structured.reflective.annotations.Value;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class TestReflectiveStructureAnnotations {
    public record TestRecordAnnotated(
        @Annotated(
            key = @Value(location = Annotation.class, field = "COMMENT"),
            stringValue = "Commented field"
        ) int a
    ) {}

    public static class TestFieldAnnotated {
        @Annotated(
            key = @Value(location = Annotation.class, field = "COMMENT"),
            stringValue = "Commented field"
        )
        public int a;
    }

    public static class TestMethodAnnotated {
        private int a;

        public void setA(int a) {
            this.a = a;
        }

        @Annotated(
            key = @Value(location = Annotation.class, field = "COMMENT"),
            stringValue = "Commented field"
        )
        public int getA() {
            return this.a;
        }
    }

    private static final Codec<TestRecordAnnotated> RECORD_ANNOTATED_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestRecordAnnotated.class)).getOrThrow();
    private static final Codec<TestFieldAnnotated> FIELD_ANNOTATED_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestFieldAnnotated.class)).getOrThrow();
    private static final Codec<TestMethodAnnotated> METHOD_ANNOTATED_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestMethodAnnotated.class)).getOrThrow();

    private final TestRecordAnnotated testRecordAnnotated = new TestRecordAnnotated(1);
    private final TestFieldAnnotated testFieldAnnotated = new TestFieldAnnotated();
    private final TestMethodAnnotated testMethodAnnotated = new TestMethodAnnotated();
    {
        testFieldAnnotated.a = 1;
        testMethodAnnotated.setA(1);
    }

    private final String toml = """
            #Commented field
            a = 1

            """;

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
    void testEncodingFieldAnnotated() {
        CodecAssertions.assertEncodesString(TomlConfigOps.COMMENTED, testFieldAnnotated, toml, TOML_TO_STRING, FIELD_ANNOTATED_CODEC);
    }

    @Test
    void testEncodingMethodAnnotated() {
        CodecAssertions.assertEncodesString(TomlConfigOps.COMMENTED, testMethodAnnotated, toml, TOML_TO_STRING, METHOD_ANNOTATED_CODEC);
    }
}
