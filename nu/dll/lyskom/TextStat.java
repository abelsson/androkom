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
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;

/**
 * <p>
 * The Text-Stat LysKOM data type (and this class) contains status information
 * about a text, such as its author, creation time, etc.
 * The most notable part of a Text-Stat is the Misc-Info selection, which
 * is a list of Selection objects containing information about recipient,
 * comments, footnotes, and so on. The Misc-Info list is likely to change
 * over time, while the rest of the Text-Stat is not.
 * </p><p>
 * Another important part is the AuxItem list. Aux-Items are arbitrary data
 * that can be attached to texts and conferences to specify things not
 * covered in the base protocol, such as Content-Type of a text, FAQ number of
 * conferences, and so on.
 * </p><p>
 * For many needs, the Misc-Info helper methods in the Text objects will
 * be quite sufficient, such as <tt>getRecipients()</tt>, <tt>addRecipient(int)</tt>,
 * and so on. However, for more complex tasks, such as finding out when a specific
 * recipient was added to a text and by whom, you will need to look into the Misc-Info
 * list of Selection objects. Each Selection in the Misc-Info list represents a group
 * of data received by the server.
 * </p><p>
 * All Misc-Info selectors specified in Protocol A are available as constants in this
 * class. To manually find all (normal) recipients to a text, and their corresponding
 * local text number, you will have to use <tt>getMiscInfo()</tt> to retreive a List
 * of Selection objects, examine each object to see if they contain the selector
 * <tt>TextStat.miscRecpt</tt>, and if they do, you will find the local text number
 * in the same Selection object with the selector tag <tt>TextStat.miscLocNo</tt>.
 * The method <tt>getMiscInfoSelections(int)</tt> in this class makes such operations
 * easier by returning all Selections containing a given type of data, such as 
 * recipient information. The above procedure can thus be simplified by doing a
 * <tt>getMiscInfoSelections(miscRecpt)</tt>, which then gives you a List containing
 * the Selection objects you are interested in.
 * </p><p>
 * For more detailed information, please consult the LysKOM Protocol A specification,
 * node "<b>The Misc-Info List</b>". And better yet, help us improve this documentation
 * and implementation.
 * </p>
 *
 * @see nu.dll.lyskom.TextStat#getMiscInfoSelections(int)
 * @see nu.dll.lyskom.Text
 * @see nu.dll.lyskom.Selection
 */
public class TextStat implements java.io.Serializable {

    // Misc-Info selection values
    /**
     * Misc-Info Selection selector for tagging recipient data.
     */ 
    public final static int miscRecpt    =  0;
    /**
     * Misc-Info Selection selector for tagging CC-recipient data.
     */ 
    public final static int miscCcRecpt  =  1;
    /**
     * Misc-Info Selection selector for tagging "comment-to" data
     */ 
    public final static int miscCommTo   =  2;
    /**
     * Misc-Info Selection selector for tagging "comment-in" data
     */ 
    public final static int miscCommIn   =  3;
    /**
     * Misc-Info Selection selector for tagging "footnote-to" data
     */ 
    public final static int miscFootnTo  =  4;
    /**
     * Misc-Info Selection selector for tagging "footnote-in" data
     */ 
    public final static int miscFootnIn  =  5;
    /**
     * Misc-Info Selection selector for tagging a local text number
     */ 
    public final static int miscLocNo    =  6;
    /**
     * Misc-Info Selection selector for tagging a receiving time
     */ 
    public final static int miscRecTime  =  7;
    /**
     * Misc-Info Selection selector for tagging person-no of the person who added a recipient
     */ 
    public final static int miscSentBy   =  8;
    /**
     * Misc-Info Selection selector for tagging the time at which a recipient was added
     */ 
    public final static int miscSentAt   =  9;
    /**
     * ?
     */ 
    public final static int miscXAuthor  = 10;
    /**
     * ?
     */ 
    public final static int miscXPerson  = 11;
    /**
     * ?
     */ 
    public final static int miscXRecpt   = 12;
    /**
     * ?
     */ 
    public final static int miscXText    = 13;
    /**
     * ?
     */ 
    public final static int miscXSystem  = 14;
    /**
     * Misc-Info Selection selector for tagging "BCC" recipient data.
     * Added in LysKOM Protocal A version 10, I think.
     */
    public final static int miscBccRecpt = 15; // prot. 10

