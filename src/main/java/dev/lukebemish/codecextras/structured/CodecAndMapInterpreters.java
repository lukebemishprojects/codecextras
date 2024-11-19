package dev.lukebemish.codecextras.structured;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.App2;
import com.mojang.datafixers.kinds.K1;

class CodecAndMapInterpreters {
    private final CodecInterpreter codecInterpreter;
    private final MapCodecInterpreter mapCodecInterpreter;

    CodecAndMapInterpreters(
        Keys<CodecInterpreter.Holder.Mu, Object> codecKeys,
        Keys<MapCodecInterpreter.Holder.Mu, Object> mapCodecKeys,
        Keys2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1> parametricCodecKeys,
        Keys2<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1> parametricMapCodecKeys
    ) {
        this.mapCodecInterpreter = new MapCodecInterpreter(mapCodecKeys, parametricMapCodecKeys) {
            @Override
            protected CodecInterpreter codecInterpreter() {
                return CodecAndMapInterpreters.this.codecInterpreter;
            }
        };
        this.codecInterpreter = new CodecInterpreter(mapCodecKeys.<CodecInterpreter.Holder.Mu>map(new Keys.Converter<>() {
            @Override
            public <B> App<CodecInterpreter.Holder.Mu, B> convert(App<MapCodecInterpreter.Holder.Mu, B> app) {
                return new CodecInterpreter.Holder<>(MapCodecInterpreter.unbox(app).codec());
            }
        }).join(codecKeys), parametricMapCodecKeys.map(new Keys2.Converter<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1>() {
            @Override
            public <A extends K1, B extends K1> App2<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, A, B> convert(App2<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, A, B> input) {
                var unboxed = ParametricKeyedValue.unbox(input);
                return new ParametricKeyedValue<>() {
                    @Override
                    public <T> App<CodecInterpreter.Holder.Mu, App<B, T>> convert(App<A, T> parameter) {
                        var mapCodec = MapCodecInterpreter.unbox(unboxed.convert(parameter));
                        return new CodecInterpreter.Holder<>(mapCodec.codec());
                    }
                };
            }
        }).join(parametricCodecKeys)) {
            @Override
            protected MapCodecInterpreter mapCodecInterpreter() {
                return CodecAndMapInterpreters.this.mapCodecInterpreter;
            }
        };
    }

    CodecAndMapInterpreters() {
        this(
            Keys.<CodecInterpreter.Holder.Mu, Object>builder().build(),
            Keys.<MapCodecInterpreter.Holder.Mu, Object>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<CodecInterpreter.Holder.Mu>, K1, K1>builder().build(),
            Keys2.<ParametricKeyedValue.Mu<MapCodecInterpreter.Holder.Mu>, K1, K1>builder().build()
        );
    }

    public CodecInterpreter codecInterpreter() {
        return codecInterpreter;
    }

    public MapCodecInterpreter mapCodecInterpreter() {
        return mapCodecInterpreter;
    }
}
