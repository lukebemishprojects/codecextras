package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;

public class CodecInterpreter extends KeyStoringInterpreter<CodecInterpreter.CodecHolder.Mu> {
	public CodecInterpreter(Keys<CodecHolder.Mu> keys) {
		super(keys.join(Keys.<CodecHolder.Mu>builder()
				.add(Interpreter.STRING, new CodecHolder<>(Codec.STRING))
				.build()
		));
	}

	@Override
	public <A> DataResult<App<CodecHolder.Mu, List<A>>> list(App<CodecHolder.Mu, A> single) {
		return DataResult.success(new CodecHolder<>(CodecHolder.unbox(single).codec.listOf()));
	}

	public record CodecHolder<T>(Codec<T> codec) implements App<CodecHolder.Mu, T> {
		public static final class Mu implements K1 {}

		static <T> CodecHolder<T> unbox(App<Mu, T> box) {
			return (CodecHolder<T>) box;
		}
	}
}
