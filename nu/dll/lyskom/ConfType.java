/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class ConfType extends Bitstring {

    public final static int rd_prot = 0;
    public final static int original = 1;
    public final static int secret = 2;
    public final static int letterbox = 3;
    public final static int allow_anonymous = 4;
    public final static int forbid_secret = 5;
    public final static int reserved2 = 6;
    public final static int reserved3 = 7;

    public ConfType(KomToken k) {
	super(k);
    }

    public ConfType(boolean[] bits) {
	super(bits);
    }

    public String toString() {
	return "Bitstring(value: "+new String(getContents());
    }
}
