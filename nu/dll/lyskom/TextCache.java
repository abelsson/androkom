/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.Hashtable;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;

class TextCache {
    static int DEBUG = 1;

    int maxAge = 60*60*1000; // one hour
    int maxSize = 100;

    Hashtable hash;
    TreeMap ageMap = new TreeMap();

    public TextCache() {
	hash = new Hashtable();
    }

    public void remove(int textNo) {
	if (Debug.ENABLED) {
	    Debug.println("TextCache.remove(" + textNo + ")");
	}
	hash.remove(new Integer(textNo));
	ageMap.remove(new Integer(textNo));	
    }

    public void add(Text t) {	
	if (t.getNo() == -1)
	    return; // throw(new TextNumberException("Text has no number"));
	checkLimits(0);
	if (DEBUG > 0) Debug.println("TextCache: adding "+t.getNo());
	if (hash.put((Object) new Integer(t.getNo()), (Object) t)!=null) {
	    if (DEBUG > 0) Debug.println("TextCache: " +
					      "replacing text #" +
					      t.getNo()+" in cache");
	}

	ageMap.put(new Integer(t.getNo()), new Long(System.currentTimeMillis()));
    }

    public boolean contains(int n) {
	checkLimits(n);
	return hash.contains(new Integer(n));
    }

    public Text get(int textNo) {
	checkLimits(textNo);
	Text t = (Text) hash.get(new Integer(textNo));
	Debug.println("TextCache: returning "+t);
	return t;
    }


    /**
     * Experimental LRU garb for the TextCache. Probably the least efficient
     * implementation available. :-)
     */
    synchronized void checkLimits(int textNo) {
	Integer iNo = new Integer(textNo);
	if (textNo > 0 && ageMap.containsKey(iNo)) {
	    long age = System.currentTimeMillis() - ((Long) ageMap.get(iNo)).longValue();
	    if (age > maxAge) {
		ageMap.remove(iNo);
		hash.remove(iNo);
		Debug.println("TextCache: removed " + iNo + " of age " + age);
	    }
	}

	if (hash.size() > maxSize) {
	    List ages = new ArrayList(ageMap.values());
	    List texts = new ArrayList(hash.values());
	    List sortedAges = new LinkedList();
	    List sortedTexts = new LinkedList();

	    Iterator ageIterator = ages.iterator();
	    Iterator textIterator = texts.iterator();
	    long lastValue = 0;
	    int lastIndex = -1;
	    while (ageIterator.hasNext()) {
		Long l = (Long) ageIterator.next();
		if (lastIndex == -1) {
		    lastIndex = 0;
		    lastValue = l.longValue();
		    sortedAges.add(l);
		    sortedTexts.add(textIterator.next());
		} else {
		    if (lastValue > l.longValue()) {
			sortedAges.add(lastIndex+1, l);
			sortedTexts.add(lastIndex+1, textIterator.next());
		    } else {
			sortedAges.add(lastIndex, l);
			sortedTexts.add(lastIndex, textIterator.next());
		    }
		    lastIndex++;
		}
	    }

	    Iterator sortedTextsIterator = sortedTexts.iterator();
	    int count = 0;
	    while (count < (hash.size() - maxSize) && sortedTextsIterator.hasNext()) {
		Text t = (Text) sortedTextsIterator.next();
		Debug.println("TextCache: size trim: removed " + t.getNo() + " of age " +
			      (System.currentTimeMillis()-((Long) ageMap.get(new Integer(t.getNo()))).longValue()));
		remove(t.getNo());
		count++;
	    }
	}
    }
    
}
