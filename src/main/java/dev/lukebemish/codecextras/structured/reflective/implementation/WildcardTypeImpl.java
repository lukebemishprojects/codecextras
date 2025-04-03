package dev.lukebemish.codecextras.structured.reflective.implementation;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;

class WildcardTypeImpl implements WildcardType {
    private final Type[] lowerBounds;
    private final Type[] upperBounds;

    WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
    }

    @Override
    public Type[] getUpperBounds() {
        return Arrays.copyOf(upperBounds, upperBounds.length);
    }

    @Override
    public Type[] getLowerBounds() {
        return Arrays.copyOf(lowerBounds, lowerBounds.length);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof WildcardType that)) return false;
        return Objects.deepEquals(lowerBounds, that.getLowerBounds()) && Objects.deepEquals(upperBounds, that.getUpperBounds());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(upperBounds) ^ Arrays.hashCode(lowerBounds);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("?");
        if (lowerBounds.length > 0) {
            builder.append(" super ");
            builder.append(lowerBounds[0].getTypeName());
            for (int i = 1; i < lowerBounds.length; i++) {
                builder.append(" & ");
                builder.append(lowerBounds[i].getTypeName());
            }
        } else if (upperBounds.length > 0 && !upperBounds[0].equals(Object.class)) {
            builder.append(" extends ");
            builder.append(upperBounds[0].getTypeName());
            for (int i = 1; i < upperBounds.length; i++) {
                builder.append(" & ");
                builder.append(upperBounds[i].getTypeName());
            }
        }
        return builder.toString();
    }
}
