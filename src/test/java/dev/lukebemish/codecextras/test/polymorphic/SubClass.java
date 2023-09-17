package dev.lukebemish.codecextras.test.polymorphic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.polymorphic.BuilderCodecs;
import dev.lukebemish.codecextras.polymorphic.PolymorphicBuilder;
import org.jetbrains.annotations.NotNull;

public class SubClass extends SuperClass {
    public static final Codec<SubClass> CODEC = BuilderCodecs.codec(SubClass.Builder.CODEC, Builder::from);
    private final String address;
    private final int height;

    protected SubClass(Builder builder) {
        super(builder.superClass);
        this.address = builder.address;
        this.height = builder.height;
    }

    public String address() {
        return address;
    }

    public float height() {
        return height;
    }

    public static class Builder implements PolymorphicBuilder<SubClass> {
        public static final Codec<Builder> CODEC = BuilderCodecs.pair(
            RecordCodecBuilder.create(i -> i.group(
                BuilderCodecs.wrap(Codec.STRING.fieldOf("address"), Builder::address, builder -> builder.address),
                BuilderCodecs.wrap(Codec.INT.fieldOf("height"), Builder::height, builder -> builder.height)
            ).apply(i, BuilderCodecs.resolver(Builder::new)::apply)),
            SuperClass.Builder.CODEC,
            builder -> builder.superClass,
            Builder::superClass
        );

        private String address;
        private int height;

        private SuperClass.Builder superClass;

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder superClass(SuperClass.Builder superClass) {
            this.superClass = superClass;
            return this;
        }

        @NotNull
        @Override
        public SubClass build() throws PolymorphicBuilder.BuilderException {
            PolymorphicBuilder.requireNonNullMember(address, "address");
            PolymorphicBuilder.requireNonNull(superClass, "Must provide settings for superClass");
            return new SubClass(this);
        }

        public static Builder from(SubClass subClass) {
            Builder builder = new Builder();
            builder.address = subClass.address;
            builder.height = subClass.height;
            builder.superClass = SuperClass.Builder.from(subClass);
            return builder;
        }
    }
}
