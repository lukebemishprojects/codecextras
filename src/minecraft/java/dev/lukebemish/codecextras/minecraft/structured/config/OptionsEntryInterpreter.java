package dev.lukebemish.codecextras.minecraft.structured.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.lukebemish.codecextras.structured.CodecInterpreter;
import dev.lukebemish.codecextras.structured.Interpreter;
import dev.lukebemish.codecextras.structured.KeyStoringInterpreter;
import dev.lukebemish.codecextras.structured.Keys;
import dev.lukebemish.codecextras.structured.Keys2;
import dev.lukebemish.codecextras.structured.ParametricKeyedValue;
import dev.lukebemish.codecextras.structured.RecordStructure;
import dev.lukebemish.codecextras.structured.Structure;
import dev.lukebemish.codecextras.types.Identity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class OptionsEntryInterpreter extends KeyStoringInterpreter<OptionsEntry.Mu, OptionsEntryInterpreter> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final CodecInterpreter codecInterpreter;
    private final DynamicOps<JsonElement> dynamicOps;

    public OptionsEntryInterpreter(
        Keys<OptionsEntry.Mu, Object> keys,
        Keys2<ParametricKeyedValue.Mu<OptionsEntry.Mu>, K1, K1> parametricKeys,
        CodecInterpreter codecInterpreter,
        DynamicOps<JsonElement> dynamicOps
    ) {
        super(
            keys.join(Keys.<OptionsEntry.Mu, Object>builder()
                    .add(Interpreter.INT, OptionsEntry.single(
                        (parent, width, original, update, info) -> new EditBox(Minecraft.getInstance().font, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, info.title()) {
                            {
                                if (!original.isJsonPrimitive()) {
                                    if (!original.isJsonNull()) {
                                        LOGGER.warn("{} is not an integer", original);
                                    }
                                } else {
                                    try {
                                        var intValue = original.getAsJsonPrimitive().getAsInt();
                                        this.setValue(intValue+"");
                                    } catch (NumberFormatException ignored) {
                                        LOGGER.warn("{} is not an integer", original);
                                    }
                                }
                                this.setResponder(string -> {
                                    if (string.isEmpty()) {
                                        update.accept(JsonNull.INSTANCE);
                                        return;
                                    }
                                    try {
                                        var intValue = Integer.parseInt(string);
                                        update.accept(new JsonPrimitive(intValue));
                                    } catch (NumberFormatException ignored) {
                                        LOGGER.warn("{} is not an integer", original);
                                    }
                                });
                            }

                            @Override
                            public void insertText(String string) {
                                super.insertText(string.replaceAll("[^0-9\\-]+", ""));
                            }
                        },
                        ComponentInfo.empty()
                    ))
                .build()),
            parametricKeys.join(Keys2.<ParametricKeyedValue.Mu<OptionsEntry.Mu>, K1, K1>builder()
                .build())
        );
        this.codecInterpreter = codecInterpreter;
        this.dynamicOps = dynamicOps;
    }

    public OptionsEntryInterpreter(
        CodecInterpreter codecInterpreter,
        DynamicOps<JsonElement> dynamicOps
    ) {
        this(
            Keys.<OptionsEntry.Mu, Object>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<OptionsEntry.Mu>, K1, K1>builder().build(),
            codecInterpreter,
            dynamicOps
        );
    }

    @Override
    public OptionsEntryInterpreter with(Keys<OptionsEntry.Mu, Object> keys, Keys2<ParametricKeyedValue.Mu<OptionsEntry.Mu>, K1, K1> parametricKeys) {
        return new OptionsEntryInterpreter(keys().join(keys), parametricKeys().join(parametricKeys), this.codecInterpreter, this.dynamicOps);
    }

    @Override
    public <A> DataResult<App<OptionsEntry.Mu, List<A>>> list(App<OptionsEntry.Mu, A> single) {
        return DataResult.error(() -> "Not yet implemented");
    }

    @Override
    public <A> DataResult<App<OptionsEntry.Mu, A>> record(List<RecordStructure.Field<A, ?>> fields, Function<RecordStructure.Container, A> creator) {
        List<RecordEntry<?>> entries = new ArrayList<>();
        List<Supplier<String>> errors = new ArrayList<>();
        for (var field : fields) {
            handleEntry(field, entries, errors);
        }
        if (!errors.isEmpty()) {
            return DataResult.error(() -> "Errors crating record screen: "+errors.stream().map(Supplier::get).collect(Collectors.joining(", ")));
        }
        ScreenFactory factory = (parent, ops, original, onClose, componentInfo) ->
            new RecordConfigScreen(parent, componentInfo.title(), entries, ops, original, onClose);
        return DataResult.success(new OptionsEntry<>(
            (parent, width, original, update, componentInfo) -> Button.builder(
                    Component.translatable("codecextras.config.configurerecord"),
                    b -> {
                        Minecraft.getInstance().setScreen(factory.open(
                            parent, dynamicOps, original, update, componentInfo
                        ));
                    }
                ).width(Button.DEFAULT_WIDTH).build(),
            factory,
            ComponentInfo.empty()
        ));
    }

    private <A, T> void handleEntry(RecordStructure.Field<A,T> field, List<RecordEntry<?>> entries, List<Supplier<String>> errors) {
        var shouldEncode = field.missingBehavior().map(RecordStructure.Field.MissingBehavior::predicate).orElse(t -> true);
        var codecResult = codecInterpreter.interpret(field.structure());
        if (codecResult.isError()) {
            errors.add(codecResult.error().orElseThrow().messageSupplier());
            return;
        }
        var optionEntryResult = field.structure().interpret(this).map(OptionsEntry::unbox);
        if (optionEntryResult.isError()) {
            errors.add(optionEntryResult.error().orElseThrow().messageSupplier());
            return;
        }
        entries.add(new RecordEntry<>(
            field.name(),
            optionEntryResult.getOrThrow().withComponentInfo(info -> info.fallbackTitle(Component.literal(field.name()))),
            shouldEncode,
            codecResult.getOrThrow()
        ));
    }

    @Override
    public <A, B> DataResult<App<OptionsEntry.Mu, B>> flatXmap(App<OptionsEntry.Mu, A> input, Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer) {
        return DataResult.error(() -> "Not yet implemented");
    }

    @Override
    public <A> DataResult<App<OptionsEntry.Mu, A>> annotate(Structure<A> original, Keys<Identity.Mu, Object> annotations) {
        return DataResult.error(() -> "Not yet implemented");
    }

    @Override
    public <E, A> DataResult<App<OptionsEntry.Mu, E>> dispatch(String key, Structure<A> keyStructure, Function<? super E, ? extends DataResult<A>> function, Set<A> keys, Function<A, Structure<? extends E>> structures) {
        return DataResult.error(() -> "Not yet implemented");
    }

    public <A> DataResult<OptionsEntry<A>> interpret(Structure<A> structure) {
        return structure.interpret(this).map(OptionsEntry::unbox);
    }
}
