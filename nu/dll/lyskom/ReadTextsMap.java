/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 * Uhh.
 *
 */

package nu.dll.lyskom;

import java.util.Set;
import java.util.HashSet;

/**
 * Class that can be used to check if a text has been read.
 *
 */
public class ReadTextsMap {

    Set textNumberSet = new HashSet();

    public void add(int no) {
	textNumberSet.add(new Integer(no));
	return;
    }

    public int count() {
	return textNumberSet.size();
    }	    

    /**
     * @deprecated changed name to contains(int)
     * @see nu.dll.lyskom.ReadTextsMap#contains(int)
     */
    public boolean exists(int no) {
	return contains(no);
    }

    public boolean contains(int no) {
	return textNumberSet.contains(new Integer(no));
    }

    public void remove(int no) {
	textNumberSet.remove(new Integer(no));
    }
}
