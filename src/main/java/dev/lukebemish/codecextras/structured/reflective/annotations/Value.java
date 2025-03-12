package dev.lukebemish.codecextras.structured.reflective.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    Class<?> location() default Value.class;

    String field() default "";

    String method() default "";

    String[] stringValue() default {};
    int[] intValue() default {};
    long[] longValue() default {};
    double[] doubleValue() default {};
    float[] floatValue() default {};
    boolean[] booleanValue() default {};
    byte[] byteValue() default {};
    short[] shortValue() default {};
    char[] charValue() default {};
}
