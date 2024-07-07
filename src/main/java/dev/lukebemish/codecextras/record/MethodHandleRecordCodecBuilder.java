package dev.lukebemish.codecextras.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

@ApiStatus.Experimental
public final class MethodHandleRecordCodecBuilder<A> {
    private final List<Field<A, ?>> fields;

    private MethodHandleRecordCodecBuilder(List<Field<A, ?>> fields) {
        this.fields = fields;
    }

    public static <A> MethodHandleRecordCodecBuilder<A> start() {
        return new MethodHandleRecordCodecBuilder<>(List.of());
    }

    public <T> MethodHandleRecordCodecBuilder<A> with(MapCodec<T> codec, Function<A, T> getter) {
        var newFields = new ArrayList<>(this.fields);
        newFields.add(new Field<>(codec, getter));
        return new MethodHandleRecordCodecBuilder<>(newFields);
    }

    public Codec<A> buildWithConstructor(MethodHandles.Lookup lookup, Class<?> clazz) {
        return buildMapWithConstructor(lookup, clazz).codec();
    }

    public MapCodec<A> buildMapWithConstructor(MethodHandles.Lookup lookup, Class<?> clazz) {
        return buildMap(() -> {
            var ctors =Arrays.stream(clazz.getDeclaredConstructors()).filter(c -> c.getParameterCount() == fields.size()).toList();
            if (ctors.isEmpty()) {
                throw new IllegalArgumentException("No constructor with " + fields.size() + " parameters found");
            } else if (ctors.size() > 1) {
                throw new IllegalArgumentException("Multiple constructors with " + fields.size() + " parameters found");
            }
            return lookup.unreflectConstructor(ctors.get(0));
        });
    }

    public Codec<A> build(HandleSupplier constructor) {
        return buildMap(constructor).codec();
    }

    @SuppressWarnings("unchecked")
    public MapCodec<A> buildMap(HandleSupplier constructor) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        var className = Type.getInternalName(MethodHandleRecordCodecBuilder.class) + "$Generated";
        cw.visit(
            Opcodes.V17,
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            className,
            null,
            Type.getInternalName(MapCodec.class),
            new String[0]
        );

