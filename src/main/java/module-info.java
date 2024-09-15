module dev.lukebemish.codecextras {
    uses dev.lukebemish.codecextras.companion.AlternateCompanionRetriever;

    requires static autoextension;
    requires static com.electronwill.nightconfig.core;
    requires static com.electronwill.nightconfig.toml;
    requires com.google.common;
    requires com.google.gson;
    requires datafixerupper;
    requires it.unimi.dsi.fastutil;
    requires static jankson;
    requires static org.jetbrains.annotations;
    requires static org.jspecify;
    requires static org.objectweb.asm;
    requires static org.slf4j;

    exports dev.lukebemish.codecextras;
    exports dev.lukebemish.codecextras.comments;
    exports dev.lukebemish.codecextras.companion;

    exports dev.lukebemish.codecextras.compat.jankson;
    exports dev.lukebemish.codecextras.compat.nightconfig;

    exports dev.lukebemish.codecextras.config;
    exports dev.lukebemish.codecextras.extension;
    exports dev.lukebemish.codecextras.mutable;
    exports dev.lukebemish.codecextras.polymorphic;
    exports dev.lukebemish.codecextras.record;
    exports dev.lukebemish.codecextras.repair;

    exports dev.lukebemish.codecextras.structured;
    exports dev.lukebemish.codecextras.structured.schema;

    exports dev.lukebemish.codecextras.types;
}
