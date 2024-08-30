package dev.lukebemish.codecextras.minecraft.structured.config;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.lukebemish.codecextras.structured.Annotation;
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
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

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
                        VerifyingEditBox.of(string -> {
                            try {
                                return DataResult.success(Integer.parseInt(string));
                            } catch (NumberFormatException e) {
                                return DataResult.error(() -> "Not an integer: "+string);
                            }
                        }, integer -> DataResult.success(integer+""), string -> string.matches("-?[0-9]*"), true),
                        new EntryCreationInfo<>(Codec.INT, ComponentInfo.empty())
                    ))
                .build()),
            parametricKeys.join(Keys2.<ParametricKeyedValue.Mu<ConfigScreenEntry.Mu>, K1, K1>builder()
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

    @Override
    public ConfigScreenInterpreter with(Keys<ConfigScreenEntry.Mu, Object> keys, Keys2<ParametricKeyedValue.Mu<ConfigScreenEntry.Mu>, K1, K1> parametricKeys) {
        return new ConfigScreenInterpreter(keys().join(keys), parametricKeys().join(parametricKeys), this.codecInterpreter);
    }

    @Override
    public <A> DataResult<App<ConfigScreenEntry.Mu, List<A>>> list(App<ConfigScreenEntry.Mu, A> single) {
        return DataResult.error(() -> "Not yet implemented");
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
        ScreenFactory<A> factory = (parent, ops, original, onClose, creationInfo) ->
            new RecordConfigScreen<>(parent, creationInfo, entries, ops, original, onClose);
        var codecResult = codecInterpreter.record(fields, creator);
        if (codecResult.isError()) {
            return DataResult.error(() -> "Error creating record codec: "+codecResult.error().orElseThrow().messageSupplier());
        }
        return DataResult.success(new ConfigScreenEntry<>(
            (parent, width, ops, original, update, creationInfo) -> Button.builder(
                    Component.translatable("codecextras.config.configurerecord"),
                    b -> {
                        Minecraft.getInstance().setScreen(factory.open(
                            parent, ops, original, update, creationInfo
                        ));
                    }
                ).width(Button.DEFAULT_WIDTH).build(),
            factory,
            new EntryCreationInfo<>(CodecInterpreter.unbox(codecResult.getOrThrow()), ComponentInfo.empty())
        ));
    }

    private <A, T> void handleEntry(RecordStructure.Field<A,T> field, List<RecordEntry<?>> entries, List<Supplier<String>> errors) {
        var shouldEncode = field.missingBehavior().map(RecordStructure.Field.MissingBehavior::predicate).orElse(t -> true);
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
            shouldEncode,
            codecResult.getOrThrow()
        ));
    }

    @Override
    public <A, B> DataResult<App<ConfigScreenEntry.Mu, B>> flatXmap(App<ConfigScreenEntry.Mu, A> input, Function<A, DataResult<B>> deserializer, Function<B, DataResult<A>> serializer) {
        var original = ConfigScreenEntry.unbox(input);
        return DataResult.success(original.withEntryCreationInfo(
            info -> info.withCodec(codec -> codec.flatXmap(deserializer, serializer)),
            info -> info.withCodec(codec -> codec.flatXmap(serializer, deserializer))
        ));
    }

    @Override
    public <A> DataResult<App<ConfigScreenEntry.Mu, A>> annotate(Structure<A> original, Keys<Identity.Mu, Object> annotations) {
        var result = original.interpret(this);
        return result.map(app -> {
            var entry = ConfigScreenEntry.unbox(app);
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
        return DataResult.error(() -> "Not yet implemented");
    }

    public <A> DataResult<ConfigScreenEntry<A>> interpret(Structure<A> structure) {
        return structure.interpret(this).map(ConfigScreenEntry::unbox);
    }
}