        MethodHandle handle;
        try {
            MethodHandle originalHandle = constructor.makeHandle();
            if (fields.size() != originalHandle.type().parameterCount()) {
                throw new IllegalArgumentException("Handle must have the same number of parameters as fields");
            }
            List<Class<?>> params = new ArrayList<>();
            for (var ignored : fields) {
                params.add(Object.class);
            }
            handle = originalHandle.asType(
                MethodType.methodType(Object.class, params)
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        List<Object> dataPartial = new ArrayList<>();
        dataPartial.add(handle);
        dataPartial.addAll(fields);

        var ctor = cw.visitMethod(
            0,
            "<init>",
            MethodType.methodType(void.class).descriptorString(),
            null,
            null
        );
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(MapCodec.class), "<init>", MethodType.methodType(void.class).descriptorString(), false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        var keys = cw.visitMethod(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            "keys",
            MethodType.methodType(Stream.class, DynamicOps.class).descriptorString(),
            null,
            null
        );
        keys.visitCode();
        keys.visitLdcInsn(fields.size());
        keys.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Stream.class));
        for (int i = 0; i < fields.size(); i++) {
            keys.visitInsn(Opcodes.DUP);
            keys.visitLdcInsn(i);
            keys.visitLdcInsn(conDyn(Type.getDescriptor(Field.class), i + 1));
            keys.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Field.class), "codec", MethodType.methodType(MapCodec.class).descriptorString(), false);
            keys.visitVarInsn(Opcodes.ALOAD, 1);
            keys.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(MapCodec.class), "keys", MethodType.methodType(Stream.class, DynamicOps.class).descriptorString(), false);
            keys.visitInsn(Opcodes.AASTORE);
        }
        keys.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Arrays.class), "stream", MethodType.methodType(Stream.class, Object[].class).descriptorString(), false);
        keys.visitInsn(Opcodes.ARETURN);
        keys.visitMaxs(0, 0);
        keys.visitEnd();

        var decode = cw.visitMethod(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            "decode",
            MethodType.methodType(DataResult.class, DynamicOps.class, MapLike.class).descriptorString(),
            null,
            null
        );
        decode.visitCode();
        for (int i = 0; i < fields.size(); i++) {
            decode.visitLdcInsn(conDyn(Type.getDescriptor(Field.class), i + 1));
            decode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Field.class), "codec", MethodType.methodType(MapCodec.class).descriptorString(), false);
            decode.visitVarInsn(Opcodes.ALOAD, 1);
            decode.visitVarInsn(Opcodes.ALOAD, 2);
            decode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(MapCodec.class), "decode", MethodType.methodType(DataResult.class, DynamicOps.class, MapLike.class).descriptorString(), false);
            decode.visitInsn(Opcodes.DUP);
            decode.visitVarInsn(Opcodes.ASTORE, 3);
            decode.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DataResult.class), "isError", MethodType.methodType(boolean.class).descriptorString(), true);
            var postLabel = new Label();
            decode.visitJumpInsn(Opcodes.IFEQ, postLabel);

            decode.visitVarInsn(Opcodes.ALOAD, 3);
            decode.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(DataResult.Error.class));
            decode.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(MethodHandleRecordCodecBuilder.class), "withError", MethodType.methodType(DataResult.class, DataResult.Error.class).descriptorString(), false);
            decode.visitInsn(Opcodes.ARETURN);

            decode.visitLabel(postLabel);
            decode.visitVarInsn(Opcodes.ALOAD, 3);
            decode.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DataResult.class), "getOrThrow", MethodType.methodType(Object.class).descriptorString(), true);
        }
        decode.visitInvokeDynamicInsn(
            "asCallSite",
            handle.type().descriptorString(),
            new Handle(
                Opcodes.H_INVOKESTATIC,
                Type.getInternalName(MethodHandleRecordCodecBuilder.class),
                "asCallSite",
                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandle.class).descriptorString(),
                false
            ),
            conDyn(Type.getDescriptor(MethodHandle.class), 0)
        );
        decode.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(DataResult.class), "success", MethodType.methodType(DataResult.class, Object.class).descriptorString(), true);
        decode.visitInsn(Opcodes.ARETURN);
        decode.visitMaxs(0, 0);
        decode.visitEnd();

        var encode = cw.visitMethod(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
            "encode",
            MethodType.methodType(RecordBuilder.class, Object.class, DynamicOps.class, RecordBuilder.class).descriptorString(),
            null,
            null
        );
        encode.visitCode();
        for (int i = 0; i < fields.size(); i++) {
            encode.visitLdcInsn(conDyn(Type.getDescriptor(Field.class), i + 1));
            encode.visitInsn(Opcodes.DUP);
            encode.visitVarInsn(Opcodes.ASTORE, 4);
            encode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Field.class), "codec", MethodType.methodType(MapCodec.class).descriptorString(), false);
            encode.visitVarInsn(Opcodes.ALOAD, 4);
            encode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Field.class), "getter", MethodType.methodType(Function.class).descriptorString(), false);
            encode.visitVarInsn(Opcodes.ALOAD, 1);
            encode.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Function.class), "apply", MethodType.methodType(Object.class, Object.class).descriptorString(), true);
            encode.visitVarInsn(Opcodes.ALOAD, 2);
            encode.visitVarInsn(Opcodes.ALOAD, 3);
            encode.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(MapCodec.class), "encode", MethodType.methodType(RecordBuilder.class, Object.class, DynamicOps.class, RecordBuilder.class).descriptorString(), false);
            encode.visitVarInsn(Opcodes.ASTORE, 3);
        }
        encode.visitVarInsn(Opcodes.ALOAD, 3);
        encode.visitInsn(Opcodes.ARETURN);
        encode.visitMaxs(0, 0);
        encode.visitEnd();

        cw.visitEnd();

        try {
            var lookup = MethodHandles.lookup().defineHiddenClassWithClassData(cw.toByteArray(), List.copyOf(dataPartial), true, MethodHandles.Lookup.ClassOption.NESTMATE);
            return (MapCodec<A>) lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static ConstantDynamic conDyn(String Descriptor, int i) {
        return new ConstantDynamic(
            "_",
            Descriptor,
            new Handle(
                Opcodes.H_INVOKESTATIC,
                Type.getInternalName(MethodHandles.class),
                "classDataAt",
                MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class, int.class).descriptorString(),
                false
            ),
            i
        );
    }

    @SuppressWarnings("unused")
    private static CallSite asCallSite(MethodHandles.Lookup ignoredLookup, String ignoredName, MethodType ignoredType, MethodHandle handle) {
        return new ConstantCallSite(handle);
    }

    @SuppressWarnings("unused")
    private static <A> DataResult<A> withError(DataResult.Error<?> original) {
        return DataResult.error(original::message);
    }

    public interface HandleSupplier {
        MethodHandle makeHandle() throws ReflectiveOperationException;
    }

    private record Field<A, T>(MapCodec<T> codec, Function<A, T> getter) {}
}
