/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */

package nu.dll.lyskom;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

/**
 * Aux-Item, a LysKOM data type introduced in version 10. An Aux-Item can be
 * attached to a conference or a text, tagging it with information otherwise not
 * covered by the protocol. LatteKOM has limited, but existing, support for
 * Aux-Items.
 */
public class AuxItem implements java.io.Serializable, Tokenizable {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    int no, tag, creator;
    KomTime createdAt;
    Bitstring flags;
    int inheritLimit;
    Hollerith data;

    /*
     * maybe this should be spec'd in a separate file, as in the aux-items.conf
     * file with lyskomd? that way we can use the same patterns for validity
     * matching.
     */
    public final static int tagContentType = 1;
    public final static int tagFastReply = 2;
    public final static int tagCrossReference = 3;
    public final static int tagNoComments = 4;
    public final static int tagPersonalComment = 5;
    public final static int tagFaqText = 14;
    public final static int tagCreatingSoftware = 15;
    public final static int tagMxAuthor = 16;
    public final static int tagMxFrom = 17;
    public final static int tagMxReplyTo = 18;
    public final static int tagMxTo = 19;
    public final static int tagMxCc = 20;
    public final static int tagMxDate = 21;
    public final static int tagMxMessageId = 22;
    public final static int tagMxInReplyTo = 23;
    public final static int tagMxMisc = 24;

    public final static int tagAllowedContentType = 30;
    public final static int tagCanonicalName = 31;
    public final static int tagMxRefuseImport = 34;
    public final static int tagMxMimePartIn = 10101;

    public final static int flagDeleted = 0;
    public final static int flagInherit = 1;
    public final static int flagSecret = 2;
    public final static int flagHideCreator = 3;
    public final static int flagDontGarb = 4;
    public final static int flagReserved2 = 5;
    public final static int flagReserved3 = 6;
    public final static int flagReserved4 = 7;

    public final static int ITEM_SIZE = 15;

    public String toString() {
        return "AUX-ITEM[no:"
                + no
                + "; tag:"
                + tag
                + "; creator:"
                + creator
                + "; createdAt:"
                + createdAt.toString()
                + "; flags: "
                + new String(flags.getContents())
                + "; data:"
                + (data.getContents().length > 20 ? new String(
                        data.getContents()).substring(0, 19)
                        + "..." : new String(data.getContents())) + "]";
    }

    /**
     * Returns this specific Aux-Item's number. An Aux-Item's number, in
     * combination with a text och conference number, is what uniquely
     * identifies that particular Aux-Item. Thus, the Aux-Item numbers for a
     * given text is ever incrementing has the Aux-Items are updated/changed.
     */
    public int getNo() {
        return no;
    }

    /**
     * Returns this Aux-Item's tag.
     */
    public int getTag() {
        return tag;
    }

    /**
     * Returns this Aux-Item's creator.
     */
    public int getCreator() {
        return creator;
    }

    /**
     * Returns the time at which this Aux-Item was created.
     */
    public KomTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the inheritance limit for this Aux-Item.
     */
    public int getInheritLimit() {
        return inheritLimit;
    }

    /**
     * Returns the data contained in this Aux-Item
     */
    public Hollerith getData() {
        return data;
    }

    /**
     * Returns the data as a String, translated according to
     * Hollerith.getContentString()
     * 
     * @see nu.dll.lyskom.Hollerith#getContentString()
     */
    public String getDataString() {
        return data.getContentString();
    }

    /**
     * Convenience constructor using flags "00000000" and inherit-limit zero.
     * 
     * Converts the data to bytes using the default server encoding.
     */
    public AuxItem(int tag, String data) {
        this(tag, data, null);
    }

    public AuxItem(int tag, String data, Session lyskom) {
        this(tag, new Bitstring("00000000"), 0, new Hollerith(data,
                lyskom != null ? lyskom.getServerEncoding()
                        : Session.defaultServerEncoding));
    }

    /**
     * Constructor used to create new AuxItem objects for RPC calls.
     */
    public AuxItem(int tag, Bitstring flags, int inheritLimit, Hollerith data) {
        this.tag = tag;
        this.flags = flags;
        this.inheritLimit = inheritLimit;
        this.data = data;
        this.createdAt = new KomTime();
    }

    /**
     * Constructor used to create new AuxItem objects for RPC calls.
     */
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

    /**
     * Constructor used internally by LatteKOM to parse incoming Aux-Items into
     * Java objects.
     */
    AuxItem(KomToken[] tokens) {
        int pcount = 0;

        this.no = tokens[pcount++].intValue();
        this.tag = tokens[pcount++].intValue();
        this.creator = tokens[pcount++].intValue();
        this.createdAt = new KomTime(tokens[pcount++].intValue(), // 0
                tokens[pcount++].intValue(), // 1
                tokens[pcount++].intValue(), // 2
                tokens[pcount++].intValue(), // 3
                tokens[pcount++].intValue(), // 4
                tokens[pcount++].intValue(), // 5
                tokens[pcount++].intValue(), // 6
                tokens[pcount++].intValue(), // 7
                tokens[pcount++].intValue());

        this.flags = new Bitstring(tokens[pcount++]);
        this.inheritLimit = tokens[pcount++].intValue();
        try {
            this.data = (Hollerith) tokens[pcount++];
        } catch (ClassCastException ex) {
            Debug.println("AuxItem.<init>(): " + ex);
            Debug.println("PCOUNT:"
                    + pcount
                    + ", NEXT:"
                    + (pcount < tokens.length ? tokens[pcount].toString()
                            : "none"));
            Debug.println("LAST:" + tokens[pcount - 1]);
            for (int i = 0; i < tokens.length; i++)
                Debug.println("--- ELEMENT #" + i + ": " + tokens[i].toString());
            System.exit(-1);
        }

    }

    /**
     * Uses the data in this AuxItem object to construct an Aux-Item-Input token
     * that can be used in RPC calls to the servern.
     */
    public KomToken toToken() {
        StringBuffer foo = new StringBuffer();
        foo.append(tag + " ");
        foo.append(new String(flags.getContents()) + " ");
        foo.append(inheritLimit + " ");
        foo.append(new String(data.toNetwork()));
        return new KomToken(foo.toString());
    }

    /**
     * Counts the number of AuxItems in an array, skipping <tt>null</tt>
     * entries.
     */
    public static int countAuxItems(AuxItem[] auxItems) {
        int c = 0;
        for (int i = 0; i < auxItems.length; i++)
            if (auxItems[i] != null)
                c++;
        return c;
    }

    public static AuxItem getFirst(int tag, List<AuxItem> auxItems) {
        for (Iterator<AuxItem> i = auxItems.iterator(); i.hasNext();) {
            AuxItem item = (AuxItem) i.next();
            if (item.getTag() == tag)
                return item;
        }
        return null;
    }

    public static List<AuxItem> getAll(int tag, List<AuxItem> auxItems) {
        List<AuxItem> list = new LinkedList<AuxItem>();
        for (Iterator<AuxItem> i = auxItems.iterator(); i.hasNext();) {
            AuxItem item = (AuxItem) i.next();
            if (item.getTag() == tag)
                list.add(item);
        }

        return list;
    }

}
