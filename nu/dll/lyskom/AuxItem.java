/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */

package nu.dll.lyskom;


/** Aux-Item, introduced in version 10 */
public class AuxItem implements java.io.Serializable {
    int no, tag, creator;
    KomTime createdAt;
    Bitstring flags;
    int inheritLimit;
    Hollerith data;

    public final static int tagContentType  = 1;

    public final static int tagCreatingSoftware = 15;

    public final static int tagMxMimeMisc       = 10102;
    public final static int tagMxEnvelopeSender = 10103;


    public final static int flagDeleted     = 0;
    public final static int flagInherit     = 1;
    public final static int flagSecret      = 2;
    public final static int flagHideCreator = 3;
    public final static int flagDontGarb    = 4;
    public final static int flagReserved2   = 5;
    public final static int flagReserved3   = 6;
    public final static int flagReserved4   = 7;
    
    public final static int ITEM_SIZE = 15;

    public String toString() {
	return "AUX-ITEM[no:"+no+"; tag:"+tag+"; creator:"+creator+
	    "; createdAt:"+createdAt.toString()+"; flags: "+
	    new String(flags.getContents())+"; data:"+
	    (data.getContents().length > 20 ?
	     new String(data.getContents()).substring(0, 19) + "..." :
	     new String(data.getContents()))+"]";
    }
	    
    public int getNo() {
	return no;
    }

    public int getTag() {
	return tag;
    }
    
    public int getCreator() {
	return creator;
    }

    public KomTime getCreatedAt() {
	return createdAt;
    }

    public int getInheritLimit() {
	return inheritLimit;
    }

    public Hollerith getData() {
	return data;
    }
    
    public String getDataString() {
	return data.getContentString();
    }

    public AuxItem(int tag, Bitstring flags, int inheritLimit,
		   Hollerith data) {
	this.tag = tag;
	this.flags = flags;
	this.inheritLimit = inheritLimit;
	this.data = data;
    }
    public AuxItem(int no, int tag, int creator, KomTime createdAt,
		   Bitstring flags, int inheritLimit, Hollerith data) {
	this.no = no;
	this.tag = tag;
	this.creator = creator;
	this.createdAt = createdAt;
	this.flags = flags;
	this.inheritLimit = inheritLimit;
	this.data = data;
    }

    public AuxItem(KomToken[] tokens) {
	int pcount = 0;

	this.no        = tokens[pcount++].toInteger();
	//Debug.println("No ("+pcount+"): "+no);
	this.tag       = tokens[pcount++].toInteger();
	//Debug.println("Tag ("+pcount+"): " +tag);
	this.creator   = tokens[pcount++].toInteger();
	this.createdAt = new KomTime(tokens[pcount++].toInteger(), // 0
				     tokens[pcount++].toInteger(), // 1
				     tokens[pcount++].toInteger(), // 2
				     tokens[pcount++].toInteger(), // 3
				     tokens[pcount++].toInteger(), // 4
				     tokens[pcount++].toInteger(), // 5
				     tokens[pcount++].toInteger(), // 6
				     tokens[pcount++].toInteger(), // 7
				     tokens[pcount++].toInteger());

	this.flags    = new Bitstring(tokens[pcount++]);
	this.inheritLimit = tokens[pcount++].toInteger();
	//Debug.println("Flags ("+pcount+"): " +flags);
	try {
	    this.data     = (Hollerith) tokens[pcount++];
	} catch (ClassCastException ex) {
	    Debug.println("AuxItem.<init>(): "+ex);
	    Debug.println("PCOUNT:"+pcount+", NEXT:"+
			       (pcount<tokens.length ?
				tokens[pcount].toString() :
				"none")
			       );
	    Debug.println("LAST:"+tokens[pcount-1]);
	    for (int i=0;i<tokens.length;i++)
		Debug.println("--- ELEMENT #"+i+": " +
				   tokens[i].toString());
	    System.exit(-1);
	}

    }

    /* creates an Aux-Item-Input token */
    public KomToken toToken() {
	StringBuffer foo = new StringBuffer();
	foo.append(tag + " ");
	foo.append(new String(flags.getContents()) + " ");
	foo.append(inheritLimit + " ");
	foo.append(new String(data.toNetwork()));
	return new KomToken(foo.toString());
    }

    public static int countAuxItems(AuxItem[] auxItems) {
	int c=0;
	for (int i=0;i<auxItems.length;i++) if (auxItems[i] != null) c++;
	return c;
    }
    
}
