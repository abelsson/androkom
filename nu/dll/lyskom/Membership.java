/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * Represents a person's membership status of a given conference.
 * <br>
 *
 * @see nu.dll.lyskom.Session#getUnreadMembership()
 * @see nu.dll.lyskom.Session#getMembership(int)
 * @see nu.dll.lyskom.Session#getMembership(int, int, int, Bitstring)
 */
public class Membership {
    int position;
    protected KomTime lastTimeRead;

    protected int conference;
    protected int priority; // is INT8, use byte?

    boolean hasReadTexts = false;
    protected int addedBy;
    protected KomTime addedAt;
    protected MembershipType type; // Membership-Type

    // seven basic types, two TIME, plus one for array prefix
    public final static int ITEM_SIZE_10 = 8 + KomTime.ITEM_SIZE*2;
    // six basic types, two TIME, plus one for array prefix
    public final static int ITEM_SIZE = 7 + KomTime.ITEM_SIZE*2;

    final static boolean DEBUG = Boolean.getBoolean("lattekom.membership-debug");

    public static class Range {
	public int first;
	public int last;
	public Range(int first, int last) {
	    this.first = first;
	    this.last = last;
	}
	public boolean contains(int no) {
	    return no >= first && no <= last;
	}
	public String toString() { return "{"+first+"-"+last+"}"; }
    }

    List ranges;

    public String toString() {
	return "[Membership<conference: " + conference + "; priority: " +
	    priority + "; readRanges: " + ranges +
	    "; type: " + type + ">]";
    }

    private Membership(int position, KomTime lastTimeRead, int conference,
		       int priority, List ranges,
		       int addedBy, KomTime addedAt, MembershipType type,
		       boolean hasReadTexts) {
	this.position = position;
	this.lastTimeRead = lastTimeRead;
	this.conference = conference;
	this.priority = priority;
	this.ranges = ranges;
	this.addedBy = addedBy;
	this.addedAt = addedAt;
	this.type = type;

	this.hasReadTexts = hasReadTexts;
    }

    public void setLastTextRead(int i) {
        synchronized (ranges) {
	    ranges.clear();
	    ranges.add(new Range(1, i));
	}
    }

    /**
     * Return this memberhips position
     */
    public int getPosition() {
	return position;
    }

    /**
     * Returns the time when this memberships conference was last read
     */
    public KomTime getLastTimeRead() {
	return lastTimeRead;
    }

    /**
     * Returns the conference this membership represents
     */
    public int getConference() {
	return conference;
    }

    /**
     * Returns the priority (0-255) of this membership
     */
    public int getPriority() {
	return priority;
    }

    /**
     * Returns the local text number of the text last read in the conference
     */
    public int getLastTextRead() {
	synchronized (ranges) {
	    if (ranges.size() >= 1) {
		Range range = (Range) ranges.get(0);
		if (range.first == 1) {
		    return range.last;
		} else {
		    return 0;
		}
	    } else {
		return 0;
	    }
	}
    }

    /**
     * Returns an array of local text number that have been read after last-text-read
     */
    public int[] getReadTexts() {
	int lastTextRead = getLastTextRead();
	List readTextsList = new LinkedList();
	synchronized (ranges) {
	    for (Iterator ri = ranges.iterator(); ri.hasNext();) {
		Range range = (Range) ri.next();
		for (int i=range.first; i <= range.last; i++) {
		    if (i > lastTextRead) {
			readTextsList.add(new Integer(i));
		    }
		}
	    }
	}
	int[] readTexts = new int[readTextsList.size()];
	int i=0;
	for (Iterator ri = readTextsList.iterator(); ri.hasNext();) {
	    readTexts[i] = ((Integer) ri.next()).intValue();
	    i++;
	}
	return readTexts;
    }

    public List getReadRanges() {
	return ranges;
    }

    public synchronized void markAsRead(int localNo) {
	synchronized (ranges) {
	    if (!hasReadTexts) {
		return;
	    }
	    if (ranges.size() == 0) {
		ranges.add(new Range(localNo, localNo));
		return;
	    }
	    Range lastRange = (Range) ranges.get(ranges.size()-1);
	    if (localNo == lastRange.last+1) {
		lastRange.last = localNo;
		return;
	    } else if (localNo > lastRange.last+1) {
		ranges.add(new Range(localNo, localNo));
		return;
	    }
	    for (ListIterator i = ranges.listIterator(); i.hasNext();) {
		Range thisRange = (Range) i.next();
		if (thisRange.contains(localNo)) return;

		Range nextRange = null;
		if (i.hasNext()) {
		    nextRange = (Range) i.next();

		    if (localNo == thisRange.last+1) {
			if (nextRange.first > localNo) {
			    thisRange.last = localNo;
			    return;
			} else {
			    if (!(nextRange.first == localNo)) {
				throw new RuntimeException("inconsistent read ranges?");
			    }
			    thisRange.last = nextRange.last;
			    i.remove();
			    return;
			}
		    } else if (localNo > thisRange.last+1 && localNo == nextRange.first-1) {
			nextRange.first = localNo;
		    } else if (localNo > thisRange.last+1 && localNo < nextRange.first-1) {
			i.previous();
			i.add(new Range(localNo, localNo));
			return;
		    }
		    i.previous();
		}
	    }
	    throw new RuntimeException("found no range!");
	}
    }

