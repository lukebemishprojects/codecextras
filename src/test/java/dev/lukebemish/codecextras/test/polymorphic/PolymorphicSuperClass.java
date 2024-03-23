package dev.lukebemish.codecextras.test.polymorphic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.polymorphic.BuilderCodecs;
import dev.lukebemish.codecextras.polymorphic.BuilderException;
import dev.lukebemish.codecextras.polymorphic.DataBuilder;
import dev.lukebemish.codecextras.polymorphic.PolymorphicBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.UnaryOperator;

public class PolymorphicSuperClass {
    // IntelliJ doesn't know what's going on...
    @SuppressWarnings("RedundantTypeArguments")
    public static final Codec<PolymorphicSuperClass> CODEC = BuilderCodecs.<PolymorphicSuperClass, Builder.Impl>operationMapCodec(
        Builder.codecSuperClass(),
        Builder::from,
        Builder.Impl::new,
        b -> b).codec();

    private final String name;
    private final int age;

    protected PolymorphicSuperClass(Builder<?> builder) {
        this.name = builder.name;
        this.age = builder.age;
    }

    public String name() {
        return name;
    }

    public int age() {
        return age;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PolymorphicSuperClass that = (PolymorphicSuperClass) object;
        return age() == that.age() && Objects.equals(name(), that.name());
    }

    public static abstract class Builder<O extends Builder<O>> implements PolymorphicBuilder<O> {
        public static <O extends Builder<O>> MapCodec<Asymmetry<UnaryOperator<O>, O>> codecSuperClass() {
            return RecordCodecBuilder.mapCodec(i -> i.group(
                BuilderCodecs.operationWrap(Codec.STRING.fieldOf("name"), Builder::name, builder -> ((Builder<O>) builder).name),
                BuilderCodecs.operationWrap(Codec.INT.fieldOf("age"), Builder::age, builder -> ((Builder<O>) builder).age)
            ).apply(i, Asymmetry.wrapJoiner(BuilderCodecs.Resolver::<O>operationApply2)));
        }

        private String name;
        private int age;

        public O name(String name) {
            this.name = name;
            return self();
        }

        public O age(int age) {
            this.age = age;
            return self();
        }

        @Override
        public void validate() throws BuilderException {
            DataBuilder.requireNonNullMember(name, "name");
        }

        public O from(PolymorphicSuperClass superClass) {
            this.name = superClass.name;
            this.age = superClass.age;
            return self();
        }

        public static class Impl extends Builder<Impl> implements DataBuilder<PolymorphicSuperClass> {

            @Override
            public Impl self() {
                return this;
            }

            @Override
            @NotNull
            public PolymorphicSuperClass build() throws BuilderException {
                validate();
                return new PolymorphicSuperClass(this);
            }
        }
    }
}
