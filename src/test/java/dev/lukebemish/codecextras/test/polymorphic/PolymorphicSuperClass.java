package dev.lukebemish.codecextras.test.polymorphic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.CodecExtras;
import dev.lukebemish.codecextras.polymorphic.BuilderCodecs;
import dev.lukebemish.codecextras.polymorphic.BuilderException;
import dev.lukebemish.codecextras.polymorphic.DataBuilder;
import dev.lukebemish.codecextras.polymorphic.PolymorphicBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class PolymorphicSuperClass {
    // IntelliJ doesn't know what's going on...
    @SuppressWarnings("RedundantTypeArguments")
    public static final Codec<PolymorphicSuperClass> CODEC = BuilderCodecs.<PolymorphicSuperClass, Builder.Impl>operationMapCodec(
        Builder.codecSuperClass(),
        Builder::from,
        Builder.Impl::new,
        b -> b::build).codec();

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

    public static abstract class Builder<O extends Builder<O>> implements PolymorphicBuilder<O> {
        public static <O extends Builder<O>> MapCodec<Asymmetry<O, UnaryOperator<O>>> codecSuperClass() {
            return Asymmetry.flatMapDecoding(CodecExtras.flatten(RecordCodecBuilder.mapCodec(i -> i.group(
                BuilderCodecs.operationWrap(Codec.STRING.fieldOf("name"), Builder::name, builder -> ((Builder<O>) builder).name),
                BuilderCodecs.operationWrap(Codec.INT.fieldOf("age"), Builder::age, builder -> ((Builder<O>) builder).age)
            ).apply(i, Asymmetry.wrapJoiner(BuilderCodecs.Resolver::<O>operationApply2)))), Function.identity());
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

        @NotNull
        public PolymorphicSuperClass build() throws BuilderException {
            validate();
            return new PolymorphicSuperClass(this);
        }

        public O from(PolymorphicSuperClass superClass) {
            this.name = superClass.name;
            this.age = superClass.age;
            return self();
        }

        public static class Impl extends Builder<Impl> {

            @Override
            public Impl self() {
                return this;
            }
        }
    }
}
