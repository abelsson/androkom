/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;

import android.util.Log;
/**
 * <p>
 * The Text-Stat LysKOM data type (and this class) contains status information
 * about a text, such as its author, creation time, etc. The most notable part
 * of a Text-Stat is the Misc-Info selection, which is a list of Selection
 * objects containing information about recipient, comments, footnotes, and so
 * on. The Misc-Info list is likely to change over time, while the rest of the
 * Text-Stat is not.
 * </p>
 * <p>
 * Another important part is the AuxItem list. Aux-Items are arbitrary data that
 * can be attached to texts and conferences to specify things not covered in the
 * base protocol, such as Content-Type of a text, FAQ number of conferences, and
 * so on.
 * </p>
 * <p>
 * For many needs, the Misc-Info helper methods in the Text objects will be
 * quite sufficient, such as <tt>getRecipients()</tt>,
 * <tt>addRecipient(int)</tt>, and so on. However, for more complex tasks, such
 * as finding out when a specific recipient was added to a text and by whom, you
 * will need to look into the Misc-Info list of Selection objects. Each
 * Selection in the Misc-Info list represents a group of data received by the
 * server.
 * </p>
 * <p>
 * All Misc-Info selectors specified in Protocol A are available as constants in
 * this class. To manually find all (normal) recipients to a text, and their
 * corresponding local text number, you will have to use <tt>getMiscInfo()</tt>
 * to retreive a List of Selection objects, examine each object to see if they
 * contain the selector <tt>TextStat.miscRecpt</tt>, and if they do, you will
 * find the local text number in the same Selection object with the selector tag
 * <tt>TextStat.miscLocNo</tt>. The method <tt>getMiscInfoSelections(int)</tt>
 * in this class makes such operations easier by returning all Selections
 * containing a given type of data, such as recipient information. The above
 * procedure can thus be simplified by doing a
 * <tt>getMiscInfoSelections(miscRecpt)</tt>, which then gives you a List
 * containing the Selection objects you are interested in.
 * </p>
 * <p>
 * For more detailed information, please consult the LysKOM Protocol A
 * specification, node "<b>The Misc-Info List</b>". And better yet, help us
 * improve this documentation and implementation.
 * </p>
 * 
 * @see nu.dll.lyskom.TextStat#getMiscInfoSelections(int)
 * @see nu.dll.lyskom.Text
 * @see nu.dll.lyskom.Selection
 */
public class TextStat implements java.io.Serializable {
	private static final long serialVersionUID = -8561645601959148297L;

	// Misc-Info selection values
    /**
     * Misc-Info Selection selector for tagging recipient data.
     */
    public final static int miscRecpt = 0;
    /**
     * Misc-Info Selection selector for tagging CC-recipient data.
     */
    public final static int miscCcRecpt = 1;
    /**
     * Misc-Info Selection selector for tagging "comment-to" data
     */
    public final static int miscCommTo = 2;
    /**
     * Misc-Info Selection selector for tagging "comment-in" data
     */
    public final static int miscCommIn = 3;
    /**
     * Misc-Info Selection selector for tagging "footnote-to" data
     */
    public final static int miscFootnTo = 4;
    /**
     * Misc-Info Selection selector for tagging "footnote-in" data
     */
    public final static int miscFootnIn = 5;
    /**
     * Misc-Info Selection selector for tagging a local text number
     */
    public final static int miscLocNo = 6;
    /**
     * Misc-Info Selection selector for tagging a receiving time
     */
    public final static int miscRecTime = 7;
    /**
     * Misc-Info Selection selector for tagging person-no of the person who
     * added a recipient
     */
    public final static int miscSentBy = 8;
    /**
     * Misc-Info Selection selector for tagging the time at which a recipient
     * was added
     */
    public final static int miscSentAt = 9;
    /**
     * ?
     */
    public final static int miscXAuthor = 10;
    /**
     * ?
     */
    public final static int miscXPerson = 11;
    /**
     * ?
     */
    public final static int miscXRecpt = 12;
    /**
     * ?
     */
    public final static int miscXText = 13;
    /**
     * ?
     */
    public final static int miscXSystem = 14;
    /**
     * Misc-Info Selection selector for tagging "BCC" recipient data. Added in
     * LysKOM Protocal A version 10, I think.
     */
    public final static int miscBccRecpt = 15; // prot. 10

