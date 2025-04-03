package dev.lukebemish.codecextras.structured.reflective.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a constructor parameter as being a serialized property, and notate what its name is. As parameter names are not
 * as consistently kept as field or method names in some cases, this annotation is necessary to use constructor-injected
 * properties in reflective structure creation.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SerializedProperty {
    String value();
}
