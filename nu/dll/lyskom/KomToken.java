/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-

package nu.dll.lyskom;
import java.io.Serializable;

public class KomToken implements Serializable {
    public final static int PRIMITIVE = 0;
    public final static int COMPL     = 1;
    public final static int ARRAY     = 2;
    public final static int HOLLERITH = 3;

    public int type = PRIMITIVE; // obsolete?
    byte[] contents = null;

    private static final int DEBUG = 0;

    boolean eol = false; // indicates last token on line

    // This is the superconstructor that implicitly gets called from
    // child class constructors. I think.
    public KomToken() {
	type = COMPL;
    }

    public boolean isEol() {
	return eol;
    }
    public void setEol(boolean b) {
	eol = b;
    }

    public KomToken(int i) {
	contents = (i+"").getBytes();
    }

    public KomToken(byte[] b) {
	contents = b;
    }

    public KomToken(String s) {
	contents = s.getBytes();
    }
    
    // alias for toInt(). should actually return an Integer object
    public int toInteger() {
	return toInt();
    }

    public int toInt() {
	if (contents == null || contents.length == 0)
	    return -1;
	try {
	    return Integer.parseInt(new String(contents));
	} catch (NumberFormatException ex) {
	    // Is this a programming error or runtime error? Hmmm?
	    throw new RuntimeException("Error parsing " + new String(contents) + " to int");
	}
    }
   
    public String toString() {
	String ktype = "TOKEN";
	if (this instanceof Hollerith)
	    return ((Hollerith) this).toString();
	else if (this instanceof KomTokenArray)
	    return ((KomTokenArray) this).toString();
	else if (type == COMPL)
	    ktype = "COMPL";
	return ktype + ":\"" + new String(getContents()) + "\"";
    }

    public byte[] toNetwork() {

	// uuh.. but this can't happen...?
	if (this instanceof Hollerith)
	    return ((Hollerith) this).toNetwork();

	if (this instanceof KomTokenArray)
	    return ((KomTokenArray) this).toNetwork();

	return getContents();
    }

    public byte[] getContents() {
	return contents;
    }

    public void setContents(byte[] c) {
	contents = c;
    }

    public int getType() {
	return type;
    }
}



