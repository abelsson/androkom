/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.mail.internet.ContentType;


/**
 * Represents the LysKOM data type Conf-Stat, containing all information about a conference.
 *
 */
public class Conference {
    int no;
    private byte[] name;
    private int nice;
    private ConfType type;
    private KomTime creationTime;
    private KomTime lastWritten;
    private int creator;
    private int presentation;
    private int supervisor;
    private int permittedSubmitters;
    private int superConf;
    private int msgOfDay;

    int keepCommented;

    private int noOfMembers;
    private int firstLocalNo; // oldest existing text
    private int noOfTexts;


    final static int DEBUG = 2;
    
    int expire; // : Garb-Nice
    AuxItem[] auxItems;


    Conference(int no) {
	this.no = no;
    }

    Conference(int no, KomToken[] tokens) {
	this.no = no;
	setFrom(tokens);
    }

    /**
     * Returns this conference's number.
     */
    public int getNo() {
	return no;
    }


    /**
     * Return this conference's name.
     */
    public byte[] getName() {
	return name;
    }


    /**
     * Return this conference's name, translated into a String according to the current platform's default encoding.
     * @deprecated You should not rely on the platform's default encoding
     */
    public String getNameString() {
	return new String(name);
    }

    /**
     * Returns an array containing the AuxItem objects associated with this Conference.
     */
    public AuxItem[] getAuxItems() {
	return auxItems;
    }

    public boolean allowsMimeType(List serverAllowList, String type) {
	ContentType ctype;
	try {
	    ctype = new ContentType(type);
	} catch (javax.mail.internet.ParseException ex1) {
	    throw new IllegalArgumentException("Invalid content-type: " + ex1.toString());
	}
	List allowed = getAllowedContentTypes();
	if (allowed.size() == 0) {
	    if (serverAllowList == null || serverAllowList.size() == 0) {
		allowed.add("text/plain");
	    }
	}
	boolean accepts = false;
	for (Iterator i = allowed.iterator(); !accepts && i.hasNext();) {
	    String ct = (String) i.next();
	    Debug.println("Conference.allowsMimeType(" + type + "): testing against " + ct);
	    accepts = accepts || ct.equals("*/*") || ctype.match(ct);
	}
	return accepts;
    }

    public List getAllowedContentTypes() {
	List allowed = new LinkedList();
	AuxItem[] auxItems = getAuxItems();
	for (int i=0; i < auxItems.length; i++) {
	    if (auxItems[i].getTag() == AuxItem.tagAllowedContentType) {
		String data = auxItems[i].getDataString();
		allowed.add(data.substring(data.indexOf(" ")+1));
	    }
	}
	return allowed;
    }


    /**
     * Returns the number of the text containing this conference's presentation (or zero if there is none).
     */
    public int getPresentation() {
	return presentation;
    }

    /**
     * Returns this conference's super-conference. This conference should be used for replies in cases
     * where a conference forbids comments.
     */
    public int getSuperConf() {
	return superConf;
    }

    /**
     * Returns this conference's type.
     */
    public ConfType getType() {
	return type;
    }

    /**
     * Constructs this Conference from the supplied KomToken array.
     */
    void setFrom(KomToken[] tokens) {
	int c = 0;
	name = tokens[c++].getContents();
	type = new ConfType(tokens[c++]);
	creationTime = KomTime.createFrom(c, tokens);
	c += KomTime.ITEM_SIZE;
	lastWritten = KomTime.createFrom(c, tokens);
	c += KomTime.ITEM_SIZE;
	creator = tokens[c++].intValue();
	presentation = tokens[c++].intValue();
	supervisor = tokens[c++].intValue();
	permittedSubmitters = tokens[c++].intValue();
	superConf = tokens[c++].intValue();
	msgOfDay = tokens[c++].intValue();
	nice = tokens[c++].intValue();
	keepCommented = tokens[c++].intValue();
	noOfMembers = tokens[c++].intValue();
	firstLocalNo = tokens[c++].intValue();
	noOfTexts = tokens[c++].intValue();
	expire = tokens[c++].intValue(); // expire : Garb-Nice;
	Debug.println("expire: " + expire);
	int auxItemArrayLength = tokens[c++].intValue();
	Debug.println("aux-item list length: " + auxItemArrayLength);
	auxItems = new AuxItem[auxItemArrayLength];
	if (auxItemArrayLength == 0) return;
	
	KomToken auxObj = tokens[c++];
	Debug.println("next object is: " + (auxObj != null ? new String(auxObj.getContents()) : "null"));
	KomToken[] auxItemTokens = 
	    ((KomTokenArray) auxObj).getTokens();

	int acount = 0, j=0;
	while (acount < auxItemTokens.length) {

	    KomToken[] ai = new KomToken[AuxItem.ITEM_SIZE];
	    for (int i=0; i < AuxItem.ITEM_SIZE; i++)
		ai[i] = auxItemTokens[acount++];

	    auxItems[j++] = new AuxItem(ai);
	    if (DEBUG > 1) {
		Debug.println("AuxItems: added "+ai+"@"+j);
	    }
	}
    }

}
