package dev.lukebemish.codecextras.test.structured.reflective;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;
import dev.lukebemish.codecextras.structured.reflective.annotations.SerializedProperty;
import dev.lukebemish.codecextras.structured.reflective.annotations.Transient;
import dev.lukebemish.codecextras.test.CodecAssertions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SequencedSet;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

public class TestReflective {
    public record TestRecord(long a, String b, TestEnum c, OptionalInt d, Optional<String> e, @Nullable String f, SequencedSet<Integer> g) {
        private static final Structure<TestRecord> STRUCTURE = ReflectiveStructureCreator.create(TestRecord.class);
    }

    public enum TestEnum {
        A,
        B,
        C
    }

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

    public static class TestAnnotations {
        public long a;
        public transient boolean b;

        private boolean c;
        @Transient
        public boolean getC() {
            return this.c;
        }
        public void setC(boolean c) {
            this.c = c;
        }

        private boolean d;
        public boolean isD() {
            return this.d;
        }
        public void setD(boolean d) {
            this.d = d;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof TestAnnotations that)) return false;
            return a == that.a && b == that.b && c == that.c && d == that.d;
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b, c, d);
        }

        @Override
        public String toString() {
            return "TestAnnotations{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                ", d=" + d +
                '}';
        }
    }

    public record TestRecursive(String name, List<TestRecursive> list) {}

    private static final Codec<TestRecord> CODEC = CodecInterpreter.create().interpret(TestRecord.STRUCTURE).getOrThrow();

    private static final Codec<TestRecord[]> ARRAY_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestRecord[].class)).getOrThrow();
    private static final Codec<int[]> PRIMITIVE_ARRAY_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(int[].class)).getOrThrow();

    private static final Codec<TestRecursive> RECURSIVE_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestRecursive.class)).getOrThrow();

    private static final Codec<TestCtor> CTOR_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestCtor.class)).getOrThrow();
    private static final Codec<TestNoArgCtor> NO_ARG_CTOR_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestNoArgCtor.class)).getOrThrow();

    private static final Codec<TestAnnotations> ANNOTATIONS_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestAnnotations.class)).getOrThrow();

    private final String json = """
            {
                "a": 1,
                "b": "test",
                "c": "A",
                "d": 2,
                "e": "test",
                "g": [1, 2, 3]
            }""";

    private final String arrayJson = """
            [
                {
                    "a": 1,
                    "b": "test",
                    "c": "A",
                    "d": 2,
                    "e": "test",
                    "g": [1, 2, 3]
                }
            ]""";

    private final String primitiveArrayJson = """
            [1, 2, 3]""";

    private final String recursiveJson = """
            {
                "name": "test1",
                "list": [
                    {
                        "name": "test2",
                        "list": []
                    },
                    {
                        "name": "test3",
                        "list": []
                    }
                ]
            }""";

    private final String ctorJson = """
            {
                "a": 1,
                "b": "test"
            }""";

    private final String annotationsJson = """
            {
                "a": 1,
                "d": true
            }""";

    private final TestRecord object = new TestRecord(1, "test", TestEnum.A, OptionalInt.of(2), Optional.of("test"), null, new LinkedHashSet<>(List.of(1, 2, 3)));
    private final TestRecord[] array = new TestRecord[] { object };
    private final int[] primitiveArray = new int[] { 1, 2, 3 };
    private final TestRecursive recursive = new TestRecursive("test1", List.of(new TestRecursive("test2", List.of()), new TestRecursive("test3", List.of())));

    private final TestNoArgCtor noArgCtor = new TestNoArgCtor();
    {
        noArgCtor.a = 1;
        noArgCtor.setB("test");
    }
    private final TestCtor ctor = new TestCtor(1, "test");

    private final TestAnnotations annotations = new TestAnnotations();
    {
        annotations.a = 1;
        annotations.setD(true);
    }

    @Test
    void testDecoding() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, json, object, CODEC);
    }

    @Test
    void testEncoding() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, object, json, CODEC);
    }

    @Test
    void testDecodingArray() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, arrayJson, array, ARRAY_CODEC);
    }

    @Test
    void testEncodingArray() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, array, arrayJson, ARRAY_CODEC);
    }

    @Test
    void testDecodingPrimitiveArray() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, primitiveArrayJson, primitiveArray, PRIMITIVE_ARRAY_CODEC);
    }

    @Test
    void testEncodingPrimitiveArray() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, primitiveArray, primitiveArrayJson, PRIMITIVE_ARRAY_CODEC);
    }

    @Test
    void testDecodingRecursive() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, recursiveJson, recursive, RECURSIVE_CODEC);
    }

    @Test
    void testEncodingRecursive() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, recursive, recursiveJson, RECURSIVE_CODEC);
    }

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

    @Test
    void testDecodingAnnotations() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, annotationsJson, annotations, ANNOTATIONS_CODEC);
    }

    @Test
    void testEncodingAnnotations() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, annotations, annotationsJson, ANNOTATIONS_CODEC);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnnotation {
        String a();
        long b();
        String[] c();
        long[] d();
        Nested e();
        Nested[] f();

        @interface Nested {
            long a();
        }
    }
    private final String testAnnotationJson = """
            {
                "a": "test",
                "b": 1,
                "c": ["test1", "test2"],
                "d": [1, 2],
                "e": {"a": 1},
                "f": [{"a": 1}, {"a": 2}]
            }""";
    private final TestAnnotation testAnnotation = new Object() {
        @TestAnnotation(
            a = "test",
            b = 1,
            c = {"test1", "test2"},
            d = {1, 2},
            e = @TestAnnotation.Nested(a = 1),
            f = {@TestAnnotation.Nested(a = 1), @TestAnnotation.Nested(a = 2)}
        )
        static class Source {

        }

        final TestAnnotation annotation = Objects.requireNonNull(Source.class.getAnnotation(TestAnnotation.class));
    }.annotation;
    private static final Codec<TestAnnotation> TEST_ANNOTATION_CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestAnnotation.class)).getOrThrow();

    @Test
    void testDecodingTestAnnotation() {
        CodecAssertions.assertDecodes(JsonOps.INSTANCE, testAnnotationJson, testAnnotation, TEST_ANNOTATION_CODEC);
    }

    @Test
    void testEncodingTestAnnotation() {
        CodecAssertions.assertEncodes(JsonOps.INSTANCE, testAnnotation, testAnnotationJson, TEST_ANNOTATION_CODEC);
    }
}
