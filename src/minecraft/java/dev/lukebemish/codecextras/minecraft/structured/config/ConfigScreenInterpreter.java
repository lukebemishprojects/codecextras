package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.common.base.Suppliers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftKeys;
import dev.lukebemish.codecextras.structured.Annotation;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Interpreter;
import dev.lukebemish.codecextras.structured.Key;
import dev.lukebemish.codecextras.structured.KeyStoringInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Keys2;
import dev.lukebemish.codecextras.structured.ParametricKeyedValue;
import dev.lukebemish.codecextras.structured.Range;
import dev.lukebemish.codecextras.structured.RecordStructure;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.types.Identity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ConfigScreenInterpreter extends KeyStoringInterpreter<ConfigScreenEntry.Mu, ConfigScreenInterpreter> {
    private final CodecInterpreter codecInterpreter;

    public ConfigScreenInterpreter(
        Keys<ConfigScreenEntry.Mu, Object> keys,
        Keys2<ParametricKeyedValue.Mu<ConfigScreenEntry.Mu>, K1, K1> parametricKeys,
        CodecInterpreter codecInterpreter
    ) {
        super(
            keys.join(Keys.<ConfigScreenEntry.Mu, Object>builder()
                .add(Interpreter.INT, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Integer.parseInt(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not an integer: "+string);
                        }
                    }, integer -> DataResult.success(integer+""), string -> string.matches("-?[0-9]*"), true),
                    new EntryCreationInfo<>(Codec.INT, ComponentInfo.empty())
                ))
                .add(Interpreter.BYTE, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Byte.parseByte(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a byte: "+string);
                        }
                    }, byteValue -> DataResult.success(byteValue+""), string -> string.matches("-?[0-9]*"), true),
                    new EntryCreationInfo<>(Codec.BYTE, ComponentInfo.empty())
                ))
                .add(Interpreter.SHORT, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Short.parseShort(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a short: "+string);
                        }
                    }, shortValue -> DataResult.success(shortValue+""), string -> string.matches("-?[0-9]*"), true),
                    new EntryCreationInfo<>(Codec.SHORT, ComponentInfo.empty())
                ))
                .add(Interpreter.LONG, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Long.parseLong(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a long: "+string);
                        }
                    }, longValue -> DataResult.success(longValue+""), string -> string.matches("-?[0-9]*"), true),
                    new EntryCreationInfo<>(Codec.LONG, ComponentInfo.empty())
                ))
                .add(Interpreter.DOUBLE, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Double.parseDouble(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a double: "+string);
                        }
                    }, doubleValue -> DataResult.success(doubleValue+""), string -> string.matches("-?[0-9]*(\\.[0-9]*)?"), true),
                    new EntryCreationInfo<>(Codec.DOUBLE, ComponentInfo.empty())
                ))
                .add(Interpreter.FLOAT, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Float.parseFloat(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a float: "+string);
                        }
                    }, floatValue -> DataResult.success(floatValue+""), string -> string.matches("-?[0-9]*(\\.[0-9]*)?"), true),
                    new EntryCreationInfo<>(Codec.FLOAT, ComponentInfo.empty())
                ))
                .add(Interpreter.BOOL, ConfigScreenEntry.single(
                    Widgets.bool(false),
                    new EntryCreationInfo<>(Codec.BOOL, ComponentInfo.empty())
                ))
                .add(Interpreter.UNIT, ConfigScreenEntry.single(
                    Widgets.bool(true),
                    new EntryCreationInfo<>(Codec.unit(Unit.INSTANCE), ComponentInfo.empty())
                ))
                .add(Interpreter.STRING, ConfigScreenEntry.single(
                    Widgets.canHandleOptional(Widgets.text(DataResult::success, DataResult::success, false)),
                    new EntryCreationInfo<>(Codec.STRING, ComponentInfo.empty())
                ))
                .add(MinecraftKeys.RESOURCE_LOCATION, ConfigScreenEntry.single(
                    Widgets.canHandleOptional(Widgets.text(ResourceLocation::read, rl -> DataResult.success(rl.toString()), string -> string.matches("([a-z0-9._-]+:)?[a-z0-9/._-]*"), false)),
                    new EntryCreationInfo<>(ResourceLocation.CODEC, ComponentInfo.empty())
                ))
                .add(MinecraftKeys.ARGB_COLOR, ConfigScreenEntry.single(
                    Widgets.color(true),
                    new EntryCreationInfo<>(Codec.INT, ComponentInfo.empty())
                ))
                .add(MinecraftKeys.RGB_COLOR, ConfigScreenEntry.single(
                    Widgets.color(false),
                    new EntryCreationInfo<>(Codec.INT, ComponentInfo.empty())
                ))
                .build()),
            parametricKeys.join(Keys2.<ParametricKeyedValue.Mu<ConfigScreenEntry.Mu>, K1, K1>builder()
                .add(Interpreter.INT_IN_RANGE, new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<ConfigScreenEntry.Mu, App<Const.Mu<Integer>, T>> convert(App<Const.Mu<Range<Integer>>, T> parameter) {
                        var range = Const.unbox(parameter);
                        var codec = Codec.INT.validate(Codec.checkRange(range.min(), range.max()));
                        return ConfigScreenEntry.single(
                            Widgets.slider(range, i -> DataResult.success(new JsonPrimitive(i)), json -> {
                                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                                    try {
                                        return DataResult.success(json.getAsJsonPrimitive().getAsInt());
                                    } catch (NumberFormatException e) {
                                        return DataResult.error(() -> "Not an integer: " + json);
                                    }
                                }
                                return DataResult.error(() -> "Not an integer: " + json);
                            }, false), new EntryCreationInfo<>(codec, ComponentInfo.empty())
                        ).withEntryCreationInfo(
                            info -> info.withCodec(codec.xmap(Const::create, Const::unbox)),
                            info -> info.withCodec(codec)
                        );
                    }
                })
                .add(Interpreter.BYTE_IN_RANGE, new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<ConfigScreenEntry.Mu, App<Const.Mu<Byte>, T>> convert(App<Const.Mu<Range<Byte>>, T> parameter) {
                        var range = Const.unbox(parameter);
                        var codec = Codec.BYTE.validate(Codec.checkRange(range.min(), range.max()));
                        return ConfigScreenEntry.single(
                            Widgets.slider(range, i -> DataResult.success(new JsonPrimitive(i)), json -> {
                                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                                    try {
                                        return DataResult.success(json.getAsJsonPrimitive().getAsByte());
                                    } catch (NumberFormatException e) {
                                        return DataResult.error(() -> "Not a byte: " + json);
                                    }
                                }
                                return DataResult.error(() -> "Not a byte: " + json);
                            }, false), new EntryCreationInfo<>(codec, ComponentInfo.empty())
                        ).withEntryCreationInfo(
                            info -> info.withCodec(codec.xmap(Const::create, Const::unbox)),
                            info -> info.withCodec(codec)
                        );
                    }
                })
                .add(Interpreter.SHORT_IN_RANGE, new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<ConfigScreenEntry.Mu, App<Const.Mu<Short>, T>> convert(App<Const.Mu<Range<Short>>, T> parameter) {
                        var range = Const.unbox(parameter);
                        var codec = Codec.SHORT.validate(Codec.checkRange(range.min(), range.max()));
                        return ConfigScreenEntry.single(
                            Widgets.slider(range, i -> DataResult.success(new JsonPrimitive(i)), json -> {
                                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                                    try {
                                        return DataResult.success(json.getAsJsonPrimitive().getAsShort());
                                    } catch (NumberFormatException e) {
                                        return DataResult.error(() -> "Not a short: " + json);
                                    }
                                }
                                return DataResult.error(() -> "Not a short: " + json);
                            }, false), new EntryCreationInfo<>(codec, ComponentInfo.empty())
                        ).withEntryCreationInfo(
                            info -> info.withCodec(codec.xmap(Const::create, Const::unbox)),
                            info -> info.withCodec(codec)
                        );
                    }
                })
                .add(Interpreter.LONG_IN_RANGE, new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<ConfigScreenEntry.Mu, App<Const.Mu<Long>, T>> convert(App<Const.Mu<Range<Long>>, T> parameter) {
                        var range = Const.unbox(parameter);
                        var codec = Codec.LONG.validate(Codec.checkRange(range.min(), range.max()));
                        return ConfigScreenEntry.single(
                            Widgets.slider(range, i -> DataResult.success(new JsonPrimitive(i)), json -> {
                                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                                    try {
                                        return DataResult.success(json.getAsJsonPrimitive().getAsLong());
                                    } catch (NumberFormatException e) {
                                        return DataResult.error(() -> "Not a long: " + json);
                                    }
                                }
                                return DataResult.error(() -> "Not a long: " + json);
                            }, false), new EntryCreationInfo<>(codec, ComponentInfo.empty())
                        ).withEntryCreationInfo(
                            info -> info.withCodec(codec.xmap(Const::create, Const::unbox)),
                            info -> info.withCodec(codec)
                        );
                    }
                })
                .add(Interpreter.FLOAT_IN_RANGE, new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<ConfigScreenEntry.Mu, App<Const.Mu<Float>, T>> convert(App<Const.Mu<Range<Float>>, T> parameter) {
                        var range = Const.unbox(parameter);
                        var codec = Codec.FLOAT.validate(Codec.checkRange(range.min(), range.max()));
                        return ConfigScreenEntry.single(
                            Widgets.slider(range, i -> DataResult.success(new JsonPrimitive(i)), json -> {
                                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                                    try {
                                        return DataResult.success(json.getAsJsonPrimitive().getAsFloat());
                                    } catch (NumberFormatException e) {
                                        return DataResult.error(() -> "Not a float: " + json);
                                    }
                                }
                                return DataResult.error(() -> "Not a float: " + json);
                            }, true), new EntryCreationInfo<>(codec, ComponentInfo.empty())
                        ).withEntryCreationInfo(
                            info -> info.withCodec(codec.xmap(Const::create, Const::unbox)),
                            info -> info.withCodec(codec)
                        );
                    }
                })
                .add(Interpreter.DOUBLE_IN_RANGE, new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<ConfigScreenEntry.Mu, App<Const.Mu<Double>, T>> convert(App<Const.Mu<Range<Double>>, T> parameter) {
                        var range = Const.unbox(parameter);
                        var codec = Codec.DOUBLE.validate(Codec.checkRange(range.min(), range.max()));
                        return ConfigScreenEntry.single(
                            Widgets.slider(range, i -> DataResult.success(new JsonPrimitive(i)), json -> {
                                if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
                                    try {
                                        return DataResult.success(json.getAsJsonPrimitive().getAsDouble());
                                    } catch (NumberFormatException e) {
                                        return DataResult.error(() -> "Not a double: " + json);
                                    }
                                }
                                return DataResult.error(() -> "Not a double: " + json);
                            }, true), new EntryCreationInfo<>(codec, ComponentInfo.empty())
                        ).withEntryCreationInfo(
                            info -> info.withCodec(codec.xmap(Const::create, Const::unbox)),
                            info -> info.withCodec(codec)
                        );
                    }
                })
                .build())
        );
        this.codecInterpreter = codecInterpreter;
    }

    public ConfigScreenInterpreter(
        CodecInterpreter codecInterpreter
    ) {
        this(
            Keys.<ConfigScreenEntry.Mu, Object>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<ConfigScreenEntry.Mu>, K1, K1>builder().build(),
            codecInterpreter
        );
    }

    public static final Key<ConfigScreenEntry.Mu> KEY = Key.create("ConfigScreenInterpreter");

    @Override
    public Optional<Key<ConfigScreenEntry.Mu>> key() {
        return Optional.of(KEY);
    }

    @Override
    public ConfigScreenInterpreter with(Keys<ConfigScreenEntry.Mu, Object> keys, Keys2<ParametricKeyedValue.Mu<ConfigScreenEntry.Mu>, K1, K1> parametricKeys) {
        return new ConfigScreenInterpreter(keys().join(keys), parametricKeys().join(parametricKeys), this.codecInterpreter);
    }

    @Override
    public <A> DataResult<App<ConfigScreenEntry.Mu, List<A>>> list(App<ConfigScreenEntry.Mu, A> single) {
        var unwrapped = ConfigScreenEntry.unbox(single);
        var codecResult = codecInterpreter.list(new CodecInterpreter.Holder<>(unwrapped.entryCreationInfo().codec())).map(CodecInterpreter::unbox);
        if (codecResult.isError()) {
            return DataResult.error(codecResult.error().orElseThrow().messageSupplier());
        }
        ScreenEntryFactory<List<A>> factory = (ops, original, onClose, creationInfo) ->
            new ListScreenEntryProvider<>(unwrapped, ops, original, onClose);
        return DataResult.success(new ConfigScreenEntry<>(
            Widgets.canHandleOptional((parent, width, ops, original, update, creationInfo, handleOptional) -> {
                if (!handleOptional && original.isJsonNull()) {
                    original = new JsonArray();
                    update.accept(original);
                }
                JsonElement finalOriginal = original;
                return Button.builder(
                    Component.translatable("codecextras.config.configurelist"),
                    b -> Minecraft.getInstance().setScreen(ScreenEntryProvider.create(factory.open(
                        ops, finalOriginal, update, creationInfo
                    ), parent, creationInfo))
                ).width(width).build();
            }),
            factory,
            unwrapped.entryCreationInfo().withCodec(codecResult.getOrThrow())
        ));
    }

    @Override
    public <A> DataResult<App<ConfigScreenEntry.Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
        List<RecordEntry<?>> entries = new ArrayList<>();
        List<Supplier<String>> errors = new ArrayList<>();
        for (var field : fields) {
            handleEntry(field, entries, errors);
        }
        if (!errors.isEmpty()) {
            return DataResult.error(() -> "Errors crating record screen: "+errors.stream().map(Supplier::get).collect(Collectors.joining(", ")));
        }
        ScreenEntryFactory<A> factory = (ops, original, onClose, creationInfo) ->
            new RecordScreenEntryProvider(entries, ops, original, onClose);
        var codecResult = codecInterpreter.record(fields, creator);
        if (codecResult.isError()) {
            return DataResult.error(() -> "Error creating record codec: "+codecResult.error().orElseThrow().messageSupplier());
        }
        return DataResult.success(new ConfigScreenEntry<>(
            Widgets.canHandleOptional((parent, width, ops, original, update, creationInfo, handleOptional) -> {
                if (!handleOptional && original.isJsonNull()) {
                    original = new JsonObject();
                    update.accept(original);
                }
                JsonElement finalOriginal = original;
                return Button.builder(
                    Component.translatable("codecextras.config.configurerecord"),
                    b -> Minecraft.getInstance().setScreen(ScreenEntryProvider.create(factory.open(
                        ops, finalOriginal, update, creationInfo
                    ), parent, creationInfo))
                ).width(width).build();
            }),
            factory,
            new EntryCreationInfo<>(CodecInterpreter.unbox(codecResult.getOrThrow()), ComponentInfo.empty())
        ));
    }

    private <A, T> void handleEntry(RecordStructure.Field<A,T> field, List<RecordEntry<?>> entries, List<Supplier<String>> errors) {
        var codecResult = codecInterpreter.interpret(field.structure());
        if (codecResult.isError()) {
            errors.add(codecResult.error().orElseThrow().messageSupplier());
            return;
        }
        var optionEntryResult = field.structure().interpret(this).map(ConfigScreenEntry::unbox);
        if (optionEntryResult.isError()) {
            errors.add(optionEntryResult.error().orElseThrow().messageSupplier());
            return;
        }
        entries.add(new RecordEntry<>(
            field.name(),
            optionEntryResult.getOrThrow().withComponentInfo(info -> info.fallbackTitle(Component.literal(field.name()))),
            field.missingBehavior(),
            codecResult.getOrThrow()
        ));
    }

    @Override
    public <A, B> DataResult<App<ConfigScreenEntry.Mu, B>> flatXmap(App<ConfigScreenEntry.Mu, A> input, Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer) {
        var original = ConfigScreenEntry.unbox(input);
        var codecOriginal = original.entryCreationInfo().codec();
        var codecMapped = codecInterpreter.flatXmap(new CodecInterpreter.Holder<>(codecOriginal), deserializer, serializer).map(CodecInterpreter::unbox);
        if (codecMapped.error().isPresent()) {
            return DataResult.error(codecMapped.error().get().messageSupplier());
        }
        return DataResult.success(original.withEntryCreationInfo(
            info -> info.withCodec(codecMapped.getOrThrow()),
            info -> info.withCodec(codecOriginal)
        ));
    }

    @Override
    public <A> DataResult<App<ConfigScreenEntry.Mu, A>> annotate(Structure<A> original, Keys<Identity.Mu, Object> annotations) {
        var result = original.interpret(this);
        var codecResult = codecInterpreter.annotate(original, annotations).map(CodecInterpreter::unbox);
        if (codecResult.isError()) {
            return DataResult.error(codecResult.error().orElseThrow().messageSupplier());
        }
        return result.map(app -> {
            var originalCodec = ConfigScreenEntry.unbox(app).entryCreationInfo().codec();
            var entry = ConfigScreenEntry.unbox(app).withEntryCreationInfo(info -> info.withCodec(codecResult.getOrThrow()), info -> info.withCodec(originalCodec));
            var withTitle = Annotation
                .get(annotations, ConfigAnnotations.TITLE)
                .or(() -> Annotation.get(annotations, Annotation.TITLE).map(Component::literal))
                .map(title -> entry.withComponentInfo(info -> info.withTitle(title)))
                .orElse(entry);
            return Annotation
                .get(annotations, ConfigAnnotations.DESCRIPTOIN)
                .or(() -> Annotation.get(annotations, Annotation.DESCRIPTION).map(Component::literal))
                .or(() -> Annotation.get(annotations, Annotation.COMMENT).map(Component::literal))
                .map(description -> withTitle.withComponentInfo(info -> info.withDescription(description)))
                .orElse(withTitle);
        });
    }

    @Override
    public <E, A> DataResult<App<ConfigScreenEntry.Mu, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Set<A> keys, Function<A, Structure<? extends E>> structures) {
        var keyResult = interpret(keyStructure).map(entry -> entry.withComponentInfo(info -> info.fallbackTitle(Component.literal(key))));
        if (keyResult.error().isPresent()) {
            return DataResult.error(keyResult.error().get().messageSupplier());
        }
        Supplier<Map<A, DataResult<ConfigScreenEntry<? extends E>>>> entries = Suppliers.memoize(() -> {
            Map<A, DataResult<ConfigScreenEntry<? extends E>>> map = new HashMap<>();
            for (var entryKey : keys) {
                var result = structures.apply(entryKey).interpret(this);
                if (result.error().isPresent()) {
                    map.put(entryKey, DataResult.error(result.error().get().messageSupplier()));
                } else {
                    map.put(entryKey, DataResult.success(ConfigScreenEntry.unbox(result.getOrThrow())));
                }
            }
            return map;
        });
        var codecResult = codecInterpreter.dispatch(key, keyStructure, function, keys, structures);
        if (codecResult.isError()) {
            return DataResult.error(() -> "Error creating dispatch codec: "+codecResult.error().orElseThrow().messageSupplier());
        }
        ScreenEntryFactory<E> factory = (ops, original, onClose, creationInfo) ->
            new DispatchScreenEntryProvider<>(keyResult.getOrThrow().entryCreationInfo(), original, key, onClose, ops, entries.get());
        return DataResult.success(new ConfigScreenEntry<>(
            Widgets.canHandleOptional((parent, width, ops, original, update, creationInfo, handleOptional) -> {
                if (!handleOptional && original.isJsonNull()) {
                    original = new JsonObject();
                    update.accept(original);
                }
                JsonElement finalOriginal = original;
                return Button.builder(
                    Component.translatable("codecextras.config.configurerecord"),
                    b -> Minecraft.getInstance().setScreen(ScreenEntryProvider.create(factory.open(
                        ops, finalOriginal, update, creationInfo
                    ), parent, creationInfo))
                ).width(width).build();
            }),
            factory,
            new EntryCreationInfo<>(CodecInterpreter.unbox(codecResult.getOrThrow()), ComponentInfo.empty())
        ));
    }

    public <A> DataResult<ConfigScreenEntry<A>> interpret(Structure<A> structure) {
        return structure.interpret(this).map(ConfigScreenEntry::unbox);
    }
}
