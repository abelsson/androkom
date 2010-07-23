/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

import java.util.*;
import java.io.Serializable;

/**
 * Represents the LysKOM data type "Time".
 */
public class KomTime implements Serializable {
	final static int DEBUG = 1;
	/**
	 * The number of KomToken items that this data type is made of.
	 */
	public static int ITEM_SIZE = 9;

	int seconds, minutes, hours, mday, month, year, weekday, yearday, isdst;

	public String toString() {
		return "[" + (year + 1900) + "-" + (month + 1) + "-" + mday + ", "
				+ hours + ":" + minutes + "]";
	}

	/**
	 * Returns <tt>true</tt> if the supplied object is of type KomTime and
	 * represents the same time as this object.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof KomTime))
			return false;
		return toString().equals(o.toString());
	}

	/**
	 * Constructs a KomTime object representing the specified time.
	 * 
	 * @param sec
	 *            Wall clock seconds
	 * @param min
	 *            Wall clock minutes
	 * @param hours
	 *            Wall clock hours (24hr format)
	 * @param mday
	 *            Day within month, starting with 1
	 * @param month
	 *            Month of year, with January being zero
	 * @param year
	 *            Years since 1900
	 * @param weekday
	 *            Day of week, Sunday being zero
	 * @param yearday
	 *            Day of year, starting with zero
	 * @param isdst
	 *            '1' if the time is daylight savings time
	 */
	public KomTime(int sec, int min, int hours, int mday, int month, int year,
			int weekday, int yearday, int isdst) {
		this.seconds = sec;
		this.minutes = min;
		this.hours = hours - isdst;
		this.mday = mday;
		this.month = month;
		this.year = year;
		this.weekday = weekday;
		this.yearday = yearday;
		this.isdst = isdst;
	}

	/**
	 * Constructs a KomTime object representing the current system time.
	 */
	public KomTime() {
		Calendar cal = new GregorianCalendar();
		this.seconds = cal.get(Calendar.SECOND);
		this.minutes = cal.get(Calendar.MINUTE);
		this.hours = cal.get(Calendar.HOUR_OF_DAY);
		this.mday = cal.get(Calendar.DAY_OF_MONTH);
		this.month = cal.get(Calendar.MONTH);
		this.year = cal.get(Calendar.YEAR) - 1900;
		this.weekday = cal.get(Calendar.DAY_OF_WEEK);
		this.yearday = cal.get(Calendar.DAY_OF_YEAR) - 1; // LysKOM Time yearday
															// starts with zero
		this.isdst = cal.get(Calendar.DST_OFFSET); // protocol violation if > 1
													// and < 0, I think
	}

	/**
	 * Returns a java.util.Date object representing this object's time,
	 * according to the Gregorian calendar.
	 */
	public Date getTime() {
		Calendar cal = new GregorianCalendar();
		cal.set(Calendar.DST_OFFSET, isdst * 60 * 1000);
		cal.set(Calendar.YEAR, year + 1900);
		cal.set(Calendar.DAY_OF_YEAR, yearday + 1); // LysKOM Time yearday
													// starts with zero
		cal.set(Calendar.HOUR_OF_DAY, hours);
		cal.set(Calendar.MINUTE, minutes);
		cal.set(Calendar.SECOND, seconds);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.DST_OFFSET, isdst);
		return cal.getTime();
	}

	/**
	 * Constructs a KomTime object from the supplied KomToken array.
	 * 
	 * @param offset
	 *            first element in the array to use
	 * @param parray
	 *            array of KomToken to use
	 */
	static KomTime createFrom(int offset, KomToken[] parray) {
		int pcount = offset;
		return new KomTime(parray[pcount++].intValue(), // 0
				parray[pcount++].intValue(), // 1
				parray[pcount++].intValue(), // 2
				parray[pcount++].intValue(), // 3
				parray[pcount++].intValue(), // 4
				parray[pcount++].intValue(), // 5
				parray[pcount++].intValue(), // 6
				parray[pcount++].intValue(), // 7
				parray[pcount++].intValue());
	}

}
