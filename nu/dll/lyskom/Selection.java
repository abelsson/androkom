/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

//import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.io.Serializable;

/**
 * <p>A Selection is tagged data, in which each object represent
 * an ordered list of tags (keys) and perhaps some data
 * belonging to each tag.
 * </p><p>
 * This implementation is not quite consistent with the LysKOM
 * Protocol A specification, since each tag can contain multiple
 * values (due to an early confusion during development), but it
 * seems to be adequate for most needs.
 * </p><p>
 * Also, this implementation can only deal with "primitive"
 * LysKOM types, such as INT32, Bitstring, Hollerith and their likes.
 * If you try to set a Selection value to a KomTokenArray or another
 * Selection, for example, you will get very unpredictable results.
 * </p>
 */
public class Selection implements Serializable, Tokenizable {
    boolean[] keys; // since a selection item can be empty
    Vector[] values;

    public static final int DEBUG = 3;

    // size is actually the parameter range that is valid in this
    // Selection; 15 (0-14) for Misc-Info, for example.
    /**
     * Creates a new Selection.
     * The <tt>size</tt> parameter specifies the highes possible 
     * tag number that will occur in this Selection.
     */
    public Selection(int size) {
	keys = new boolean[size];
	values = new Vector[size];
    }

    /**
     * Adds the tag <tt>key</tt> to this Selection.
     * The supplied object may be null to indicate a tag without a trailing value.
     */
    public Selection add(int key, Object o) {
	keys[key] = true;

	if (values[key] == null)
	    values[key] = new Vector(1);

	values[key].addElement(o);
	return this;
    }

    /**
     * Adds the tag <tt>key</tt> to this selection, with the value <tt>value</tt>.
     */
    public Selection add(int key, int value) {
	keys[key] = true;
	values[key] = new Vector(1);
	values[key].addElement(new Integer(value));
	return this;
    }

    
    /**
     * Returns <tt>true</tt> if this Selection contains the tag <tt>key</tt>
     */
    public boolean contains(int key) {
	return keys[key];
    }

    /**
     * Returns an Enumeration of all the values associated with
     * the specified tag <tt>key</tt>.
     *
     * @deprecated This should have no real use in the real world.
     */
    public Enumeration get(int key)
    throws NoSuchKeyException {
	if (!keys[key])
	    throw(new NoSuchKeyException());

	return values[key].elements();
    }

    public int getKey() {
	for (int i=0; i < keys.length; i++) {
	    if (keys[i]) return i;
	}
	return -1;
    }

    public Object getValue() {
	return values[getKey()].get(0);
    }

    public int getIntValue() {
	return ((Integer) getValue()).intValue();
    }

    /**
     * Returns an int representation of the data tagged with <tt>key</tt>,
     * providing that the data is stored as an Integer.
     */
    public int getIntValue(int key) {
	if (!keys[key]) throw new NoSuchKeyException("key " + key);
	return ((Integer) values[key].elementAt(0)).intValue();
    }

    public Object getValue(int key) {
	if (!keys[key]) return null;
	return values[key].elementAt(0);
    }

    /**
     * Returns a KomTime value stored with tag <tt>key</tt>.
     */
    public KomTime getTimeValue(int key) {
	if (!keys[key]) throw new NoSuchKeyException("key " + key);
	return (KomTime) values[key].elementAt(0);
    }

    /**
     * Returns a KomToken value stored with tag <tt>key</tt>.
     */
    public KomToken getTokenValue(int key) {
	if (!keys[key]) throw new NoSuchKeyException("key " + key);
	return (KomToken) values[key].elementAt(0);
    }

    /**
     * Removes the object <tt>value</tt> from the tag <tt>key</tt>.
     * It does not remove the tag itself, which then becomes
     * selector without a trailing value.
     *
     * @return <tt>true</tt> if the tag was found in this selection and contained <tt>value</tt>
     */
    public boolean remove(int key, Object value) {
	if (keys[key]) {
	    return values[key].removeElement(value);
	} else {
	    return false;
	}
    }


    /**
     * Clears the selection by removing the supplied key and its value(s).
     *
     * @return <tt>true</tt> if the supplied key was found
     */
    public boolean clear(int key) {
	if (keys[key]) {
	    keys[key] = false;
	    values[key] = new Vector();
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * Returns an array of integers representing all the selectors (keys)
     * in this Selection.
     */
    public int[] getKeys() {
	int keycount = 0;
	for (int i=0; i < keys.length; i++)
	    if (keys[i]) keycount++;
	int[] allkeys = new int[keycount];
	for (int i=0, c=0; i < keys.length; i++)
	    if (keys[i]) allkeys[c++] = i;
	return allkeys;
    }

    /**
     * Returns a vector containing all elements associated with a key.
     *
     * @deprecated This should have no real use in the real world.
     */
    public Vector getVector(int key)
    throws NoSuchKeyException {
	if (!keys[key])
	    throw(new NoSuchKeyException());
	return values[key];
    }

    /**
     * Returns an integer array containing all elements' values
     * converted into integers, for a given key.
     *
     * @deprecated This should have no real use in the real world.
     */
    public int[] getIntArray(int no) {
        try {
            Vector v = getVector(no);
            int[] stats = new int[v.size()];
            int i=0;
            for(Enumeration e = v.elements();
                e.hasMoreElements(); i++)
                stats[i]  = ((Integer) e.nextElement()).intValue();
            return stats;
        } catch (NoSuchKeyException e) {
            int[] i= {};
            return i;
        }
    }

    /**
     * Returns the first object tagged with the supplied key.
     *
     * @deprecated This should have no real use in the real world.
     */
    public Object getFirst(int key)
    throws NoSuchKeyException {
	if (!keys[key])
	    throw(new NoSuchKeyException());

	return values[key].elements().nextElement();
    }

    /**
     * Returns the number of keys (supplied selectors) in this Selection.
     */
    public int countKeys() {
	int c = 0;
	for (int i=0;i<keys.length;i++) if (keys[i]) c++;
	return c;
    }

    /**
     * Returns the number of "trail" values in this Selection.
     */
    public int size() {
	int c = 0;
	for (int i=0; i<keys.length; i++) {
	    if (!keys[i]) continue;

	    if (values[i] != null)
		c += values[i].size();
	    else
		c++;
	}
	return c;
    }

    /**
     * Converts this selection into one KomToken object.
     */
    public KomToken toToken() {
	int l = countKeys();
	StringBuffer foo = new StringBuffer();
	for (int i=0;i<keys.length;i++) {
	    if (!keys[i]) continue;
	    if (values[i] == null) {
		foo.append(""+i);
	    } else {
		int j=0;
		for (Enumeration e = values[i].elements();
		     e.hasMoreElements();j++) {
		    foo.append(""+i);
		    Object o = e.nextElement();
		    if (o == null)
			continue;
		    if (o instanceof Integer)
			foo.append(" " + ((Integer)o).intValue());
		    else if (o instanceof KomToken)
			foo.append(" " + 
				   new String(((KomToken)o).toNetwork()));
		    else
			foo.append(" " + values[i]);
		    if (j < values[i].size()-1)
			foo.append(" ");
		}
	    }

	    if (i < l-1)
		foo.append(" ");
	}
	KomToken ugh = new KomToken(foo.toString());
	if (DEBUG > 2) 
	    Debug.println("Tokenized Selection: "+ugh.toString());
	return ugh;
    }
}
