package dev.lukebemish.codecextras.test.structured.reflective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import dev.lukebemish.codecextras.structured.reflective.annotations.SerializedProperty;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class TestReflectiveConstructors {
    public static class TestNoArgCtor {
        public int a;
        private @Nullable String b;

        public @Nullable String getB() {
            return this.b;
        }

        public void setB(String b) {
            this.b = b;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof TestNoArgCtor that)) return false;
            return a == that.a && Objects.equals(b, that.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }

        @Override
        public String toString() {
            return "TestNoArgCtor{" +
                "a=" + a +
                ", b='" + b + '\'' +
                '}';
        }
    }

    public static class TestCtor {
        public final int a;
        private final String b;

        public String getB() {
            return this.b;
        }

        public TestCtor(
            @SerializedProperty("a") int a,
            @SerializedProperty("b") String b
        ) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof TestCtor testCtor)) return false;
            return a == testCtor.a && Objects.equals(b, testCtor.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }

        @Override
        public String toString() {
            return "TestCtor{" +
                "a=" + a +
                ", b='" + b + '\'' +
                '}';
        }
    }

    private static final Codec<TestCtor> CTOR_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestCtor.class)).getOrThrow();
    private static final Codec<TestNoArgCtor> NO_ARG_CTOR_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestNoArgCtor.class)).getOrThrow();

    private final String ctorJson = """
            {
                "a": 1,
                "b": "test"
            }""";

    private final TestNoArgCtor noArgCtor = new TestNoArgCtor();
    {
        noArgCtor.a = 1;
        noArgCtor.setB("test");
    }
    private final TestCtor ctor = new TestCtor(1, "test");

    @Test
    void testDecodingCtor() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, ctorJson, ctor, CTOR_CODEC);
    }

    @Test
    void testEncodingCtor() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, ctor, ctorJson, CTOR_CODEC);
    }

    @Test
    void testDecodingNoArgCtor() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, ctorJson, noArgCtor, NO_ARG_CTOR_CODEC);
    }

    @Test
    void testEncodingNoArgCtor() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, noArgCtor, ctorJson, NO_ARG_CTOR_CODEC);
    }
}
