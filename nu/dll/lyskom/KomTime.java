/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

// simple LysKOM Tihttp://java.sun.com/aboutJava/communityprocess/jsr/jsr_051_ioapis.htmlme implementation
public class KomTime {
    final static int DEBUG = 1;
    public static int ITEM_SIZE = 9;

    int seconds, minutes, hours, mday, month, year, weekday, yearday, isdst;

    public String toString() {
	return "["+(year+1900)+"-"+month+"-"+mday+", "+hours+":"+minutes+"]";
    }

    public KomTime(int sec, int min, int hours, int mday, int month,
		    int year, int weekday, int yearday, int isdst) {
	this.seconds = sec;
	this.minutes = min;
	this.hours = hours;
	this.mday = mday;
	this.month = month;
	this.year = year;
	this.weekday = weekday;
	this.yearday = yearday;
	this.isdst = isdst;
    }

    public static KomTime createFrom(int offset, KomToken[] parray) {
	int pcount = offset;
	if (false)
	    Debug.println("-->KomTime.createFromArray("+offset+", KomToken["+parray.length+"])");
	return new KomTime(parray[pcount++].toInteger(), // 0
			   parray[pcount++].toInteger(), // 1
			   parray[pcount++].toInteger(), // 2
			   parray[pcount++].toInteger(), // 3
			   parray[pcount++].toInteger(), // 4
			   parray[pcount++].toInteger(), // 5
			   parray[pcount++].toInteger(), // 6
			   parray[pcount++].toInteger(), // 7
			   parray[pcount++].toInteger());
    }

}

