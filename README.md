# CodecExtras

[![Version](https://img.shields.io/maven-central/last-update/dev.lukebemish/codecextras?style=for-the-badge&color=blue&label=Latest%20Version&prefix=v)](https://maven.lukebemish.dev/releases/dev/lukebemish/codecextras/)

Various extensions to the codecs from Mojang's [DFU](https://github.com/Mojang/DataFixerUpper), including:
- Codecs with comments
- Record codecs with large numbers of entries
- Codecs which decode to a different type than they encode from
- Codecs representing changes to mutable data

Artifacts are available on maven central (as of v3; for instructions for older versions, check the README on the appropriate branch):
```gradle
repositories {
    mavenCentral()
}
```

The main artifact depends only on DFU and `org.slf4j:slf4j-api`, and may be jar-in-jar-ed:
```gradle
dependencies {
    implementation('dev.lukebemish:codecextras:<version>')
}
```

For utilities supporting `StreamCodec`s and other MC-specific features, you will want to depend on the relevant artifact by capability. If using neoforge:
```gradle
dependencies {
    implementation('dev.lukebemish:codecextras:<version>') {
        capabilities {
            requireFeature('minecraft-neoforge')
        }
    }
}
```

Or if using fabric:
```gradle
dependencies {
    modImplementation('dev.lukebemish:codecextras:<version>') {
        capabilities {
            requireFeature('minecraft-fabric')
        }
    }
}
```

And if you need a platform-agnostic API for multiloader:
```gradle
dependencies {
    implementation('dev.lukebemish:codecextras:<version>') {
        capabilities {
            requireFeature('minecraft-common')
        }
    }
}
```

This element depends on the main artifact, and may also be jar-in-jar-ed -- though you may need to use a newer version of loom or NeoGradle for support for capabilities in jar-in-jar.
