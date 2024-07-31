package dev.lukebemish.codecextras.structured;

public final class Key<A> {
    private final String name;

    private Key(String name) {
        this.name = name;
    }

    public static <A> Key<A> create(String name) {
        var className = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getSimpleName();
        return new Key<>(className + ":" + name);
    }

    public String name() {
        return name;
    }
}
