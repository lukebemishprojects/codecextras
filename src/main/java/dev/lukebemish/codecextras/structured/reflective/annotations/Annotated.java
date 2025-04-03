package dev.lukebemish.codecextras.structured.reflective.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a property to notate that an annotation should be added to the structure for that field in a final record structure.
 */
@Target({ElementType.METHOD, ElementType.RECORD_COMPONENT, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Annotated {
    /**
     * {@return a {@link Value} pointing to an {@link dev.lukebemish.codecextras.structured.Key} for the annotation}
     */
    Value key();

    /**
     * {@return a {@link Value} pointing to the value assigned to the annotation}
     */
    Value value();
}