    public final static int MISC_INFO_COUNT = 16;

    final static int INITIAL_AUX_LENGTH = 0;

    static int DEBUG = 1;

    AuxItem[] auxItems = new AuxItem[INITIAL_AUX_LENGTH];
    int auxItemCount = 0;

    KomTime creationTime;
    int author;
    int lines;
    int chars;
    int marks;

    int no;

    //public Selection miscInfo;
    List miscInfo = new LinkedList();
    Map localMap = new HashMap();

    /**
     * Creates an empty TextStat object
     */
    public TextStat() {
	this(0);
    }

    protected TextStat(int no) {
	super();
	this.no = no;
	miscInfo = new LinkedList();
    }

    protected void setNo(int no) {
	this.no = no;
    }

    /**
     * Returns the text number this TextStat information belongs to.
     */
    public int getNo() {
	return no;
    }

    /**
     * Returns the author of the text.
     */
    public int getAuthor() {
	return author;
    }

    /**
     * Returns an array of AuxItem object representing the AuxItem list attached to this text.
     */
    public AuxItem[] getAuxItems() {
	return auxItems;
    }

    /**
     * Returns the time at which this text was created.
     */
    public KomTime getCreationTime() {
	return creationTime;
    }

    /**
     * Adds an AuxItem object to this text.
     */
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

    public boolean containsAuxItem(int tag) {
	for (int i=0; i < auxItems.length; i++) {
	    if (auxItems[i].getTag() == tag) return true;
	}
	return false;
    }

    /**
     * Splits the content-type aux item into content type
     * and auxillary content-type information such as the charset.
     * Returns an array in which the first element is a String
     * with the actual content-type, and the second element
     * is a java.util.Properties containing any other data
     * trailing the content-type (eg. "charset").
     */
    private Object[] parseContentTypeAuxItem() {
	Object[] r = new Object[2];
	Hollerith[] _data = getAuxData(AuxItem.tagContentType);
	String contentType = "text/x-kom-basic";
	
	if (_data != null && _data.length > 0) {
	    contentType = _data[0].getContentString();
	}

        StringTokenizer toker = new StringTokenizer(contentType, ";");
        contentType = toker.nextToken();
        Properties ctData = new Properties();
        while (toker.hasMoreTokens()) {
            StringTokenizer tokfan = new StringTokenizer(toker.nextToken(), "=");
            ctData.setProperty(tokfan.nextToken(), tokfan.nextToken());
        }
        if (contentType.equals("x-kom/text")) contentType = "text/x-kom-basic";
	return new Object[] { contentType, ctData };
    }

    /**
     * Returns the content-type for this text.
     */
    public String getContentType() {
	return (String) parseContentTypeAuxItem()[0];
    }

    /**
     * Returns the charset for this text.
     */
    public String getCharset() {
	return ((Properties) parseContentTypeAuxItem()[1]).getProperty("charset", "iso-8859-1");
    }

    public int getSize() {
	return chars;
    }

    /**
     * Returns the number of AuxItem objects attached to this text.
     */
    public int countAuxItems() {
	int c=0;
	for (int i=0;i<auxItems.length;i++)
	    if (auxItems[i] != null) c++;
	return c;
    }

    /**
     * Returns the AuxItem data for a given AuxItem tag.
     *
     * @see nu.dll.lyskom.AuxItem
     */
    public Hollerith[] getAuxData(int tag) {
	AuxItem[] items = getAuxItems();
	int c=0;
	List list = new LinkedList();
	for (int i=0; i<items.length; i++) {
	    if (items[i].getTag() == tag) {
		list.add(items[i].getData());
		c++;
	    }
	}

	Hollerith[] result = new Hollerith[c];
	for (int i=0; i<list.size(); i++)
	    result[i] = (Hollerith) list.get(i);

	return result;
    }

    public List getAuxItems(int tag) {
	AuxItem[] items = getAuxItems();
	List list = new LinkedList();
	for (int i=0; i < items.length; i++) {
	    if (items[i].getTag() == tag) list.add(items[i]);
	}
	return list;
    }

    /**
     * Returns a List containing Selection objects, which in turn
     * makes up the Misc-Info data for this text.
     */
    public List getMiscInfo() {
	return miscInfo;
    }

