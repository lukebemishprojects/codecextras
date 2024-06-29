package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import java.util.List;

public interface Structure<A> {
	<Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter);
	default Annotations annotations() {
		return Annotations.empty();
	}

	default <T> Structure<A> annotate(Key<T> key, T value) {
		var outer = this;
		var annotations = annotations().with(key, value);
		return annotatedDelegatingStructure(outer, annotations);
	}

	default Structure<A> annotate(Annotations annotations) {
		var outer = this;
		var combined = annotations().join(annotations);
		return annotatedDelegatingStructure(outer, combined);
	}

	private static <A> Structure<A> annotatedDelegatingStructure(Structure<A> outer, Annotations annotations) {
		return new Structure<A>() {
			@Override
			public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
				return outer.interpret(interpreter);
			}

			@Override
			public Annotations annotations() {
				return annotations;
			}
		};
	}

	default Structure<List<A>> listOf() {
		var outer = this;
		return new Structure<>() {
			@Override
			public <Mu extends K1> DataResult<App<Mu, List<A>>> interpret(Interpreter<Mu> interpreter) {
				return outer.interpret(interpreter).flatMap(interpreter::list);
			}
		};
	}

	static <A> Structure<A> keyed(Key<A> key) {
		return new Structure<>() {
			@Override
			public <Mu extends K1> DataResult<App<Mu, A>> interpret(Interpreter<Mu> interpreter) {
				return interpreter.keyed(key);
			}
		};
	}

	static <A> Structure<A> record(RecordStructure.Builder<A> builder) {
		return RecordStructure.create(builder);
	}

	Structure<Unit> UNIT = keyed(Interpreter.UNIT);
	Structure<Boolean> BOOL = keyed(Interpreter.BOOL);
	Structure<Byte> BYTE = keyed(Interpreter.BYTE);
	Structure<Short> SHORT = keyed(Interpreter.SHORT);
	Structure<Integer> INT = keyed(Interpreter.INT);
	Structure<Long> LONG = keyed(Interpreter.LONG);
	Structure<Float> FLOAT = keyed(Interpreter.FLOAT);
	Structure<Double> DOUBLE = keyed(Interpreter.DOUBLE);
	Structure<String> STRING = keyed(Interpreter.STRING);
}
