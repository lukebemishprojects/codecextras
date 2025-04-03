package dev.lukebemish.codecextras.structured.reflective;

/**
 * Built-in options for modifying the behavior of {@link ReflectiveStructureCreator}.
 */
public enum SimpleCreatorOption implements CreationOption {
    /**
     * Fields are assumed to be not-null, instead of nullable, by default
     */
    NOT_NULL_BY_DEFAULT
}
