/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;


/**
 * Helper class to aid debug tracing. The println() and print() methods has
 * no effect unless the system property "lattekom.debug" is set to "true".
 */
public class Debug {
    /**
     * <tt>true</tt> iff the "lattekom.debug" system property is non-null and equal to "true".
     */
    public final static boolean ENABLED = Boolean.getBoolean(getPropertyKey());

    static {
	if (ENABLED) {
	    println("LatteKOM debug output enabled.");
	}
    }
    
    /**
     * Prints a message to stderr if <tt>ENABLED</tt> is <tt>true</tt>,
     * terminating with a linefeed.
     */
    public static void println(String msg) {
	if (ENABLED) System.err.println(Thread.currentThread().getName() + ": " + msg);
    }

    /**
     * Prints a message to stderr if <tt>ENABLED</tt> is <tt>true</tt>.
     */
    public static void print(String msg) {
	if (ENABLED) System.err.print(msg);
    }

    public static String getPropertyKey() {
	return "lattekom.debug";
    }
}


