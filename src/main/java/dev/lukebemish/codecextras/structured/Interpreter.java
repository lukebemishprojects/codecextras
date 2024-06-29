package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;

import java.util.List;

public interface Interpreter<Mu extends K1> {
	<A> DataResult<App<Mu, List<A>>> list(App<Mu, A> single);

	<A> DataResult<App<Mu, A>> keyed(Key<A> key);

	Key<Unit> UNIT = Key.create("UNIT");
	Key<Boolean> BOOL = Key.create("BOOL");
	Key<Byte> BYTE = Key.create("BYTE");
	Key<Short> SHORT = Key.create("SHORT");
	Key<Integer> INT = Key.create("INT");
	Key<Long> LONG = Key.create("LONG");
	Key<Float> FLOAT = Key.create("FLOAT");
	Key<Double> DOUBLE = Key.create("DOUBLE");
	Key<String> STRING = Key.create("STRING");
}
