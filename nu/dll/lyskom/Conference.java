/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class Conference {
    int no;
    private byte[] name;
    private int nice;
    private Bitstring type;
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


    public Conference(int no) {
	this.no = no;
    }

    public Conference(int no, KomToken[] tokens) {
	this.no = no;
	setFrom(tokens);
    }

    public int getNo() {
	return no;
    }

    public AuxItem[] getAuxItems() {
	return auxItems;
    }

    public int getPresentation() {
	return presentation;
    }

    public void setFrom(KomToken[] tokens) {
	int c = 0;
	name = tokens[c++].getContents();
	type = new Bitstring(tokens[c++]);
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
