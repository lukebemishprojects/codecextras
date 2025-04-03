package dev.lukebemish.codecextras.structured.reflective.implementation;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

class GenericArrayTypeImpl implements GenericArrayType {
    private final Type genericComponentType;

    GenericArrayTypeImpl(Type genericComponentType) {
        this.genericComponentType = genericComponentType;
    }

    @Override
    public Type getGenericComponentType() {
        return genericComponentType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof GenericArrayType that)) return false;
        return Objects.equals(genericComponentType, that.getGenericComponentType());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(genericComponentType);
    }

    @Override
    public String toString() {
        return genericComponentType.getTypeName() + "[]";
    }
}
