/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.*;

public class TextStatCache {

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

    public TextStat get(int textNo) {
	TextStat t = (TextStat) hash.get(new Integer(textNo));
	if (t != null) Debug.println("TextCache: returning "+textNo);
		
	return t;
    }
}
