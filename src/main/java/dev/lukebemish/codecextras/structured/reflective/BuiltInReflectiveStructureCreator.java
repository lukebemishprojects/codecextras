package dev.lukebemish.codecextras.structured.reflective;

import com.google.auto.service.AutoService;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import dev.lukebemish.codecextras.structured.RecordStructure;
import dev.lukebemish.codecextras.structured.Structure;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@ApiStatus.Internal
@AutoService(ReflectiveStructureCreator.class)
public class BuiltInReflectiveStructureCreator implements ReflectiveStructureCreator {
    @Override
    public Map<Class<?>, Creator> creators() {
        return ImmutableMap.<Class<?>, Creator>builder()
            .put(Unit.class, () -> Structure.UNIT)
            .put(Boolean.class, () -> Structure.BOOL)
            .put(Byte.class, () -> Structure.BYTE)
            .put(Short.class, () -> Structure.SHORT)
            .put(Integer.class, () -> Structure.INT)
            .put(Long.class, () -> Structure.LONG)
            .put(Float.class, () -> Structure.FLOAT)
            .put(Double.class, () -> Structure.DOUBLE)
            .put(String.class, () -> Structure.STRING)
            .put(Character.class, () -> Structure.CHAR)
            .put(Dynamic.class, () -> Structure.PASSTHROUGH)
            // Primitives
            .put(Boolean.TYPE, () -> Structure.BOOL)
            .put(Byte.TYPE, () -> Structure.BYTE)
            .put(Short.TYPE, () -> Structure.SHORT)
            .put(Integer.TYPE, () -> Structure.INT)
            .put(Long.TYPE, () -> Structure.LONG)
            .put(Float.TYPE, () -> Structure.FLOAT)
            .put(Double.TYPE, () -> Structure.DOUBLE)
            .put(Character.TYPE, () -> Structure.CHAR)
            // Arrays
            .put(boolean[].class, () -> Structure.BOOL.listOf().xmap(list -> {
                var bools = new boolean[list.size()];
                for (int i = 0; i < bools.length; i++) {
                    bools[i] = list.get(i);
                }
                return bools;
            }, bools -> {
                var list = new ArrayList<Boolean>(bools.length);
                for (var b : bools) {
                    list.add(b);
                }
                return list;
            }))
            .put(byte[].class, () -> Structure.BYTE.listOf().xmap(list -> {
                var bytes = new byte[list.size()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = list.get(i);
                }
                return bytes;
            }, bytes -> {
                var list = new ArrayList<Byte>(bytes.length);
                for (var b : bytes) {
                    list.add(b);
                }
                return list;
            }))
            .put(short[].class, () -> Structure.SHORT.listOf().xmap(list -> {
                var shorts = new short[list.size()];
                for (int i = 0; i < shorts.length; i++) {
                    shorts[i] = list.get(i);
                }
                return shorts;
            }, shorts -> {
                var list = new ArrayList<Short>(shorts.length);
                for (var s : shorts) {
                    list.add(s);
                }
                return list;
            }))
            .put(int[].class, () -> Structure.INT.listOf().xmap(list -> {
                var ints = new int[list.size()];
                for (int i = 0; i < ints.length; i++) {
                    ints[i] = list.get(i);
                }
                return ints;
            }, ints -> {
                var list = new ArrayList<Integer>(ints.length);
                for (var i : ints) {
                    list.add(i);
                }
                return list;
            }))
            .put(long[].class, () -> Structure.LONG.listOf().xmap(list -> {
                var longs = new long[list.size()];
                for (int i = 0; i < longs.length; i++) {
                    longs[i] = list.get(i);
                }
                return longs;
            }, longs -> {
                var list = new ArrayList<Long>(longs.length);
                for (var l : longs) {
                    list.add(l);
                }
                return list;
            }))
            .put(float[].class, () -> Structure.FLOAT.listOf().xmap(list -> {
                var floats = new float[list.size()];
                for (int i = 0; i < floats.length; i++) {
                    floats[i] = list.get(i);
                }
                return floats;
            }, floats -> {
                var list = new ArrayList<Float>(floats.length);
                for (var f : floats) {
                    list.add(f);
                }
                return list;
            }))
            .put(double[].class, () -> Structure.DOUBLE.listOf().xmap(list -> {
                var doubles = new double[list.size()];
                for (int i = 0; i < doubles.length; i++) {
                    doubles[i] = list.get(i);
                }
                return doubles;
            }, doubles -> {
                var list = new ArrayList<Double>(doubles.length);
                for (var d : doubles) {
                    list.add(d);
                }
                return list;
            }))
            .put(char[].class, () -> Structure.CHAR.listOf().xmap(list -> {
                var chars = new char[list.size()];
                for (int i = 0; i < chars.length; i++) {
                    chars[i] = list.get(i);
                }
                return chars;
            }, chars -> {
                var list = new ArrayList<Character>(chars.length);
                for (var c : chars) {
                    list.add(c);
                }
                return list;
            }))
            .put(BigInteger.class, () -> Structure.BIG_INTEGER)
            .put(BigDecimal.class, () -> Structure.BIG_DECIMAL)
            .put(Duration.class, () -> Structure.DURATION)
            .put(Instant.class, () -> Structure.INSTANT)
            .put(LocalDate.class, () -> Structure.LOCAL_DATE)
            .put(LocalDateTime.class, () -> Structure.LOCAL_DATE_TIME)
            .put(LocalTime.class, () -> Structure.LOCAL_TIME)
            .put(MonthDay.class, () -> Structure.MONTH_DAY)
            .put(OffsetDateTime.class, () -> Structure.OFFSET_DATE_TIME)
            .put(OffsetTime.class, () -> Structure.OFFSET_TIME)
            .put(Period.class, () -> Structure.PERIOD)
            .put(Year.class, () -> Structure.YEAR)
            .put(YearMonth.class, () -> Structure.YEAR_MONTH)
            .put(ZonedDateTime.class, () -> Structure.ZONED_DATE_TIME)
            .put(ZoneId.class, () -> Structure.ZONE_ID)
            .put(ZoneOffset.class, () -> Structure.ZONE_OFFSET)
            .build();
    }

