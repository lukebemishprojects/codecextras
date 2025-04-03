package dev.lukebemish.codecextras.structured.reflective.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows reference to a value in code, from within another annotation. Values represented may be either direct, with
 * the value directly embedded in the annotation, or indirect, with the value being retrieved from a static field or
 * static getter.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    /**
     * {@return the location where the getter or field for an indirect value may be found}
     */
    Class<?> location() default Value.class;

    /**
     * {@return the name of the field to retrieve an indirect value from}
     */
    String field() default "";

    /**
     * {@return the name of the method to retrieve an indirect value from}
     */
    String method() default "";

    /**
     * {@return a direct string value} Must contain at most 1 value.
     */
    String[] stringValue() default {};

    /**
     * {@return a direct int value} Must contain at most 1 value.
     */
    int[] intValue() default {};

    /**
     * {@return a direct long value} Must contain at most 1 value.
     */
    long[] longValue() default {};

    /**
     * {@return a direct double value} Must contain at most 1 value.
     */
    double[] doubleValue() default {};

    /**
     * {@return a direct float value} Must contain at most 1 value.
     */
    float[] floatValue() default {};

    /**
     * {@return a direct boolean value} Must contain at most 1 value.
     */
    boolean[] booleanValue() default {};

    /**
     * {@return a direct byte value} Must contain at most 1 value.
     */
    byte[] byteValue() default {};

    /**
     * {@return a direct short value} Must contain at most 1 value.
     */
    short[] shortValue() default {};

    /**
     * {@return a direct char value} Must contain at most 1 value.
     */
    char[] charValue() default {};
}
