/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

/**
 * Represents the LysKOM data type Conf-Type, which is a Bitstring.
 *
 * Contains helper constants for the meaning of the Conf-Type flags.
 * Use them with the getBitAt()/setBitAt() in Bitstring to deal with
 * ConfType information.
 *
 */
public class ConfType extends Bitstring {

    /**
     * Read-protected conference
     */ 
    public final static int rd_prot = 0;
    /**
     * Only original texts are allowed (no comments)
     */ 
    public final static int original = 1;
    /**
     * Secret conference
     */
    public final static int secret = 2;
    /**
     * Conference is a person's letterbox
     */
    public final static int letterbox = 3;
    /**
     * Conference allows anonymous texts
     */
    public final static int allow_anonymous = 4;
    /**
     * Conference forbids secret membership.
     */
    public final static int forbid_secret = 5;
    /**
     * Reserved flag.
     */
    public final static int reserved2 = 6;
    /**
     * Reserved flag.
     */
    public final static int reserved3 = 7;

    ConfType(KomToken k) {
	super(k);
    }

    /**
     * <tt>getBitAt(original)</tt>
     */ 
    public boolean original() {
	return getBitAt(original);
    }

    /**
     * is letterbox (person)
     */
    public boolean letterbox() {
	return getBitAt(letterbox);
    }

    /**
     * Creates a ConfType based on the supplied <tt>boolean[]</tt>.
     */
    public ConfType(boolean[] bits) {
	super(bits);
    }

    public String toString() {
	return "Bitstring(value: "+new String(getContents());
    }
}

