/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;
import java.util.Enumeration;

public class TextStat {

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

    public Selection miscInfo;

    public TextStat() {
	this(0);
    }

    public TextStat(int no) {
	super();
	this.no = no;
	miscInfo = new Selection(MISC_INFO_COUNT);
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

    public Selection getMiscInfo() {
	return miscInfo;
    }

    /* Gah. all createFrom() should be constructors, I guess. */
    public static TextStat createFrom(int no, RpcReply reply) {
	KomToken[] params = reply.getParameters();
	TextStat ts = new TextStat(no);
	Selection miscInfo = ts.getMiscInfo();

	int pcount = 0;
	ts.creationTime = new KomTime(params[pcount++].toInteger(), // 0
				      params[pcount++].toInteger(), // 1
				      params[pcount++].toInteger(), // 2
				      params[pcount++].toInteger(), // 3
				      params[pcount++].toInteger(), // 4
				      params[pcount++].toInteger(), // 5
				      params[pcount++].toInteger(), // 6
				      params[pcount++].toInteger(), // 7
				      params[pcount++].toInteger());

	ts.author = params[pcount++].toInteger();
	ts.lines  = params[pcount++].toInteger();
	ts.chars  = params[pcount++].toInteger();
	ts.marks  = params[pcount++].toInteger();

	int arrayLength = params[pcount++].toInteger();
	KomToken[] miscInfoTokens =
	    ((KomTokenArray) params[pcount++]).getTokens();

	int auxItemArrayLength = params[pcount++].toInteger();
	KomToken[] auxItemTokens =
	    ((KomTokenArray) params[pcount++]).getTokens();

	int mcount = 0;

	// This should probably be in Selection.createFrom(RpcReply)
	for (int i=0;i<arrayLength;i++) {
	    int selectionId = miscInfoTokens[mcount++].toInteger();

	    switch (selectionId) {
		/* items to be stored as Integer */
	    case 0: // recipient : Conf-No
	    case 1: // cc-recipient
	    case 2: // comment-to
	    case 3: // commented-in
	    case 4: // footnote-to
	    case 5: // footnote-in
	    case 6: // loc-no                  ! Lokalt textnummer
	    case 8: // sent-by
		miscInfo.add(selectionId,
			     (Object) new Integer(miscInfoTokens[mcount++].
						  toInteger()));
		break;
		
		/* items to be stored as KomTime */
	    case 7: // rec-time : Time
	    case 9: // sent-at : Time
		KomTime stm = new KomTime(miscInfoTokens[mcount++].toInteger(),
					  miscInfoTokens[mcount++].toInteger(),
					  miscInfoTokens[mcount++].toInteger(),
					  miscInfoTokens[mcount++].toInteger(),
					  miscInfoTokens[mcount++].toInteger(),
					  miscInfoTokens[mcount++].toInteger(),
					  miscInfoTokens[mcount++].toInteger(),
					  miscInfoTokens[mcount++].toInteger(),
					  miscInfoTokens[mcount++].toInteger()
					  );
		miscInfo.add(selectionId, (Object) stm);
		break;
	    default:
		break;
	    }
	    
	    if (false) {
		    try {
			Debug.print(", objects: ");
			for (Enumeration e = miscInfo.get(selectionId);
			     e.hasMoreElements();)
			    Debug.print("<"+e.nextElement()+">");
			if (DEBUG>0) Debug.println(", mcount: "+mcount);
		    } catch (NoSuchKeyException ex) {
			Debug.println("Not reached!");
			System.exit(-10);
		    }
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



