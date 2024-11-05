package dev.lukebemish.codecextras.structured;

public final class Key2<A, B> {
    private final String name;

    private Key2(String name) {
        this.name = name;
    }

    public static <A, B> Key2<A, B> create(String name) {
        var className = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getSimpleName();
        return new Key2<>(className + ":" + name);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "Key2[" + name + "]";
    }
}
