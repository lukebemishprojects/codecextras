package dev.lukebemish.codecextras.jmh;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Measurement(time = 5)
@Warmup(time = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(value = Scope.Benchmark)
public class LargeRecordsDecode {
	private final AtomicInteger counter = new AtomicInteger(0);

	@Benchmark
	public void recordCodecBuilder(Blackhole blackhole) {
		JsonElement json = TestRecord.makeData(counter.getAndIncrement());
		var result = TestRecord.RCB.decode(JsonOps.INSTANCE, json);
		blackhole.consume(result.result().orElseThrow());
	}

	@Benchmark
	public void keyedRecordCodecBuilder(Blackhole blackhole) {
		JsonElement json = TestRecord.makeData(counter.getAndIncrement());
		var result = TestRecord.KRCB.decode(JsonOps.INSTANCE, json);
		blackhole.consume(result.result().orElseThrow());
	}

	@Benchmark
	public void extendedRecordCodecBuilder(Blackhole blackhole) {
		JsonElement json = TestRecord.makeData(counter.getAndIncrement());
		var result = TestRecord.ERCB.decode(JsonOps.INSTANCE, json);
		blackhole.consume(result.result().orElseThrow());
	}
}
