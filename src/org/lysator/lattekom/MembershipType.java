/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

/**
 * Represents a Membership-Type. TODO: document the flags.
 */
public class MembershipType extends Bitstring {
	private static final long serialVersionUID = 4585427743367499039L;
	public static final int bitcount = 8;
	public static final int invitation = 0;
	public static final int passive = 1;
	public static final int secret = 2;
	public static final int reserved1 = 3;
	public static final int reserved2 = 4;
	public static final int reserved3 = 5;
	public static final int reserved4 = 6;
	public static final int reserved5 = 7;

	public MembershipType(boolean[] bits) {
		byte[] contents = new byte[bitcount];
		for (int i = 0; i < bitcount; i++) {
			if (i < bits.length)
				contents[i] = (byte) (bits[i] ? '1' : '0');
			else
				contents[i] = (byte) '0';
		}
		setContents(contents);
	}

	public MembershipType(KomToken token) {
		super(token);
	}

}
