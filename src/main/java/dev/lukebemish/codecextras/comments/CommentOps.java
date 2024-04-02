package dev.lukebemish.codecextras.comments;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import dev.lukebemish.codecextras.companion.Companion;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public interface CommentOps<T> extends Companion<T, CommentOps.CommentOpsToken> {
	CommentOpsToken TOKEN = new CommentOpsToken();

	DataResult<T> commentToMap(T map, T key, T comment);

	default DataResult<T> commentToMap(final T map, final Map<T, T> comments) {
		return commentToMap(map, MapLike.forMap(comments, parentOps()));
	}

	default DataResult<T> commentToMap(final T map, final MapLike<T> comments) {
		final AtomicReference<DataResult<T>> result = new AtomicReference<>(DataResult.success(map));

		comments.entries().forEach(entry ->
			result.setPlain(result.getPlain().flatMap(r -> commentToMap(r, entry.getFirst(), entry.getSecond())))
		);
		return result.getPlain();
	}

	DynamicOps<T> parentOps();

	final class CommentOpsToken implements Companion.CompanionToken {
		private CommentOpsToken() {}
	}
}
