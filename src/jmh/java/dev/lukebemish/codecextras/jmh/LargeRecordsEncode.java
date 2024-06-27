package dev.lukebemish.codecextras.jmh;

import com.mojang.serialization.JsonOps;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Measurement(time = 2)
@Warmup(time = 2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(value = Scope.Thread)
public class LargeRecordsEncode {
	private int counter;

	@Benchmark
	public void recordCodecBuilder(Blackhole blackhole) {
		TestRecord object = TestRecord.makeRecord(counter++);
		var result = TestRecord.RCB.encodeStart(JsonOps.INSTANCE, object);
		blackhole.consume(result.result().orElseThrow());
	}

	@Benchmark
	public void keyedRecordCodecBuilder(Blackhole blackhole) {
		TestRecord object = TestRecord.makeRecord(counter++);
		var result = TestRecord.KRCB.encodeStart(JsonOps.INSTANCE, object);
		blackhole.consume(result.result().orElseThrow());
	}

	@Benchmark
	public void extendedRecordCodecBuilder(Blackhole blackhole) {
		TestRecord object = TestRecord.makeRecord(counter++);
		var result = TestRecord.ERCB.encodeStart(JsonOps.INSTANCE, object);
		blackhole.consume(result.result().orElseThrow());
	}
}
