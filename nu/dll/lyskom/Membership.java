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

    TextMapping textMap;

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

    public void setTextMapping(TextMapping tm) {
	this.textMap = tm;
    }

    public TextMapping getTextMapping() {
	if (textMap != null) textMap.first();
	return textMap;
    }

    /*
    public boolean equals(Object o) {
	if (o instanceof Integer) return ((Integer) o).intValue() == conference;
	if (!(o instanceof Membership)) return false;
	return ((Membership) o).getNo() == conference;
    }
    */

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
	Debug.println("Reported Membership-List ARRAY length: " + length);
	KomToken[] memberships = ((KomTokenArray) parameters[pcount++]).getTokens();
	
	/*
	KomToken[][] memberships =
	    KomTokenArray.split((KomTokenArray) parameters[pcount++]);
	*/
	Membership[] ml = new Membership[memberships.length];

	Debug.println("Membership-List length: "+length+", array length: "+ml.length);
	Debug.println("From reply: " + reply);
	int i=0, membershipCount = 0;
	while (i < memberships.length) {

	    int position = memberships[i++].toInteger();
	    Debug.println("Membership position: " + position);
	    KomTime lastTimeRead = KomTime.createFrom(i, memberships);
	    i +=  KomTime.ITEM_SIZE;
	    Debug.println("Membership last-time-read: " + lastTimeRead.getTime().toString());
	    int conf = memberships[i++].toInteger();
	    Debug.println("Membership conf-no: " + conf);
	    int prio = memberships[i++].toInteger();
	    Debug.println("Membership prio: " + prio);
	    int lastTextRead = memberships[i++].toInteger(); // !! last -> lastTextRead
	    Debug.println("Membership last-text-read: " + lastTextRead);
	    int readTextsLength = memberships[i++].toInteger();
	    Debug.println("Membership read-texts ARRAY length: " + readTextsLength);
	    KomToken[] readTextsTokens = new KomToken[readTextsLength];
	    int[] readTexts = new int[readTextsLength];

	    if (readTextsLength == 0) {
		i += 2; // skip
	    } else {
		//i += 1;
		Debug.println("Membership read-texts exp ARRAY got " + memberships[i].getClass().getName());
		readTextsTokens = ((KomTokenArray) memberships[i]).getTokens();
		readTexts = new int[readTextsTokens.length];
		if (readTextsTokens.length == 0) {
		    Debug.println("Membership read-texts array is actually empty");
		    i += 2;
		} else {
		    for (int k=0;k<readTextsTokens.length;k++) {
			Debug.println("Membership read-texts ARRAY index " + k + ": " + readTextsTokens[k].intValue());
			readTexts[k] = readTextsTokens[k].intValue();
		    }
		    i += 1;
		}
	    }
	    

	    int addedBy = memberships[i++].toInteger(); // not used
	    Debug.println("Membership added-by: " + new String(memberships[i].getContents()));
	    KomTime addedAt = KomTime.createFrom(i, memberships); // not used
	    i += KomTime.ITEM_SIZE;
	    Debug.println("Membership added-at: " + addedAt.getTime().toString());
	    Bitstring type = new Bitstring();
	    type = Bitstring.createFrom(i++, memberships); // not used
	    Debug.println("Membership type: " + new String(type.getContents()));

	    ml[membershipCount++] = new Membership(lastTimeRead, conf, prio, lastTextRead,
				     readTexts);
	    Debug.println("done, parsed membership of conf " + conf + ", " + i + " elements");
	    //for (int k=0;k<memberships[i].length;k++) {
	    //Debug.print("["+k+"="+memberships[i][k]+"]");
	    //}
	    //Debug.println("...");
	    
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
