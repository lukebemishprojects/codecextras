package dev.lukebemish.codecextras.compat.nightconfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapLike;
import dev.lukebemish.codecextras.comments.CommentOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class NightConfigCommentOps<T extends CommentedConfig, O extends CommentedNightConfigOps<T>> implements CommentOps<Object> {
	public abstract O parentOps();

	@Override
	public DataResult<Object> commentToMap(Object map, MapLike<Object> comments) {
		if (!(map instanceof CommentedConfig config)) {
			return DataResult.error(() -> "Not a map: "+map);
		}
		List<Object> missed = new ArrayList<>();
		T newConfig = parentOps().copyConfig(config);
		comments.entries().forEach(pair -> {
			if (!(pair.getFirst() instanceof String key)) {
				missed.add(pair.getFirst());
				return;
			}
			if (!(pair.getSecond() instanceof String value)) {
				missed.add(pair.getSecond());
				return;
			}
			newConfig.setComment(key, value);
		});
		if (!missed.isEmpty()) {
			return DataResult.error(() -> "Not strings: "+missed);
		}
		return DataResult.success(newConfig);
	}

	@Override
	public DataResult<Object> commentToMap(Object map, Map<Object, Object> comments) {
		if (!(map instanceof CommentedConfig config)) {
			return DataResult.error(() -> "Not a map: "+map);
		}
		List<Object> missed = new ArrayList<>();
		T newConfig = parentOps().copyConfig(config);
		for (var entry : comments.entrySet()) {
			if (!(entry.getKey() instanceof String key)) {
				missed.add(entry.getKey());
				continue;
			}
			if (!(entry.getValue() instanceof String value)) {
				missed.add(entry.getValue());
				continue;
			}
			newConfig.setComment(key, value);
		}
		if (!missed.isEmpty()) {
			return DataResult.error(() -> "Not strings: "+missed);
		}
		return DataResult.success(newConfig);
	}

	@Override
	public DataResult<Object> commentToMap(Object map, Object key, Object comment) {
		if (map instanceof CommentedConfig config) {
			CommentedConfig newConfig = parentOps().copyConfig(config);
			if (!(key instanceof String keyString)) {
				return DataResult.error(() -> "Not a string: "+key);
			}
			if (!(comment instanceof String commentString)) {
				return DataResult.error(() -> "Not a string: "+comment);
			}
			newConfig.setComment(keyString, commentString);
			return DataResult.success(newConfig);
		}
		return DataResult.error(() -> "Not a map: "+map);
	}
}
