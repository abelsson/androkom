/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class Debug {
    public static boolean ENABLED = false;

    static {
        try {
            ENABLED = System.getProperty("LATTEKOM_DEBUG").equals("1");
        } catch (SecurityException ex1) {
            
        } catch (NullPointerException ex2) {
            
        } catch (IllegalArgumentException ex3) {
            
        }        
        
    }

    
    //public static boolean ENABLED = false;

    public static void println(String msg) {
	if (ENABLED) System.err.println(Thread.currentThread().getName() + ": " + msg);
    }
    public static void print(String msg) {
	if (ENABLED) System.err.print(msg);
    }
}

