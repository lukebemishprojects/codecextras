package dev.lukebemish.codecextras.test.polymorphic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.polymorphic.BuilderCodecs;
import dev.lukebemish.codecextras.polymorphic.BuilderException;
import dev.lukebemish.codecextras.polymorphic.DataBuilder;
import org.jetbrains.annotations.NotNull;

public class SuperClass {
    public static final Codec<SuperClass> CODEC = BuilderCodecs.mapCodec(Builder.CODEC, Builder::from).codec();

    private final String name;
    private final int age;

    protected SuperClass(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
    }

    public String name() {
        return name;
    }

    public int age() {
        return age;
    }

    public static class Builder implements DataBuilder<SuperClass> {
        public static final MapCodec<Builder> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuilderCodecs.wrap(Codec.STRING.fieldOf("name"), Builder::name, builder -> builder.name),
            BuilderCodecs.wrap(Codec.INT.fieldOf("age"), Builder::age, builder -> builder.age)
        ).apply(i, BuilderCodecs.resolver(Builder::new)::apply));

        private String name;
        private int age;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        @NotNull
        @Override
        public SuperClass build() throws BuilderException {
            DataBuilder.requireNonNullMember(name, "name");
            return new SuperClass(this);
        }

        public static Builder from(SuperClass superClass) {
            Builder builder = new Builder();
            builder.name = superClass.name;
            builder.age = superClass.age;
            return builder;
        }
    }
}
