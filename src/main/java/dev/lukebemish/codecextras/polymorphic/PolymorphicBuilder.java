package dev.lukebemish.codecextras.polymorphic;

public interface PolymorphicBuilder<B extends PolymorphicBuilder<B>> {
	B self();

	void validate() throws BuilderException;
}
