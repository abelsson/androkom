/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

import java.io.*;
import java.util.Enumeration;
import java.util.List;

/**
 * Represents an array of KomToken objects. Each element in a KomTokenArray
 * can consist of several KomToken objects.
 */ 
public class KomTokenArray extends KomToken {

    static int DEBUG = 10;
    KomToken[] objects;
    int length;

    /**
     * Constructs an empty KomTokenArray
     */
    public KomTokenArray(int length) { // empty array
	this.length = length;
	objects = new KomToken[0];
    }

    /**
     * Constructs a KomTokenArray of the specified length,
     * with the elements being the Strings converted into bytes
     * according to KomToken(String)
     *
     * @see nu.dll.lyskom.KomToken#KomToken(String)
     */
    public KomTokenArray(int length, String[] s) {
	this.length = length;
	objects = new KomToken[s.length];
	for (int i=0;i<s.length;i++)
	    objects[i] = new KomToken(s[i]);
    }

    /**
     * Constructs a KomTokenArray of the specified length,
     * with the elements being the Strings converted into bytes
     * according to KomToken(int)
     *
     * @see nu.dll.lyskom.KomToken#KomToken(int)
     */
    public KomTokenArray(int length, int[] n) {
	this.length = length;
	objects = new KomToken[n.length];
	for (int i=0;i<n.length;i++)
	    objects[i] = new KomToken(n[i]);
    }

    /**
     * Constructs a KomTokenArray containing the objects supplied.
     */
    public KomTokenArray(int length, KomToken[] o) {
	this.length = length;
	objects = o;
	contents = toNetwork();
    }

    /**
     * Creates a KomTokenArray of the supplied length using the
     * supplied Tokenizable objects.
     */
    public KomTokenArray(int length, Tokenizable[] o) {
	this.length = length;
	objects = new KomToken[o.length];
	for (int i=0; i < o.length; i++) {
	    objects[i] = o[i].toToken();
	}
	contents = toNetwork();
    }

    /**
     * Creates a KomTokenArray of the supplied length out of
     * each of the element in the List, which must contain
     * only Tokenizable objects.
     */
    public KomTokenArray(int length, List tokenList) {
	this.length = length;
	objects = new KomToken[tokenList.size()];
	for (int i=0; i < objects.length; i++) {
	    objects[i] = ((Tokenizable) tokenList.get(i)).toToken();
	}
	contents = toNetwork();
    }

    /* end of constructors */

    /** Enumeration implementation **/

    // 990915: removed

    /** end of Enumeration **/

    /**
     * Returns an <tt>int</tt> array by converting all objects in this
     * KomTokenArray into integers by calling their intValue() method.
     *
     * @see nu.dll.lyskom.KomToken#intValue()
     */
    public int[] intValues() {
	int[] vals = new int[objects.length];
	for (int i=0; i<objects.length; i++) 
	    vals[i] = objects[i].intValue();
	return vals;
    }

    // todo: rename
    // returns: KomToken-array-of-arrays
    /**
     * Static helper class that splits a two dimensional KomTokenArray
     * into a two dimensional KomToken array (<tt>KomToken[][]</tt>),
     * by dividing the number of objects by the length reported
     * by the KomTokenArray.
     */
    public static KomToken[][] split(KomTokenArray komarray) {		
	int length = komarray.getLength();
	KomToken[] objects = komarray.getTokens();
	if (DEBUG > 0) {
	    Debug.println("split(): length: "+length+" actual: "+
			       objects.length);
	}
	if (length == objects.length)
	    return null;
	int sublength = objects.length / length;
	if (DEBUG>0) Debug.println("split(): sublength: "+sublength);
	KomToken[][] splitted = new KomToken[length][sublength];
	int ocount = 0;
	for (int i=0;i<length;i++) {
	    if (DEBUG>0) Debug.print("split(): assigning to #"+i+
					    ", ocount "+ocount);
	    KomToken[] subarray = new KomToken[sublength];
	    for (int j=0;j<sublength;j++)
		subarray[j] = objects[ocount++];

	    if (DEBUG>0) Debug.print("-"+ (ocount-1) + "\n");
	    splitted[i] = subarray;
	}
	return splitted;
    }

    /**
     * Returns the length of this array. Note that this does not
     * have to be the same as the number of KomToken objects stored.
     */
    public int getLength() {
	return length;
    }

    /**
     * Returns an array with all KomToken objects in this Array.
     */
    public KomToken[] getTokens() {
	return objects;
    }

    public String toString() {
	return "ARRAY("+objects.length+"):"+new String(toNetwork());
    }

    /**
     * Converts this into a LysKOM ARRAY suitable for sending to the server,
     * by calling each of the objects' toNetwork() method.
     * <br>
     * This is implemented using a StringBuffer, so all objects will first
     * be converted into Strings using iso-8859-1, then transformed
     * back into a byte array using the same encoding. This should
     * be consistent, but there is probably some overhead.
     */
    public byte[] toNetwork() {
	try {
	    StringBuffer buff = new StringBuffer();
	    buff.append(length + " {");
	    for (int i=0 ; i < objects.length ; i++) {
		buff.append(" ");
		if (objects[i] instanceof KomTokenArray) {
		    KomTokenArray foo = (KomTokenArray) objects[i];
		    buff.append(new String(((KomTokenArray) objects[i]).toNetwork(), Session.serverEncoding));
		} else if (objects[i] instanceof Hollerith) {
		    buff.append(new String(((Hollerith) objects[i]).toNetwork(), Session.serverEncoding));
		} else {
		    buff.append(new String(objects[i].toNetwork(), Session.serverEncoding));
		}
	    }
	    buff.append(" }");
	    return buff.toString().getBytes(Session.serverEncoding);
	} catch (UnsupportedEncodingException ex1) {
	    throw new RuntimeException("Unsupported encoding: " + ex1.getMessage());
	}
    }
		

}
