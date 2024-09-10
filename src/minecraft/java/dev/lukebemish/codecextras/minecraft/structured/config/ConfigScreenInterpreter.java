package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import dev.lukebemish.codecextras.StringRepresentation;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftInterpreters;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftKeys;
import dev.lukebemish.codecextras.minecraft.structured.MinecraftStructures;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigScreenInterpreter extends KeyStoringInterpreter<ConfigScreenEntry.Mu, ConfigScreenInterpreter> {
    private static final Logger LOGGER = LogUtils.getLogger();

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
                    }, integer -> DataResult.success(integer+""), string -> string.matches("^-?[0-9]*$"), true),
                    new EntryCreationInfo<>(Codec.INT, ComponentInfo.empty())
                ))
                .add(Interpreter.BYTE, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Byte.parseByte(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a byte: "+string);
                        }
                    }, byteValue -> DataResult.success(byteValue+""), string -> string.matches("^-?[0-9]*$"), true),
                    new EntryCreationInfo<>(Codec.BYTE, ComponentInfo.empty())
                ))
                .add(Interpreter.SHORT, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Short.parseShort(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a short: "+string);
                        }
                    }, shortValue -> DataResult.success(shortValue+""), string -> string.matches("^-?[0-9]*$"), true),
                    new EntryCreationInfo<>(Codec.SHORT, ComponentInfo.empty())
                ))
                .add(Interpreter.LONG, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Long.parseLong(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a long: "+string);
                        }
                    }, longValue -> DataResult.success(longValue+""), string -> string.matches("^-?[0-9]*$"), true),
                    new EntryCreationInfo<>(Codec.LONG, ComponentInfo.empty())
                ))
                .add(Interpreter.DOUBLE, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Double.parseDouble(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a double: "+string);
                        }
                    }, doubleValue -> DataResult.success(doubleValue+""), string -> string.matches("^-?[0-9]*(\\.[0-9]*)?$"), true),
                    new EntryCreationInfo<>(Codec.DOUBLE, ComponentInfo.empty())
                ))
                .add(Interpreter.FLOAT, ConfigScreenEntry.single(
                    Widgets.text(string -> {
                        try {
                            return DataResult.success(Float.parseFloat(string));
                        } catch (NumberFormatException e) {
                            return DataResult.error(() -> "Not a float: "+string);
                        }
                    }, floatValue -> DataResult.success(floatValue+""), string -> string.matches("^-?[0-9]*(\\.[0-9]*)?$"), true),
                    new EntryCreationInfo<>(Codec.FLOAT, ComponentInfo.empty())
                ))
                .add(Interpreter.BOOL, ConfigScreenEntry.single(
                    Widgets.bool(),
                    new EntryCreationInfo<>(Codec.BOOL, ComponentInfo.empty())
                ))
                .add(Interpreter.UNIT, ConfigScreenEntry.single(
                    Widgets.unit(),
                    new EntryCreationInfo<>(Codec.unit(Unit.INSTANCE), ComponentInfo.empty())
                ))
                .add(Interpreter.EMPTY_LIST, ConfigScreenEntry.single(
                    Widgets.unit(Component.translatable("codecextras.config.unit.empty")),
                    new EntryCreationInfo<>(MinecraftInterpreters.CODEC_INTERPRETER.interpret(Structure.EMPTY_LIST).getOrThrow(), ComponentInfo.empty())
                ))
                .add(Interpreter.EMPTY_MAP, ConfigScreenEntry.single(
                    Widgets.unit(Component.translatable("codecextras.config.unit.empty")),
                    new EntryCreationInfo<>(MinecraftInterpreters.CODEC_INTERPRETER.interpret(Structure.EMPTY_MAP).getOrThrow(), ComponentInfo.empty())
                ))
                .add(Interpreter.STRING, ConfigScreenEntry.single(
                    Widgets.wrapWithOptionalHandling(Widgets.text(DataResult::success, DataResult::success, false)),
                    new EntryCreationInfo<>(Codec.STRING, ComponentInfo.empty())
                ))
                .add(Interpreter.PASSTHROUGH, ConfigScreenEntry.single(
                    Widgets.wrapWithOptionalHandling(ConfigScreenInterpreter::byJson),
                    new EntryCreationInfo<>(Codec.PASSTHROUGH, ComponentInfo.empty())
                ))
                .add(MinecraftKeys.ITEM_NON_AIR, ConfigScreenEntry.single(
                    Widgets.pickWidget(new StringRepresentation<>(
                        () -> BuiltInRegistries.ITEM.holders().<Holder<Item>>map(Function.identity()).toList(),
                        holder -> {
                            if (holder instanceof Holder.Reference<Item> reference) {
                                return reference.key().location().toString();
                            } else {
                                var value = holder.value();
                                return BuiltInRegistries.ITEM.getKey(value).toString();
                            }
                        },
                        string -> {
                            var rlResult = ResourceLocation.read(string);
                            if (rlResult.error().isPresent()) {
                                return null;
                            }
                            var key = rlResult.getOrThrow();
                            return BuiltInRegistries.ITEM.getHolder(key).orElse(null);
                        },
                        false
                    )),
                    new EntryCreationInfo<>(ItemStack.ITEM_NON_AIR_CODEC, ComponentInfo.empty())
                ))
                .add(MinecraftKeys.RESOURCE_LOCATION, ConfigScreenEntry.single(
                    Widgets.wrapWithOptionalHandling(Widgets.text(ResourceLocation::read, rl -> DataResult.success(rl.toString()), string -> string.matches("^([a-z0-9._-]+:)?[a-z0-9/._-]*$"), false)),
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
                .add(MinecraftKeys.DATA_COMPONENT_PATCH_KEY, ConfigScreenEntry.single(
                    (parent, width, context, original, update, creationInfo, handleOptional) -> {
                        boolean[] removes = new boolean[1];
                        DataComponentType<?>[] type = new DataComponentType<?>[1];
                        var problems = new EntryCreationContext.ProblemMarker[2];
                        if (!original.isJsonNull()) {
                            var initialResult = creationInfo.codec().parse(context.ops(), original);
                            if (initialResult.error().isPresent()) {
                                LOGGER.error("Error parsing data component patch key: {}", initialResult.error().get());
                            } else {
                                removes[0] = initialResult.getOrThrow().removes();
                                type[0] = initialResult.getOrThrow().type();
                            }
                        }
                        var remainingWidth = width - Button.DEFAULT_HEIGHT - 5;
                        var cycle = CycleButton.<Boolean>builder(bool -> {
                                if (bool == Boolean.TRUE) {
                                    return Component.translatable("codecextras.config.datacomponent.keytoggle.removes");
                                }
                                return Component.empty();
                            }).withValues(List.of(true, false))
                            .withInitialValue(removes[0])
                            .displayOnlyValue()
                            .create(0, 0, Button.DEFAULT_HEIGHT, Button.DEFAULT_HEIGHT, Component.translatable("codecextras.config.datacomponent.keytoggle"), (b, bool) -> {
                                removes[0] = bool;
                                if (type[0] != null) {
                                    var result = creationInfo.codec().encodeStart(context.ops(), new MinecraftKeys.DataComponentPatchKey<>(type[0], removes[0]));
                                    if (result.error().isPresent()) {
                                        problems[0] = context.problem(problems[0], "Error encoding data component patch key: "+result.error().get().message());
                                    } else {
                                        context.resolve(problems[0]);
                                        update.accept(result.getOrThrow());
                                    }
                                }
                            });
                        var typeKeys = BuiltInRegistries.DATA_COMPONENT_TYPE.registryKeySet().stream().toList();
                        var actual = Widgets.pickWidget(
                            new StringRepresentation<>(() -> typeKeys, key -> key.location().toString())
                        ).create(parent, remainingWidth, context, type[0] == null ? JsonNull.INSTANCE : new JsonPrimitive(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type[0]).toString()), json -> {
                            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                                String string = json.getAsJsonPrimitive().getAsString();
                                var rlResult = ResourceLocation.read(string);
                                if (rlResult.error().isPresent()) {
                                    problems[1] = context.problem(problems[1], "Error reading resource location: "+rlResult.error().get().message());
                                    return;
                                }
                                var key = rlResult.getOrThrow();
                                var typeResult = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(key);
                                if (typeResult.isEmpty()) {
                                    problems[1] = context.problem(problems[1], "Unknown data component type: "+key);
                                    return;
                                }
                                type[0] = typeResult.get();
                                var result = creationInfo.codec().encodeStart(context.ops(), new MinecraftKeys.DataComponentPatchKey<>(type[0], removes[0]));
                                if (result.error().isPresent()) {
                                    problems[1] = context.problem(problems[1], "Error encoding data component patch key: "+result.error().get().message());
                                } else {
                                    context.resolve(problems[1]);
                                    update.accept(result.getOrThrow());
                                }
                            } else {
                                problems[1] = context.problem(problems[1], "Not a string: "+json);
                            }
                        }, creationInfo.withCodec(ResourceKey.codec(Registries.DATA_COMPONENT_TYPE)), false);
                        var tooltipToggle = Tooltip.create(Component.translatable("codecextras.config.datacomponent.keytoggle"));
                        var tooltipType = Tooltip.create(creationInfo.componentInfo().description());
                        cycle.setTooltip(tooltipToggle);
                        actual.visitWidgets(w -> w.setTooltip(tooltipType));
                        var layout = new EqualSpacingLayout(width, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
                        layout.addChild(cycle, LayoutSettings.defaults().alignVerticallyMiddle());
                        layout.addChild(actual, LayoutSettings.defaults().alignVerticallyMiddle());
                        return layout;
                    },
                    new EntryCreationInfo<>(MinecraftInterpreters.CODEC_INTERPRETER.interpret(MinecraftStructures.DATA_COMPONENT_PATCH_KEY).getOrThrow(), ComponentInfo.empty())
                )).build()),
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
                .add(Interpreter.STRING_REPRESENTABLE, new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<ConfigScreenEntry.Mu, App<Identity.Mu, T>> convert(App<StringRepresentation.Mu, T> parameter) {
                        var representation = StringRepresentation.unbox(parameter);
                        var codec = representation.codec();
                        var identityCodec = codec.<App<Identity.Mu, T>>xmap(Identity::new, app -> Identity.unbox(app).value());
                        return ConfigScreenEntry.single(
                            Widgets.pickWidget(representation),
                            new EntryCreationInfo<>(codec, ComponentInfo.empty())
                        ).withEntryCreationInfo(i -> i.withCodec(identityCodec), i -> i.withCodec(codec));
                    }
                })
                .add(MinecraftKeys.RESOURCE_KEY, new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<ConfigScreenEntry.Mu, App<MinecraftKeys.ResourceKeyHolder.Mu, T>> convert(App<MinecraftKeys.RegistryKeyHolder.Mu, T> parameter) {
                        var registryKey = MinecraftKeys.RegistryKeyHolder.unbox(parameter).value();
                        var codec = ResourceKey.codec(registryKey);
                        var holderCodec = codec.<App<MinecraftKeys.ResourceKeyHolder.Mu, T>>xmap(MinecraftKeys.ResourceKeyHolder::new, app -> MinecraftKeys.ResourceKeyHolder.unbox(app).value());
                        return ConfigScreenEntry.single(
                            (parent, width, context, original, update, creationInfo, handleOptional) -> {
                                var registry = context.registryAccess().registry(registryKey);
                                LayoutFactory<ResourceKey<T>> wrapped;
                                Function<ResourceKey<T>, String> mapper = key -> key.location().toString();
                                if (registry.isPresent()) {
                                    Supplier<List<ResourceKey<T>>> values = () -> registry.get().registryKeySet().stream().sorted(Comparator.<ResourceKey<T>, String>comparing(key -> key.location().getNamespace()).thenComparing(key -> key.location().getPath())).toList();
                                    wrapped = Widgets.pickWidget(new StringRepresentation<>(values, mapper));
                                } else {
                                    wrapped = (parent2, width2, context2, original2, update2, creationInfo2, handleOptional2) -> Widgets.text(
                                        ResourceLocation::read,
                                        rl -> DataResult.success(rl.toString()),
                                        string -> string.matches("^([a-z0-9._-]+:)?[a-z0-9/._-]*$"),
                                        false
                                    ).create(parent2, width2, context2, original2, update2, creationInfo2.withCodec(ResourceLocation.CODEC), handleOptional2);
                                }
                                return wrapped.create(parent, width, context, original, update, creationInfo, handleOptional);
                            },
                            new EntryCreationInfo<>(codec, ComponentInfo.empty())
                        ).withEntryCreationInfo(i -> i.withCodec(holderCodec), i -> i.withCodec(codec));
                    }
                })
                .add(MinecraftKeys.FALLBACK_DATA_COMPONENT_TYPE, new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<ConfigScreenEntry.Mu, App<Identity.Mu, T>> convert(App<MinecraftKeys.DataComponentTypeHolder.Mu, T> parameter) {
                        var type = MinecraftKeys.DataComponentTypeHolder.unbox(parameter).value();
                        var codec = type.codec();
                        if (codec == null) {
                            LOGGER.error("{} is not a persistent component", type);
                            return ConfigScreenEntry.single(
                                (parent, width, context, original, update, creationInfo, handleOptional) -> {
                                    var button = Button.builder(
                                        Component.translatable("codecextras.config.datacomponent.notpersistent", type),
                                        b -> {
                                        }
                                    ).width(width).build();
                                    button.active = false;
                                    return button;
                                },
                                new EntryCreationInfo<>(Codec.EMPTY.codec().flatXmap(
                                    ignored -> DataResult.error(() -> type + " is not a persistent component"),
                                    ignored -> DataResult.error(() -> type + " is not a persistent component")
                                ), ComponentInfo.empty())
                            );
                        }
                        var identityCodec = codec.<App<Identity.Mu, T>>xmap(Identity::new, app -> Identity.unbox(app).value());
                        return ConfigScreenEntry.single(
                            Widgets.wrapWithOptionalHandling(ConfigScreenInterpreter::byJson),
                            new EntryCreationInfo<>(identityCodec, ComponentInfo.empty())
                        );
                    }
                })
                .build())
        );
        this.codecInterpreter = codecInterpreter;
    }

    private static final Codec<BigDecimal> BIG_DECIMAL_CODEC = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<BigDecimal> read(final DynamicOps<T> ops, final T input) {
            return ops
                .getNumberValue(input)
                .map(number -> number instanceof BigDecimal bigDecimal ? bigDecimal : new BigDecimal(number.toString()));
        }

        @Override
        public <T> T write(final DynamicOps<T> ops, final BigDecimal value) {
            return ops.createNumeric(value);
        }

        @Override
        public String toString() {
            return "BigDecimal";
        }
    };

    private static <T> LayoutElement byJson(Screen parentOuter, int widthOuter, EntryCreationContext contextOuter, JsonElement originalOuter, Consumer<JsonElement> updateOuter, EntryCreationInfo<T> creationInfoOuter, boolean handleOptionalOuter) {
        var entryHolder = new Object() {
            final EntryCreationInfo<JsonElement> jsonInfo = new EntryCreationInfo<>(
                Codec.PASSTHROUGH.xmap(d -> d.convert(contextOuter.ops()).getValue(), v -> new Dynamic<>(contextOuter.ops(), v)),
                ComponentInfo.empty()
            );
            final EntryCreationInfo<String> stringInfo = new EntryCreationInfo<>(
                Codec.STRING,
                ComponentInfo.empty()
            );
            final EntryCreationInfo<Number> numberInfo = new EntryCreationInfo<>(
                BIG_DECIMAL_CODEC.<Number>xmap(Function.identity(), number -> number instanceof BigDecimal bigDecimal ? bigDecimal : new BigDecimal(number.toString())),
                ComponentInfo.empty()
            );
            final EntryCreationInfo<Boolean> booleanInfo = new EntryCreationInfo<>(
                Codec.BOOL,
                ComponentInfo.empty()
            );
            final ConfigScreenEntry<String> stringEntry = ConfigScreenEntry.single(
                Widgets.text(DataResult::success, DataResult::success, false),
                stringInfo
            );
            final ConfigScreenEntry<Number> numberEntry = ConfigScreenEntry.single(
                Widgets.text(s -> {
                    if (s.isEmpty()) {
                        return DataResult.success(BigDecimal.ZERO);
                    }
                    try {
                        return DataResult.success(new BigDecimal(s));
                    } catch (NumberFormatException e) {
                        return DataResult.error(() -> "Not a number: "+s);
                    }
                }, n -> DataResult.success(n.toString()), s -> s.matches("^-?[0-9]*(\\.[0-9]*)?$"), false),
                numberInfo
            );
            final ConfigScreenEntry<Boolean> booleanEntry = ConfigScreenEntry.single(
                Widgets.bool(),
                booleanInfo
            );
            static final Gson GSON = new GsonBuilder().create();
            final ConfigScreenEntry<JsonElement> rawJsonEntry = ConfigScreenEntry.single(
                Widgets.text(s -> {
                    try {
                        return DataResult.success(GSON.fromJson(s, JsonElement.class));
                    } catch (JsonSyntaxException e) {
                        return DataResult.error(() -> "Invalid JSON `"+s+"`: "+e.getMessage());
                    }
                }, n -> DataResult.success(n.toString()), false),
                jsonInfo
            );
            final ConfigScreenEntry<JsonElement> jsonEntry = ConfigScreenEntry.single(
                (parent, width, context, original, update, creationInfo, handleOptional) -> {
                    enum JsonType {
                        OBJECT("codecextras.config.json.object"),
                        ARRAY("codecextras.config.json.array"),
                        STRING("codecextras.config.json.string"),
                        NUMBER("codecextras.config.json.number"),
                        BOOLEAN("codecextras.config.json.boolean"),
                        RAW("codecextras.config.json.raw");

                        private final Component component;


                        JsonType(String translationKey) {
                            component = Component.translatable(translationKey);
                        }

                        static @Nullable JsonType of(JsonElement element) {
                            if (element.isJsonObject()) {
                                return OBJECT;
                            }
                            if (element.isJsonArray()) {
                                return ARRAY;
                            }
                            if (element.isJsonPrimitive()) {
                                var primitive = element.getAsJsonPrimitive();
                                if (primitive.isString()) {
                                    return STRING;
                                }
                                if (primitive.isNumber()) {
                                    return NUMBER;
                                }
                                if (primitive.isBoolean()) {
                                    return BOOLEAN;
                                }
                            }
                            return null;
                        }
                    }

                    if (original.isJsonNull()) {
                        original = new JsonObject();
                        if (!handleOptional) {
                            update.accept(original);
                        }
                    }

                    var remainingWidth = width - Button.DEFAULT_HEIGHT - 5;
                    JsonType[] type = new JsonType[] {JsonType.of(original)};
                    if (type[0] == null) {
                        type[0] = JsonType.OBJECT;
                    }
                    Map<JsonType, JsonElement> elements = new EnumMap<>(JsonType.class);
                    elements.put(JsonType.RAW, original);
                    elements.put(JsonType.OBJECT, new JsonObject());
                    elements.put(JsonType.ARRAY, new JsonArray());
                    elements.put(JsonType.STRING, new JsonPrimitive(""));
                    elements.put(JsonType.NUMBER, new JsonPrimitive(0));
                    elements.put(JsonType.BOOLEAN, new JsonPrimitive(false));
                    elements.put(type[0], original);
                    var outerEntryHolder = this;
                    var holder = new Object() {
                        private final Runnable onTypeUpdate = () -> {
                            var currentType = type[0];
                            var component = currentType.component;
                            var tooltip = Tooltip.create(component);
                            this.cycle.setTooltip(tooltip);
                            for (var entry : this.layouts.entrySet()) {
                                var layout = entry.getValue();
                                if (entry.getKey() == currentType) {
                                    layout.visitWidgets(w -> {
                                        w.visible = true;
                                        w.active = true;
                                    });
                                } else {
                                    layout.visitWidgets(w -> {
                                        w.visible = false;
                                        w.active = false;
                                    });
                                }
                            }
                        };
                        private final EntryCreationContext.ProblemMarker[] problems = new EntryCreationContext.ProblemMarker[1];
                        private final Consumer<JsonElement> checkedUpdate = newJsonValue -> creationInfo.codec().parse(context.ops(), newJsonValue).ifError(error -> {
                            problems[0] = context.problem(problems[0], "Coult not encode: "+error.message());
                        }).ifSuccess(json -> {
                            context.resolve(problems[0]);
                            update.accept(json);
                        });
                        private final CycleButton<JsonType> cycle = CycleButton.<JsonType>builder(t -> Component.empty())
                            .withValues(List.of(JsonType.RAW, JsonType.OBJECT, JsonType.ARRAY, JsonType.STRING, JsonType.NUMBER, JsonType.BOOLEAN))
                            .withInitialValue(type[0] == null ? JsonType.OBJECT : type[0])
                            .displayOnlyValue()
                            .create(0, 0, Button.DEFAULT_HEIGHT, Button.DEFAULT_HEIGHT, Component.translatable("codecextras.config.json.type"), (b, t) -> {
                                type[0] = t;
                                onTypeUpdate.run();
                                checkedUpdate.accept(elements.get(type[0]));
                            });
                        private final Map<JsonType, LayoutElement> layouts = new EnumMap<>(JsonType.class);

                        {
                            layouts.put(JsonType.OBJECT, Button.builder(Component.translatable("codecextras.config.configurerecord"), b -> {
                                Minecraft.getInstance().setScreen(ScreenEntryProvider.create(
                                    new UnboundedMapScreenEntryProvider<>(stringEntry, jsonEntry, context, elements.get(JsonType.OBJECT), newJsonValue -> {
                                        elements.put(JsonType.OBJECT, newJsonValue);
                                        checkedUpdate.accept(newJsonValue);
                                    }), parent, context, creationInfo.componentInfo()
                                ));
                            }).width(remainingWidth).build());
                            layouts.put(JsonType.ARRAY, Button.builder(Component.translatable("codecextras.config.configurelist"), b -> {
                                Minecraft.getInstance().setScreen(ScreenEntryProvider.create(
                                    new ListScreenEntryProvider<>(jsonEntry, context, elements.get(JsonType.ARRAY), newJsonValue -> {
                                        elements.put(JsonType.ARRAY, newJsonValue);
                                        checkedUpdate.accept(newJsonValue);
                                    }), parent, context, creationInfo.componentInfo()
                                ));
                            }).width(remainingWidth).build());
                            layouts.put(JsonType.STRING, stringEntry.layout().create(parent, remainingWidth, context, elements.get(JsonType.STRING), newJsonValue -> {
                                elements.put(JsonType.STRING, newJsonValue);
                                checkedUpdate.accept(newJsonValue);
                            }, outerEntryHolder.stringInfo, handleOptional));
                            layouts.put(JsonType.NUMBER, numberEntry.layout().create(parent, remainingWidth, context, elements.get(JsonType.NUMBER), newJsonValue -> {
                                elements.put(JsonType.NUMBER, newJsonValue);
                                checkedUpdate.accept(newJsonValue);
                            }, outerEntryHolder.numberInfo, handleOptional));
                            layouts.put(JsonType.BOOLEAN, booleanEntry.layout().create(parent, remainingWidth, context, elements.get(JsonType.BOOLEAN), newJsonValue -> {
                                elements.put(JsonType.BOOLEAN, newJsonValue);
                                checkedUpdate.accept(newJsonValue);
                            }, outerEntryHolder.booleanInfo, handleOptional));
                            layouts.put(JsonType.RAW, outerEntryHolder.rawJsonEntry.layout().create(parent, remainingWidth, context, elements.get(JsonType.RAW), newJsonValue -> {
                                elements.put(JsonType.RAW, newJsonValue);
                                checkedUpdate.accept(newJsonValue);
                            }, outerEntryHolder.jsonInfo, handleOptional));
                            onTypeUpdate.run();
                        }
                    };
                    var layout = new EqualSpacingLayout(width, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
                    var frame = new FrameLayout(remainingWidth, Button.DEFAULT_HEIGHT);
                    for (var entry : holder.layouts.entrySet()) {
                        var layoutElement = entry.getValue();
                        frame.addChild(layoutElement, LayoutSettings.defaults().alignVerticallyMiddle());
                    }
                    layout.addChild(holder.cycle, LayoutSettings.defaults().alignVerticallyMiddle());
                    layout.addChild(frame, LayoutSettings.defaults().alignVerticallyMiddle());
                    return layout;
                },
                jsonInfo
            );
        };
        Codec<JsonElement> jsonWrappingCodec = entryHolder.jsonInfo.codec().validate(json -> creationInfoOuter.codec().parse(contextOuter.ops(), json).map(t -> json));
        return entryHolder.jsonEntry.layout().create(
            parentOuter, widthOuter, contextOuter, originalOuter, updateOuter, creationInfoOuter.withCodec(jsonWrappingCodec), handleOptionalOuter
        );
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
    public Stream<KeyConsumer<?, ConfigScreenEntry.Mu>> keyConsumers() {
        return Stream.of(
            new KeyConsumer<ConfigScreenEntry.Mu, ConfigScreenEntry.Mu>() {
                @Override
                public Key<ConfigScreenEntry.Mu> key() {
                    return KEY;
                }

                @Override
                public <T> App<ConfigScreenEntry.Mu, T> convert(App<ConfigScreenEntry.Mu, T> input) {
                    return input;
                }
            }
        );
    }

    @Override
    public <L, R> DataResult<App<ConfigScreenEntry.Mu, Either<L, R>>> either(App<ConfigScreenEntry.Mu, L> left, App<ConfigScreenEntry.Mu, R> right) {
        var codecLeft = ConfigScreenEntry.unbox(left).entryCreationInfo().codec();
        var codecRight = ConfigScreenEntry.unbox(right).entryCreationInfo().codec();
        var codecResult = codecInterpreter.either(new CodecInterpreter.Holder<>(codecLeft), new CodecInterpreter.Holder<>(codecRight)).map(CodecInterpreter::unbox);
        if (codecResult.isError()) {
            return DataResult.error(codecResult.error().orElseThrow().messageSupplier());
        }
        return DataResult.success(ConfigScreenEntry.single(
            Widgets.either(
                ConfigScreenEntry.unbox(left).layout(),
                ConfigScreenEntry.unbox(right).layout()
            ),
            new EntryCreationInfo<>(codecResult.getOrThrow(), ComponentInfo.empty())
        ));
    }

    @Override
    public <A> DataResult<App<ConfigScreenEntry.Mu, A>> bounded(App<ConfigScreenEntry.Mu, A> input, Supplier<Set<A>> values) {
        var codec = codecInterpreter.bounded(new CodecInterpreter.Holder<>(ConfigScreenEntry.unbox(input).entryCreationInfo().codec()), values).map(CodecInterpreter::unbox);
        if (codec.isError()) {
            return DataResult.error(codec.error().orElseThrow().messageSupplier());
        }
        return DataResult.success(ConfigScreenEntry.single(
            (parent, width, context, original, update, creationInfo, handleOptional) -> {
                List<A> knownValues = new ArrayList<>();
                Map<A, String> stringValues = new HashMap<>();
                Map<String, A> inverse = new HashMap<>();
                for (var value : values.get()) {
                    var encoded = codec.getOrThrow().encodeStart(context.ops(), value);
                    if (encoded.error().isPresent()) {
                        LOGGER.error("Error encoding value `{}`: {}", value, encoded.error().get());
                        continue;
                    }
                    String string;
                    var result = encoded.getOrThrow();
                    if (result.isJsonPrimitive()) {
                        string = result.getAsString();
                    } else {
                        string = result.toString();
                    }
                    knownValues.add(value);
                    stringValues.put(value, string);
                    inverse.put(string, value);
                }
                var wrapped = Widgets.pickWidget(new StringRepresentation<>(() -> knownValues, stringValues::get, inverse::get, false));
                return wrapped.create(parent, width, context, original, update, creationInfo, handleOptional);
            },
            ConfigScreenEntry.unbox(input).entryCreationInfo().withCodec(codec.getOrThrow())
        ));
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
        ScreenEntryFactory<List<A>> factory = (context, original, onClose, creationInfo) ->
            new ListScreenEntryProvider<>(unwrapped, context, original, onClose);
        return DataResult.success(new ConfigScreenEntry<>(
            Widgets.wrapWithOptionalHandling((parent, width, context, original, update, creationInfo, handleOptional) -> {
                if (original.isJsonNull()) {
                    original = new JsonArray();
                    if (!handleOptional) {
                        update.accept(original);
                    }
                }
                JsonElement[] finalOriginal = new JsonElement[] {original};
                var subContext = context.subContext();
                return Button.builder(
                    Component.translatable("codecextras.config.configurelist"),
                    b -> Minecraft.getInstance().setScreen(ScreenEntryProvider.create(factory.openChecked(
                        subContext, finalOriginal[0], value -> {
                            finalOriginal[0] = value;
                            update.accept(value);
                        }, creationInfo
                    ), parent, subContext, creationInfo.componentInfo()))
                ).width(width).build();
            }),
            factory,
            new EntryCreationInfo<>(codecResult.getOrThrow(), ComponentInfo.empty())
        ));
    }

    @Override
    public <K, V> DataResult<App<ConfigScreenEntry.Mu, Map<K, V>>> unboundedMap(App<ConfigScreenEntry.Mu, K> key, App<ConfigScreenEntry.Mu, V> value) {
        var unwrappedKey = ConfigScreenEntry.unbox(key);
        var unwrappedValue = ConfigScreenEntry.unbox(value);
        var codecResult = codecInterpreter.unboundedMap(new CodecInterpreter.Holder<>(unwrappedKey.entryCreationInfo().codec()), new CodecInterpreter.Holder<>(unwrappedValue.entryCreationInfo().codec())).map(CodecInterpreter::unbox);
        if (codecResult.isError()) {
            return DataResult.error(codecResult.error().orElseThrow().messageSupplier());
        }
        ScreenEntryFactory<Map<K, V>> factory = (context, original, onClose, creationInfo) ->
            new UnboundedMapScreenEntryProvider<>(unwrappedKey, unwrappedValue, context, original, onClose);
        return DataResult.success(new ConfigScreenEntry<>(
            Widgets.wrapWithOptionalHandling((parent, width, context, original, update, creationInfo, handleOptional) -> {
                if (original.isJsonNull()) {
                    original = new JsonObject();
                    if (!handleOptional) {
                        update.accept(original);
                    }
                }
                JsonElement[] finalOriginal = new JsonElement[] {original};
                var subContext = context.subContext();
                return Button.builder(
                    Component.translatable("codecextras.config.configurelist"),
                    b -> Minecraft.getInstance().setScreen(ScreenEntryProvider.create(factory.openChecked(
                        subContext, finalOriginal[0], jsonValue -> {
                            finalOriginal[0] = jsonValue;
                            update.accept(jsonValue);
                        }, creationInfo
                    ), parent, subContext, creationInfo.componentInfo()))
                ).width(width).build();
            }),
            factory,
            new EntryCreationInfo<>(codecResult.getOrThrow(), ComponentInfo.empty())
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
        ScreenEntryFactory<A> factory = (context, original, onClose, creationInfo) ->
            new RecordScreenEntryProvider(entries, context, original, onClose);
        var codecResult = codecInterpreter.record(fields, creator);
        if (codecResult.isError()) {
            return DataResult.error(() -> "Error creating record codec: "+codecResult.error().orElseThrow().messageSupplier());
        }
        return DataResult.success(new ConfigScreenEntry<>(
            Widgets.wrapWithOptionalHandling((parent, width, context, original, update, creationInfo, handleOptional) -> {
                if (original.isJsonNull()) {
                    original = new JsonObject();
                    if (!handleOptional) {
                        update.accept(original);
                    }
                }
                JsonElement[] finalOriginal = new JsonElement[] {original};
                var subContext = context.subContext();
                return Button.builder(
                    Component.translatable("codecextras.config.configurerecord"),
                    b -> Minecraft.getInstance().setScreen(ScreenEntryProvider.create(factory.openChecked(
                        subContext, finalOriginal[0], value -> {
                            finalOriginal[0] = value;
                            update.accept(value);
                        }, creationInfo
                    ), parent, subContext, creationInfo.componentInfo()))
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
    public <A, B> DataResult<App<ConfigScreenEntry.Mu, B>> flatXmap(App<ConfigScreenEntry.Mu, A> input, Function<A, DataResult<B>> to, Function<B, DataResult<A>> from) {
        var original = ConfigScreenEntry.unbox(input);
        var codecOriginal = original.entryCreationInfo().codec();
        var codecMapped = codecInterpreter.flatXmap(new CodecInterpreter.Holder<>(codecOriginal), to, from).map(CodecInterpreter::unbox);
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
                .get(annotations, ConfigAnnotations.DESCRIPTION)
                .or(() -> Annotation.get(annotations, Annotation.DESCRIPTION).map(Component::literal))
                .or(() -> Annotation.get(annotations, Annotation.COMMENT).map(Component::literal))
                .map(description -> withTitle.withComponentInfo(info -> info.withDescription(description)))
                .orElse(withTitle);
        });
    }

    @Override
    public <E, A> DataResult<App<ConfigScreenEntry.Mu, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Supplier<Set<A>> keys, Function<A, DataResult<Structure<? extends E>>> structures) {
        var keyResult = interpret(keyStructure).map(entry -> entry.withComponentInfo(info -> info.fallbackTitle(Component.literal(key))));
        if (keyResult.error().isPresent()) {
            return DataResult.error(keyResult.error().get().messageSupplier());
        }
        Supplier<Map<A, Supplier<DataResult<ConfigScreenEntry<? extends E>>>>> entries = Suppliers.memoize(() -> {
            Map<A, Supplier<DataResult<ConfigScreenEntry<? extends E>>>> map = new HashMap<>();
            for (var entryKey : keys.get()) {
                map.put(entryKey, Suppliers.memoize(() -> structures.apply(entryKey).flatMap(it -> it.interpret(this)).map(ConfigScreenEntry::unbox)));
            }
            return map;
        });
        var codecResult = codecInterpreter.dispatch(key, keyStructure, function, keys, structures);
        if (codecResult.isError()) {
            return DataResult.error(() -> "Error creating dispatch codec: "+codecResult.error().orElseThrow().messageSupplier());
        }
        ScreenEntryFactory<E> factory = (context, original, onClose, creationInfo) ->
            new DispatchScreenEntryProvider<>(keyResult.getOrThrow(), original, key, onClose, context, entries.get());
        return DataResult.success(new ConfigScreenEntry<>(
            Widgets.wrapWithOptionalHandling((parent, width, context, original, update, creationInfo, handleOptional) -> {
                if (original.isJsonNull()) {
                    original = new JsonObject();
                    if (!handleOptional) {
                        update.accept(original);
                    }
                }
                JsonElement[] finalOriginal = new JsonElement[] {original};
                var subContext = context.subContext();
                return Button.builder(
                    Component.translatable("codecextras.config.configurerecord"),
                    b -> Minecraft.getInstance().setScreen(ScreenEntryProvider.create(factory.openChecked(
                        subContext, finalOriginal[0], value -> {
                            finalOriginal[0] = value;
                            update.accept(value);
                        }, creationInfo
                    ), parent, subContext, creationInfo.componentInfo()))
                ).width(width).build();
            }),
            factory,
            new EntryCreationInfo<>(CodecInterpreter.unbox(codecResult.getOrThrow()), ComponentInfo.empty())
        ));
    }

    @Override
    public <K, V> DataResult<App<ConfigScreenEntry.Mu, Map<K, V>>> dispatchedMap(Structure<K> keyStructure, Supplier<Set<K>> keys, Function<K, DataResult<Structure<? extends V>>> valueStructures) {
        var keyResult = interpret(keyStructure);
        if (keyResult.error().isPresent()) {
            return DataResult.error(keyResult.error().get().messageSupplier());
        }
        Supplier<Map<K, Supplier<DataResult<ConfigScreenEntry<? extends V>>>>> entries = Suppliers.memoize(() -> {
            Map<K, Supplier<DataResult<ConfigScreenEntry<? extends V>>>> map = new HashMap<>();
            for (var entryKey : keys.get()) {
                map.put(entryKey, Suppliers.memoize(() -> valueStructures.apply(entryKey).flatMap(it -> it.interpret(this)).map(ConfigScreenEntry::unbox)));
            }
            return map;
        });
        var codecResult = codecInterpreter.dispatchedMap(keyStructure, keys, valueStructures);
        if (codecResult.isError()) {
            return DataResult.error(() -> "Error creating dispatch codec: "+codecResult.error().orElseThrow().messageSupplier());
        }
        ScreenEntryFactory<Map<K, V>> factory = (context, original, onClose, creationInfo) ->
            new DispatchedMapScreenEntryProvider<>(keyResult.getOrThrow(), original, onClose, context, entries.get());
        return DataResult.success(new ConfigScreenEntry<>(
            Widgets.wrapWithOptionalHandling((parent, width, context, original, update, creationInfo, handleOptional) -> {
                if (original.isJsonNull()) {
                    original = new JsonObject();
                    if (!handleOptional) {
                        update.accept(original);
                    }
                }
                JsonElement[] finalOriginal = new JsonElement[] {original};
                var subContext = context.subContext();
                return Button.builder(
                    Component.translatable("codecextras.config.configurerecord"),
                    b -> Minecraft.getInstance().setScreen(ScreenEntryProvider.create(factory.openChecked(
                        subContext, finalOriginal[0], value -> {
                            finalOriginal[0] = value;
                            update.accept(value);
                        }, creationInfo
                    ), parent, subContext, creationInfo.componentInfo()))
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
