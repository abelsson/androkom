/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;
import java.util.Enumeration;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Stack;

public class TextStat implements java.io.Serializable {

    // Misc-Info selection values
    public final static int miscRecpt    =  0;
    public final static int miscCcRecpt  =  1;
    public final static int miscCommTo   =  2;
    public final static int miscCommIn   =  3;
    public final static int miscFootnTo  =  4;
    public final static int miscFootnIn  =  5;
    public final static int miscLocNo    =  6;
    public final static int miscRecTime  =  7;
    public final static int miscSentBy   =  8;
    public final static int miscSentAt   =  9;
    public final static int miscXAuthor  = 10;
    public final static int miscXPerson  = 11;
    public final static int miscXRecpt   = 12;
    public final static int miscXText    = 13;
    public final static int miscXSystem  = 14;
    public final static int miscBccRecpt = 15; // prot. 10

    public final static int MISC_INFO_COUNT = 15;

    final static int INITIAL_AUX_LENGTH = 0;

    static int DEBUG = 1;

    AuxItem[] auxItems = new AuxItem[INITIAL_AUX_LENGTH];
    int auxItemCount = 0;

    public KomTime creationTime;
    public int author;
    public int lines;
    public int chars;
    public int marks;

    int no;

    //public Selection miscInfo;
    public List miscInfo = new LinkedList();

    public TextStat() {
	this(0);
    }

    public TextStat(int no) {
	super();
	this.no = no;
	miscInfo = new LinkedList();
    }

    public void setNo(int no) {
	this.no = no;
    }

    public int getNo() {
	return no;
    }

    public int getAuthor() {
	return author;
    }

    public AuxItem[] getAuxItems() {
	return auxItems;
    }

    public KomTime getCreationTime() {
	return creationTime;
    }

    public void addAuxItem(AuxItem a) {
	if (auxItemCount == auxItems.length) {
	    AuxItem[] newArray = new AuxItem[++auxItemCount];
	    for (int i=0;i<auxItems.length;i++) newArray[i] = auxItems[i];
	    auxItems = newArray;
	    auxItems[auxItems.length-1] = a;
	    return;
	} else {
	    auxItems[auxItemCount++] = a;
	}
    }

    public int countAuxItems() {
	int c=0;
	for (int i=0;i<auxItems.length;i++)
	    if (auxItems[i] != null) c++;
	return c;
    }

    //    public Selection getMiscInfo() {
    public List getMiscInfo() {
	return miscInfo;
    }

    public void clearMiscInfoEntry(int key) {
	Iterator i = miscInfo.iterator();
	Stack toRemove = new Stack();
	int count = 0;
	while (i.hasNext()) {
	    Selection selection = (Selection) i.next();
	    if (selection.contains(key)) {
		toRemove.push(selection);
		Debug.println("removed misc-info selection " + selection);
		count++;
	    }
	}
	while (!toRemove.isEmpty()) miscInfo.remove(toRemove.pop());

	Debug.println("clearMiscInfoEntry: found " + count + " selections with flag " + key);
    }

    public void removeMiscInfoEntry(int key, int value) {
	Iterator i = miscInfo.iterator();
	int count = 0;
	while (i.hasNext()) {
	    Selection selection = (Selection) i.next();
	    if (selection.contains(key)) {
		selection.remove(key, new Integer(value));
		Debug.println("removed key " + key + ", value " + value + " from misc-info " + selection);
		count++;
	    }
	}
	Debug.println("removeMiscInfoEntry(" + key + ", " + value + "): found " + count + " keys");
    }

    public void addMiscInfoEntry(int key, Integer value) {
	addMiscInfoEntry(key, value.intValue());
    }

    public void addMiscInfoEntry(int key, int value) {
	Iterator i = miscInfo.iterator();
	/*
	while (i.hasNext()) {
	    Selection selection = (Selection) i.next();
	    if (selection.contains(key)) {
		selection.add(key, new Integer(value));
		Debug.println("adding key " + key + ", value " + value + " to selection " + selection);
		return;
	    }
	    }*/
	Selection selection = new Selection(TextStat.MISC_INFO_COUNT);
		Debug.println("adding key " + key + ", value " + value + " to new selection " + selection);
	selection.add(key, new Integer(value));	
	miscInfo.add(selection);
    }

