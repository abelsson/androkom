/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

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
    protected int lastTextRead; // local!
    protected int[] readTexts; // local!
    protected int addedBy;
    protected KomTime addedAt;
    protected MembershipType type; // Membership-Type

    public static int ITEM_SIZE = 8 + KomTime.ITEM_SIZE*2;

    final static int DEBUG = Integer.getInteger("lattekom.membership-debug", 0).intValue();

    //TextMapping textMap;

    Membership() {
	super();
    }

    Membership(int offset, KomToken[] tk) {
	this.position = tk[offset++].toInteger();
	this.lastTimeRead = KomTime.createFrom(offset, tk);
	offset += KomTime.ITEM_SIZE;
	this.conference = tk[offset++].toInteger();
	this.priority = tk[offset++].toInteger();
	this.lastTextRead = tk[offset++].toInteger();
	int readTextsArraySize = tk[offset++].toInteger();
	this.readTexts = ((KomTokenArray) tk[offset++]).intValues();
	this.addedBy = tk[offset++].toInteger();
        this.addedAt = KomTime.createFrom(offset, tk);
	offset += KomTime.ITEM_SIZE;
	this.type = new MembershipType(tk[offset++]);
    }

    Membership(int position, KomTime lastTimeRead, int conference,
	       int priority, int lastTextRead, int[] readTexts,
	       int addedBy, KomTime addedAt, MembershipType type) {
	this.position = position;
	this.lastTimeRead = lastTimeRead;
	this.conference = conference;
	this.priority = priority;
	this.lastTextRead = lastTextRead;
	this.readTexts = readTexts;
	this.addedBy = addedBy;
	this.addedAt = addedAt;
	this.type = type;
    }

    Membership(KomTime lastTimeRead, int conference,
	       int priority, int lastTextRead, int[] readTexts) {
	this.lastTimeRead = lastTimeRead;
	this.conference = conference;
	this.priority = priority;
	this.lastTextRead = lastTextRead;
	this.readTexts = readTexts;
    }

    public void setLastTextRead(int i) {
        lastTextRead = i;
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
	return lastTextRead;
    }

    /**
     * Returns an array of local text number that have been read after last-text-read
     */
    public int[] getReadTexts() {
	return readTexts;
    }

    public synchronized void markAsRead(int localNo) {
	if (localNo == lastTextRead+1) {
	    lastTextRead = localNo;
	    return;
	} 

	if (localNo > lastTextRead+1) {
	    int[] tmp = new int[readTexts.length+1];
	    for (int i=0; i < readTexts.length; i++) {
		if (localNo == readTexts[i]) return;
		tmp[i] = readTexts[i];
	    }
	    tmp[tmp.length-1] = localNo;
	}
    }

    public boolean isRead(int localNo) {
	if (localNo <= lastTextRead) return true;
	for (int i=0; i < readTexts.length; i++) {
	    if (localNo == readTexts[i]) return true;
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
    public Bitstring getType() {
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

    /**
     * Constructs a Membership array out of the supplied RpcReply object
     */
    public static Membership[] createFrom(RpcReply reply) {
	KomToken[] parameters = reply.getParameters();
	int pcount = 0;
	int length = parameters[pcount++].toInteger();
	KomToken[] memberships = ((KomTokenArray) parameters[pcount++]).getTokens();
	
	Membership[] ml = new Membership[memberships.length];

	int i=0, membershipCount = 0;
	while (i < memberships.length) {

	    int position = memberships[i++].toInteger();
	    KomTime lastTimeRead = KomTime.createFrom(i, memberships);
	    i +=  KomTime.ITEM_SIZE;
	    int conf = memberships[i++].toInteger();
	    int prio = memberships[i++].toInteger();
	    int lastTextRead = memberships[i++].toInteger(); // !! last -> lastTextRead
	    int readTextsLength = memberships[i++].toInteger();
	    
	    KomToken[] readTextsTokens = ((KomTokenArray) memberships[i++]).getTokens();
 	    int[] readTexts = new int[readTextsLength];

	    for (int rtIdx=0; rtIdx < readTextsTokens.length; rtIdx++) {
		readTexts[rtIdx] = readTextsTokens[rtIdx].intValue();
	    }

	    int addedBy = memberships[i++].toInteger(); // not used
	    KomTime addedAt = KomTime.createFrom(i, memberships); // not used
	    i += KomTime.ITEM_SIZE;
	    Bitstring type = new Bitstring();
	    type = Bitstring.createFrom(i++, memberships); // not used

	    ml[membershipCount++] = new Membership(lastTimeRead, conf, prio, lastTextRead,
				     readTexts);
	    if (DEBUG > 0) dprintln("done, parsed membership of conf " + conf + ", " + i + " elements");
	    
	}
	return ml;
	
    }

    private static void dprintln(String s) {
	if (DEBUG > 0) Debug.println(s);
    }

}
