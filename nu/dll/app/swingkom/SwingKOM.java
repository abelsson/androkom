// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.app.swingkom;

import nu.dll.lyskom.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

public class SwingKOM implements Runnable {
    LKOMFrame appFrame;
    public static void main(String[] argv) throws Exception {
	System.out.println("Swing LKOM frontend (c) 1999 Rasmus Sten");
	UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

	//System.setErr(new PrintStream(new FileOutputStream(new File("nisse.stderr")), true));
	new SwingKOM();
    }
    public SwingKOM() {
	appFrame = new LKOMFrame();
	appFrame.setVisible(true);
	new Thread(this, "Main thread").start();
    }
    public void run() {
	
    }
    
    public static void panic(Exception e) {
        System.err.println("*** FATAL Exception, exiting: " + e.getClass().getName() + ": " + e.getMessage());
        System.exit(42);
    }
}
