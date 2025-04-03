package dev.lukebemish.codecextras.test.groovy.structured.reflective

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlParser
import com.electronwill.nightconfig.toml.TomlWriter
import com.mojang.serialization.Codec
import dev.lukebemish.codecextras.compat.nightconfig.TomlConfigOps
import dev.lukebemish.codecextras.structured.CodecInterpreter
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator
import dev.lukebemish.codecextras.test.CodecAssertions
import groovy.transform.EqualsAndHashCode
import org.junit.jupiter.api.Test

import java.util.function.Function

class TestCaptureGroovydoc {
    @EqualsAndHashCode
    static class TestClass {
        /**@
         * This is a test field
         */
        long a
    }

    static final Codec<TestClass> CODEC = CodecInterpreter.create().interpret(ReflectiveStructureCreator.create(TestClass)).getOrThrow()

    private final TestClass test = new TestClass().tap {
        a = 10
    }

    private final String toml = """#This is a test field
a = 10

"""

    private final Config tomlParsed = new TomlParser().parse(toml)

    private static final Function<Object, String> TOML_TO_STRING = { toml ->
        if (toml instanceof Config) {
            return new TomlWriter().writeToString(toml)
        } else {
            return toml.toString()
        }
    }

    @Test
    void testEncoding() {
        CodecAssertions.assertEncodesString(TomlConfigOps.COMMENTED, test, toml, TOML_TO_STRING, CODEC)
    }

    @Test
    void testDecoding() {
        CodecAssertions.assertDecodes(TomlConfigOps.COMMENTED, tomlParsed, test, CODEC)
    }
}
