package dev.lukebemish.codecextras.structured.reflective.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Make a property use a specific structure, instead of a reflectively-generated one.
 */
@Target({ElementType.METHOD, ElementType.RECORD_COMPONENT, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Structured {
    /**
     * {@return a {@link Value} pointing to an {@link dev.lukebemish.codecextras.structured.Structure}}
     */
    Value value();

    /**
     * {@return Whether to allow direct use of structures representing optional-typed properties} Normally the structure provided
     * is used as the structure of the field, and an encircling {@link java.util.Optional} type, or its various
     * friends such as {@link java.util.OptionalInt}, is interpreted as making the field optional; if this is set to
     * true, the structure provided will instead be used directly for the optional type.
     */
    boolean directOptional() default false;
}
