// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.app.swingkom;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;

import javax.swing.plaf.metal.*;

public class LKOMFrame extends JFrame {

    static String welcomeMessage = "Swing-LinhKOM v0.1, (c) 1999 Rasmus Sten";

    JMenuBar menuBar;
    JLabel statusBar;
    JDesktopPane desktop;

    static final Integer DOCLAYER = new Integer(5);

    public LKOMFrame() {
	super("Swing-LinhKOM");

	Rectangle defaultRectangle = new Rectangle(700,500);
	Point defaultPosition = new Point(10,10);
	setBounds(defaultRectangle);
	setLocation(defaultPosition);

	// setup content
        desktop = new JDesktopPane();
        getContentPane().add(desktop);

	setupStatus();
	setStatus(welcomeMessage);
	setupMenus();
	this.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		quit();
	    }
	});


	newSession();
    }

    public void setStatus(String s) {
	statusBar.setText(s);
    }
    
    void setupMenus() {
	menuBar = new JMenuBar();
	menuBar.setOpaque(true);
	JMenu file = setupFileMenu();

	menuBar.add(file);
	setJMenuBar(menuBar);
    }

    JMenu setupFileMenu() {
	JMenu file = new JMenu("Session");
	JMenuItem newSession = new JMenuItem("New session");
	JMenuItem quit = new JMenuItem("Quit");

	newSession.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		newSession();
	    }
	});
	quit.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		quit();
	    }
	});

	file.add(newSession);
	file.addSeparator();
	file.add(quit);
	return file;
    }

    void newSession() {
	JInternalFrame doc = new SessionFrame(0);
	desktop.add(doc, DOCLAYER);
	System.err.println("newSession(): internal frame created");
	try { 
	    doc.setSelected(true); 
	} catch (java.beans.PropertyVetoException e2) {
	    System.err.println(e2.toString());
	}
    }

    void setupStatus() {
	statusBar = new JLabel();
	getContentPane().add(statusBar, BorderLayout.SOUTH);
    }
    
    public void quit() {
	System.exit(0);
    }

}
	    
