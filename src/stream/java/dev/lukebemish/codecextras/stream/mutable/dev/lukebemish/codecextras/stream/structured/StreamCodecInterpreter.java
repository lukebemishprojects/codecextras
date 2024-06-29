package dev.lukebemish.codecextras.stream.mutable.dev.lukebemish.codecextras.stream.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.structured.Interpreter;
import dev.lukebemish.codecextras.structured.KeyStoringInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.RecordStructure;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public class StreamCodecInterpreter<B extends ByteBuf> extends KeyStoringInterpreter<StreamCodecInterpreter.Holder.Mu<B>> {
	public StreamCodecInterpreter(Keys<Holder.Mu<B>> keys) {
		super(keys.join(Keys.<Holder.Mu<B>>builder()
			.add(Interpreter.UNIT, new Holder<>(StreamCodec.of((buf, data) -> {}, buf -> Unit.INSTANCE)))
			.add(Interpreter.BOOL, new Holder<>(ByteBufCodecs.BOOL))
			.add(Interpreter.BYTE, new Holder<>(ByteBufCodecs.BYTE))
			.add(Interpreter.SHORT, new Holder<>(ByteBufCodecs.SHORT))
			.add(Interpreter.INT, new Holder<>(ByteBufCodecs.VAR_INT))
			.add(Interpreter.LONG, new Holder<>(ByteBufCodecs.VAR_LONG))
			.add(Interpreter.FLOAT, new Holder<>(ByteBufCodecs.FLOAT))
			.add(Interpreter.DOUBLE, new Holder<>(ByteBufCodecs.DOUBLE))
			.add(Interpreter.STRING, new Holder<>(ByteBufCodecs.STRING_UTF8))
			.build()
		));
	}

	public StreamCodecInterpreter() {
		this(Keys.<Holder.Mu<B>>builder().build());
	}

	@Override
	public <A> DataResult<App<Holder.Mu<B>, List<A>>> list(App<Holder.Mu<B>, A> single) {
		return DataResult.success(new Holder<>(list(unbox(single))));
	}

	private static <B extends ByteBuf, T> StreamCodec<B, List<T>> list(StreamCodec<B, T> elementCodec) {
		return ByteBufCodecs.<B, T>list().apply(elementCodec);
	}

	@Override
	public <A> DataResult<App<Holder.Mu<B>, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
		var streamFields = new ArrayList<Field<A, B, ?>>();
		for (var field : fields) {
			DataResult<App<Holder.Mu<B>, A>> result = recordSingleField(field, streamFields);
			if (result != null) return result;
		}
		return DataResult.success(new Holder<>(StreamCodec.of(
			(buf, data) -> {
				for (var field : streamFields) {
					encodeSingleField(buf, field, data);
				}
			},
			buf -> {
				var builder = RecordStructure.Container.builder();
				for (var field : streamFields) {
					decodeSingleField(buf, field, builder);
				}
				return creator.apply(builder.build());
			}
		)));
	}

	private static <B extends ByteBuf, A, F> void encodeSingleField(B buf, Field<A, B, F> field, A data) {
		field.codec.encode(buf, field.getter.apply(data));
	}

	private static <B extends ByteBuf, A, F> void decodeSingleField(B buf, Field<A, B, F> field, RecordStructure.Container.Builder builder) {
		var value = field.codec.decode(buf);
		builder.add(field.key(), value);
	}

	private <A, F> @Nullable DataResult<App<Holder.Mu<B>, A>> recordSingleField(RecordStructure.Field<A, F> field, ArrayList<Field<A, B, ?>> streamFields) {
		var result = field.structure().interpret(this);
		if (result.error().isPresent()) {
			return DataResult.error(result.error().orElseThrow().messageSupplier());
		}
		streamFields.add(new Field<>(unbox(result.result().orElseThrow()), field.key(), field.getter()));
		return null;
	}

	public static <B, T> StreamCodec<? super B, T> unbox(App<Holder.Mu<B>, T> box) {
		return Holder.unbox(box).streamCodec();
	}

	public record Holder<B, T>(StreamCodec<? super B, T> streamCodec) implements App<StreamCodecInterpreter.Holder.Mu<B>, T> {
		public static final class Mu<B> implements K1 {}

		static <B, T> StreamCodecInterpreter.Holder<B, T> unbox(App<StreamCodecInterpreter.Holder.Mu<B>, T> box) {
			return (StreamCodecInterpreter.Holder<B, T>) box;
		}
	}

	private record Field<A, B, T>(StreamCodec<? super B, T> codec, RecordStructure.Key<T> key, Function<A, T> getter) {}
}
