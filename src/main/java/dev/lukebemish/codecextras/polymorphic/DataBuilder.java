package dev.lukebemish.codecextras.polymorphic;

import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.ApiStatus;

// TODO: document this class...
@FunctionalInterface
public interface DataBuilder<O> {
    O build() throws BuilderException;

    @ApiStatus.NonExtendable
    default DataResult<O> buildResult() {
        try {
            return DataResult.success(this.build());
        } catch (BuilderException e) {
            return DataResult.error(e::getMessage);
        }
    }

    @ApiStatus.NonExtendable
    default O buildUnsafe() {
        try {
            return this.build();
        } catch (BuilderException e) {
            throw new RuntimeException(e);
        }
    }

    static void requireNonNullMember(Object o, String name) throws BuilderException {
        requireNonNull(o, "Member '" + name + "' cannot be null");
    }

    static void requireNonNull(Object o, String message) throws BuilderException {
        if (o == null) {
            throw new BuilderException(message);
        }
    }

}
