package dev.lukebemish.codecextras.structured.reflective.implementation;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

final class TypeResolver {
    private TypeResolver() {}

    static Type resolve(Type type, TypeVariable<? extends Class<?>>[] variables, Type[] values) {
        switch (type) {
            case Class<?> ignored -> {
                return type;
            }
            case ParameterizedType parameterizedType -> {
                var args = parameterizedType.getActualTypeArguments();
                for (var i = 0; i < args.length; i++) {
                    args[i] = resolve(args[i], variables, values);
                }
                return new ParameterizedTypeImpl(
                    args,
                    parameterizedType.getRawType(),
                    parameterizedType.getOwnerType() == null ? null : resolve(parameterizedType.getOwnerType(), variables, values)
                );
            }
            case GenericArrayType genericArrayType -> {
                return new GenericArrayTypeImpl(resolve(genericArrayType.getGenericComponentType(), variables, values));
            }
            case WildcardType wildcardType -> {
                var lowerBounds = wildcardType.getLowerBounds();
                var upperBounds = wildcardType.getUpperBounds();
                for (var i = 0; i < lowerBounds.length; i++) {
                    lowerBounds[i] = resolve(lowerBounds[i], variables, values);
                }
                for (var i = 0; i < upperBounds.length; i++) {
                    upperBounds[i] = resolve(upperBounds[i], variables, values);
                }
                return new WildcardTypeImpl(lowerBounds, upperBounds);
            }
            case TypeVariable<?> typeVariable -> {
                for (var i = 0; i < variables.length; i++) {
                    if (variables[i].equals(typeVariable)) {
                        return resolve(values[i], variables, values);
                    }
                }
            }
            default -> {}
        }
        return type;
    }
}
