/**
 * Provides codecs able to handle mutable data. A given object should store mutable data in a series of
 * {@link dev.lukebemish.codecextras.mutable.DataElement}s. Each of these can be accessed and encoded/decoded using a
 * {@link dev.lukebemish.codecextras.mutable.DataElementType}.
 * For example: <blockquote><pre>{@code
 * public class Foo {
 *     public static final DataElementType<Foo, String> STRING = DataElementType.create("string", Codec.STRING, f -> f.string);
 *
 *     private final DataElement<String> string = new DataElement.Simple<>(""); // default value
 * }
 * }</pre></blockquote>
 * The codecs provided by {@link dev.lukebemish.codecextras.mutable.DataElementType#codec(boolean, dev.lukebemish.codecextras.mutable.DataElementType[])}
 * allow you to encode either the full state of a series of data elements, or the changes to the state since it was last
 * marked as clean; when decoding, these codecs decode a {@link java.util.function.Consumer} that can be applied to an object to
 * set the mutable state.
 */
@NullMarked
@ApiStatus.Experimental
package dev.lukebemish.codecextras.mutable;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
