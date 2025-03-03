package dev.lukebemish.codecextras.structured.reflective;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SerializedProperty {
    String property();
}
