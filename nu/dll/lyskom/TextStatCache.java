/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.*;

class TextStatCache {

    Hashtable hash;
    
    public TextStatCache() {
	hash = new Hashtable();
    }

    public synchronized void add(TextStat t) {
	if (t.getNo() == -1)
	    return; // throw(new TextNumberException("Text has no number"));

	Debug.println("TextStatCache: adding "+t.getNo());
	if (hash.put((Object) new Integer(t.getNo()), (Object) t)!=null) {
	    Debug.println("TextStatCache: " +
		          "replacing text-stat #" +
			  t.getNo()+" in cache");
	}
    }

    public void clear() {
	hash.clear();
    }

    public synchronized boolean remove(int textNo) {
	return hash.remove(new Integer(textNo)) != null;
    }

    public TextStat get(int textNo) {
	TextStat t = (TextStat) hash.get(new Integer(textNo));
	if (t != null) Debug.println("TextStatCache: returning "+t);
		
	return t;
    }
}
