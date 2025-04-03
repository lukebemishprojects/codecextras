package dev.lukebemish.codecextras.structured.reflective.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add a comment to the field representing a property in the final record structure. Equivalent to {@link Annotated}
 * with {@link dev.lukebemish.codecextras.structured.Annotation#COMMENT}.
 */
@Target({ElementType.METHOD, ElementType.RECORD_COMPONENT, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Comment {
    String value();
}
