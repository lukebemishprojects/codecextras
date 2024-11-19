/**
 * CodecExtras' structure API allows you to create any number of representations of the structure of a type of data from
 * a single shared representation.
 * <p>
 * The core piece of this API is {@link dev.lukebemish.codecextras.structured.Structure}.
 * If you define a {@code Structure<T>} representing a data structure for some type {@code T}, you can then interpret
 * that structure into any number of types parameterized by {@code T} dependent on that structure, say {@code F<T>}, by
 * using an appropriate {@link dev.lukebemish.codecextras.structured.Interpreter} of type {@code Interpreter<F.Mu>}.
 * For instance, you can turn a {@code Structure<T>} into a {@link com.mojang.serialization.Codec} by using a
 * {@link dev.lukebemish.codecextras.structured.CodecInterpreter}.
 * <p>
 * CodecExtras' core module has a number of interpreter representations built in, including:
 * <ul>
 *     <li>{@link dev.lukebemish.codecextras.structured.CodecInterpreter}, for creating a {@link com.mojang.serialization.Codec}
 *     <li>{@link dev.lukebemish.codecextras.structured.MapCodecInterpreter}, for creating a {@link com.mojang.serialization.MapCodec}
 *     <li>{@link dev.lukebemish.codecextras.structured.IdentityInterpreter}, which extracts the default value from a structure made up of optional components
 *     <li>{@link dev.lukebemish.codecextras.structured.schema.JsonSchemaInterpreter}, which creates a JSON schema describing how a structure would be (de)serialized by a {@link com.mojang.serialization.Codec}
 * </ul>
 * The interpreter system is extensible, so you can implement your own interpreters for your own types. The {@code codecextras-minecraft}
 * module provides a number of interpreters for Minecraft-specific types, including stream codecs and config screens.
 */
@NullMarked
@ApiStatus.Experimental
package dev.lukebemish.codecextras.structured;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