    public boolean isRead(int localNo) {
	synchronized (ranges) {
	    for (Iterator i = ranges.iterator();i.hasNext();) {
		Range r = (Range) i.next();
		if (r.contains(localNo)) return true;
	    }
	}
	return false;
    }

    /**
     * Returns the number of the person who added this memberhip
     */
    public int getAddedBy() {
	return addedBy;
    }

    /**
     * Returns the time at which this membership was added
     */
    public KomTime getAddedAt() {
	return addedAt;
    }

    /**
     * Returns the type of this membership. TODO: document the Membership types.
     */ 
    public MembershipType getType() {
	return type;
    }

    /*
    public boolean equals(Object o) {
	if (o instanceof Integer) return ((Integer) o).intValue() == conference;
	if (!(o instanceof Membership)) return false;
	return ((Membership) o).getNo() == conference;
    }
    */

    /**
     * Equal to getConference()
     *
     * @see nu.dll.lyskom.Membership#getConference()
     */ 
    public int getNo() {
	return conference;
    }

    public boolean hasReadTexts() {
	return hasReadTexts;
    }

    public static Membership createFrom(int offset, KomToken[] tokens, boolean proto10) {
	int i = offset;

	int position = tokens[i++].intValue();
	KomTime lastTimeRead = KomTime.createFrom(i, tokens);
	i +=  KomTime.ITEM_SIZE;
	int conf = tokens[i++].intValue();
	int prio = tokens[i++].intValue();

	List ranges = new LinkedList();

	boolean hasReadTexts = false;
	if (!proto10) {
	    int rangeArraySize = tokens[i++].intValue();
	    KomToken[] rangeTokens = ((KomTokenArray) tokens[i++]).getTokens();
	    int rangeTokenCount = 0;
	    // xxx: assert that rangeTokenCount is divisible by 2?
	    hasReadTexts = rangeArraySize == rangeTokens.length/2;
	    dprintln("parsing range array (" + rangeArraySize + "): " + Arrays.asList(rangeTokens));
	    for (int rangeNo=0; hasReadTexts && rangeNo < rangeArraySize; rangeNo++) {
		int firstRead = rangeTokens[rangeTokenCount++].intValue();
		int lastRead = rangeTokens[rangeTokenCount++].intValue();
		ranges.add(new Range(firstRead, lastRead));
	    }
	} else {
	    int lastTextRead = tokens[i++].intValue();
	    int readTextsLength = tokens[i++].intValue();

	    if (lastTextRead > 0) {
		ranges.add(new Range(1, lastTextRead));
	    }

	    KomToken[] readTextsTokens = ((KomTokenArray) tokens[i++]).getTokens();
	    int[] readTexts = new int[readTextsTokens.length];
	    hasReadTexts = readTextsTokens.length == readTextsLength;
	    
	    for (int rtIdx=0; rtIdx < readTextsTokens.length; rtIdx++) {
		readTexts[rtIdx] = readTextsTokens[rtIdx].intValue();
	    }

	    if (readTexts.length > 0) {
		Range range = new Range(readTexts[0], readTexts[0]);
		ranges.add(range);
		for (int j=1; j < readTexts.length; j++) {
		    if (readTexts[j] == readTexts[j-1]+1) {
			range.last = readTexts[j];
		    } else {
			range = new Range(readTexts[j], readTexts[j]);
			ranges.add(range);
		    }
		}
	    }
	}
	
	int addedBy = tokens[i++].intValue();
	KomTime addedAt = KomTime.createFrom(i, tokens);
	i += KomTime.ITEM_SIZE;
	
	MembershipType type = new MembershipType(Bitstring.createFrom(i++, tokens));
	
	Membership m = new Membership(position, lastTimeRead, conf, prio, ranges,
				      addedBy, addedAt, type, hasReadTexts);
	
	if (DEBUG) {
	    dprintln("Membership: parsed " + m + 
		     (!hasReadTexts ? " (no read-texts supplied)" : ""));
	}
	return m;
    }

    public static Membership[] createFromArray(int offset, KomToken[] parameters,
					       boolean proto10) {
	int pcount = offset;
	int length = parameters[pcount++].intValue();
	KomToken[] tokens = ((KomTokenArray) parameters[pcount++]).getTokens();
	
	Membership[] ml = new Membership[length];

	int i=0, membershipCount = 0;
	while (i < tokens.length) {
	    ml[membershipCount++] = createFrom(i, tokens, proto10);
	    i += proto10 ? ITEM_SIZE_10 : ITEM_SIZE;
	}
	return ml;
	
    }

    private static void dprintln(String s) {
	if (DEBUG) Debug.println(s);
    }

}
