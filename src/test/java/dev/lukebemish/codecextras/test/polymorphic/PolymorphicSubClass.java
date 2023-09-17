package dev.lukebemish.codecextras.test.polymorphic;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.polymorphic.BuilderCodecs;
import dev.lukebemish.codecextras.polymorphic.BuilderException;
import dev.lukebemish.codecextras.polymorphic.DataBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public class PolymorphicSubClass extends PolymorphicSuperClass {
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
        public static <O extends PolymorphicSubClass.Builder<O>> MapCodec<Either<O, UnaryOperator<O>>> codecSubClass() {
            MapCodec<Either<O, UnaryOperator<O>>> self = RecordCodecBuilder.mapCodec(i -> i.group(
                BuilderCodecs.operationWrap(Codec.STRING.fieldOf("address"), Builder::address, builder -> ((Builder<O>) builder).address),
                BuilderCodecs.operationWrap(Codec.INT.fieldOf("height"), Builder::height, builder -> ((Builder<O>) builder).height)
            ).apply(i, BuilderCodecs.<O>operationResolver()::apply));
            return BuilderCodecs.flatMapPair(
                self,
                PolymorphicSuperClass.Builder.codecSuperClass(),
                o -> o.map(l -> DataResult.success(Either.left(l)), r -> DataResult.error(() -> "Cannot reconcile partial operator with builder")),
                (oEither, pEither) -> oEither.map(l ->
                    DataResult.success(Either.left(l)), op ->
                    pEither.map(p1 -> DataResult.error(() -> "Cannot reconcile partial operator with builder"), pOp -> DataResult.success(Either.<O, UnaryOperator<O>>right(o1 -> op.apply(pOp.apply(o1)))))
                ));
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
