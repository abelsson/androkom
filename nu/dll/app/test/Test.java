// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.app.test;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import nu.dll.lyskom.*;

public class Test implements AsynchMessageReceiver {
    Session foo;
    int noRead = 0;

    public String confNoToName(String n)
	throws IOException {
	int no = 0;
	try { no = Integer.parseInt(n); }
	catch (NumberFormatException e) {}
	return confNoToName(no);
    }

    public String confNoToName(Integer n)
    throws IOException {
	return confNoToName(n.intValue());
    }

    public String confNoToName(int n)
    throws IOException {
	if (foo == null) return null;
	byte[] name = foo.getConfName(n);
	if (name == null)
	    return "Person "+n+" (N/A)";
	return new String(name) + " (#" + n + ")";
    }

    public static String joinString(String s, String[] all) {
	StringBuffer foo = new StringBuffer(all.length*all[0].length());
	for (int i=0;i<all.length;i++) {
	    foo.append(all[i]);
	    if ((i+1) < all.length)
		foo.append(s);
	}
	return foo.toString();
    }
	    

    public Test(String[] argv) {
	try {
	    foo = new Session();
	    foo.connect("kom.lysator.liu.se", 4894);
	    //foo.addAsynchMessageReceiver(this);	    
	    ConfInfo names[] = foo.lookupName(crtReadLine("Vad heter du? "),
					      true, false);
	    if (names != null) {
		for (int i=0;i<names.length;i++)
		    System.out.println("Alternativ "+i+": " +
				       names[i].getNameString());
		if (names.length != 1) {
		    System.out.println("Flertydigt namn");
		    System.exit(-1);
		}
		System.out.println("Loggar in som " +
				   new String(names[0].confName) + " (" +
				   names[0].confNo + ")");
		if (!foo.login(names[0].confNo, crtReadLine("lösenord> "),
			       false)) {
		    Debug.println("Login failed!");
		    System.exit(-32);
		}
		System.out.println("Logged in.");
		// jag=6804, testmote=14, linh=6800
		// jag@stacken=275 testmote@stacken=262
		

		/*
		if (foo.sendMessage(14, "Test."))
		    System.out.println("Sent message");
		else
		    System.out.println("Message sending failed");
		*/


		//4303588, 100035, 4257987, 4244657
		
		int me = foo.getMyPerson().getNo();
		boolean read = true;
		/*
		  Text t = new Text("....", "woosh, woosh, woosh");
		  t.addRecipient(14).addCcRecipient(8383);
		  System.out.println("CREATED: " + foo.createText(t));
		*/
		if (read) {

		    boolean eof = false;
		    int txt = -1;
		    int rc = 0;
		    while (!eof) {
			if (rc != -2)
			    txt = foo.nextUnreadText(true);
			else {
			    txt = rc = 0;
			}
			if (txt == -1) {
			    int newConf = foo.nextUnreadConference(true);
			    if (newConf == -1) {
				System.out.println("Inga olästa.");
				crtReadLine("(inget)> ");
				continue;
			    }

			    System.out.println("Nästa olästa möte: " +
					       confNoToName(newConf));
			    continue;
			}

			if (txt == 0) continue;
			System.out.println("-- visar träd " + txt);
			rc = displayTextTree(txt, 0);
			eof = rc == -1 ? true : false;
		    }
		}
		System.out.print("\n\n-- Read " + noRead + " texts.\n");

		foo.logout(true);
		System.out.println("Logged out");
		System.exit(0);
	    } else {
		Debug.println("Name lookup error");
		System.exit(-1);
	    }
	} catch (ProtocolException ex) {
	    Debug.println("Test: "+ex);
	} catch (IOException ex) {
	    Debug.println("Test:  "+ex);
	}
	System.exit(0);
    }
    