    public final static int MISC_INFO_COUNT = 16;

    final static int INITIAL_AUX_LENGTH = 0;

	private static final String TAG = "Androkom TextStat";

    static int DEBUG = 1;

    AuxItem[] auxItems = new AuxItem[INITIAL_AUX_LENGTH];
    int auxItemCount = 0;

    KomTime creationTime;
    int author;
    int lines;
    int chars;
    int marks;

    int no;

    // public Selection miscInfo;
    List<Selection> miscInfo = new LinkedList<Selection>();
    Map<Integer, Integer> localMap = new HashMap<Integer, Integer>();

    /**
     * Creates an empty TextStat object
     */
    public TextStat() {
        this(0);
    }

    protected TextStat(int no) {
        super();
        this.no = no;
        miscInfo = new LinkedList<Selection>();
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
     * Returns an array of AuxItem object representing the AuxItem list attached
     * to this text.
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

            for (int i = 0; i < auxItems.length; i++)
                newArray[i] = auxItems[i];

            auxItems = newArray;
            auxItems[auxItems.length - 1] = a;
        } else {
            auxItems[auxItemCount++] = a;
        }
    }

    /**
     * @deprecated
     * @see #setAuxItem(AuxItem)
     */
    public void replaceOrAddAuxItem(AuxItem a) {
        setAuxItem(a);
    }

    /**
     * Sets the specified aux-item.
     * 
     * If the aux-item already exists, it is replaces with the supplied
     * parameter. Otherwise, the aux-item is added to the list of aux-items in
     * this text-stat.
     */
    public void setAuxItem(AuxItem a) {
        for (int i = 0; i < auxItems.length; i++) {
            if (a.getTag() == auxItems[i].getTag()) {
                auxItems[i] = a;
                return;
            }
        }
        addAuxItem(a);
    }

    public boolean containsAuxItem(int tag) {
        for (int i = 0; i < auxItems.length; i++) {
            if (auxItems[i].getTag() == tag)
                return true;
        }
        return false;
    }

    /**
     * Splits the content-type aux item into content type and auxillary
     * content-type information such as the charset. Returns an array in which
     * the first element is a String with the actual content-type, and the
     * second element is a java.util.Properties containing any other data
     * trailing the content-type (eg. "charset"). TODO: this should only need to
     * be done once. TODO: does not correctly parse values containing "=" TODO:
     * there are probably other cases we can't handle as well
     */
    private Object[] parseContentTypeAuxItem() {
        String contentTypeString = getFullContentType();
        try {
            ContentType contentType = new ContentType(contentTypeString);

            Properties ctData = new Properties();
            @SuppressWarnings("unchecked")
            Enumeration<String> pnames = (Enumeration<String>) contentType
                    .getParameterList().getNames();
            while (pnames.hasMoreElements()) {
                String key = (String) pnames.nextElement();
                String value = contentType.getParameterList().get(key);
                ctData.setProperty(key, value);
            }

            if (contentType.match("x-kom/text"))
                contentTypeString = "text/x-kom-basic";
            contentType = new ContentType(contentTypeString);
            return new Object[] { contentType.toString(), ctData };
        } catch (MimeUtility.ParseException ex1) {
            throw new RuntimeException("Error parsing content-type \""
                    + contentTypeString + "\": " + ex1.toString());
        }
    }

    public String getFullContentType() {
        Hollerith[] _data = getAuxData(AuxItem.tagContentType);
        String contentType = "text/x-kom-basic";

        if (_data != null && _data.length > 0) {
            contentType = _data[0].getContentString();
        }
        return contentType;
    }

    public Properties getContentTypeParameters() {
        return (Properties) parseContentTypeAuxItem()[1];
    }

    /**
     * Returns the content-type for this text.
     * 
     * This method returns only the base content-type, excluding any parameters.
     * Use getFullContentType() to retreive the entire content-type value.
     */
    public String getContentType() {
        return (String) parseContentTypeAuxItem()[0];
    }

    public boolean isMimeType(String type) {   	
        try {
        	ContentType ct = new ContentType(getFullContentType());
        
            return ct.match(type);
        } catch (MimeUtility.ParseException ex1) {
            throw new RuntimeException("Unable to parse text content-type.");
        }
    }

    /**
     * Returns the Java charset for this text.
     */
    public String getCharset() {
    	String retVal = "iso-8859-1";
    	try {
    		retVal = MimeUtility
    		.javaCharset(((Properties) parseContentTypeAuxItem()[1])
    				.getProperty("charset", "iso-8859-1"));
    	} catch (Exception e) {
    		Log.d(TAG, "getCharset "+e);

            e.printStackTrace();    		
    	}
    	return retVal;
    }

    public void setContentType(String newContentTypeString) {  	
        String oldContentTypeString = getFullContentType();
        ContentType oldContentType = null;
        ContentType newContentType = null;
        try {
            oldContentType = new ContentType(oldContentTypeString);
            newContentType = new ContentType(newContentTypeString);
            // copy all content-type parameters (such as charset) to the new
            // content type
            // note that this might not always be what you want. if you want to
            // clear
            // the parameter list, you should set the tagContentType AuxItem
            // manually
            // with setAuxItem() instead.
            for (@SuppressWarnings("unchecked")
            Enumeration<String> e = oldContentType.getParameterList()
                    .getNames(); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                String value = oldContentType.getParameterList().get(name);
                newContentType.getParameterList().set(name, value);
            }
            setAuxItem(new AuxItem(AuxItem.tagContentType,
                    newContentType.toString()));
        } catch (MimeUtility.ParseException ex) {
            throw new RuntimeException(
                    "Error parsing contents while trying to parse content-type: "
                            + ex.toString());
        }
    }

    public void setCharset(String charset) {
        Object[] ct = parseContentTypeAuxItem();
        String contentType = (String) ct[0];
        Properties props = (Properties) ct[1];
        props.setProperty("charset", MimeUtility.mimeCharset(charset));
        setAuxItem(new AuxItem(AuxItem.tagContentType, createContentTypeString(
                contentType, props)));
    }

    private String createContentTypeString(String contentType, Properties p) {
        try {
            ContentType ct = new ContentType(contentType);
            for (Iterator<Entry<Object, Object>> i = p.entrySet().iterator(); i
                    .hasNext();) {
                Entry<Object, Object> entry = i.next();
                ct.getParameterList().set((String) entry.getKey(),
                        (String) entry.getValue());
            }
            return ct.toString();
        } catch (MimeUtility.ParseException ex) {
            throw new IllegalArgumentException("Unable to parse content-type "
                    + contentType);
        }
    }

    public int getSize() {
        return chars;
    }

    /**
     * Returns the number of AuxItem objects attached to this text.
     */
    public int countAuxItems() {
        int c = 0;
        for (int i = 0; i < auxItems.length; i++)
            if (auxItems[i] != null)
                c++;
        return c;
    }

    /**
     * Returns the AuxItem data for a given AuxItem tag.
     * 
     * @see nu.dll.lyskom.AuxItem
     */
    public Hollerith[] getAuxData(int tag) {
        AuxItem[] items = getAuxItems();
        int c = 0;
        List<Hollerith> list = new LinkedList<Hollerith>();
        for (int i = 0; i < items.length; i++) {
            if (items[i].getTag() == tag) {
                list.add(items[i].getData());
                c++;
            }
        }

        Hollerith[] result = new Hollerith[c];
        for (int i = 0; i < list.size(); i++)
            result[i] = (Hollerith) list.get(i);

        return result;
    }

    public List<AuxItem> getAuxItems(int tag) {
        AuxItem[] items = getAuxItems();
        List<AuxItem> list = new LinkedList<AuxItem>();
        for (int i = 0; i < items.length; i++) {
            if (items[i].getTag() == tag)
                list.add(items[i]);
        }
        return list;
    }

    /**
     * Returns a List containing Selection objects, which in turn makes up the
     * Misc-Info data for this text.
     */
    public List<Selection> getMiscInfo() {
        return miscInfo;
    }

    public int getMarks() {
        return marks;
    }

    /**
     * Removes all Selections containing the specified key. For example,
     * <tt>clearMiscInfoEntry(miscRecpt)</tt> will remove all (normal)
     * recipients from this text.
     */
    public void clearMiscInfoEntry(int key) {
        Iterator<Selection> i = miscInfo.iterator();
        Stack<Selection> toRemove = new Stack<Selection>();
        int count = 0;
        while (i.hasNext()) {
            Selection selection = (Selection) i.next();
            if (selection.contains(key)) {
                toRemove.push(selection);
                Debug.println("removed misc-info selection " + selection);
                count++;
            }
        }
        while (!toRemove.isEmpty())
            miscInfo.remove(toRemove.pop());

        Debug.println("clearMiscInfoEntry: found " + count
                + " selections with flag " + key);
    }

    /**
     * Removes tag <tt>key</tt> if it contains <tt>value</tt> in this texts
     * Misc-Info list. For example, a convenient way of subtracting the
     * recipient no. 4711 may be: <tt>removeMiscInfo(miscRecpt, 4711</tt>.
     */
    public void removeMiscInfoEntry(int key, int value) {
        Iterator<Selection> i = miscInfo.iterator();
        int count = 0;
        while (i.hasNext()) {
            Selection selection = (Selection) i.next();
            if (selection.contains(key)) {
                selection.remove(key, new Integer(value));
                Debug.println("removed key " + key + ", value " + value
                        + " from misc-info " + selection);
                count++;
            }
        }
        Debug.println("removeMiscInfoEntry(" + key + ", " + value + "): found "
                + count + " keys");
    }

    public void addMiscInfo(Selection selection) {
        miscInfo.add(selection);
    }

    /**
     * Adds a new Misc-Info Selection entry with the tag <tt>key</tt> and the
     * value <tt>value</tt>.
     */
    public void addMiscInfoEntry(int key, Integer value) {
        addMiscInfoEntry(key, value.intValue());
    }

    /**
     * Adds a new Misc-Info Selection entry with the tag <tt>key</tt> and the
     * value <tt>value</tt>.
     */
    public void addMiscInfoEntry(int key, int value) {
        miscInfo.add(new Selection().add(key, new Integer(value)));
    }

    /**
     * Returns a List of all Selections containing the tag <tt>key</tt>. To
     * retreive a list of all Selections containing recipient information, do
     * <tt>getMiscInfoSelections(miscRecpt)</tt>. To retreive all selections
     * containing a local text number, i.e. all types of recipients, you can
     * search for that selector tag as well (
     * <tt>getMiscInfoSelections(miscLocNo)</tt>).
     */
    public List<Selection> getMiscInfoSelections(int key) {
        List<Selection> l = new LinkedList<Selection>();
        Iterator<Selection> i = miscInfo.iterator();
        while (i.hasNext()) {
            Selection s = (Selection) i.next();
            if (s.contains(key))
                l.add(s);
        }
        return l;
    }

    /**
     * Returns an array containing all values tagged by the specified key
     * <tt>no</tt> in this texts Misc-Info list. For example,
     * <tt>getStatInts(miscRecpt)</tt> will return an integer array containing
     * all recipients to this text.
     */
    public int[] getStatInts(int no) {
        List<Object> values = new LinkedList<Object>();
        Iterator<Selection> i = miscInfo.iterator();
        while (i.hasNext()) {
            Selection selection = (Selection) i.next();
            if (selection.contains(no)) {
                values.add(selection.get(no));
            }
        }
        int[] stats = new int[values.size()];
        Iterator<Object> vi = values.iterator();
        for (int j = 0; j < stats.length; j++) {
            stats[j] = ((Integer) vi.next()).intValue();
        }
        return stats;
    }

    public List<Integer> getAllRecipients() {
        List<Integer> recipients = new LinkedList<Integer>();
        for (int i = 0; i < miscInfo.size(); i++) {
            Selection misc = (Selection) miscInfo.get(i);
            int key = misc.getKey();
            if (key == TextStat.miscRecpt || key == TextStat.miscCcRecpt
                    || key == TextStat.miscBccRecpt) {
                recipients.add(new Integer(misc.getIntValue()));
            }
        }
        return recipients;
    }

    /**
     * Returns an array containing all the texts of which this is a comment to.
     */
    public int[] getCommented() {
        return getStatInts(miscCommTo);
    }

    public int[] getComments() {
        return getStatInts(miscCommIn);
    }

    /**
     * Returns an array containing all recipients for this text.
     */
    public int[] getRecipients() {
        return getStatInts(miscRecpt);
    }

    public boolean hasRecipient(int no) {
        int[] r = getStatInts(miscRecpt);
        for (int i = 0; i < r.length; i++)
            if (r[i] == no)
                return true;

        int[] c = getStatInts(miscCcRecpt);
        for (int i = 0; i < c.length; i++)
            if (c[i] == no)
                return true;

        int[] b = getStatInts(miscBccRecpt);
        for (int i = 0; i < b.length; i++)
            if (b[i] == no)
                return true;
        return false;
    }

    /**
     * Returns an array containing all CC-recipients for this text.
     */
    public int[] getCcRecipients() {
        return getStatInts(TextStat.miscCcRecpt);
    }

    /**
     * Walks through the recipient list of this text and return this texts local
     * text number for that recipient, or -1 if the recipient is not found in
     * this texts recipient list.
     * 
     * @param confNo
     *            The recipient to search for
     * @see nu.dll.lyskom.Session#localToGlobal(int, int, int)
     * @see nu.dll.lyskom.TextMapping
     */
    // more error handling (as everywhere)
    public int getLocal(int confNo) throws RuntimeException {
        Integer locNo = (Integer) localMap.get(new Integer(confNo));
        if (locNo != null)
            return locNo.intValue();

        List<Selection> miscInfo = getMiscInfoSelections(TextStat.miscLocNo);
        Iterator<Selection> i = miscInfo.iterator();
        while (i.hasNext()) {
            Selection selection = (Selection) i.next();

            int rcpt = 0;
            if (selection.contains(TextStat.miscRecpt))
                rcpt = selection.getIntValue(TextStat.miscRecpt);
            if (selection.contains(TextStat.miscCcRecpt))
                rcpt = selection.getIntValue(TextStat.miscCcRecpt);
            if (selection.contains(TextStat.miscBccRecpt))
                rcpt = selection.getIntValue(TextStat.miscBccRecpt);

            if (rcpt == confNo) {
                int no = selection.getIntValue(TextStat.miscLocNo);
                localMap.put(new Integer(confNo), new Integer(no));
                return no;
            }

        }
        Debug.println("Text.getLocal(" + confNo + "): recipient not found");
        return -1;
    }

    static TextStat createFrom(int no, RpcReply reply) {
        return createFrom(no, reply.getParameters(), 0, false);
    }

    static TextStat createFrom(int no, KomToken[] params, int offset,
            boolean textStatOld) {
        TextStat ts = new TextStat(no);
        List<Selection> miscInfo = ts.getMiscInfo();

        int pcount = offset;
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
        ts.lines = params[pcount++].intValue();
        ts.chars = params[pcount++].intValue();
        ts.marks = params[pcount++].intValue();

        int arrayLength = params[pcount++].intValue();
        KomToken[] miscInfoTokens = ((KomTokenArray) params[pcount++])
                .getTokens();

        KomToken auxItemArrayLengthToken = params[pcount++];
        if (auxItemArrayLengthToken.isEmpty()) {
            auxItemArrayLengthToken = params[pcount++];
        }

        KomToken auxItemArrayToken = params[pcount++];

        KomToken[] auxItemTokens = ((KomTokenArray) auxItemArrayToken)
                .getTokens();

        int mcount = 0;

        Selection selection = null;
        for (int i = 0; i < arrayLength; i++) {
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
                selection = new Selection();
                miscInfo.add(selection);

            case 6: // loc-no ! Lokalt textnummer
            case 8: // sent-by
                int value = miscInfoTokens[mcount++].intValue();
                selection.add(selectionId, value);
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
                        miscInfoTokens[mcount++].intValue());
                selection.add(selectionId, stm);
                break;
            default:
                break;
            }

        }

        if (!textStatOld) {
            int acount = 0;
            while (acount < auxItemTokens.length) {
                KomToken[] ai = new KomToken[AuxItem.ITEM_SIZE];
                for (int i = 0; i < AuxItem.ITEM_SIZE; i++)
                    ai[i] = auxItemTokens[acount++];

                ts.addAuxItem(new AuxItem(ai));
            }
        }

        return ts;

    }
}
