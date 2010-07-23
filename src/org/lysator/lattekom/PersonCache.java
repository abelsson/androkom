/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

import java.util.Hashtable;

// this is a copy of TextCache, but for Person objects.
// their common code (like, everything except the casting) should be
// joined, somehow.

class PersonCache {
    static int DEBUG = 0;
    private Hashtable<Integer, Person> hash;

    public PersonCache() {
        hash = new Hashtable<Integer, Person>();
    }

    public void remove(int persNo) {
        hash.remove(new Integer(persNo));
    }

    public void clear() {
        hash.clear();
    }

    public void add(Person p) {
        if (p.getNo() == -1)
            return;

        if (DEBUG > 0)
            Debug.println("PersonCache: adding " + p.getNo());

        if (hash.put(new Integer(p.getNo()), p) != null) {
            if (DEBUG > 0)
                Debug.println("PersonCache: " + "replacing Person #"
                        + p.getNo() + " in cache");
        }
    }

    public Person get(int personNo) {
        Person p = (Person) hash.get(new Integer(personNo));
        if (p != null) {
            if (DEBUG > 0) {
                Debug.println("PersonCache: returning " + personNo);
            }
        }
        return p;
    }
}
