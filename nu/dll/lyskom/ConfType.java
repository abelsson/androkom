/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class ConfType extends Bitstring {

    public ConfType(KomToken bits) {
	super(bits);
    }

    public String toString() {
	return "Bitstring(value: "+new String(getContents());
    }
}
