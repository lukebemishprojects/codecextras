package dev.lukebemish.codecextras.jmh;

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

public class LargeRecordsEncode {
    @Measurement(time = 2, iterations = 5)
    @Warmup(time = 2, iterations = 2)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @BenchmarkMode(Mode.AverageTime)
    @State(value = Scope.Thread)
    public static class Average {
        private int counter;

        @Benchmark
        public void recordCodecBuilder(Blackhole blackhole) {
            TestRecord record = TestRecord.makeRecord(counter++);
            var result = TestRecord.RCB.encodeStart(JsonOps.INSTANCE, record);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void keyedRecordCodecBuilder(Blackhole blackhole) {
            TestRecord record = TestRecord.makeRecord(counter++);
            var result = TestRecord.KRCB.encodeStart(JsonOps.INSTANCE, record);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void curriedRecordCodecBuilder(Blackhole blackhole) {
            TestRecord record = TestRecord.makeRecord(counter++);
            var result = TestRecord.CRCB.encodeStart(JsonOps.INSTANCE, record);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void methodHandleRecordCodecBuilder(Blackhole blackhole) {
            TestRecord record = TestRecord.makeRecord(counter++);
            var result = TestRecord.MHRCB.encodeStart(JsonOps.INSTANCE, record);
            blackhole.consume(result.result().orElseThrow());
        }
    }

    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @BenchmarkMode(Mode.SingleShotTime)
    @State(value = Scope.Thread)
    @Fork(value = 20)
    public static class SingleShot {
        private TestRecord record;

        @Setup
        public void setup() {
            record = TestRecord.makeRecord(0);
            TestRecord.RCB.encodeStart(JsonOps.INSTANCE, record);
            TestRecord.KRCB.encodeStart(JsonOps.INSTANCE, record);
            TestRecord.CRCB.encodeStart(JsonOps.INSTANCE, record);
        }

        @Benchmark
        public void recordCodecBuilder(Blackhole blackhole) {
            var result = TestRecord.RCB.encodeStart(JsonOps.INSTANCE, record);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void keyedRecordCodecBuilder(Blackhole blackhole) {
            var result = TestRecord.KRCB.encodeStart(JsonOps.INSTANCE, record);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void curriedRecordCodecBuilder(Blackhole blackhole) {
            var result = TestRecord.CRCB.encodeStart(JsonOps.INSTANCE, record);
            blackhole.consume(result.result().orElseThrow());
        }

        @Benchmark
        public void methodHandleRecordCodecBuilder(Blackhole blackhole) {
            var result = TestRecord.MHRCB.encodeStart(JsonOps.INSTANCE, record);
            blackhole.consume(result.result().orElseThrow());
        }
    }
}
