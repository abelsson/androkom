/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class Text extends Hollerith implements java.io.Serializable {

    public final static int DEBUG = 1;

    boolean cached = false;

    int textNo = -1;
    TextStat stat = null;
    Map localMap = new HashMap();

    public Text setCached(boolean b) {
	cached = b; return this;
    }

    public boolean isCached(boolean n) { return cached; }

    /** constructors **/
    public Text() {
	setContents(new byte[] {});
	stat = new TextStat();
    }
    public Text(int no, byte[] contents) {
	setContents(contents);
	textNo = no;
    }

    public Text(int textNo) {	
	this.textNo = textNo;
    }

    public Text(int textNo, TextStat stat) {
	this.textNo = textNo;
	this.stat = stat;
    }

    public Text(String subject, String body) {
	stat = new TextStat();
	setContents((subject+"\n"+body).getBytes());
    }

    public Text(byte[] contents) {
	setContents(contents);
    }

    /** end of constructors **/

    public void trimContents() {
	byte[] contents = getContents();

	if (contents == null || contents.length == 0)
	    return;

	int lastchar = 0;	
	for (int i=contents.length-1; i >= 0; i--) {
	    if (contents[i] == ' ' || contents[i] == '\n') continue;
	    lastchar = i;
	    break;
	}
	byte[] newcontents = new byte[lastchar+1];
	for (int i=0; i < newcontents.length; i++)
	    newcontents[i] = contents[i];
	setContents(newcontents);
	    
    }

    public int getRows() {
	int count = 0;
	for (int i=0; i < contents.length; i++)
	    if (contents[i] == '\n') count++;
	return count;
    }

    public int getAuthor() {
	return stat.author;
    }

    public Hollerith[] getAuxData(int tag) {
	AuxItem[] items = stat.getAuxItems();
	int c=0;
	for (int i=0; i<items.length; i++)
	    if (items[i].getTag() == tag) c++;

	Hollerith[] result = new Hollerith[c];
	for (int i=0; i<items.length; i++)
	    if (items[i].getTag() == tag) result[i] = items[i].getData();

	return result;
    }

    public Text addRecipients(int[] no) {
	for (int i=0; i < no.length; i++)
	    addRecipient(no[i]);
	return this;
    }

    public Text addRecipient(int no) {
	addMiscInfoEntry(TextStat.miscRecpt, no);
	return this;
    }

    public void clearRecipients() {
	clearMiscInfoEntry(TextStat.miscRecpt);
    }

    public void removeRecipient(int conf) {
	removeMiscInfoEntry(TextStat.miscRecpt, conf);
    }

    public void removeCcRecipient(int conf) {
	removeMiscInfoEntry(TextStat.miscCcRecpt, conf);
    }

    public Text addCcRecipients(int[] no) {
	for (int i=0; i < no.length; i++) 
	    addCcRecipient(no[i]);
	return this;
    }
    public Text addCcRecipient(int no) {
	addMiscInfoEntry(TextStat.miscCcRecpt, no);
	return this;
    }

    public Object clone() {
	Text t = new Text();
	t.setContents(getContents());
	t.addRecipients(getRecipients());
	t.addCcRecipients(getCcRecipients());
	return t;
    }

    /**
     * @deprecated moved to TextStat class
     */
    public void clearMiscInfoEntry(int key) {
	stat.clearMiscInfoEntry(key);
    }

    /**
     * @deprecated moved to TextStat class
     */
    public void removeMiscInfoEntry(int key, int value) {
	stat.removeMiscInfoEntry(key, value);
    }

    /**
     * @deprecated moved to TextStat class
     */   
    public void addMiscInfoEntry(int key, int value) {
	stat.addMiscInfoEntry(key, value);
    }
    public Text addCommented(int no) {
	addMiscInfoEntry(TextStat.miscCommTo, no);
	return this;
    }

    public Text addFootnoted(int no) {
	addMiscInfoEntry(TextStat.miscFootnTo, no);
	return this;
    }

    // more error handling (as everywhere)
    public int getLocal(int confNo)
    throws RuntimeException { 
	Integer locNo = (Integer) localMap.get(new Integer(confNo));
	if (locNo != null) return locNo.intValue();

	List miscInfo = stat.getMiscInfo();
	Iterator i = miscInfo.iterator();
	while (i.hasNext()) {
	    Selection selection = (Selection) i.next();
	    int[] keys = selection.getKeys();
	    for (int j=0; j < keys.length; j++) {
		if (keys[j] == TextStat.miscRecpt || keys[j] == TextStat.miscCcRecpt) {
		    Enumeration e = selection.get(keys[j]);
		    while (e.hasMoreElements()) {
			Integer i2 = (Integer) e.nextElement();
			if (i2.intValue() == confNo) {
			    Enumeration e2 = selection.get(TextStat.miscLocNo);
			    if (e2.hasMoreElements()) {
				locNo = (Integer) e2.nextElement();
				localMap.put(new Integer(confNo), locNo);
				Debug.println("Text.getLocal(" + confNo + "): returning " + locNo);
				return locNo.intValue();
			    } else {
				Debug.println("Text.getLocal(" + confNo + "): recipient found but no local");
			    }
			}
		    }
		}
	    }
	}
	Debug.println("Text.getLocal(" + confNo + "): recipient not found");
	return -1;

	/*
	int[] statKeys = stat.getMiscInfo().getKeys();
	List recipients = new LinkedList();
	for (int i=0; i < statKeys.length; i++) {
	    if (statKeys[i] == TextStat.miscRecpt || statKeys[i] == TextStat.miscCcRecpt) {
		Enumeration e = stat.getMiscInfo().get(statKeys[i]);
		while (e.hasMoreElements()) {
		    Integer r = (Integer) e.nextElement();
		    Debug.println("adding recipient " + r + " pos " + recipients.size());
		    recipients.add(r);
		}
	    }
	}

	Enumeration locals;
	try {
	    locals = stat.getMiscInfo().get(stat.miscLocNo);
	} catch (NoSuchKeyException ex) {
	    throw(new RuntimeException("No local numbers in misc-info"));
	}
	for (int i=0; i < recipients.size(); i++) {
	    int local = ((Integer) locals.nextElement()).intValue();
	    if (((Integer) recipients.get(i)).intValue() == confNo) {
		return local;
	    }
	}
	*/
	/*
	int[] ccs = getCcRecipients();
	for (int i=0; i < rcpts.length && locals.hasMoreElements(); i++) {
           int local = ((Integer) locals.nextElement()).intValue();
	   if (ccs[i] == confNo) {
                return local;
	   }
	}
	*/	    
	/*
	StringBuffer sb = new StringBuffer("miscLocNo items: ");
	locals = stat.getMiscInfo().get(stat.miscLocNo);
	while(locals.hasMoreElements()) {
	    sb.append("<" + (Integer) locals.nextElement() + ">");
	}
	Debug.println("text " + getNo() + " locals: " + sb.toString());
	return 0;
	*/
	//throw(new RuntimeException("No local number found for rcpt " + confNo + " in text " + getNo() +
	//", " + sb.toString()));

    }

    public byte[] getSubject() {
	int i=0; byte[] b = getContents();
	while (i < b.length && b[i] != '\n') i++;
	if (i >= b.length) { byte[] r = {} ; return r; }
	byte[] r = new byte[i];
	for (i=0;i<r.length;i++)
	    r[i] = b[i];
	return r;
    }
    
    public byte[] getBody() {
	int i=0; byte[] b = getContents();
	while (i < b.length && b[i] != '\n') { i++; }
	if (i >= b.length) return new byte[] {};
	
	byte[] r = new byte[b.length-i-1]; // -1 is \n
	i++; // skip '\n'
	System.arraycopy(b, i, r, 0, r.length);

	Debug.println("Text.getBody(): returning \"" + new String(r) + "\"");
	return r;
    }

    public List getBodyList() {
	byte[] body = getBody();
	List bodyList = new LinkedList();
	int i=0;
	int lastLf = 0;
	while (i < body.length) {
	    if (body[i] == '\n') {
		byte[] thisLine = new byte[i-lastLf];
		System.arraycopy(body, lastLf, thisLine, 0, thisLine.length);
		bodyList.add(new String(thisLine));
		lastLf = i+1;
		Debug.println("Text.getBodyList(): adding " + new String(thisLine) + " to bodyList");
	    }
	    i++;
	}
	if (lastLf == 0 && body.length > 0) {
	    bodyList.add(new String(body));
	    Debug.println("Text.getBodyList(): adding " + new String(body) + " to bodyList");
	} else if (lastLf < i) {
	    byte[] thisLine = new byte[body.length-lastLf];
	    System.arraycopy(body, lastLf, thisLine, 0, thisLine.length);
	    bodyList.add(new String(thisLine));
	    Debug.println("Text.getBodyList(): adding " + new String(thisLine) + " to bodyList");

	}
	Debug.println("Text.getBodyList(): returning " + bodyList.size() + " rows");
	return bodyList;
    }

    /**
     * Returns an int[] for Misc-Info members with integer values
     *
     * @deprecated moved to TextStat
     */    
    public int[] getStatInts(int no) {
	return stat.getStatInts(no);
    }

    public Date getCreationTime() {
	return getStat().getCreationTime().getTime();
    }

    public String getCreationTimeString() {
	return getStat().getCreationTime().toString();
    }

    public int[] getRecipients() {
	return getStatInts(TextStat.miscRecpt);
    }

    public int[] getCcRecipients() {
	return getStatInts(TextStat.miscCcRecpt);
    }

    public int[] getCommented() {
	return getStatInts(TextStat.miscCommTo);
    }

    public int[] getComments() {
	return getStatInts(TextStat.miscCommIn);
    }

    public int[] getFootnoted() {
	return getStatInts(TextStat.miscFootnTo);
    }

    public int[] getFootnotes() {
	return getStatInts(TextStat.miscFootnIn);
    }

    public int[] getSenders() {
	return getStatInts(TextStat.miscSentBy);
    }

    public int[] getSendTimes() {
	return getStatInts(TextStat.miscSentAt);
    }

    public void setStat(TextStat stat) {
	this.stat = stat;
    }

    public TextStat getStat() {
	return stat;
    }

    public int getNo() {
	return textNo;
    }

    public void setNo(int n) {
	textNo = n;
    }
    
}






