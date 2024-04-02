package dev.lukebemish.codecextras.companion;

import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Optional;

class MapDelegatingOps<T> extends DelegatingOps<T> {
	Map<Companion.CompanionToken, Companion<T, ? extends Companion.CompanionToken>> companions;

	public MapDelegatingOps(DynamicOps<T> delegate, Map<Companion.CompanionToken, Companion<T, ? extends Companion.CompanionToken>> companions) {
		super(delegate);
		this.companions = companions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <O extends Companion.CompanionToken, C extends Companion<T, O>> Optional<C> getCompanion(O token) {
		var companion = companions.get(token);
		if (companion != null) {
			return Optional.of((C) companion);
		}
		return super.getCompanion(token);
	}
}
