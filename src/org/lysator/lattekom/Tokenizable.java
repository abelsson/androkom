package org.lysator.lattekom;

/**
 * Indicates that this object can be converted into a KomToken by calling its
 * toToken() method.
 */
public interface Tokenizable {
	public KomToken toToken();
}
