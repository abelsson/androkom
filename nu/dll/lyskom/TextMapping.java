/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

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

/**
 * This class handles storage of global text numbers for one conference.
 */
public class TextMapping implements Enumeration {
    public final static int DEBUG = 4;
    Hashtable hash = new Hashtable();
    int enumc = 0;
    List list = new LinkedList();
    //int[] list;

    public int localToGlobal(int n) {
	Integer i = (Integer) hash.get((Object) new Integer(n));
	if (i == null)
	    return -1;
	return i.intValue();
    }

    public int globalToLocal(int n) {
	Debug.println("TextMapping.globalToLocal(" + n + ")");
	Enumeration e = hash.keys();
	while (e.hasMoreElements()) {
	    Integer localNo = (Integer) e.nextElement();
	    Integer globalNo = (Integer) hash.get(localNo);
	    if (localNo.equals(globalNo))
		return localNo.intValue();
	}
	Debug.println("TextMapping.globalToLocal(" + n + "): nothing found");
	return -1;
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
	return list != null && enumc < list.size();
    }
    /**
     * Returns the next Global number
     */
    public Object nextElement() {
	if (enumc > list.size()) {
	    Debug.println("no such element, enum=" + enumc);
	    return null;
	}
	Debug.println("returning " + localToGlobal(((Integer) list.get(enumc)).intValue()) + "/" +
		      ((Integer) list.get(enumc)).intValue() + " - " + enumc);
	return list != null ? (Object) new Integer(localToGlobal(((Integer)
								  list.get(enumc++)).intValue())) : null;
    }
    public int lastLocal() {
	return list == null ? -1 : list.size() > 0 ?
	    ((Integer) list.get(list.size()-1)).intValue() : -1;
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
	    if (textNumberPairArrayLength == 0) { break; }
	    if (hash == null)
		hash = new Hashtable(textNumberPairArrayLength);
	    KomToken[] pairs = ((KomTokenArray) tk[offset++]).getTokens();
	    {
		int i=0, j=0;
		//list = new int[pairs.length/2];
		// FIXME: Can a sparse mapping return pairs with
		// global == 0? If so, this code needs to respect the
		// keepZeroes parameter
		while (i<pairs.length) {
		    int value1 = pairs[i++].intValue();
		    int value2 = pairs[i++].intValue();
		    list.add(new Integer(value1));
		    hash.put(new Integer(value1), new Integer(value2));
		}
	    } 
	    break;
	case 1: // dense
	    if (DEBUG > 0)
		Debug.println("TextMapping.update(): dense mode");
	    int firstLocalNo = tk[offset++].toInteger();
	    int arraySize = tk[offset++].toInteger();
	    int[] numbers = ((KomTokenArray) tk[offset++]).intValues();
	    //list = new int[numbers.length];
	    for (int i=0; i < numbers.length; i++) {
		if ((numbers[i] != 0) || keepZeroes) {
		    if (DEBUG > 3) 
			Debug.println("TextMapping.update(): " + (firstLocalNo + i) +
				      "==" + numbers[i]);
		    list.add(new Integer(firstLocalNo + i));
		    hash.put(new Integer(firstLocalNo + i), new Integer(numbers[i]));
		}
	    }
	}
	if (DEBUG > 1)
	    Debug.println("lastLocal: " + lastLocal() + ", list.length==" + list.size() + ", enumc==" + enumc);
    }

    boolean search(int localNo) {
	return list.contains(new Integer(localNo));
    }
    /**
     * Remove a local-global pair from this mapping
     *
     * @return true iff the localNo was present to begin with
     */
    public boolean removePair(int localNo) {
	if (hash.remove(new Integer(localNo)) != null) {
	    if (!search(localNo)) return true;
	    return list.remove(new Integer(localNo));
	} else return false;

    }
    /**
     * Return the number of local-global pairs in this mapping
     */
    
    public int size() {
	return hash.size();
    }
}

	    


