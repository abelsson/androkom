/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 * Uhh.
 *
 */

package nu.dll.lyskom;

public class ReadTextsMap {

    Key firstKey = null;
    Key lastKey = null;

    public int keys = 0;

    public void add(int no) {
	if (exists(no)) return;
	if (firstKey == null) {
	    lastKey = firstKey = new Key(no, null);
	} else {
	    lastKey = lastKey.nextKey = new Key(no, lastKey);
	}
	keys++;
    }

    public int count() {
	int c = 0;
	Key k = firstKey;
	while (k != null) {
	    k = k.nextKey; c++;
	}
	return c;
    }	    

    public boolean exists(int no) {
	Key k = firstKey;
	while (k != null && k.value != no) k = k.nextKey;
	if (k != null) return true;
	return false;
    }

    public void remove(int no) {
	Key k = firstKey;
	while (k != null && k.value != no) k = k.nextKey;	
	if (k != null) remove(k);	
    }

    private void remove(Key k) {
	if (k.prevKey != null) {
	    k.prevKey.nextKey = k.nextKey;
	} else {
	    if (k.nextKey != null) {
		firstKey = firstKey.nextKey;
		firstKey.prevKey = null;
	    } else firstKey = null;
	}
	
	keys--;
    }

    class Key {
	public Key nextKey, prevKey;
	public int value;
	public Key(int n, Key prev) {
	    value = n;
	    prevKey = prev;
	}
    }
}
