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

public class Text extends Hollerith implements java.io.Serializable {

    public final static int DEBUG = 1;

    boolean cached = false;

    int textNo = -1;
    TextStat stat = null;

    public Text setCached(boolean b) {
	cached = b; return this;
    }

    public boolean isCached(boolean n) { return cached; }

    /** constructors **/
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

    public Text addRecipient(int no) {
	stat.getMiscInfo().add(TextStat.miscRecpt,
			       (Object) new Integer(no));
	return this;
    }

    public Text addCcRecipient(int no) {
	stat.getMiscInfo().add(TextStat.miscCcRecpt,
			       (Object) new Integer(no));
	return this;

    }
    
    public Text addCommented(int no) {
	stat.getMiscInfo().add(TextStat.miscCommTo,
			       (Object) new Integer(no));
	return this;
    }

    public Text addFootnoted(int no) {
	stat.getMiscInfo().add(TextStat.miscFootnTo,
			       (Object) new Integer(no));
	return this;
    }

    // more error handling (as everywhere)
    public int getLocal(int confNo)
    throws RuntimeException { 
	int[] rcpts = getRecipients();
	Enumeration locals;
	try {
	    locals = stat.getMiscInfo().get(stat.miscLocNo);
	} catch (NoSuchKeyException ex) {
	    throw(new RuntimeException("No local numbers in misc-info"));
	}
	for (int i=0; i < rcpts.length; i++) {
	    int local = ((Integer) locals.nextElement()).intValue();
	    if (rcpts[i] == confNo) {
		return local;
	    }
	}
	int[] ccs = getCcRecipients();
	for (int i=0; i < rcpts.length; i++) {
           int local = ((Integer) locals.nextElement()).intValue();
	   if (ccs[i] == confNo) {
                return local;
	   }
	}
	    
	StringBuffer sb = new StringBuffer("miscLocNo items: ");
	locals = stat.getMiscInfo().get(stat.miscLocNo);
	while(locals.hasMoreElements()) {
	    sb.append("<" + (Integer) locals.nextElement() + ">");
	}
	throw(new RuntimeException("No local number found for rcpt " + confNo + " in text " + getNo() +
				   ", " + sb.toString()));

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
	if (i >= b.length) i = 0;
	byte[] r = new byte[stat.chars-i-1]; // -1 is \n
	i++; // skip '\n'
	for (int j=0;i<stat.chars;j++, i++) r[j] = b[i];
	return r;
    }

    /**
     * Returns an int[] for Misc-Info members with integer values
     *
     * OBSOLETE: replaced by Selection.getIntArray()
     */
    public int[] getStatInts(int no) {
	try {
	    Vector v = stat.getMiscInfo().getVector(no);
	    int[] stats = new int[v.size()];
	    int i=0;
	    for(Enumeration e = v.elements();
		e.hasMoreElements(); i++)
		stats[i]  = ((Integer) e.nextElement()).intValue();
	    return stats;
	} catch (NoSuchKeyException e) {
	    int[] i= {};
	    return i;
	}
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
