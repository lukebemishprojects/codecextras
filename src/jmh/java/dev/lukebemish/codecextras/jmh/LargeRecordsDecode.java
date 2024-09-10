package dev.lukebemish.codecextras.jmh;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

public class LargeRecordsDecode {
    @Measurement(time = 2, iterations = 5)
    @Warmup(time = 2, iterations = 2)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @BenchmarkMode(Mode.AverageTime)
    @State(value = Scope.Thread)
    public static class Average {
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
        public void curriedRecordCodecBuilder(Blackhole blackhole) {
            JsonElement json = TestRecord.makeData(counter++);
            var result = TestRecord.CRCB.decode(JsonOps.INSTANCE, json);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void methodHandleRecordCodecBuilder(Blackhole blackhole) {
            JsonElement json = TestRecord.makeData(counter++);
            var result = TestRecord.MHRCB.decode(JsonOps.INSTANCE, json);
            blackhole.consume(result.result().orElseThrow());
        }
    }

    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @BenchmarkMode(Mode.SingleShotTime)
    @State(value = Scope.Thread)
    @Fork(value = 20)
    public static class SingleShot {
        private JsonElement json;

        @Setup
        public void setup() {
            json = TestRecord.makeData(0);
            TestRecord.RCB.decode(JsonOps.INSTANCE, json);
            TestRecord.KRCB.decode(JsonOps.INSTANCE, json);
            TestRecord.CRCB.decode(JsonOps.INSTANCE, json);
        }

        @Benchmark
        public void recordCodecBuilder(Blackhole blackhole) {
            var result = TestRecord.RCB.decode(JsonOps.INSTANCE, json);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void keyedRecordCodecBuilder(Blackhole blackhole) {
            var result = TestRecord.KRCB.decode(JsonOps.INSTANCE, json);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void curriedRecordCodecBuilder(Blackhole blackhole) {
            var result = TestRecord.CRCB.decode(JsonOps.INSTANCE, json);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void methodHandleRecordCodecBuilder(Blackhole blackhole) {
            var result = TestRecord.MHRCB.decode(JsonOps.INSTANCE, json);
            blackhole.consume(result.result().orElseThrow());
        }
    }
}
