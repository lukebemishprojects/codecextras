# CodecExtras

[![Version](https://img.shields.io/badge/dynamic/xml?style=for-the-badge&color=blue&label=Latest%20Version&prefix=v&query=metadata%2F%2Flatest&url=https%3A%2F%2Fmaven.lukebemish.dev%2Freleases%2Fdev%2Flukebemish%2Fcodecextras%2Fmaven-metadata.xml)](https://maven.lukebemish.dev/releases/dev/lukebemish/codecextras/)

Various extensions to the codecs from Mojang's [DFU](https://github.com/Mojang/DataFixerUpper), including:
- Codecs with comments
- Record codecs with large numbers of entries
- Codecs which decode to a different type than they encode from
- Codecs representing changes to mutable data

Artifacts are available on my maven:
```gradle
repositories {
    maven {
        url = 'https://maven.lukebemish.dev/releases'
    }
}
```

The main artifact depends only on DFU and `org.slf4j:slf4j-api`, and may be jar-in-jar-ed:
```gradle
dependencies {
    implementation('dev.lukebemish:codecextras:<version>')
}
```

For utilities supporting `StreamCodec`s, you will want to depend on the relevant artifact by capability:
```gradle
dependencies {
    implementation('dev.lukebemish:codecextras:<version>') {
        capabilities {
            requireCapability('dev.lukebemish:codecextras-stream')
        }
    }
}
```

Or if using fabric:
```gradle
dependencies {
    modImplementation('dev.lukebemish:codecextras:<version>') {
        capabilities {
            requireCapability('dev.lukebemish:codecextras-stream-intermediary')
        }
    }
}
```

This element depends on the main artifact, and may also be jar-in-jar-ed -- though you may need to use a newer version of loom or NeoGradle for support for capabilities in jar-in-jar.
