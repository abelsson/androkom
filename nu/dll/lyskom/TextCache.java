/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.Hashtable;

public class TextCache {
    static int DEBUG = 1;
    Hashtable hash;
    
    public TextCache() {
	hash = new Hashtable();
    }

    public void remove(int textNo) {
	hash.remove(new Integer(textNo));
    }

    public void add(Text t) {
	if (t.getNo() == -1)
	    return; // throw(new TextNumberException("Text has no number"));

	if (DEBUG > 0) Debug.println("TextCache: adding "+t.getNo());
	if (hash.put((Object) new Integer(t.getNo()), (Object) t)!=null) {
	    if (DEBUG > 0) Debug.println("TextCache: " +
					      "replacing text #" +
					      t.getNo()+" in cache");
	}
    }

    public Text get(int textNo) {
	Text t = (Text) hash.get(new Integer(textNo));
	if (t != null)
	    if (DEBUG > 0) 
		Debug.println("TextCache: returning "+textNo);
		
	return t;
    }
}
