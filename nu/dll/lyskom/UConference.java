/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class UConference {
    private byte[] name;
    private Bitstring type;
    private int highestLocalNo;
    private int nice;

    int no;
    
    public UConference(int no) {
	this.no = no;
    }
    public UConference(int no, KomToken[] tokens) {
	this.no = no;
	setFrom(tokens);
    }

    public void setFrom(KomToken[] tokens) {
	int c = 0;
	name = tokens[c++].getContents();
	type = new Bitstring(tokens[c++]);
	highestLocalNo = tokens[c++].toInteger();
	nice = tokens[c++].toInteger();
    }

    public int getNo() {
	return no;
    }

    public int getHighestLocalNo() {
	return highestLocalNo;
    }

    public byte[] getName() {
	return name;
    }

}
