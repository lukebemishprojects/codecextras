package dev.lukebemish.codecextras.structured.reflective.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    Class<?> location();

    String field() default "";

    String method() default "";
}
