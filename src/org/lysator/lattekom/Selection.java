/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map.Entry;
import java.io.Serializable;

/**
 * <p>
 * A Selection is tagged data, in which each object represent an ordered list of
 * tags (keys) and perhaps some data belonging to each tag.
 * </p>
 * <p>
 * This implementation can only deal with "primitive" LysKOM types, such as
 * INT32, Bitstring, Hollerith and their likes. If you try to set a Selection
 * value to a KomTokenArray or another Selection, for example, you will get very
 * unpredictable results.
 * </p>
 */
public class Selection implements Serializable, Tokenizable {

    Map<Integer, Object> values;
    LinkedList<Integer> keyList;

    public static final int DEBUG = 3;

    /**
     * @deprecated size doesn't matter
     */
    public Selection(int size) {
        this();
    }

    public Selection() {
        values = new HashMap<Integer, Object>();
        keyList = new LinkedList<Integer>();
    }

    /**
     * Adds the tag <tt>key</tt> to this Selection. The supplied object may be
     * null to indicate a tag without a trailing value.
     */
    public Selection add(int key, Object o) {
        values.put(new Integer(key), o);
        keyList.add(new Integer(key));
        return this;
    }

    /**
     * Adds the tag <tt>key</tt> to this selection, with the value
     * <tt>value</tt>.
     */
    public Selection add(int key, int value) {
        add(key, new Integer(value));
        return this;
    }

    /**
     * Returns <tt>true</tt> if this Selection contains the tag <tt>key</tt>
     */
    public boolean contains(int key) {
        return values.containsKey(new Integer(key));
    }

    public int getKey() {
        if (keyList.isEmpty())
            return -1;
        return ((Integer) keyList.get(0)).intValue();
    }

    /**
     * @deprecated
     */
    public Object getValue() {
        return getFirst();
    }

    public Object getFirst() {
        if (keyList.isEmpty())
            return null;
        return values.get(keyList.get(0));
    }

    public int getIntValue() {
        return ((Integer) getFirst()).intValue();
    }

    /**
     * Returns an int representation of the data tagged with <tt>key</tt>,
     * providing that the data is stored as an Integer.
     */
    public int getIntValue(int key) {
        return ((Integer) get(key)).intValue();
    }

    public Object get(int key) {
        if (!values.containsKey(new Integer(key)))
            throw new NoSuchKeyException("key " + key);
        return values.get(new Integer(key));
    }

    /**
     * @deprecated
     */
    public Object getValue(int key) {
        return get(key);
    }

    /**
     * Returns a KomTime value stored with tag <tt>key</tt>.
     */
    public KomTime getTimeValue(int key) {
        return (KomTime) get(key);
    }

    /**
     * Returns a KomToken value stored with tag <tt>key</tt>.
     */
    public KomToken getTokenValue(int key) {
        return (KomToken) get(key);
    }

    /**
     * Removes the object <tt>value</tt> from the tag <tt>key</tt>. It does not
     * remove the tag itself, which then becomes selector without a trailing
     * value.
     * 
     * @return <tt>true</tt> if the tag was found in this selection and
     *         contained <tt>value</tt>
     */
    public boolean remove(int key, Object value) {
        if (get(key).equals(value)) {
            values.put(new Integer(key), null);
            return true;
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
        values.remove(new Integer(key));
        return keyList.remove(new Integer(key));
    }

    /**
     * Returns an array of integers representing all the selectors (keys) in
     * this Selection.
     */
    public int[] getKeys() {
        int[] allkeys = new int[keyList.size()];
        int count = 0;
        for (Iterator<Integer> i = keyList.iterator(); i.hasNext(); count++) {
            allkeys[count] = ((Integer) i.next()).intValue();
        }
        return allkeys;
    }

    /**
     * Returns the number of keys (supplied selectors) in this Selection.
     */
    public int countKeys() {
        return keyList.size();
    }

    /**
     * Returns the number of "trail" values in this Selection.
     */
    public int size() {
        int count = 0;
        for (Iterator<Entry<Integer, Object>> i = values.entrySet().iterator(); i
                .hasNext();) {
            if (i.next().getValue() != null)
                count++;
        }
        return count;
    }

    /**
     * Converts this selection into one KomToken object.
     */
    public KomToken toToken() {
        List<Integer> keys = new ArrayList<Integer>(keyList);
        Collections.sort(keys);
        StringBuffer foo = new StringBuffer();
        for (Iterator<Integer> i = keys.iterator(); i.hasNext();) {
            Integer key = (Integer) i.next();
            foo.append(key);
            Object o = values.get(key);
            if (o == null)
                continue;
            if (o instanceof Integer)
                foo.append(" " + ((Integer) o).intValue());
            else if (o instanceof KomToken)
                foo.append(" " + new String(((KomToken) o).toNetwork()));
            else
                foo.append(" " + o);

            if (i.hasNext())
                foo.append(" ");
        }

        return new KomToken(foo.toString());
    }
}
