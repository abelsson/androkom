/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

//import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.io.Serializable;

/** NOTE: as of now, only Integer and KomToken objects are handled
 ** correctly by the toToken() method.
 */

/**
 * todo: move the selection stuff in TextStat to a createFrom() method
 *       here. (or constructor)
 */
public class Selection implements Serializable {
    boolean[] keys; // since a selection item can be empty
    Vector[] values;

    public static final int DEBUG = 3;

    // size is actually the parameter range that is valid in this
    // Selection; 15 (0-14) for Misc-Info, for example.
    public Selection(int size) {
	keys = new boolean[size];
	values = new Vector[size];
    }

    public Selection add(int key, Object o) {
	keys[key] = true;

	if (values[key] == null)
	    values[key] = new Vector(1);

	values[key].addElement(o);
	return this;
    }

    public Enumeration get(int key)
    throws NoSuchKeyException {
	if (!keys[key])
	    throw(new NoSuchKeyException());

	return values[key].elements();
    }

    public int[] getKeys() {
	int keycount = 0;
	for (int i=0; i < keys.length; i++)
	    if (keys[i]) keycount++;
	int[] allkeys = new int[keycount];
	for (int i=0, c=0; i < keys.length; i++)
	    if (keys[i]) allkeys[c++] = i;
	return allkeys;
    }

    public Vector getVector(int key)
    throws NoSuchKeyException {
	if (!keys[key])
	    throw(new NoSuchKeyException());
	return values[key];
    }

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

    public Object getFirst(int key)
    throws NoSuchKeyException {
	if (!keys[key])
	    throw(new NoSuchKeyException());

	return values[key].elements().nextElement();
    }

    public int countKeys() {
	int c = 0;
	for (int i=0;i<keys.length;i++) if (keys[i]) c++;
	return c;
    }

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
    
