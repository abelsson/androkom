/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

// warning, this is a mishmash of pre-v10 and post-v10 stuff, a lot
// of old junk needs to be cleaned up.
public class Membership {
    int position;
    public KomTime lastTimeRead;
    public int conference;
    public int priority; // is INT8, use byte?
    public int lastTextRead; // local!
    public int[] readTexts; // local!
    public int addedBy;
    public KomTime addedAt;
    public Bitstring type; // Membership-Type

    public static int ITEM_SIZE = 8 + KomTime.ITEM_SIZE*2;

    final static int DEBUG = 1;

    public Membership() {
	super();
    }

    public Membership(int offset, KomToken[] tk) {
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
	this.type = new Bitstring(tk[offset++]);
    }

    public Membership(int position, KomTime lastTimeRead, int conference,
		      int priority, int lastTextRead, int[] readTexts,
		      int addedBy, KomTime addedAt, Bitstring type) {
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

    public Membership(KomTime lastTimeRead, int conference,
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

    public int getLastTextRead() {
        return lastTextRead;   
    }

    public int getNo() {
	return conference;
    }

    public static Membership[] createFrom(RpcReply reply) {
	KomToken[] parameters = reply.getParameters();
	int pcount = 0;
	int length = parameters[pcount++].toInteger();
	KomToken[][] memberships =
	    KomTokenArray.split((KomTokenArray) parameters[pcount++]);
	Membership[] ml = new Membership[memberships.length];

	if (DEBUG>0) Debug.println("Membership-List length: "+length+", array length: "+ml.length);
	for (int i=0;i<memberships.length;i++) {
	    if (DEBUG>0) Debug.println("membership #"+i+"...");
	    int j=0;
	    try {
		
		int position = memberships[i][j++].toInteger(); // not used
		KomTime lastTimeRead = KomTime.createFrom(j, memberships[i]);
		j = j + KomTime.ITEM_SIZE;
		int conf = memberships[i][j++].toInteger();
		int prio = memberships[i][j++].toInteger();
		int lastTextRead = memberships[i][j++].toInteger(); // !! last -> lastTextRead
		int readTextsLength = memberships[i][j++].toInteger();
		KomToken[] readTextsTokens = ((KomTokenArray) memberships[i][j++]).getTokens();
		int[] readTexts;
		if (readTextsTokens.length == 0) { // readTextLength can't 
		    readTexts = new int[0];        // be trusted?
		} else {
		    readTexts = new int[readTextsLength];
		    for (int k=0;k<readTextsLength;k++)
			readTexts[k] = readTextsTokens[k].toInteger();
		}
		int addedBy = memberships[i][j++].toInteger(); // not used
		KomTime addedAt = KomTime.createFrom(j, memberships[i]); // not used
		j = j + KomTime.ITEM_SIZE;
		Bitstring type = Bitstring.createFrom(j, memberships[i]); // not used
												     
		ml[i] = new Membership(lastTimeRead, conf, prio, lastTextRead,
				       readTexts);
	    } catch (ArrayIndexOutOfBoundsException ex) {
		Debug.println("Oj-bang: "+ex);
		Debug.println("i="+i+", j="+j);
		for (int k=0;k<memberships[i].length;k++) {
		    Debug.print("["+k+"="+memberships[i][k]+"]");
		}
		System.err.println("Aeeeee!");
		ex.printStackTrace();
		System.exit(-1);
	    }
	   
	}
	return ml;

    }

    // reads 14 elements? **BROKEN!**
    public static Membership createFrom(int offset, KomToken[] tokens) {
	if (DEBUG>0) Debug.println("-->Membership.createFrom("+offset+", KomToken["+tokens.length+"])");
	Membership m = new Membership();
	m.lastTimeRead = KomTime.createFrom(offset, tokens);
	offset = offset + 9;
	m.conference = tokens[offset++].toInteger();
	m.priority = tokens[offset++].toInteger();
	m.lastTextRead = tokens[offset++].toInteger();
	
	/*
	int len = tokens[offset++].toInteger();
	if (DEBUG>0) Debug.println("Membership->read-texts array len: " +
				      len);
	if (len == -1)
	    Debug.println("Oops? offset: "+offset+" Token: "+tokens[offset-1]);
	*/

	KomToken[] rtexts = ((KomTokenArray) tokens[offset++]).getTokens();
	m.readTexts = new int[rtexts.length];
	for(int i=offset;i<rtexts.length;i++)
	    m.readTexts[i] = rtexts[i].toInteger();
	return m;
    }

}