    /**
     * Returns an int[] for Misc-Info members with integer values
     *
     */
    public int[] getStatInts(int no) {
	List values = new LinkedList();
	Iterator i = miscInfo.iterator();
	while (i.hasNext()) {
	    Selection selection = (Selection) i.next();
	    if (selection.contains(no)) {
		Enumeration e = selection.get(no);
		while (e.hasMoreElements()) values.add(e.nextElement());
	    }
	}
	int[] stats = new int[values.size()];
	i = values.iterator();
	for (int j=0; j < stats.length; j++) {
	    stats[j] = ((Integer) i.next()).intValue();
	}
	return stats;
    }

    /* Gah. all createFrom() should be constructors, I guess. */
    public static TextStat createFrom(int no, RpcReply reply) {
	KomToken[] params = reply.getParameters();
	TextStat ts = new TextStat(no);
	List miscInfo = ts.getMiscInfo();

	int pcount = 0;
	ts.creationTime = new KomTime(params[pcount++].intValue(), // 0
				      params[pcount++].intValue(), // 1
				      params[pcount++].intValue(), // 2
				      params[pcount++].intValue(), // 3
				      params[pcount++].intValue(), // 4
				      params[pcount++].intValue(), // 5
				      params[pcount++].intValue(), // 6
				      params[pcount++].intValue(), // 7
				      params[pcount++].intValue());

	ts.author = params[pcount++].intValue();
	ts.lines  = params[pcount++].intValue();
	ts.chars  = params[pcount++].intValue();
	ts.marks  = params[pcount++].intValue();

	int arrayLength = params[pcount++].intValue();
	KomToken[] miscInfoTokens =
	    ((KomTokenArray) params[pcount++]).getTokens();

	int auxItemArrayLength = params[pcount++].intValue();
	Debug.println("TextStat.createFrom(): aux-item list length: " + auxItemArrayLength);
	KomToken[] auxItemTokens =
	    ((KomTokenArray) params[pcount++]).getTokens();

	int mcount = 0;

	// This should probably be in Selection.createFrom(RpcReply)
	int lastMajorSelectionId = -1;
	Selection selection = null;
	for (int i=0;i<arrayLength;i++) {
	    int selectionId = miscInfoTokens[mcount++].intValue();


	    switch (selectionId) {
		/* items to be stored as Integer */
	    case 0: // recipient : Conf-No
	    case 1: // cc-recipient
	    case 2: // comment-to
	    case 3: // commented-in
	    case 4: // footnote-to
	    case 5: // footnote-in
	    case 15: // bcc-recipient
		lastMajorSelectionId = selectionId;
		Debug.println("new misc-info selection group: " + selectionId);
		selection = new Selection(MISC_INFO_COUNT);
		miscInfo.add(selection);

	    case 6: // loc-no                  ! Lokalt textnummer
	    case 8: // sent-by
		int value = miscInfoTokens[mcount++].intValue();
		Debug.println("adding key " + selectionId + ", value " + value + " to misc-info group " + lastMajorSelectionId);
		selection.add(selectionId, new Integer(value));
		//miscInfo.add(selectionId,
		//     (Object) new Integer(miscInfoTokens[mcount++].
		//			  toInteger()));
		break;
		
		/* items to be stored as KomTime */
	    case 7: // rec-time : Time
	    case 9: // sent-at : Time
		KomTime stm = new KomTime(miscInfoTokens[mcount++].intValue(),
					  miscInfoTokens[mcount++].intValue(),
					  miscInfoTokens[mcount++].intValue(),
					  miscInfoTokens[mcount++].intValue(),
					  miscInfoTokens[mcount++].intValue(),
					  miscInfoTokens[mcount++].intValue(),
					  miscInfoTokens[mcount++].intValue(),
					  miscInfoTokens[mcount++].intValue(),
					  miscInfoTokens[mcount++].intValue()
					  );
		selection.add(selectionId, (Object) stm);
		break;
	    default:
		break;
	    }
	    
	}

	int acount = 0;
	while (acount < auxItemTokens.length) {
	    KomToken[] ai = new KomToken[AuxItem.ITEM_SIZE];
	    for (int i=0; i < AuxItem.ITEM_SIZE; i++)
		ai[i] = auxItemTokens[acount++];

	    ts.addAuxItem(new AuxItem(ai));
	}


	return ts;

    }
}
