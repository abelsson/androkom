/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

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
	creator = tokens[c++].toInteger();
	presentation = tokens[c++].toInteger();
	supervisor = tokens[c++].toInteger();
	permittedSubmitters = tokens[c++].toInteger();
	superConf = tokens[c++].toInteger();
	msgOfDay = tokens[c++].toInteger();
	nice = tokens[c++].toInteger();
	keepCommented = tokens[c++].toInteger();
	noOfMembers = tokens[c++].toInteger();
	firstLocalNo = tokens[c++].toInteger();
	noOfTexts = tokens[c++].toInteger();
	expire = tokens[c++].toInteger(); // expire : Garb-Nice;
	Debug.println("expire: " + expire);
	int auxItemArrayLength = tokens[c++].toInteger();
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
