// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.app.test;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Stack;
import java.util.Enumeration;
import java.util.StringTokenizer;
import nu.dll.lyskom.*;

/**
 * Test2 - LysKOM client test program. Can read texts and post comments.
 * Takes two optional arguments: the username and the password.
 *
 */
public class Test2 implements AsynchMessageReceiver {
    Session foo;
    int noRead = 0;
    Text lastText = null;
    int lastTextNo = -1;

    public String confNoToName(String n)
	throws IOException {
	int no = 0;
	try { no = Integer.parseInt(n); }
	catch (NumberFormatException e) {
	    throw new RuntimeException(e.getMessage());
	}
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
	return new String(name);
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
	    

    public Test2(String[] argv) {
	try {
	    foo = new Session();
	    foo.connect("kom.lysator.liu.se", 4894);
	    System.out.println("Ansluten till kom.lysator.liu.se");
	    //foo.addAsynchMessageReceiver(this);	    
	    
	    ConfInfo names[] = new ConfInfo[0];
	    
	    while (names.length != 1) {
		names = foo.lookupName(argv.length > 0 ? argv[0] : crtReadLine("Vad heter du? "),
				       true, false);

		if (names.length > 1) {
		    System.err.println("Flertydigt namn.");
		    if (argv.length > 0) System.exit(-42);
		    for (int i=0;i<names.length;i++)
			System.out.println("\tAlternativ "+i+": " +
					   names[i].getNameString());
		}
		if (names.length == 0)
		    System.err.println("Namnet finns inte.");
	    }

	    System.out.println("Loggar in som " +
			       new String(names[0].confName) + " (" +
			       names[0].confNo + ")");
	    if (!foo.login(names[0].confNo, argv.length > 1 ? argv[1] : crtReadLine("lösenord> "), false)) {
		System.err.println("Inloggningen misslyckades!");
		System.exit(-32);
	    }
	    System.out.println("Inloggad. Välkommen till LysKOM!");
	    //4303588, 100035, 4257987, 4244657
	    
	    int me = foo.getMyPerson().getNo();
	    boolean go = true;
	    Stack toread = new Stack(); // used to store unread comments
	    int rc = 1;
	    while (go) {
		int currentConference = foo.getCurrentConference();
		if (lastTextNo != 0) {
		    while (rc == 1) {
			try {
			    rc = doCommand(lastText, crtReadLine(genericPrompt()));
			} catch (CmdErrException ex) {
			    System.err.println("%Fel: " + ex.getMessage());
			    rc = 1;
			}
		    }
		}
		if (rc == 0) { // default-action (read next text)
		    int textNo = foo.nextUnreadText(false);
		    if (textNo == -1) { // if no unread text in current conference,
			int nextConf = foo.nextUnreadConference(false); // change conference
			if (nextConf == -1) System.out.println("Det finns inga fler olästa möten.");
			else {
			    System.out.println("Går till möte: " + confNoToName(nextConf));
			    foo.changeConference(nextConf);
			}
			rc = 1;
			continue;
		    }
		    
		    Text text;
		    
		    // if we have an unread comment on the stack from earlier texts
		    // pop it off
		    if (!toread.empty()) {
			textNo = ((Integer) toread.pop()).intValue();
		    }
		    
		    text = foo.getText(textNo);
		    displayText(textNo); noRead++;
		    // markAsRead() takes an array of texts
		    int ur[] = { text.getLocal(foo.getCurrentConference()) };
		    foo.markAsRead(foo.getCurrentConference(), ur);

		    // add the text to the ReadTextsMap
		    foo.getReadTexts().add(textNo);

		    // retreive all comments of the texts, push them onto
		    // the 'toread' stack for later viewing (in reverse)
		    int[] comments = text.getComments();
		    for (int i=comments.length-1; i >= 0; i--)
			toread.push((Object) new Integer(comments[i]));
		    rc = 1;
		    lastTextNo = textNo;
		    lastText = text;
		    
		}
		if (rc == -1) go = false;
	    }
	    
	    System.out.print("\n\n-- Read " + noRead + " texts.\n");
	    
	    foo.logout(true);
	    System.out.println("Logged out");
	    System.exit(0);
	} catch (ProtocolException ex) {
	    System.out.println("Test: "+ex);
	} catch (IOException ex) {
	    System.out.println("Test:  "+ex);
	}
	System.exit(0);
    }

    String genericPrompt()
    throws IOException {
	int conf = foo.getCurrentConference();
	String curConf = conf == -1 ? "Ej närvarande i något möte" : confNoToName(conf);
	return curConf + "> ";
    }
    
    /**
     * Returns:
     * -1 if user wants to quit
     * 0 if user wants to continue (read next text)
     * 1 if user should be presented with a new prompt
     *
     * throws CmdErrException if user input could not be understood
     *
     * CmdErrException should not be in nu.dll.lyskom.
     *
     */
    int doCommand(Text t, String s)
    throws CmdErrException, IOException {
	if (s == null) return -1;
	if (s.equals("")) return 0;

	StringTokenizer st = new StringTokenizer(s);
	String cmd = st.nextToken();
	if (cmd.equals("?")) {
	    System.out.println("-- kommandon\n" +
			       "\tå <inläggsnummer>\n" +
			       "\tk [inläggsnummer]\n" +
			       "\tg <mötesnamn>\n" +
			       "\ti\n" +
			       "\tåk\n" +
			       "\tåu\n" +
			       "\tq\n");
	    return 1;
	}
	if (cmd.equals("å")) { // återse inlägg (å <txtno>)
	    String txt = st.hasMoreTokens() ? st.nextToken() : null;
	    if (txt == null) throw new CmdErrException("du måste ange inläggsnummer");
	    int txtNo = 0;
	    try {
		txtNo = Integer.parseInt(txt);
	    } catch (NumberFormatException ex) {
		throw(new CmdErrException("trasigt inläggsnummer: " + txt));
	    }
	    lastText = foo.getText(txtNo);
	    displayText(txtNo);
	    return 1;
	}
	if (cmd.equals("k")) { // kommentera (k [txtno])
	    String opt = null;
	    if (st.hasMoreTokens()) opt = st.nextToken();
	    if (opt != null) {
		try {
		    t = foo.getText(Integer.parseInt(opt));
		} catch (NumberFormatException ex) {
		    throw new CmdErrException("trasigt inläggsnummer: " + opt);
		}
	    }
	    StringBuffer textb = new StringBuffer();
	    System.out.println("-- Kommentar till text " + t.getNo() + ".");
	    System.out.println("-- Skriv din kommentar, avsluta med \".\" på tom rad.");
	    String row = crtReadLine("> ");
	    while (row != null && !row.equals(".")) {
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
	if (cmd.equals("i")) { // skriv inlägg i nuvarande möte
	    StringBuffer textb = new StringBuffer();
	    System.out.println("-- Inlägg i möte " + confNoToName(foo.getCurrentConference()));
	    System.out.println("Avsluta med \".\" på tom rad.");
	    String subject = crtReadLine("Ämne> ");
	    if (subject == null) return 1;
	    String row = crtReadLine("> ");
	    while (row != null && !row.equals(".")) {
		textb.append(row + "\n");
		row = crtReadLine("> ");
	    }
	    System.out.print("\nSkapar inlägg... ");
	    int newText = foo.createText(new Text(subject, textb.toString()).addRecipient(foo.getCurrentConference()));
	    if (newText > 0) System.out.println("text nummer " + newText + " skapad.");
	    else System.out.println("misslyckades att skapa inlägg.");
	    return 1;
	}		 
	if (cmd.equals("åk")) { // återse (det) kommenterade
	    int[] commented = t.getCommented();
	    if (commented.length == 0)
		throw(new CmdErrException("Texten är ingen kommentar"));
	    System.out.println("återse det kommenterade (text " + commented[0] + ")");
	    lastText = foo.getText(commented[0]);
	    displayText(commented[0]);
	    return 1;
	}
	if (cmd.equals("åu")) { // återse urinlägg
	    // footnotes breaks the chain
	    System.out.print("Söker efter urinlägg för text " + t.getNo() + "... ");
	    TextStat ts = foo.getTextStat(t.getNo());
	    while (ts.getMiscInfo().getIntArray(TextStat.miscCommTo).length > 0)
		ts = foo.getTextStat(ts.getMiscInfo().getIntArray(TextStat.miscCommTo)[0]);
	    System.out.println("hittade text " + ts.getNo());
	    displayText(lastText = t = foo.getText(ts.getNo()));
	    return 1;
	}
	if (cmd.equals("e")) { // endast
	    String antal = st.nextToken();
	    try {
		foo.endast(Integer.parseInt(antal));
	    } catch (NumberFormatException ex) {
		throw new CmdErrException("Kunde inte parsa " + antal);
	    }
	    return 1;

	}
	if (cmd.equals("g")) { // gå (till möte) (g <mötesnamn>)
	    StringBuffer optb = new StringBuffer("");
	    while (st.hasMoreTokens())
		optb.append(" " + st.nextToken());
	    String opt = optb.toString().trim();
	    
	    ConfInfo names[] = foo.lookupName(opt, false, true);
	    if (names.length == 0) {
		System.out.println("%Fel: mötet finns inte");
		return 1;
	    }
	    if (names.length > 1) {
		System.out.println("%Fel: flertydigt mötesnamn");
		for (int i=0; i < names.length; i++)
		    System.out.println("-- alternativ " + (i+1) + ": " + names[i].getNameString());
		return 1;
	    }
	    System.out.println("-- gå till möte: " + names[0].getNameString());
	    foo.changeConference(names[0].getNo());
	    return 1;
	}
	if (cmd.equals("nm")) { // nästa möte
	    int nextConf = foo.nextUnreadConference(true);
	    if (nextConf != -1)
		System.out.println("nästa möte - " + confNoToName(nextConf));
	    else
		System.out.println("Det finns inga fler olästa möten.");
	    return 1;
	}
	if (cmd.equals("q") || cmd.equals(".")) return -1; // avsluta
	throw(new CmdErrException("Förstod inte \"" + cmd + "\""));
		      
    }

    /**
     * Reads one line of input from STDIN.
     * Returns null if EOF is encountered (eg, user presses ^D on an empty line).
     * Otherwise, returns a String with the user input, without trailing '\n'.
     */
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
	return sb.toString().trim();
    }

    void displayText(int tn) throws IOException {
	Text t = foo.getText(tn);
        if (t == null) {
            System.err.println("%Fel: text " + tn + " finns inte.");
            return;
        }
	displayText(t);
    }

    /**
     * Displays a text in elisp-client-style.
     */
    void displayText(Text text) throws IOException {
	System.out.println(text.getNo()+" " + text.getCreationTimeString() + " /" + text.getRows() + " " +
			   (text.getRows() > 1 ? "rader" : "rad") + "/ " +
			   (text.getAuthor() > 0 ? new String(confNoToName(text.getAuthor())) : "anonym person"));
	int[] rcpts = text.getStatInts(TextStat.miscRecpt);
	int[] locals = text.getStatInts(TextStat.miscLocNo);
	int[] commented = text.getCommented();
	for (int i=0; i < commented.length; i++)
	    System.out.println("Kommentar till text " + commented[i] + " av " +
			       confNoToName(foo.getTextStat(commented[i]).getAuthor()));

	for (int i=0; i<rcpts.length; i++)  // recipients
	    System.out.println("Mottagare: " +
			       confNoToName(rcpts[i]) + " <" + locals[i] + ">");

	System.out.println("Ärende: " + new String(text.getSubject()));
	System.out.println("------------------------------------------------------------");
	System.out.println(new String(text.getBody()));
	System.out.println("(" + text.getNo() + ") /" + confNoToName(text.getAuthor()) +  "/--------------------");

	int[] comments = text.getComments();
	for (int i=0; i < comments.length; i++) {
	    System.out.print("Kommentar i text " + comments[i]);
	    TextStat ts = foo.getTextStat(comments[i]);
	    if (ts != null) 
		System.out.println(" av " + confNoToName(ts.getAuthor()));
	    else
		System.out.println(" (hemlig)");
	}

	if (true) {
	    AuxItem[] auxs = text.getStat().getAuxItems();
	    if (text.getStat().countAuxItems() > 0) System.out.println("\t** Aux-saker:");
	    for (int i=0; i<text.getStat().countAuxItems(); i++)
		System.out.println("\taux["+i+"]: "+auxs[i].toString());
	}
    }
	
	
    public static void main(String[] argv) {
	System.out.println("IJKLKOM (c) 1999 Rasmus Sten");
	new Test2(argv);
    }
    
    public void asynchMessage(AsynchMessage m) {
	System.out.println("** asynkront meddelande: " + m);
    }

}









