/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

public class ConferenceCache {
    static int DEBUG = 1;
    Hashtable confHash, uConfHash;
    Vector listeners;

    public ConferenceCache() {
	confHash = new Hashtable();
	uConfHash = new Hashtable();
	listeners = new Vector(1);
    }

    public void addCacheListener(CacheListener l) {
	listeners.addElement((Object) l);
    }

    public void removeCacheListener(CacheListener l) {
	listeners.removeElement((Object) l);
    }

    private void _add(Object t) {
	Hashtable hash;
	int confNo;
	if (t instanceof Conference) {
	    hash = confHash;
	    confNo = ((Conference) t).getNo();
	} else if (t instanceof UConference) {
	    hash = uConfHash;
	    confNo = ((UConference) t).getNo();
	} else {
	    return; // throw(new ConferenceCacheException("Bad class"));
	}

	if (confNo == -1) return; // throw something

	if (hash.put(new Integer(confNo), t) != null) {
            for (Enumeration e = listeners.elements();e.hasMoreElements();) {
                ((CacheListener) e.nextElement()).itemUpdated(t);
            }
	} else {
            for (Enumeration e = listeners.elements();e.hasMoreElements();) {
                ((CacheListener) e.nextElement()).itemAdded(t);
            }
	}
    }

    public void add(Conference t) {
	_add((Object) t);
    }

    public void add(UConference t) {
	_add((Object) t);
    }

    public Conference getConference(int conferenceNo) {
	Conference t = (Conference) confHash.get(new Integer(conferenceNo));
        Debug.println("ConferenceCache: returning "+t);
	return t;
    }

    public UConference getUConference(int conferenceNo) {
	UConference t = (UConference) uConfHash.get(new Integer(conferenceNo));
	Debug.println("ConferenceCache: returning "+t);
		
	return t;
    }
}
