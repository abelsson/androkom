/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 * Uhh.
 *
 */

package nu.dll.lyskom;

/**
 * Class that can be used to check if a text has been read.
 *
 * This implementation is really slow when it comes to a couple of thousand texts.
 */
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
	Debug.println("ReadTextsMap: added " + lastKey);
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

    /**
     * @deprecated changed name to contains(int)
     * @see nu.dll.lyskom.ReadTextsMap#contains(int)
     */
    public boolean exists(int no) {
	return contains(no);
    }

    public boolean contains(int no) {
	Key k = firstKey;
	while (k != null && k.value != no) {
	    k = k.nextKey;
	}
	if (k != null && k.value == no) {
	    Debug.println("ReadTextsMap.exists(): found " + k + " in list");
	    return true;
	}
	Debug.println("ReadTextsMap.exists(): " + no + " not found in list");
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
	public String toString() {
	    
	    return "[ReadTextsMap.Key; value=" + value + ", prev=" + (prevKey != null ? ""+prevKey.value : "null")
		+ ", next=" + (nextKey != null ? ""+nextKey.value : "null") + "]";
	}
    }
}
