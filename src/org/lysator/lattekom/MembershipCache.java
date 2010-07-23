/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

import java.util.Hashtable;

class MembershipCache {
    final static boolean DEBUG = Boolean
            .getBoolean("lattekom.membership-cache.debug");
    Hashtable<Integer, Membership> hash;

    public MembershipCache() {
        hash = new Hashtable<Integer, Membership>();
    }

    public void clear() {
        hash.clear();
    }

    public boolean contains(int conf) {
        return hash.containsKey(new Integer(conf));
    }

    public Membership add(Membership p) {
        if (p.getNo() == -1)
            throw new IllegalArgumentException("Membership has no number");

        if (DEBUG)
            Debug.println("MembershipCache: adding " + p.getNo());

        if (hash.put(new Integer(p.getNo()), p) != null) {
            if (DEBUG)
                Debug.println("MembershipCache: " + "replacing Membership #"
                        + p.getNo() + " in cache");
        }
        return p;
    }

    public boolean remove(int membershipNo) {
        return hash.remove(new Integer(membershipNo)) != null;
    }

    public Membership get(int membershipNo) {
        Membership p = (Membership) hash.get(new Integer(membershipNo));
        if (p != null) {
            if (DEBUG) {
                Debug.println("MembershipCache: returning " + membershipNo);
            }
        }
        return p;
    }
}
