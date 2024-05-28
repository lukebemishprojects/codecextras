package dev.lukebemish.codecextras.companion;

import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Optional;

class MapDelegatingOps<T> extends DelegatingOps<T> {
	Map<Companion.CompanionToken, Optional<Companion<T, ? extends Companion.CompanionToken>>> companions;

	MapDelegatingOps(DynamicOps<T> delegate, Map<Companion.CompanionToken, Optional<Companion<T, ? extends Companion.CompanionToken>>> companions) {
		super(delegate);
		this.companions = companions;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <O extends Companion.CompanionToken, C extends Companion<T, O>> Optional<C> getCompanion(O token) {
		var companion = companions.get(token);
		if (companion != null) {
			return (Optional<C>) companion;
		}
		return super.getCompanion(token);
	}
}
