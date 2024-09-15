package dev.lukebemish.codecextras.companion;

import com.google.common.collect.MapMaker;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public abstract class DelegatingOps<T> implements AccompaniedOps<T> {
    protected final DynamicOps<T> delegate;
    @Nullable protected final AccompaniedOps<T> accompanied;
    public DelegatingOps(DynamicOps<T> delegate) {
        this.delegate = delegate;
        this.accompanied = AccompaniedOps.find(delegate).orElse(null);
    }

    public static <T, Q extends Companion.CompanionToken> DynamicOps<T> of(Q token, Companion<T, Q> companion, DynamicOps<T> delegate) {
        var possibleParent = retrieveMapOps(delegate);
        if (possibleParent != null) {
            if (possibleParent.getSecond() instanceof MapDelegatingOps<T> mapOps) {
                Map<Companion.CompanionToken, Optional<Companion<T, ? extends Companion.CompanionToken>>> map = new HashMap<>(mapOps.companions);
                map.put(token, Optional.of(companion));
                return possibleParent.getFirst().delegate(delegate, new MapDelegatingOps<>(delegate, map));
            }
            return possibleParent.getFirst().delegate(delegate, new MapDelegatingOps<>(delegate, Map.of(token, Optional.of(companion))));
        } else {
            return new MapDelegatingOps<>(delegate, Map.of(token, Optional.of(companion)));
        }
    }

    public static <T, Q extends Companion.CompanionToken> DynamicOps<T> without(Q token, DynamicOps<T> delegate) {
        var possibleParent = retrieveMapOps(delegate);
        if (possibleParent != null) {
            if (possibleParent.getSecond() instanceof MapDelegatingOps<T> mapOps) {
                Map<Companion.CompanionToken, Optional<Companion<T, ? extends Companion.CompanionToken>>> map = new HashMap<>(mapOps.companions);
                map.put(token, Optional.empty());
                return possibleParent.getFirst().delegate(delegate, new MapDelegatingOps<>(delegate, map));
            }
            return possibleParent.getFirst().delegate(delegate, new MapDelegatingOps<>(delegate, Map.of(token, Optional.empty())));
        } else {
            return new MapDelegatingOps<>(delegate, Map.of(token, Optional.empty()));
        }
    }

    private static final List<AlternateCompanionRetriever> ALTERNATE_COMPANION_RETRIEVERS;
    private static final Map<ModuleLayer, List<AlternateCompanionRetriever>> RETRIEVERS = new MapMaker().weakKeys().weakValues().makeMap();

    static {
        List<AlternateCompanionRetriever> retrievers = new ArrayList<>();
        retrievers.add(new AlternateCompanionRetriever() {
            @Override
            public <A> Optional<AccompaniedOps<A>> locateCompanionDelegate(DynamicOps<A> ops) {
                if (ops instanceof MapDelegatingOps<A> mapOps) {
                    return Optional.of(mapOps);
                }
                return Optional.empty();
            }

            @Override
            public <A> AccompaniedOps<A> delegate(DynamicOps<A> ops, AccompaniedOps<A> delegate) {
                return delegate;
            }
        });
        retrievers.addAll(ServiceLoader.load(AlternateCompanionRetriever.class).stream().map(ServiceLoader.Provider::get).toList());
        ALTERNATE_COMPANION_RETRIEVERS = List.copyOf(retrievers);
    }

    static List<AlternateCompanionRetriever> forOps(DynamicOps<?> ops) {
        var clazz = ops.getClass();
        var layer = clazz.getModule().getLayer();
        if (layer == null) {
            return ALTERNATE_COMPANION_RETRIEVERS;
        }
        return RETRIEVERS.computeIfAbsent(layer, k -> {
            List<AlternateCompanionRetriever> retrievers = new ArrayList<>();
            retrievers.add(new AlternateCompanionRetriever() {
                @Override
                public <A> Optional<AccompaniedOps<A>> locateCompanionDelegate(DynamicOps<A> ops) {
                    if (ops instanceof MapDelegatingOps<A> mapOps) {
                        return Optional.of(mapOps);
                    }
                    return Optional.empty();
                }

                @Override
                public <A> AccompaniedOps<A> delegate(DynamicOps<A> ops, AccompaniedOps<A> delegate) {
                    return delegate;
                }
            });
            retrievers.addAll(ServiceLoader.load(layer, AlternateCompanionRetriever.class).stream().map(ServiceLoader.Provider::get).toList());
            if (layer != DelegatingOps.class.getModule().getLayer()) {
                retrievers.addAll(ServiceLoader.load(AlternateCompanionRetriever.class).stream().map(ServiceLoader.Provider::get).toList());
            }
            return List.copyOf(retrievers);
        });
    }

    private static <T> @Nullable Pair<AlternateCompanionRetriever, AccompaniedOps<T>> retrieveMapOps(DynamicOps<T> ops) {
        for (AlternateCompanionRetriever retriever : forOps(ops)) {
            Optional<AccompaniedOps<T>> companionDelegate = retriever.locateCompanionDelegate(ops);
            if (companionDelegate.isPresent()) {
                return Pair.of(retriever, companionDelegate.get());
            }
        }
        return null;
    }

    @Override
    public T empty() {
        return delegate.empty();
    }

    @Override
    public T emptyMap() {
        return delegate.emptyMap();
    }

    @Override
    public T emptyList() {
        return delegate.emptyList();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, T input) {
        return delegate.convertTo(outOps, input);
    }

    @Override
    public DataResult<Number> getNumberValue(T input) {
        return delegate.getNumberValue(input);
    }

    @Override
    public Number getNumberValue(T input, Number defaultValue) {
        return delegate.getNumberValue(input, defaultValue);
    }

    @Override
    public T createNumeric(Number i) {
        return delegate.createNumeric(i);
    }

    @Override
    public T createByte(byte value) {
        return delegate.createByte(value);
    }

    @Override
    public T createShort(short value) {
        return delegate.createShort(value);
    }

    @Override
    public T createInt(int value) {
        return delegate.createInt(value);
    }

    @Override
    public T createLong(long value) {
        return delegate.createLong(value);
    }

    @Override
    public T createFloat(float value) {
        return delegate.createFloat(value);
    }

    @Override
    public T createDouble(double value) {
        return delegate.createDouble(value);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(T input) {
        return delegate.getBooleanValue(input);
    }

    @Override
    public T createBoolean(boolean value) {
        return delegate.createBoolean(value);
    }

    @Override
    public DataResult<String> getStringValue(T input) {
        return delegate.getStringValue(input);
    }

    @Override
    public T createString(String value) {
        return delegate.createString(value);
    }

    @Override
    public DataResult<T> mergeToList(T list, T value) {
        return delegate.mergeToList(list, value);
    }

    @Override
    public DataResult<T> mergeToList(T list, List<T> values) {
        return delegate.mergeToList(list, values);
    }

    @Override
    public DataResult<T> mergeToMap(T map, T key, T value) {
        return delegate.mergeToMap(map, key, value);
    }

    @Override
    public DataResult<T> mergeToMap(T map, Map<T, T> values) {
        return delegate.mergeToMap(map, values);
    }

    @Override
    public DataResult<T> mergeToMap(T map, MapLike<T> values) {
        return delegate.mergeToMap(map, values);
    }

    @Override
    public DataResult<T> mergeToPrimitive(T prefix, T value) {
        return delegate.mergeToPrimitive(prefix, value);
    }

    @Override
    public DataResult<Stream<Pair<T, T>>> getMapValues(T input) {
        return delegate.getMapValues(input);
    }

    @Override
    public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T input) {
        return delegate.getMapEntries(input);
    }

    @Override
    public T createMap(Stream<Pair<T, T>> map) {
        return delegate.createMap(map);
    }

    @Override
    public DataResult<MapLike<T>> getMap(T input) {
        return delegate.getMap(input);
    }

    @Override
    public T createMap(Map<T, T> map) {
        return delegate.createMap(map);
    }

    @Override
    public DataResult<Stream<T>> getStream(T input) {
        return delegate.getStream(input);
    }

    @Override
    public DataResult<Consumer<Consumer<T>>> getList(T input) {
        return delegate.getList(input);
    }

    @Override
    public T createList(Stream<T> input) {
        return delegate.createList(input);
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(T input) {
        return delegate.getByteBuffer(input);
    }

    @Override
    public T createByteList(ByteBuffer input) {
        return delegate.createByteList(input);
    }

    @Override
    public DataResult<IntStream> getIntStream(T input) {
        return delegate.getIntStream(input);
    }

    @Override
    public T createIntList(IntStream input) {
        return delegate.createIntList(input);
    }

    @Override
    public DataResult<LongStream> getLongStream(T input) {
        return delegate.getLongStream(input);
    }

    @Override
    public T createLongList(LongStream input) {
        return delegate.createLongList(input);
    }

    @Override
    public T remove(T input, String key) {
        return delegate.remove(input, key);
    }

    @Override
    public boolean compressMaps() {
        return delegate.compressMaps();
    }

    @Override
    public DataResult<T> get(T input, String key) {
        return delegate.get(input, key);
    }

    @Override
    public DataResult<T> getGeneric(T input, T key) {
        return delegate.getGeneric(input, key);
    }

    @Override
    public T set(T input, String key, T value) {
        return delegate.set(input, key, value);
    }

    @Override
    public T update(T input, String key, Function<T, T> function) {
        return delegate.update(input, key, function);
    }

    @Override
    public T updateGeneric(T input, T key, Function<T, T> function) {
        return delegate.updateGeneric(input, key, function);
    }

    @Override
    public ListBuilder<T> listBuilder() {
        return delegate.listBuilder();
    }

    @Override
    public RecordBuilder<T> mapBuilder() {
        return delegate.mapBuilder();
    }

    @Override
    public <E> Function<E, DataResult<T>> withEncoder(Encoder<E> encoder) {
        return delegate.withEncoder(encoder);
    }

    @Override
    public <E> Function<T, DataResult<Pair<E, T>>> withDecoder(Decoder<E> decoder) {
        return delegate.withDecoder(decoder);
    }

    @Override
    public <E> Function<T, DataResult<E>> withParser(Decoder<E> decoder) {
        return delegate.withParser(decoder);
    }

    @Override
    public <U> U convertList(DynamicOps<U> outOps, T input) {
        return delegate.convertList(outOps, input);
    }

    @Override
    public <U> U convertMap(DynamicOps<U> outOps, T input) {
        return delegate.convertMap(outOps, input);
    }

    @Override
    public <O extends Companion.CompanionToken, C extends Companion<T, O>> Optional<C> getCompanion(O token) {
        if (accompanied != null) {
            return accompanied.getCompanion(token);
        }
        return AccompaniedOps.super.getCompanion(token);
    }
}
