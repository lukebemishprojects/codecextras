package dev.lukebemish.codecextras.structured.reflective.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a property as lenient. Equivalent to {@link Annotated} with
 * {@link dev.lukebemish.codecextras.structured.Annotation#LENIENT}.
 */
@Target({ElementType.METHOD, ElementType.RECORD_COMPONENT, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Lenient {
}