    @Override
    public Map<Class<?>, ParameterizedCreator> parameterizedCreators() {
        return ImmutableMap.<Class<?>, ParameterizedCreator>builder()
            .put(Either.class, parameters -> Structure.unboundedMap(parameters[0].create(), parameters[1].create()))
            // Collections
            .put(Collection.class, collectionMaker(ArrayList::new))
            .put(SequencedCollection.class, collectionMaker(ArrayList::new))
            .put(Deque.class, collectionMaker(ArrayDeque::new))
            .put(Set.class, collectionMaker(LinkedHashSet::new))
            .put(NavigableSet.class, collectionMaker(TreeSet::new))
            .put(SequencedSet.class, collectionMaker(LinkedHashSet::new))
            .put(SortedSet.class, collectionMaker(TreeSet::new))
            .put(Queue.class, collectionMaker(ArrayDeque::new))
            .put(List.class, collectionMaker(ArrayList::new))
            .put(BlockingDeque.class, collectionMaker(LinkedBlockingDeque::new))
            .put(BlockingQueue.class, collectionMaker(LinkedBlockingQueue::new))
            .put(TransferQueue.class, collectionMaker(LinkedTransferQueue::new))
            .put(ImmutableList.class, collectionMaker(ImmutableList::copyOf))
            // Map-likes
            .put(Map.class, mapMaker(LinkedHashMap::new))
            .put(NavigableMap.class, mapMaker(TreeMap::new))
            .put(SortedMap.class, mapMaker(TreeMap::new))
            .put(SequencedMap.class, mapMaker(LinkedHashMap::new))
            .put(ConcurrentMap.class, mapMaker(ConcurrentHashMap::new))
            .put(ConcurrentNavigableMap.class, mapMaker(ConcurrentSkipListMap::new))
            .put(ImmutableMap.class, mapMaker(ImmutableMap::copyOf))
            .build();
    }

