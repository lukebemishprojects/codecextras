package dev.lukebemish.codecextras.test.polymorphic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.CodecExtras;
import dev.lukebemish.codecextras.polymorphic.BuilderCodecs;
import dev.lukebemish.codecextras.polymorphic.BuilderException;
import dev.lukebemish.codecextras.polymorphic.DataBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class PolymorphicSubClass extends PolymorphicSuperClass {
    // IntelliJ doesn't know what's going on...
    @SuppressWarnings("RedundantTypeArguments")
    public static final Codec<PolymorphicSubClass> CODEC = BuilderCodecs.<PolymorphicSubClass, Builder.Impl>operationMapCodec(
        PolymorphicSubClass.Builder.codecSubClass(),
        PolymorphicSubClass.Builder::from,
        PolymorphicSubClass.Builder.Impl::new,
        b -> b::build).codec();

    private final String address;
    private final int height;

    protected PolymorphicSubClass(Builder<?> builder) {
        super(builder);
        this.address = builder.address;
        this.height = builder.height;
    }

    public String address() {
        return address;
    }

    public float height() {
        return height;
    }

    public static abstract class Builder<O extends Builder<O>> extends PolymorphicSuperClass.Builder<O> {
        public static <O extends PolymorphicSubClass.Builder<O>> MapCodec<Asymmetry<O, UnaryOperator<O>>> codecSubClass() {
            MapCodec<Asymmetry<O, UnaryOperator<O>>> self = Asymmetry.flatMapDecoding(CodecExtras.flatten(RecordCodecBuilder.mapCodec(i -> i.group(
                BuilderCodecs.operationWrap(Codec.STRING.fieldOf("address"), Builder::address, builder -> ((Builder<O>) builder).address),
                BuilderCodecs.operationWrap(Codec.INT.fieldOf("height"), Builder::height, builder -> ((Builder<O>) builder).height)
            ).apply(i, Asymmetry.wrapJoiner(BuilderCodecs.Resolver::<O>operationApply2)))), Function.identity());
            return BuilderCodecs.flatMapPair(
                self,
                PolymorphicSuperClass.Builder.codecSuperClass(),
                a -> a.encoding().map(Asymmetry::encoding),
                (oA, pA) -> oA.decoding().flatMap(o -> pA.decoding().map(p -> Asymmetry.decoding(o1 -> o.apply(p.apply(o1)))))
            );
        }

        private String address;
        private int height;

        public O address(String address) {
            this.address = address;
            return self();
        }

        public O height(int height) {
            this.height = height;
            return self();
        }

        @Override
        public void validate() throws BuilderException {
            super.validate();
            DataBuilder.requireNonNullMember(address, "address");
        }

        @NotNull
        public PolymorphicSubClass build() throws BuilderException {
            validate();
            return new PolymorphicSubClass(this);
        }

        public O from(PolymorphicSubClass subClass) {
            super.from(subClass);
            this.address = subClass.address;
            this.height = subClass.height;
            return self();
        }

        public static class Impl extends PolymorphicSubClass.Builder<PolymorphicSubClass.Builder.Impl> {

            @Override
            public PolymorphicSubClass.Builder.Impl self() {
                return this;
            }
        }
    }
}