    int doCommand(Text t, String s)
    throws CmdErrException, IOException {
	if (s == null) return -1;
	if (s.equals("")) return 0;

	StringTokenizer st = new StringTokenizer(s);
	String cmd = st.nextToken();
	if (cmd.equals("å")) {
	    String txt = st.nextToken();
	    int txtNo = 0;
	    try {
		txtNo = Integer.parseInt(txt);
	    } catch (NumberFormatException ex) {
		throw(new CmdErrException("trasigt inläggsnummer: " + txt));
	    }
	    displayText(txtNo);
	    return 1;
	}
	if (cmd.equals("k")) {
	    StringBuffer textb = new StringBuffer();
	    System.out.println("-- Kommentar till text " + t.getNo() + ".");
	    System.out.println("-- Skriv din kommentar, avsluta med EOF.");
	    String row = crtReadLine("> ");
	    while (row != null) {
		textb.append(row + "\n");
		row = crtReadLine("> ");
	    }
	    System.out.print("\nSkapar kommentar... ");


	    int newText = foo.reply(t.getNo(),
				    new Text(new String(t.getSubject()),
					     textb.toString()));
	    if (newText > 0)
		System.out.println("text nummer " + newText + " skapad.");

	    return 1;
	}
	if (cmd.equals("åk")) {
	    int[] commented = t.getCommented();
	    if (commented.length == 0)
		throw(new CmdErrException("Texten är ingen kommentar"));
	    System.out.println("återse det kommenterade (text " + commented[0] + ")");
	    displayText(commented[0]);
	    return 1;
	}
	if (cmd.equals("e")) {
	    String antal = st.nextToken();
	    try {
		foo.endast(Integer.parseInt(antal));
	    } catch (NumberFormatException ex) {
		throw new CmdErrException("Kunde inte parsa " + antal);
	    }
	    return 1;

	}
	if (cmd.equals("nm")) {
	    System.out.println("nästa möte - " + confNoToName(foo.nextUnreadConference(true)));
	    return -2;
	}
	throw(new CmdErrException("Förstod inte \"" + cmd + "\""));
		      
    }

    String crtReadLine(String prompt)
    throws IOException {
	if (prompt != null) System.out.print(prompt);
	int b = System.in.read();
	StringBuffer sb = new StringBuffer();
	while (b != -1 && b != '\n') {
	    sb.append((char) b);
	    b = System.in.read();
	}
	if (b == -1) return null;
	return sb.toString();
    }



    int displayTextTree(int tn, int depth) throws IOException {
	Text t = foo.getText(tn);
	displayText(tn);
	noRead++;
	int ur[] = { t.getLocal(foo.getCurrentConference()) };
	foo.markAsRead(foo.getCurrentConference(), ur);
	foo.getReadTexts().add(tn);
	int[] comments = t.getComments();
	int rc = 1;
	while (rc == 1) {
	    try {
		String cmd = crtReadLine(comments.length == 0 && depth == 0 ? "(nästa inlägg)> " : "(nästa kommentar)> ");
		rc = doCommand(t, cmd);
		if (rc == -1 || rc == -2) return rc;
		
	    } catch (CmdErrException ex) {
		System.out.println("%Fel: " + ex.getMessage());
		rc = 1;
	    }
	}
	if (comments.length == 0) return 0;

	for (int i=0; i < comments.length; i++) {
	    return displayTextTree(comments[i], ++depth);
	}
	return 0;
    }

    void displayText(int tn) throws IOException {
	Text t1 = foo.getText(tn);

	System.out.println("Text "+t1.getNo()+", av " +
			   (t1.getAuthor() > 0 ? new String(confNoToName(t1.getAuthor())) : "anonym person"));
	int[] rcpts = t1.getStatInts(TextStat.miscRecpt);
	for (int i=0; i<rcpts.length; i++)  // recipients
	    System.out.println("Skickad till " +
			       new String(confNoToName(rcpts[i])));
	int[] mkeys = t1.getStat().getMiscInfo().getKeys();
	for (int i=0; i < mkeys.length; i++) {
	    try {
		switch (mkeys[i]) {
		case TextStat.miscLocNo:
		    System.out.println("Lokalt textnummer " + t1.getStat().getMiscInfo().get(mkeys[i]).nextElement());
		    break;
		}
	    } catch (NoSuchKeyException ex) {
		Debug.println("miscinfo " + mkeys[i] + " barfed: " + ex.getMessage());
	    }
	}

	int[] commented = t1.getCommented();
	for (int i=0; i < commented.length; i++) {
	    System.out.println("Kommentar till text " + commented[i] + " av " +
			       confNoToName(foo.getText(commented[i]).getAuthor()));
	}
 	    
	System.out.println("Arende: " + new String(t1.getSubject()));
	System.out.println("Text:\n"+new String(t1.getBody()) + "\n------------");

	int[] comments = t1.getComments();
	for (int i=0; i < comments.length; i++) {
	    System.out.println("Kommentar i text " + comments[i] + " av " +
			       confNoToName(foo.getText(comments[i]).getAuthor()));
	}

	System.out.println("Aux-saker:");
	AuxItem[] auxs = t1.getStat().getAuxItems();
	for (int i=0; i<t1.getStat().countAuxItems(); i++)
	    System.out.println("aux["+i+"]: "+auxs[i].toString());
	System.out.print("\n\n");
    }
	
	
    public static void main(String[] argv) {
	System.out.println("LKOM (c) 1999 Rasmus Sten");
	new Test(argv);
    }
    
    public void asynchMessage(AsynchMessage m) {
	System.out.println("** asynkront meddelande: " + m);
    }

}









