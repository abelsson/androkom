/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.Hashtable;
import java.util.Enumeration;

/**

              Text-Mapping ::=
                    ( range-begin           :    Local-Text-No;
                      range-end             :    Local-Text-No;
                      later-texts-exists    :    BOOL;
                      block                 :    Local-To-Global-Block;
                    )

              Local-To-Global-Block ::= SELECTION
                    ( 0=sparse sparse-block :   ARRAY Text-Number-Pair;
                      1=dense  dense-block  :   Text-List;
                    )

              Text-Number-Pair ::=
                    ( local-number          :   Local-Text-No;
                      global-number         :   Text-No;
                    )
**/

public class TextMapping implements Enumeration {
    public final static int DEBUG = 4;
    Hashtable hash = new Hashtable();
    int enumc = 0;
    int[] list;
    public int localToGlobal(int n) {
	Integer i = (Integer) hash.get((Object) new Integer(n));
	if (i == null)
	    return -1;
	return i.intValue();
    }
    /**
     * Move the "cursor" to the beginning of the list of pairs
     * (similarly to java.sql.ResultSet.first()
     *
     * I thought that I needed this method, but on second thought, I
     * didn't. Maybe it'll come in handy later.
     */
    public void first() {
	enumc = 0;
    }
    public boolean hasMoreElements() {
	return list != null && enumc < list.length;
    }
    /**
     * Returns the next Global number
     */
    public Object nextElement() {
	if (DEBUG > 3) 
	    Debug.println("returning " + localToGlobal(list[enumc]) + "/" +
			  list[enumc] + " - " + enumc);
	return list != null ? (Object) new Integer(localToGlobal(list[enumc++])) : null;
    }
    public int lastLocal() {
	return list == null ? -1 : list.length > 0 ? list[list.length-1] : -1;
    }
    /**
     * Update the TextMapping with results from a localToGlobal call
     *
     * @param offset Not really sure when this could be anything else than 0 -- Rasmus?
     * @param tk comes from RpcReply.getParameters()
     * @param keepZeroes Keep the pairs where the global part is 0 --
     * that is, texts that can't be read (probably since they've been
     * deleted)
     */
    public void update(int offset, KomToken[] tk, boolean keepZeroes) {
	int rangeBegin = tk[offset++].toInteger();
	int rangeEnd = tk[offset++].toInteger();
	boolean laterTextsExists = (tk[offset++].toInteger() == 1 ?
				    true : false);

	switch (tk[offset++].toInteger()) { // sparse/dense?
	case 0: // sparse -- local-global pairs
	    if (DEBUG > 0) Debug.println("TextMapping.update(): sparse mode");
	    int textNumberPairArrayLength = tk[offset++].toInteger();
	    if (textNumberPairArrayLength == 0) { list = new int[0]; break; }
	    if (hash == null)
		hash = new Hashtable(textNumberPairArrayLength);
	    KomToken[] pairs = ((KomTokenArray) tk[offset++]).getTokens();
	    {
		int i=0, j=0;
		list = new int[pairs.length/2];
		// FIXME: Can a sparse mapping return pairs with
		// global == 0? If so, this code needs to respect the
		// keepZeroes parameter
		while (i<pairs.length) {
		    hash.put((Object) new Integer(list[j++] =
						  pairs[i++].toInteger()),
			     (Object) new Integer(pairs[i++].toInteger()));
		}
	    } 
	    break;
	case 1: // dense
	    if (DEBUG > 0)
		Debug.println("TextMapping.update(): dense mode");
	    int firstLocalNo = tk[offset++].toInteger();
	    int arraySize = tk[offset++].toInteger();
	    int[] numbers = ((KomTokenArray) tk[offset++]).intValues();
	    list = new int[numbers.length];
	    for (int i=0; i < numbers.length; i++) {
		if ((numbers[i] != 0) || keepZeroes) {
		    if (DEBUG > 3) 
			Debug.println("TextMapping.update(): " + (firstLocalNo + i) +
				      "==" + numbers[i]);
		    list[i] = firstLocalNo + i;
		    hash.put((Object) new Integer(firstLocalNo + i),
			     (Object) new Integer(numbers[i]));
		}
	    }
	}
	if (DEBUG > 1)
	    Debug.println("lastLocal: " + lastLocal() + ", list.length==" + list.length + ", enumc==" + enumc);
    }

    boolean search(int localNo) {
	for (int i=0; i < list.length; i++)
	    if (list[i] == localNo) return true;
	return false;
    }
    /**
     * Remove a local-global pair from this mapping
     *
     * @return true iff the localNo was present to begin with
     */
    public boolean removePair(int localNo) {
	if (hash.remove(new Integer(localNo)) != null) {
	    if (!search(localNo)) return true;

	    // we need to remove it from int[] list as well, or the
	    // Enumeration interface will be seriously broken
	    
	    int[] newList = new int[list.length - 1];
	    int j = 0;
	    for (int i = 0; i < list.length; i++) {
		if (list[i] != localNo) {
		    newList[j] = list[i];
		    j++;
		}
	    }
	    list = newList;
	    return true;
	} else return false;

    }
    /**
     * Return the number of local-global pairs in this mapping
     */
    
    public int size() {
	return hash.size();
    }
}

	    


