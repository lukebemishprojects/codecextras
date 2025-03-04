package dev.lukebemish.codecextras.structured.reflective;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// TODO: targets
@Retention(RetentionPolicy.RUNTIME)
public @interface SerializedProperty {
    String property();
}
