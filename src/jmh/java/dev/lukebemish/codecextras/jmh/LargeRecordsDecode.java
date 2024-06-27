package dev.lukebemish.codecextras.jmh;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Measurement(time = 2)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(value = Scope.Thread)
public class LargeRecordsDecode {
	private int counter;

	@Benchmark
	public void recordCodecBuilder(Blackhole blackhole) {
		JsonElement json = TestRecord.makeData(counter++);
		var result = TestRecord.RCB.decode(JsonOps.INSTANCE, json);
		blackhole.consume(result.result().orElseThrow());
	}

	@Benchmark
	public void keyedRecordCodecBuilder(Blackhole blackhole) {
		JsonElement json = TestRecord.makeData(counter++);
		var result = TestRecord.KRCB.decode(JsonOps.INSTANCE, json);
		blackhole.consume(result.result().orElseThrow());
	}

	@Benchmark
	public void extendedRecordCodecBuilder(Blackhole blackhole) {
		JsonElement json = TestRecord.makeData(counter++);
		var result = TestRecord.ERCB.decode(JsonOps.INSTANCE, json);
		blackhole.consume(result.result().orElseThrow());
	}
}