    /**
     * Removes all Selections containing the specified key.
     * For example, <tt>clearMiscInfoEntry(miscRecpt)</tt> will
     * remove all (normal) recipients from this text.
     */
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

    /**
     * Removes tag <tt>key</tt> if it contains <tt>value</tt> in this texts Misc-Info list.
     * For example, a convenient way of subtracting the recipient no. 4711 may be:
     * <tt>removeMiscInfo(miscRecpt, 4711</tt>.
     */
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

    /**
     * Adds a new Misc-Info Selection entry with the tag <tt>key</tt>
     * and the value <tt>value</tt>.
     */
    public void addMiscInfoEntry(int key, Integer value) {
	addMiscInfoEntry(key, value.intValue());
    }

    /**
     * Adds a new Misc-Info Selection entry with the tag <tt>key</tt>
     * and the value <tt>value</tt>.
     */
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
     * Returns a List of all Selections containing the tag <tt>key</tt>.
     * To retreive a list of all Selections containing recipient information,
     * do <tt>getMiscInfoSelections(miscRecpt)</tt>. To retreive all
     * selections containing a local text number, i.e. all types of recipients,
     * you can search for that selector tag as well (<tt>getMiscInfoSelections(miscLocNo)</tt>).
     */
    public List getMiscInfoSelections(int key) {
	List l = new LinkedList();
	Iterator i = miscInfo.iterator();
	while (i.hasNext()) {
	    Selection s = (Selection) i.next();
	    if (s.contains(key)) l.add(s);
	}
	return l;
    }

    /**
     * Returns an array containing all values tagged by the specified key <tt>no</tt>
     * in this texts Misc-Info list. For example, <tt>getStatInts(miscRecpt)</tt>
     * will return an integer array containing all recipients to this text.
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

    /**
     * Returns an array containing all recipients for this text.
     */
    public int[] getRecipients() {
	return getStatInts(TextStat.miscRecpt);
    }

    /**
     * Returns an array containing all CC-recipients for this text.
     */
    public int[] getCcRecipients() {
	return getStatInts(TextStat.miscCcRecpt);
    }

    /**
     * Walks through the recipient list of this text and return
     * this texts local text number for that recipient, or -1
     * if the recipient is not found in this texts recipient list.
     *
     * @param confNo The recipient to search for
     * @see nu.dll.lyskom.Session#localToGlobal(int, int, int)
     * @see nu.dll.lyskom.TextMapping
     */
    // more error handling (as everywhere)
    public int getLocal(int confNo)
    throws RuntimeException { 
	Integer locNo = (Integer) localMap.get(new Integer(confNo));
	if (locNo != null) return locNo.intValue();

	List miscInfo = getMiscInfoSelections(TextStat.miscLocNo);
	Iterator i = miscInfo.iterator();
	while (i.hasNext()) {	    
	    Selection selection = (Selection) i.next();

	    int rcpt = 0;
	    if (selection.contains(TextStat.miscRecpt))
		rcpt = selection.getIntValue(TextStat.miscRecpt);
	    if (selection.contains(TextStat.miscCcRecpt))
		rcpt = selection.getIntValue(TextStat.miscCcRecpt);

	    if (rcpt == confNo) {
		int no = selection.getIntValue(TextStat.miscLocNo);
		localMap.put(new Integer(confNo), new Integer(no));
		return no;
	    }

	}
	Debug.println("Text.getLocal(" + confNo + "): recipient not found");
	return -1;
    }



    /* Gah. all createFrom() should be constructors, I guess. */
    static TextStat createFrom(int no, RpcReply reply) {
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


	KomToken auxItemArrayLengthToken = params[pcount++];
	if (auxItemArrayLengthToken.isEmpty()) {
	    auxItemArrayLengthToken = params[pcount++];
	}

	int auxItemArrayLength = auxItemArrayLengthToken.intValue();
	KomToken auxItemArrayToken = params[pcount++];
	if (Debug.ENABLED) {
	    Debug.println("TextStat.createFrom(): aux-item list length: " + auxItemArrayLength);
	    Debug.println("Aux-Item Array token class: " + auxItemArrayToken.getClass().getName());
	    Debug.println("Aux-Item Array token contents: " + new String(auxItemArrayToken.getContents()));
	}

	KomToken[] auxItemTokens = ((KomTokenArray) auxItemArrayToken).getTokens();

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
