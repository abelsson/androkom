/**
 * SnarfKOM
 *
 * Copyright (c) 2000 Rasmus Sten
 *
 */
package nu.dll.app.snarfkom;

import java.io.*;
import java.net.ProtocolException;
import java.util.Enumeration;
import nu.dll.lyskom.*;

public class SnarfKom {

    final static String DEFAULT_SERVER = "kom.lysator.liu.se";
    final static int DEFAULT_PORT      = 4894;

    private Session session;
    private String username;

    ConfInfo me;
    int myNo;

    int unreadConfs[];
    int currentConference = -1;

    UConference currentConfStat;

    public SnarfKom(String[] argv)
    throws IOException, ProtocolException {
	username = argv.length > 0 ? argv[0] : null;
	session = new Session();
	println("Connecting to " + DEFAULT_SERVER + ":" + DEFAULT_PORT +
		"...");
	session.connect(DEFAULT_SERVER, DEFAULT_PORT);
	println("Connected, fetching username...");
	ConfInfo names[] = session.lookupName(username, true, false);
	if (names == null) bail(-1, "Failed to lookup username " + username);
	if (names.length != 1) {
	    println("Possible matches are:");
	    for (int i=0; i < names.length; i++)
		println("\t" + new String(names[i].confName));
	    bail(0, "Ambigous name");
	}
	ConfInfo myPerson = names[0];
	println("Enter password for \"" + new String(myPerson.confName) +
		"\":");
	if (!session.login(myPerson.confNo, readln(), false))
	    bail(0, "Login failed!");
	println("Login OK, welcome to LysKOM.");
	me = myPerson; myNo = me.confNo;
	System.exit(0);
    }

    public void getUnreads() {
	unreadConfs = session.getUnreadConfs(myNo);
	
    }

    public int nextUnreadRead() {
    }

    public static String readln() {
	try {
	    return new LineNumberReader(new InputStreamReader(System.in)).readLine();
	} catch (IOException ex) {
	    bail(-1, "I/O error reading stdin!");
	    return null;
	}

    }

    public static void main(String[] argv) {
	try {
	    new SnarfKom(argv);
	} catch (ProtocolException ex) {
	    Debug.println("Protocol error: " + ex.getMessage());
	    bail(-1, "Error in server communication, " +
		 "bailing out...");	    
	} catch (IOException ex) {
	    Debug.println("I/O error: " + ex.getMessage());
	    bail(-1, "I/O error, bailing out...");
	}
    }

    public static void println(String msg) {
	System.out.println(msg);
    }

    public static void bail(int errno, String msg) {
	System.err.println(msg);
	System.exit(errno);
    }

}