    @SuppressWarnings({"rawtypes", "Convert2MethodRef", "unchecked"})
    private static <T extends Collection> ParameterizedCreator collectionMaker(Function<List<?>, T> function) {
        return parameters -> parameters[0].create().listOf().xmap(function::apply, c -> new ArrayList<>(c));
    }

    @SuppressWarnings({"rawtypes", "Convert2MethodRef", "unchecked"})
    private static <T extends Map> ParameterizedCreator mapMaker(Function<Map<?, ?>, T> function) {
        return parameters -> Structure.unboundedMap(parameters[0].create(), parameters[1].create()).xmap(function::apply, c -> new LinkedHashMap<>(c));
    }

    private static ConstantDynamic conDyn(String descriptor, int i) {
        return new ConstantDynamic(
            "_",
            descriptor,
            new Handle(
                Opcodes.H_INVOKESTATIC,
                org.objectweb.asm.Type.getInternalName(MethodHandles.class),
                "classDataAt",
                MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class, int.class).descriptorString(),
                false
            ),
            i
        );
    }

    public static <T, R> Function<T, R> functionWrapper(MethodHandle getter) {
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        var name = BuiltInReflectiveStructureCreator.class.getName().replace('.', '/') + "$GetterWrapper";
        writer.visit(Opcodes.V21, Opcodes.ACC_FINAL, name, null, "java/lang/Object", new String[]{"java/util/function/Function"});
        var mv = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = writer.visitMethod(Opcodes.ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        invokeAsCallSite(mv, "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        writer.visitEnd();
        var bytes = writer.toByteArray();
        try {
            var lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, List.of(getter), true, MethodHandles.Lookup.ClassOption.NESTMATE);
            @SuppressWarnings("unchecked") var instance = (Function<T, R>) lookup.lookupClass().getConstructor().newInstance();
            return instance;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokeAsCallSite(MethodVisitor mv, String descriptor, int index) {
        mv.visitInvokeDynamicInsn(
            "asCallSite",
            descriptor,
            new Handle(
                Opcodes.H_INVOKESTATIC,
                org.objectweb.asm.Type.getInternalName(BuiltInReflectiveStructureCreator.class),
                "asCallSite",
                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandle.class).descriptorString(),
                false
            ),
            conDyn(org.objectweb.asm.Type.getDescriptor(MethodHandle.class), index)
        );
    }

    private static CallSite asCallSite(MethodHandles.Lookup ignoredLookup, String ignoredName, MethodType type, MethodHandle handle) {
        return new ConstantCallSite(handle.asType(type));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Function<RecordStructure.Container, ?> add(RecordStructure<?> builder, String name, Type type, Function<?, Object> getter, Function<Type, Structure<?>> creator) {
        if (type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() instanceof Class<?> rawType) {
            if (rawType.equals(Optional.class)) {
                var innerType = parameterizedType.getActualTypeArguments()[0];
                return builder.addOptional(name, (Structure) creator.apply(innerType), (Function) getter);
            }
        } else if (type instanceof Class<?> clazz) {
            if (clazz.equals(OptionalInt.class)) {
                return builder.addOptionalInt(name, (Function) getter);
            } else if (clazz.equals(OptionalDouble.class)) {
                return builder.addOptionalDouble(name, (Function) getter);
            } else if (clazz.equals(OptionalLong.class)) {
                return builder.addOptionalLong(name, (Function) getter);
            }
        }
        Function<RecordStructure.Container, Optional<?>> key = builder.addOptional(name, (Structure) creator.apply(type), (Function) getter.andThen(Optional::ofNullable));
        return key.andThen(o -> o.orElse(null));
    }

    @Override
    public List<FlexibleCreator> flexibleCreators() {
        return ImmutableList.<FlexibleCreator>builder()
            .add(new FlexibleCreator() {
                @SuppressWarnings({"unchecked", "rawtypes"})
                @Override
                public Structure<?> create(Class<?> exact, TypedCreator[] parameters, Function<Type, Structure<?>> creator) {
                    try {
                        var ctor = exact.getConstructor(Collection.class);
                        var function = functionWrapper(MethodHandles.lookup().unreflectConstructor(ctor));
                        return parameters[0].create().listOf().xmap(function::apply, c -> new ArrayList((Collection) c));
                    } catch (NoSuchMethodException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public boolean supports(Class<?> exact, TypedCreator[] parameters) {
                    if (parameters.length == 1 && Collection.class.isAssignableFrom(exact)) {
                        try {
                            var ctor = exact.getConstructor(Collection.class);
                            return ctor.accessFlags().contains(AccessFlag.PUBLIC);
                        } catch (NoSuchMethodException e) {
                            return false;
                        }
                    }
                    return false;
                }
            })
            .add(new FlexibleCreator() {
                @SuppressWarnings({"unchecked", "rawtypes"})
                @Override
                public Structure<?> create(Class<?> exact, TypedCreator[] parameters, Function<Type, Structure<?>> creator) {
                    var keyType = parameters[0].rawType();
                    return Structure.unboundedMap(parameters[0].create(), parameters[1].create()).xmap(map -> {
                        var enumMap = new EnumMap(keyType);
                        enumMap.putAll(map);
                        return enumMap;
                    }, Function.identity());
                }

                @Override
                public boolean supports(Class<?> exact, TypedCreator[] parameters) {
                    return exact.equals(EnumMap.class) && parameters.length == 2;
                }
            })
            .add(new FlexibleCreator() {
                @Override
                public Structure<?> create(Class<?> exact, TypedCreator[] parameters, Function<Type, Structure<?>> creator) {
                    Supplier<Object[]> values = Suppliers.memoize(() -> {
                        try {
                            return (Object[]) exact.getMethod("values").invoke(null);
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    return Structure.stringRepresentable(values, t -> ((Enum<?>)t).name());
                }

                @Override
                public boolean supports(Class<?> exact, TypedCreator[] parameters) {
                    return Enum.class.isAssignableFrom(exact);
                }
            })
            .add(new FlexibleCreator() {
                private Function<List<?>, ?> arrayMaker(Class<?> arrayComponentType) {
                    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    var name = BuiltInReflectiveStructureCreator.class.getName().replace('.', '/') + "$ArrayMaker";
                    cw.visit(Opcodes.V21, Opcodes.ACC_FINAL, name, null, "java/lang/Object", new String[]{"java/util/function/Function"});
                    var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                    mv.visitCode();
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
                    mv.visitCode();
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitTypeInsn(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(List.class));
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, org.objectweb.asm.Type.getInternalName(List.class), "size", "()I", true);
                    mv.visitTypeInsn(Opcodes.ANEWARRAY, org.objectweb.asm.Type.getInternalName(arrayComponentType));
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, org.objectweb.asm.Type.getInternalName(List.class), "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", true);
                    mv.visitTypeInsn(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(arrayComponentType.arrayType()));
                    mv.visitInsn(Opcodes.ARETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    cw.visitEnd();
                    var bytes = cw.toByteArray();
                    try {
                        var lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, List.of(arrayComponentType), true, MethodHandles.Lookup.ClassOption.NESTMATE);
                        @SuppressWarnings("unchecked") var instance = (Function<List<?>, ?>) lookup.lookupClass().getConstructor().newInstance();
                        return instance;
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

                @SuppressWarnings({"rawtypes", "unchecked"})
                @Override
                public Structure<?> create(Class<?> exact, TypedCreator[] parameters, Function<Type, Structure<?>> creator) {
                    var arrayMaker = arrayMaker(exact.getComponentType());
                    return creator.apply(exact.getComponentType()).listOf().xmap(arrayMaker::apply, obj -> {
                        var array = (Object[]) obj;
                        var list = new ArrayList<>(array.length);
                        list.addAll(Arrays.asList(array));
                        return (List) list;
                    });
                }

                @Override
                public boolean supports(Class<?> exact, TypedCreator[] parameters) {
                    return exact.isArray() && !exact.getComponentType().isPrimitive();
                }
            })
            .add(new FlexibleCreator() {
                private List<Constructor<?>> validCtors(Class<?> exact) {
                    List<Constructor<?>> validCtors = new ArrayList<>();
                    for (var ctor : exact.getConstructors()) {
                        if (!ctor.accessFlags().contains(AccessFlag.PUBLIC)) {
                            continue;
                        }
                        if (ctor.getParameterCount() == 0) {
                            validCtors.add(ctor);
                        } else {
                            var hasSerializedProperties = true;
                            for (var param : ctor.getParameters()) {
                                if (!param.isAnnotationPresent(SerializedProperty.class)) {
                                    hasSerializedProperties = false;
                                    break;
                                }
                                var annotation = param.getAnnotation(SerializedProperty.class);
                                try {
                                    var field = exact.getField(annotation.property());
                                    if (field.getType().equals(param.getType()) && field.accessFlags().contains(AccessFlag.PUBLIC) && !field.accessFlags().contains(AccessFlag.STATIC)) {
                                        continue;
                                    }
                                } catch (NoSuchFieldException ignored) {}

                                try {
                                    var getterMethod = exact.getMethod("get" + annotation.property().substring(0, 1).toUpperCase() + annotation.property().substring(1));
                                    if (getterMethod.getGenericReturnType().equals(param.getParameterizedType()) && getterMethod.accessFlags().contains(AccessFlag.PUBLIC) && !getterMethod.accessFlags().contains(AccessFlag.STATIC)) {
                                        continue;
                                    }
                                } catch (NoSuchMethodException ignored) {}
                                hasSerializedProperties = false;
                                break;
                            }
                            if (hasSerializedProperties) {
                                validCtors.add(ctor);
                            }
                        }
                    }
                    return validCtors;
                }

                @Override
                public Structure<?> create(Class<?> exact, TypedCreator[] parameters, Function<Type, Structure<?>> creator) {
                    return Structure.record(builder -> {
                        Constructor<?> validCtor;
                        if (exact.isRecord()) {
                            Class<?>[] types = new Class<?>[exact.getRecordComponents().length];
                            for (int i = 0; i < types.length; i++) {
                                types[i] = exact.getRecordComponents()[i].getType();
                            }
                            try {
                                validCtor = exact.getConstructor(types);
                            } catch (NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            var ctors = validCtors(exact);
                            validCtor = ctors.getFirst();
                        }

                        Objects.requireNonNull(validCtor);

                        Map<String, Function<?, Object>> getters = new HashMap<>();
                        Map<String, MethodHandle> setters = new HashMap<>();
                        Map<String, Integer> ctorSetters = new HashMap<>();
                        String[] ctorSettersArray = new String[validCtor.getParameterCount()];
                        Map<String, Type> types = new HashMap<>();
                        Map<String, List<AnnotatedElement>> context = new HashMap<>();
                        if (!exact.isRecord()) {
                            for (int i = 0; i < validCtor.getParameterCount(); i++) {
                                var param = validCtor.getParameters()[i];
                                var annotation = param.getAnnotation(SerializedProperty.class);

                                var thisContext = context.computeIfAbsent(annotation.property(), k -> new ArrayList<>());
                                thisContext.add(param);

                                ctorSetters.put(annotation.property(), i);
                                ctorSettersArray[i] = annotation.property();
                                types.put(annotation.property(), param.getParameterizedType());

                                // Prefer the bean getter method, then the field

                                try {
                                    var getterMethod = exact.getMethod("get" + annotation.property().substring(0, 1).toUpperCase() + annotation.property().substring(1));
                                    if (getterMethod.getGenericReturnType().equals(param.getParameterizedType()) && getterMethod.accessFlags().contains(AccessFlag.PUBLIC) && !getterMethod.accessFlags().contains(AccessFlag.STATIC)) {
                                        var getter = functionWrapper(MethodHandles.lookup().unreflect(getterMethod));
                                        getters.put(annotation.property(), getter);
                                        thisContext.add(getterMethod);
                                        continue;
                                    }
                                } catch (NoSuchMethodException ignored) {
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }

                                try {
                                    var field = exact.getField(annotation.property());
                                    if (field.getType().equals(param.getType()) && field.accessFlags().contains(AccessFlag.PUBLIC) && !field.accessFlags().contains(AccessFlag.STATIC)) {
                                        var getter = functionWrapper(MethodHandles.lookup().unreflectGetter(field));
                                        getters.put(annotation.property(), getter);
                                        thisContext.add(field);
                                    }
                                } catch (NoSuchFieldException ignored) {
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            for (var method : exact.getMethods()) {
                                if (method.accessFlags().contains(AccessFlag.PUBLIC) && !method.accessFlags().contains(AccessFlag.STATIC)) {
                                    var isGetter = method.getParameterCount() == 0 && (
                                        method.getName().startsWith("get") ||
                                            (method.getName().startsWith("is") && method.getGenericReturnType().equals(Boolean.TYPE))
                                    );
                                    var isSetter = method.getParameterCount() == 1 && method.getName().startsWith("set") && method.getGenericReturnType().equals(Void.TYPE);
                                    if (isGetter) {
                                        var property = method.getName().substring(method.getName().startsWith("is") ? 2 : 3);
                                        property = property.substring(0, 1).toLowerCase() + property.substring(1);
                                        if (!types.containsKey(property) && method.getGenericReturnType().equals(types.get(property))) {
                                            types.put(property, method.getGenericReturnType());
                                            if (!getters.containsKey(property)) {
                                                try {
                                                    var getter = functionWrapper(MethodHandles.lookup().unreflect(method));
                                                    getters.put(property, getter);
                                                } catch (IllegalAccessException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }
                                            context.computeIfAbsent(property, k -> new ArrayList<>()).add(method);
                                        }
                                    } else if (isSetter) {
                                        var property = method.getName().substring(3);
                                        property = property.substring(0, 1).toLowerCase() + property.substring(1);
                                        if (!types.containsKey(property) && method.getParameterTypes()[0].equals(types.get(property))) {
                                            types.put(property, method.getGenericReturnType());
                                            if (!setters.containsKey(property)) {
                                                try {
                                                    var setter = MethodHandles.lookup().unreflect(method);
                                                    setters.put(property, setter);
                                                } catch (IllegalAccessException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }
                                            context.computeIfAbsent(property, k -> new ArrayList<>()).add(method);
                                        }
                                    }
                                }
                            }

                            for (var field : exact.getFields()) {
                                if (field.accessFlags().contains(AccessFlag.PUBLIC) && !field.accessFlags().contains(AccessFlag.STATIC) && !field.accessFlags().contains(AccessFlag.TRANSIENT)) {
                                    try {
                                        if (!types.containsKey(field.getName())) {
                                            types.put(field.getName(), field.getGenericType());
                                        }
                                        context.computeIfAbsent(field.getName(), k -> new ArrayList<>()).add(field);
                                        if (!getters.containsKey(field.getName())) {
                                            var getter = functionWrapper(MethodHandles.lookup().unreflectGetter(field));
                                            getters.put(field.getName(), getter);
                                        }
                                        if (!setters.containsKey(field.getName()) && (!ctorSetters.containsKey(field.getName())) && !field.accessFlags().contains(AccessFlag.FINAL)) {
                                            var setter = MethodHandles.lookup().unreflectSetter(field);
                                            setters.put(field.getName(), setter);
                                        }
                                    } catch (IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        } else {
                            for (int i = 0; i < exact.getRecordComponents().length; i++) {
                                var component = exact.getRecordComponents()[i];

                                var thisContext = context.computeIfAbsent(component.getName(), k -> new ArrayList<>());
                                thisContext.add(component);

                                ctorSetters.put(component.getName(), i);
                                ctorSettersArray[i] = component.getName();
                                types.put(component.getName(), component.getGenericType());

                                try {
                                    var getterMethod = exact.getMethod(component.getName());
                                    var getter = functionWrapper(MethodHandles.lookup().unreflect(getterMethod));
                                    thisContext.add(getterMethod);
                                    getters.put(component.getName(), getter);
                                } catch (NoSuchMethodException ignored) {
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                        var properties = new LinkedHashSet<String>();
                        for (var entry : types.keySet()) {
                            if (getters.containsKey(entry) && (setters.containsKey(entry) || ctorSetters.containsKey(entry))) {
                                properties.add(entry);
                            }
                        }
                        var propertyList = new ArrayList<>(properties);
                        var keyList = new ArrayList<Function<RecordStructure.Container,  ?>>(propertyList.size());
                        for (var property : propertyList) {
                            var getter = getters.get(property);
                            var key = add(builder, property, types.get(property), getter, creator);
                            keyList.add(key);
                        }
                        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                        var name = BuiltInReflectiveStructureCreator.class.getName().replace('.', '/') + "$RecordWrapper";
                        cw.visit(Opcodes.V21, Opcodes.ACC_FINAL, name, null, "java/lang/Object", new String[]{"java/util/function/Function"});
                        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                        mv.visitCode();
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                        mv.visitInsn(Opcodes.RETURN);
                        mv.visitMaxs(0, 0);
                        mv.visitEnd();
                        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
                        var classData = new ArrayList<>();
                        Map<String, Integer> offsetMap = new HashMap<>();
                        String ctorDescriptor;
                        try {
                            var ctorHandle = MethodHandles.lookup().unreflectConstructor(validCtor);
                            classData.add(ctorHandle);
                            ctorDescriptor = ctorHandle.type().changeReturnType(Void.TYPE).descriptorString();
                            var j = 1;
                            for (int i = 0; i < propertyList.size(); i++) {
                                var key = keyList.get(i);
                                var property = propertyList.get(i);
                                classData.add(key);
                                offsetMap.put(property, j);
                                j++;
                                if (!ctorSetters.containsKey(property)) {
                                    var setter = setters.get(property).asType(MethodType.methodType(Void.TYPE, Object.class, Object.class));
                                    classData.add(setter);
                                    j++;
                                }
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                        mv.visitVarInsn(Opcodes.ALOAD, 1);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, RecordStructure.Container.class.getName().replace('.', '/'));
                        mv.visitVarInsn(Opcodes.ASTORE, 2);
                        mv.visitTypeInsn(Opcodes.NEW, exact.getName().replace('.', '/'));
                        mv.visitInsn(Opcodes.DUP);
                        for (int i = 0; i < ctorSettersArray.length; i++) {
                            var property = ctorSettersArray[i];
                            int keyOffset = offsetMap.get(property);
                            mv.visitLdcInsn(conDyn(org.objectweb.asm.Type.getDescriptor(Function.class), keyOffset));
                            mv.visitVarInsn(Opcodes.ALOAD, 2);
                            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, org.objectweb.asm.Type.getInternalName(Function.class), "apply", MethodType.methodType(Object.class, Object.class).descriptorString(), true);
                            convertType(mv, validCtor.getParameters()[i].getType());
                        }
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, exact.getName().replace('.', '/'), "<init>", ctorDescriptor, false);
                        mv.visitVarInsn(Opcodes.ASTORE, 3);
                        for (var property : propertyList) {
                            if (!ctorSetters.containsKey(property)) {
                                mv.visitVarInsn(Opcodes.ALOAD, 3);
                                var keyOffset = offsetMap.get(property);
                                mv.visitLdcInsn(conDyn(org.objectweb.asm.Type.getDescriptor(Function.class), keyOffset));
                                mv.visitVarInsn(Opcodes.ALOAD, 2);
                                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, org.objectweb.asm.Type.getInternalName(Function.class), "apply", MethodType.methodType(Object.class, Object.class).descriptorString(), true);
                                invokeAsCallSite(mv, "(Ljava/lang/Object;Ljava/lang/Object;)V", offsetMap.get(property)+1);
                            }
                        }
                        mv.visitVarInsn(Opcodes.ALOAD, 3);
                        mv.visitInsn(Opcodes.ARETURN);
                        mv.visitMaxs(0, 0);
                        mv.visitEnd();
                        cw.visitEnd();
                        var bytes = cw.toByteArray();
                        try {
                            var lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, classData, true, MethodHandles.Lookup.ClassOption.NESTMATE);
                            @SuppressWarnings("unchecked") var instance = (Function<RecordStructure.Container, Object>) lookup.lookupClass().getConstructor().newInstance();
                            return instance;
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                @Override
                public int priority() {
                    // This takes low priority as it is meant as a final fallback
                    return -10;
                }

                @Override
                public boolean supports(Class<?> exact, TypedCreator[] parameters) {
                    // For a class to be record-able, it must meet a few conditions. It must:
                    // - Be a record
                    // - OR: Have a public no-args constructor
                    // - OR: Have a single public constructor with all args marked with @SerializedProperty
                    // Then, the structure works by:
                    // - constructing the object, with any properties that are present in that fashion
                    // - setting any properties that have setters present
                    // Properties are included if they have both a getter (method or public field) and setter (ctor argument, method, or public non-final field) present.
                    if (exact.isRecord()) {
                        return true;
                    }

                    return validCtors(exact).size() == 1;
                }
            })
            .build();
    }

    private static void convertType(MethodVisitor mv, Class<?> type) {
        if (type.isPrimitive()) {
            switch (type.getName()) {
                case "void" -> mv.visitInsn(Opcodes.POP);
                case "boolean" -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Boolean.class.getName().replace('.', '/'));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Boolean.class.getName().replace('.', '/'), "booleanValue", "()Z", false);
                }
                case "byte" -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Byte.class.getName().replace('.', '/'));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Byte.class.getName().replace('.', '/'), "byteValue", "()B", false);
                }
                case "short" -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Short.class.getName().replace('.', '/'));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Short.class.getName().replace('.', '/'), "shortValue", "()S", false);
                }
                case "int" -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Integer.class.getName().replace('.', '/'));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Integer.class.getName().replace('.', '/'), "intValue", "()I", false);
                }
                case "long" -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Long.class.getName().replace('.', '/'));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Long.class.getName().replace('.', '/'), "longValue", "()J", false);
                }
                case "float" -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Float.class.getName().replace('.', '/'));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Float.class.getName().replace('.', '/'), "floatValue", "()F", false);
                }
                case "double" -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Double.class.getName().replace('.', '/'));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Double.class.getName().replace('.', '/'), "doubleValue", "()D", false);
                }
                case "char" -> {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Character.class.getName().replace('.', '/'));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Character.class.getName().replace('.', '/'), "charValue", "()C", false);
                }
            }
        } else {
            mv.visitTypeInsn(Opcodes.CHECKCAST, org.objectweb.asm.Type.getInternalName(type));
        }
    }
}
