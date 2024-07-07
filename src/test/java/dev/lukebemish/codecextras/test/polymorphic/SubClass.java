package dev.lukebemish.codecextras.test.polymorphic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.Asymmetry;
import dev.lukebemish.codecextras.polymorphic.BuilderCodecs;
import dev.lukebemish.codecextras.polymorphic.BuilderException;
import dev.lukebemish.codecextras.polymorphic.DataBuilder;
import java.util.Objects;
import java.util.function.Function;

public class SubClass extends SuperClass {
    public static final Codec<SubClass> CODEC = BuilderCodecs.mapCodec(Builder.CODEC, Builder::from).codec();
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

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        SubClass subClass = (SubClass) object;
        return height() == subClass.height() && Objects.equals(address(), subClass.address());
    }

    public static class Builder implements DataBuilder<SubClass> {
        public static final MapCodec<Builder> CODEC = BuilderCodecs.mapPair(
            Asymmetry.join(RecordCodecBuilder.mapCodec(i -> i.group(
                    BuilderCodecs.wrap(Codec.STRING.fieldOf("address"), Builder::address, builder -> builder.address),
                    BuilderCodecs.wrap(Codec.INT.fieldOf("height"), Builder::height, builder -> builder.height)
                ).apply(i, Asymmetry.wrapJoiner(BuilderCodecs.resolver(Builder::new)::apply2))),
                Function.identity(), Function.identity()),
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

        @Override
        public SubClass build() throws BuilderException {
            DataBuilder.requireNonNullMember(address, "address");
            DataBuilder.requireNonNull(superClass, "Must provide settings for superClass");
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
