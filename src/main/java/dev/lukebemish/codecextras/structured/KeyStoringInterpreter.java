package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;

public abstract class KeyStoringInterpreter<Mu extends K1> implements Interpreter<Mu> {
    private final Keys<Mu> keys;

    protected KeyStoringInterpreter(Keys<Mu> keys) {
        this.keys = keys;
    }

    @Override
    public <A> DataResult<App<Mu, A>> keyed(Key<A> key) {
        return keys.get(key).map(DataResult::success).orElse(DataResult.error(() -> "Unknown key "+key.name()));
    }
}
