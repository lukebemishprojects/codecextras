package dev.lukebemish.codecextras.companion;

import com.mojang.serialization.DynamicOps;
import java.util.Optional;

public interface AccompaniedOps<T> extends DynamicOps<T> {
	default <O extends Companion.CompanionToken, C extends Companion<T, O>> Optional<C> getCompanion(O token) {
		return Optional.empty();
	}
}
