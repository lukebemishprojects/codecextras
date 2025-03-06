package dev.lukebemish.codecextras.structured.reflective.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.RECORD_COMPONENT, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Annotated {
    Value key();
    Value[] value() default {};
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
