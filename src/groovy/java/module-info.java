import dev.lukebemish.codecextras.groovy.structured.reflective.implementation.GroovyReflectiveStructureCreator;
import dev.lukebemish.codecextras.structured.reflective.ReflectiveStructureCreator;

module dev.lukebemish.codeceextras.groovy {
    requires static org.jetbrains.annotations;
    requires static org.jspecify;
    requires static org.slf4j;
    requires static com.google.auto.service;

    requires dev.lukebemish.codecextras;
    requires org.apache.groovy;

    requires com.google.common;
    requires com.google.gson;
    requires datafixerupper;
    requires it.unimi.dsi.fastutil;

    provides ReflectiveStructureCreator with GroovyReflectiveStructureCreator;
}
