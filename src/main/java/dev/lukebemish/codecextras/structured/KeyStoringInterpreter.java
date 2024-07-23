package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.DataResult;

public abstract class KeyStoringInterpreter<Mu extends K1> implements Interpreter<Mu> {
    private final Keys<Mu, Object> keys;
    private final Keys2<ParametricKeyedValue.Mu<Mu>, K1, K1> parametricKeys;

    protected KeyStoringInterpreter(Keys<Mu, Object> keys, Keys2<ParametricKeyedValue.Mu<Mu>, K1, K1> parametricKeys) {
        this.keys = keys;
        this.parametricKeys = parametricKeys;
    }

    @Override
    public <A> DataResult<App<Mu, A>> keyed(Key<A> key) {
        return keys.get(key).map(DataResult::success).orElse(DataResult.error(() -> "Unknown key "+key.name()));
    }

    @Override
    public <MuO extends K1, MuP extends K1, T> DataResult<App<Mu, App<MuO, T>>> parametricallyKeyed(Key2<MuP, MuO> key, App<MuP, T> parameter) {
        return parametricKeys.get(key)
            .map(ParametricKeyedValue::unbox)
            .map(val -> val.convert(parameter))
            .map(DataResult::success)
            .orElse(DataResult.error(() -> "Unknown key "+key.name()));
    }
}
