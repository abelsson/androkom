// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.app.swingkom;

import nu.dll.lyskom.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class SwingKOM implements Runnable {
    LKOMFrame appFrame;
    public static void main(String[] argv) {
	System.out.println("Swing LKOM frontend (c) 1999 Rasmus Sten");
	new SwingKOM();
    }
    public SwingKOM() {
	appFrame = new LKOMFrame();
	appFrame.setVisible(true);
	new Thread(this, "Main thread").start();
    }
    public void run() {
	
    }
}
