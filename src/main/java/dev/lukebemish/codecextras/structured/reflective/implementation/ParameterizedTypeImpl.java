package dev.lukebemish.codecextras.structured.reflective.implementation;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

class ParameterizedTypeImpl implements ParameterizedType {
    private final Type[] actualTypeArguments;
    private final Type rawType;
    private final @Nullable Type ownerType;

    ParameterizedTypeImpl(Type[] actualTypeArguments, Type rawType, @Nullable Type ownerType) {
        this.actualTypeArguments = actualTypeArguments;
        this.rawType = rawType;
        this.ownerType = ownerType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return Arrays.copyOf(actualTypeArguments, actualTypeArguments.length);
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public @Nullable Type getOwnerType() {
        return ownerType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ParameterizedType that)) return false;
        return Objects.deepEquals(actualTypeArguments, that.getActualTypeArguments()) && Objects.equals(rawType, that.getRawType()) && Objects.equals(ownerType, that.getOwnerType());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(actualTypeArguments) ^ Objects.hashCode(ownerType) ^ Objects.hashCode(rawType);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (ownerType != null) {
            builder.append(ownerType.getTypeName());
            builder.append("$");
        }
        builder.append(rawType.getTypeName());
        builder.append("<");
        builder.append(actualTypeArguments[0].getTypeName());
        for (int i = 1; i < actualTypeArguments.length; i++) {
            builder.append(", ");
            builder.append(actualTypeArguments[i].getTypeName());
        }
        builder.append(">");
        return builder.toString();
    }
}
