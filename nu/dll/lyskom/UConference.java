/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

/**
 * An UConference is a minimalistic set of status information about a conference,
 * such as it's name and type. Usually used for situations where you don't 
 * need the full status of a conference for a task.
 */
public class UConference {
    protected byte[] name;
    protected Bitstring type;
    protected int highestLocalNo;
    protected int nice;

    int no;
    
    UConference(int no) {
	this.no = no;
    }
    UConference(int no, KomToken[] tokens) {
	this.no = no;
	setFrom(tokens);
    }

    void setFrom(KomToken[] tokens) {
	int c = 0;
	name = tokens[c++].getContents();
	type = new Bitstring(tokens[c++]);
	highestLocalNo = tokens[c++].intValue();
	nice = tokens[c++].intValue();
    }

    /**
     * Returns the conference number this UConference represents.
     */
    public int getNo() {
	return no;
    }

    /**
     * Returns the highest local text number in this conference.
     */
    public int getHighestLocalNo() {
	return highestLocalNo;
    }

    public void setHighestLocalNo(int highestLocalNo) {
	this.highestLocalNo = highestLocalNo;
    }

    /**
     * Returns the name of this conference.
     */
    public byte[] getName() {
	return name;
    }

    /**
     * Returns the "garb-nice" time for this conference - ie. how long unmarked texts are kept
     * before being garbage collected by the server.
     */ 
    public int getNice() {
	return nice;
    }

    public String toString() {
	return "UConference[conf-no: " + no + "]";
    }

}
