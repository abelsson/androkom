/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.*;
import java.io.Serializable;

// simple LysKOM Time implementation
public class KomTime implements Serializable {
    final static int DEBUG = 1;
    public static int ITEM_SIZE = 9;

    int seconds, minutes, hours, mday, month, year, weekday, yearday, isdst;

    public String toString() {
	return "["+(year+1900)+"-"+(month+1)+"-"+mday+", "+hours+":"+minutes+"]";
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

    public Date getTime() {
	Calendar cal = new GregorianCalendar();
	cal.set(Calendar.DST_OFFSET, isdst*60*1000);
	cal.set(Calendar.YEAR, year+1900);
	cal.set(Calendar.DAY_OF_YEAR, yearday);
	cal.set(Calendar.HOUR_OF_DAY, hours);
	cal.set(Calendar.MINUTE, minutes);
	cal.set(Calendar.SECOND, seconds);
	cal.set(Calendar.MILLISECOND, 0);
	return cal.getTime();
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
