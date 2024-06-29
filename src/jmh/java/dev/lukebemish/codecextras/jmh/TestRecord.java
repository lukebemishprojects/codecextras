package dev.lukebemish.codecextras.jmh;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lukebemish.codecextras.ExtendedRecordCodecBuilder;
import dev.lukebemish.codecextras.record.KeyedRecordCodecBuilder;
import dev.lukebemish.codecextras.record.MethodHandleRecordCodecBuilder;
import java.lang.invoke.MethodHandles;

record TestRecord(
	int a, int b, int c, int d,
	int e, int f, int g, int h,
	int i, int j, int k, int l,
	int m, int n, int o, int p
) {
	public static final Codec<TestRecord> RCB = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("a").forGetter(TestRecord::a),
		Codec.INT.fieldOf("b").forGetter(TestRecord::b),
		Codec.INT.fieldOf("c").forGetter(TestRecord::c),
		Codec.INT.fieldOf("d").forGetter(TestRecord::d),
		Codec.INT.fieldOf("e").forGetter(TestRecord::e),
		Codec.INT.fieldOf("f").forGetter(TestRecord::f),
		Codec.INT.fieldOf("g").forGetter(TestRecord::g),
		Codec.INT.fieldOf("h").forGetter(TestRecord::h),
		Codec.INT.fieldOf("i").forGetter(TestRecord::i),
		Codec.INT.fieldOf("j").forGetter(TestRecord::j),
		Codec.INT.fieldOf("k").forGetter(TestRecord::k),
		Codec.INT.fieldOf("l").forGetter(TestRecord::l),
		Codec.INT.fieldOf("m").forGetter(TestRecord::m),
		Codec.INT.fieldOf("n").forGetter(TestRecord::n),
		Codec.INT.fieldOf("o").forGetter(TestRecord::o),
		Codec.INT.fieldOf("p").forGetter(TestRecord::p)
	).apply(i, TestRecord::new));

	public static final Codec<TestRecord> ERCB = ExtendedRecordCodecBuilder
		.start(Codec.INT.fieldOf("a"), TestRecord::a)
		.field(Codec.INT.fieldOf("b"), TestRecord::b)
		.field(Codec.INT.fieldOf("c"), TestRecord::c)
		.field(Codec.INT.fieldOf("d"), TestRecord::d)
		.field(Codec.INT.fieldOf("e"), TestRecord::e)
		.field(Codec.INT.fieldOf("f"), TestRecord::f)
		.field(Codec.INT.fieldOf("g"), TestRecord::g)
		.field(Codec.INT.fieldOf("h"), TestRecord::h)
		.field(Codec.INT.fieldOf("i"), TestRecord::i)
		.field(Codec.INT.fieldOf("j"), TestRecord::j)
		.field(Codec.INT.fieldOf("k"), TestRecord::k)
		.field(Codec.INT.fieldOf("l"), TestRecord::l)
		.field(Codec.INT.fieldOf("m"), TestRecord::m)
		.field(Codec.INT.fieldOf("n"), TestRecord::n)
		.field(Codec.INT.fieldOf("o"), TestRecord::o)
		.field(Codec.INT.fieldOf("p"), TestRecord::p)
		.build(p -> o -> n -> m -> l -> k -> j -> i -> h -> g -> f -> e -> d -> c -> b -> a -> new TestRecord(
			a, b, c, d,
			e, f, g, h,
			i, j, k, l,
			m, n, o, p
		));

	public static final Codec<TestRecord> MHRCB = MethodHandleRecordCodecBuilder.<TestRecord>start()
		.with(Codec.INT.fieldOf("a"), TestRecord::a)
		.with(Codec.INT.fieldOf("c"), TestRecord::c)
		.with(Codec.INT.fieldOf("d"), TestRecord::d)
		.with(Codec.INT.fieldOf("b"), TestRecord::b)
		.with(Codec.INT.fieldOf("e"), TestRecord::e)
		.with(Codec.INT.fieldOf("f"), TestRecord::f)
		.with(Codec.INT.fieldOf("g"), TestRecord::g)
		.with(Codec.INT.fieldOf("h"), TestRecord::h)
		.with(Codec.INT.fieldOf("i"), TestRecord::i)
		.with(Codec.INT.fieldOf("j"), TestRecord::j)
		.with(Codec.INT.fieldOf("k"), TestRecord::k)
		.with(Codec.INT.fieldOf("l"), TestRecord::l)
		.with(Codec.INT.fieldOf("m"), TestRecord::m)
		.with(Codec.INT.fieldOf("n"), TestRecord::n)
		.with(Codec.INT.fieldOf("o"), TestRecord::o)
		.with(Codec.INT.fieldOf("p"), TestRecord::p)
		.buildWithConstructor(MethodHandles.lookup(), TestRecord.class);

	public static final Codec<TestRecord> KRCB = KeyedRecordCodecBuilder.codec(builder -> {
		var a = builder.add(Codec.INT.fieldOf("a"), TestRecord::a);
		var b = builder.add(Codec.INT.fieldOf("b"), TestRecord::b);
		var c = builder.add(Codec.INT.fieldOf("c"), TestRecord::c);
		var d = builder.add(Codec.INT.fieldOf("d"), TestRecord::d);
		var e = builder.add(Codec.INT.fieldOf("e"), TestRecord::e);
		var f = builder.add(Codec.INT.fieldOf("f"), TestRecord::f);
		var g = builder.add(Codec.INT.fieldOf("g"), TestRecord::g);
		var h = builder.add(Codec.INT.fieldOf("h"), TestRecord::h);
		var i = builder.add(Codec.INT.fieldOf("i"), TestRecord::i);
		var j = builder.add(Codec.INT.fieldOf("j"), TestRecord::j);
		var k = builder.add(Codec.INT.fieldOf("k"), TestRecord::k);
		var l = builder.add(Codec.INT.fieldOf("l"), TestRecord::l);
		var m = builder.add(Codec.INT.fieldOf("m"), TestRecord::m);
		var n = builder.add(Codec.INT.fieldOf("n"), TestRecord::n);
		var o = builder.add(Codec.INT.fieldOf("o"), TestRecord::o);
		var p = builder.add(Codec.INT.fieldOf("p"), TestRecord::p);
		return container -> new TestRecord(
			container.get(a), container.get(b), container.get(c), container.get(d),
			container.get(e), container.get(f), container.get(g), container.get(h),
			container.get(i), container.get(j), container.get(k), container.get(l),
			container.get(m), container.get(n), container.get(o), container.get(p)
		);
	});

	public static TestRecord makeRecord(int i) {
		return new TestRecord(
			i, i + 1, i + 2, i + 3, i + 4, i + 5, i + 6, i + 7, i + 8, i + 9, i + 10, i + 11, i + 12, i + 13, i + 14, i + 15
		);
	}

	public static JsonObject makeData(int i) {
		JsonObject json = new JsonObject();
		for (int j = 0; j < 16; j++) {
			json.addProperty(Character.toString('a'+j), i+j);
		}
		return json;
	}
}
