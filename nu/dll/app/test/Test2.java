// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.app.test;

import java.io.*;
import java.net.ProtocolException;
import java.util.Stack;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Locale;
import java.util.Date;
import java.util.Random;

import java.text.SimpleDateFormat;
import java.text.MessageFormat;

import java.util.Calendar;
import java.util.GregorianCalendar;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.Container;
import java.awt.Font;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JPasswordField;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;


import nu.dll.lyskom.*;

/**
 * Test2 - LysKOM client test program. Can read texts and post comments.
 * Takes two optional arguments: the username and the password.
 *
 */
public class Test2 implements AsynchMessageReceiver, ConsoleListener, Runnable {

    class GuiInput {
	String string = "";
	public void setString(String s) {
	    this.string = s;
	}

	public String getString() {
	    return string;
	}
    }

    Session foo;
    int noRead = 0;
    Text lastText = null;
    int lastTextNo = -1;
    LinkedList messages = new LinkedList();
    boolean likesAsynchMessages = false;
    String lastPromptShown = null;

    Object consoleLock = new Object();

    protected Stack toread = new Stack(); // used to store unread comments
    protected Stack toreview = new Stack(); // återse-stack

    static String encoding = System.getProperty("lattekom.encoding");
    static String lineSeparator = System.getProperty("line.separator");

    static boolean showAux = Boolean.getBoolean("lattekom.showaux");
    static boolean dontMarkOwnTextsAsRead = Boolean.getBoolean("lattekom.dont-mark-own-texts-as-read");
    static boolean useAnsiColors = Boolean.getBoolean("lattekom.use-ansi");
    static boolean doKeepActive = Boolean.getBoolean("lattekom.keep-active");
    static boolean useGui = Boolean.getBoolean("lattekom.use-gui");

    static String fixedWhatIAmDoing = System.getProperty("lattekom.whatiamdoing");

    boolean macBreak = Boolean.getBoolean("lattekom.usecrlf");
    int linesPerScreen = Integer.getInteger("lattekom.rows", new Integer(24)).intValue();
    String server = System.getProperty("lyskom.server");

    Locale locale = new Locale("sv", "se");  // language and location

    SimpleDateFormat fullTimeFormat = new SimpleDateFormat("EEEE d MMMM yyyy k:mm", locale);
    SimpleDateFormat timestampFormat = fullTimeFormat;
    SimpleDateFormat thisYearTimeFormat = new SimpleDateFormat("EEEE d MMMM k:mm", locale);
    SimpleDateFormat yesterdayTimeFormat = new SimpleDateFormat("'igår' HH:mm", locale);
    SimpleDateFormat withinLastWeekTimeFormat = new SimpleDateFormat("EEEE's' HH:mm", locale);
    SimpleDateFormat todayTimeFormat = new SimpleDateFormat("'idag' HH:mm", locale);

    AsynchInvoker asynchInvoker;

    GuiInput guiInput = null;
    Console console = null;
    JFrame consoleFrame = null;

    String lastWhatIAmDoing = "";

    Text lastSavedText = null;
    int linesSinceLastPrompt = 0;

    boolean embedded = false;

    static {
	if (encoding == null) {
	    if (System.getProperty("os.name").startsWith("Windows") && !useGui) encoding = "Cp437";
	    else encoding = "ISO-8859-1";
	}
    }

    public void setStatus(String s) 
    throws IOException {
	if (fixedWhatIAmDoing == null && !lastWhatIAmDoing.equals(s)) {
	    foo.changeWhatIAmDoing(s);
	    lastWhatIAmDoing = s;
	}
	if (useGui) {
	    if (!embedded && foo != null && foo.getMyPerson() != null) {
		consoleFrame.setTitle("LatteKOM/T2 - " + server + " - " + confNoToName(foo.getMyPerson().getNo()) +
				      (foo.getCurrentConference() > 0 ? " i " + confNoToName(foo.getCurrentConference()) : "") +
				      " - " + s);
	    } else if (!embedded) {
		consoleFrame.setTitle("LatteKOM/T2 - " + s);
	    }
	}
	    
    }

    public void setLastText(Text t) {
	lastText = t;
    }

    public Text getLastText() {
	return lastText;	
    }

    public Text getLastSavedText() {
	return lastSavedText;
    }

    public void setLastSavedText(Text t) {
	lastSavedText = t;
    }

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
	Debug.println("confNoToName() looking up name for #" + n);
	byte[] name = null;
	try {
	    name = foo.getConfName(n);
	} catch (RpcFailure ex1) {
	    return "Möte " + ex1.getErrorStatus() + " (fel " + ex1.getError() + ")";
	}
	if (name == null)
	    return "Person "+n+" (N/A)";
	if (useAnsiColors)
	    return "\u001b[01;34m" + bytesToString(name) + "\u001b[0m";

	return bytesToString(name);
    }

    /**
     * Convert byte-arrays coming from the server into String objects, using the
     * encoding specified by system.property lyskom.encoding (default "iso-8859-1")
     */
    public String bytesToString(byte[] bytes) {
	try {
	    return new String(bytes, Session.serverEncoding);
	} catch (UnsupportedEncodingException ex1) {
	    throw new RuntimeException("Unsupported Encoding");
	}
    }

    /**
     * note: should not print out error messages here, but rather change the return
     * codes to better reflect what actually happened (like -1 for "not found" and
     * -2 for "ambigous name"
     */
    public int parseNameArgs(String arg, boolean wantPersons, boolean wantConfs)
    throws IOException {
	int confNo = 0;
	if (arg.startsWith("m ") || arg.startsWith("p ")) {
	    StringTokenizer st = new StringTokenizer(arg, " ");
	    st.nextToken();
	    try {
		confNo = Integer.parseInt(st.nextToken());
		return confNo;
	    } catch (NumberFormatException ex1) {
		consoleWriteLn("%Fel: kunde inte tolka mötesnummer " + ex1.getMessage());
	    }
	}

	ConfInfo[] names = foo.lookupName(arg, wantPersons, wantConfs);
	if (names.length == 0) {
	    consoleWriteLn("%Fel: hittar inget sådant möte eller sådan person");
	    return -1;
	}
	if (names.length > 1) {
	    consoleWriteLn("%Fel: flertydigt namn");
	    for (int i=0; i < names.length; i++)
		consoleWriteLn("-- Alternativ " + (i+1) + ": " + names[i].getNameString() + " (#" + names[i].getNo()  + ")");
	    return -1;
	}
	return names[0].getNo();
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

    public String komTimeFormat(Date d) {
	Calendar now = new GregorianCalendar(locale);
	Calendar then = new GregorianCalendar(locale);

	then.setTime(d);
	if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
	    if (now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) { // today
		return todayTimeFormat.format(d);
	    }
	    Calendar yesterday = (Calendar) now.clone();
	    yesterday.add(Calendar.DAY_OF_YEAR, -1);
	    if (then.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) { 
		return yesterdayTimeFormat.format(d);
	    }
	    Calendar weekago = (Calendar) now.clone();
	    weekago.add(Calendar.DAY_OF_YEAR, -6);
	    if (d.after(weekago.getTime())) {
		return withinLastWeekTimeFormat.format(d);
	    }
	    return thisYearTimeFormat.format(d);
	}
	return fullTimeFormat.format(d);
    }

    boolean serverSynch = false;
    void handleMessages() {
	while (messages.size() > 0) {
	    AsynchMessage m = null;
	    synchronized (messages) {
		m = (AsynchMessage) messages.removeFirst();
	    }
	    KomToken[] params = m.getParameters();
	    switch (m.getNumber()) {
	    case Asynch.new_text_old:
		try {
		    foo.updateUnreads();
		} catch (IOException ex1) {
		    throw new RuntimeException("I/O error: " + ex1.getMessage());
		}
		break;
	    case Asynch.new_name:
		consoleWriteLn(((Hollerith) params[1]).getContentString() + " har bytt namn till " + 
			       ((Hollerith) params[2]).getContentString() + ".");
		break;
	    case Asynch.send_message:
		String recipient = null, sender = null;
		try {
		    recipient = params[0].intValue() != 0 ? confNoToName(params[0].intValue()) : null;
		    sender = confNoToName(params[1].intValue());
		} catch (IOException ex) {
		    System.err.println("Det gick inte att utföra get-conf-name: " + ex.getMessage());
		}
		if (!useGui) {
		    consoleWrite("\007\007"); // beep!
		} else if (!embedded) {
		    consoleFrame.requestFocus();
		    consoleFrame.toFront();
		}
		consoleWriteLn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		if (params[0].intValue() == foo.getMyPerson().getNo()) {
		    consoleWriteLn("Personligt meddelande från " + sender + 
				   " (" + komTimeFormat(m.getArrivalTime()) + "):");
		} else if (params[0].intValue() == 0) {
		    consoleWriteLn("Alarmmeddelande från " + sender + " (" + komTimeFormat(m.getArrivalTime()) + "):");
		} else {
		    consoleWriteLn("Meddelande till " + recipient);
		    consoleWriteLn("från " + sender +
				   " (" + komTimeFormat(m.getArrivalTime()) + "):");
		}
		consoleWriteLn("");
		consoleWriteLn(((Hollerith) params[2]).getContentString());
		consoleWriteLn("----------------------------------------------------------------");
		break;
	    case Asynch.login:
		try {
		    consoleWriteLn(confNoToName(params[0].intValue()) + " loggade in i LysKOM (" +
				   komTimeFormat(m.getArrivalTime()) + ")");

		} catch (IOException ex) {
		    System.err.println("Det gick inte att utföra get-conf-name: " + ex.getMessage());
		}
		break;
	    case Asynch.logout:
		try {
		    consoleWriteLn(confNoToName(params[0].intValue()) + " loggade ut ur LysKOM (" +
				   komTimeFormat(m.getArrivalTime()) + ")");

		} catch (IOException ex) {
		    System.err.println("Det gick inte att utföra get-conf-name: " + ex.getMessage());
		}
		break;
	    case Asynch.sync_db:
		serverSynch = !serverSynch;
		if (serverSynch) {
		    consoleWriteLn("** Servern synkroniserar just nu databasen.");
		} else {
		    consoleWriteLn("** Servern är klar med synkroniseringen.");
		}
		break;
		//case Asynch.new_text:
		//break;
	    default: 
		consoleWriteLn("Asynkront meddelande av typen " + m.getNumber());
		
	    }
	}
    }

    public void setDefaultUser(String s) {
	defaultUser = s;
    }

    /**
     * This is a bad way of passing passwords; recommended practice is to pass
     * passwords by char[] and clearing the array directly after use.
     * But why bother...
     */
    public void setDefaultPassword(String s) {
	defaultPassword = s;
    }

    String defaultUser = null;
    String defaultPassword = null;
    boolean parseArgs(String[] argv) {
	try {
	    int i=0;
	    while (i < argv.length) {
		String arg = argv[i++];		
		if (arg.equals("-name")) {
		    defaultUser = argv[i++];
		} else
		if (arg.equals("-password")) {
		    defaultPassword = argv[i++];
		} else
		if (arg.equals("-gui")) {
		    useGui = true;
		    initGui();
		} else
		if (arg.equals("-server")) {
		    server = argv[i++];
		} else
		if (arg.equals("-?") || arg.equals("-help") ||
		    arg.equals("--help") || arg.equals("-h")) {
		    usage(null);
		    return false;
		} else {
		    usage("Kunde ej tolka parameter: " + arg);
		    return false;
		}
		    
	    }
	    return true;
	} catch (ArrayIndexOutOfBoundsException ex1) {
	    usage("Felaktigt antal parametrar");
	    return false;
	}
    }

    void usage(String message) {
	if (message != null)
	    consoleWriteLn("Fel: " + message);

	consoleWriteLn(" Parametrar:");
	consoleWriteLn("   -name <namn>           Logga in som <namn>");
	consoleWriteLn("   -password <lösenord>   Logga in med <lösenord>");
	consoleWriteLn("   -server <server>       Anslut till <server>");
	consoleWriteLn("   -gui                   Aktivera GUI-konsoll");
    }

    CommandMap commands = null;
    public void initCommands() {
	commands = new CommandMap(foo, this);
	commands.addCommand(ConfCommands.class);
	commands.addCommand(TextCommands.class);
    }

    public String getServerString() {
	return server;
    }

    public void run() {
	Debug.println("line separator: \"" + lineSeparator + "\"");
	try {
	    while (server == null) {
		server = crtReadLine("Vilken server vill du ansluta till? ");
		if (server == null) { // user might have pressed ^D, or stdin == /dev/null
		    consoleWriteLn("Det verkar vara problem med att läsa från stdin. Det kan hända att");
		    consoleWriteLn("Javamiljön för denna applikation inte har någon inmatningsterminal.");
		    consoleWriteLn("Om så är fallet, så bör en ny konsoll nu öppnas utav applikationen.");
		    consoleWriteLn("Om detta inte sker, starta om programmet med parametern \"-gui\", eller");
		    consoleWriteLn("sätt Javasystem-propertyn \"lattekom.use-gui\" till värdet \"true\".");
		    String test = crtReadLine("\nOm denna applikation har en inmatningsterminal, tryck <Enter>.");
		    if (test == null) { // assume we must use a GUI console
			useGui = true;
			initGui();
		    }
		}
	    }
	    foo = new Session();

	    initCommands();

	    foo.connect(server, 4894);
	    consoleWriteLn("Ansluten till " + server);
	    foo.addAsynchMessageReceiver(this);	    
	    
	    ConfInfo names[] = new ConfInfo[0];

	    int loginUser = 0;
	    String loginName = null;
	    String loginPassword = null;
	    while (!foo.getLoggedIn()) {
		while (loginUser == 0 && names.length != 1) {
		    String enteredName = defaultUser != null ? defaultUser : crtReadLine("Vad heter du? ");
		    names = foo.lookupName(enteredName,
					   true, false);
		    setStatus("Loggar in.");
		    if (names.length > 1) {
			System.err.println("Flertydigt namn.");
			if (defaultUser != null) defaultUser = null;
			for (int i=0;i<names.length;i++)
			    consoleWriteLn("\tAlternativ "+i+": " +
					   names[i].getNameString());
		    }
		    if (names.length == 0) {
			consoleWriteLn("Namnet finns inte.");
			if (crtReadLine("Vill du skapa ny person med namnet \"" + enteredName + "\"? (j/N)").equals("j")) {
			    consoleWriteLn("Du kan senare välja att byta namn, om du vill.");
			    consoleWriteLn("");
			    boolean passMatch = false;
			    String newPassword = null;
			    while (!passMatch) {
				newPassword = crtReadLine("Ange ett lösenord: ");
			    
				passMatch = newPassword.equals(crtReadLine("Upprepa: "));
				if (!passMatch) {
				    consoleWriteLn("Du angav inte samma lösenord båda gångerna. Försök igen.");
				}
			    }
			    try {
				int persNo = foo.createPerson(enteredName, newPassword, new Bitstring("0"), new AuxItem[] {});
				consoleWriteLn("Du fick nummer " + persNo);
				loginPassword = newPassword;
				loginName = enteredName;
				loginUser = persNo;			    
			    } catch (RpcFailure e1) {
				consoleWriteLn("%Fel: det gick inte att skapa någon person. Felkod " + e1.getError());
			    }

			}
		    }
		}
		if (loginUser == 0) {
		    loginName = names[0].getNameString();
		    consoleWriteLn(loginName);
		    if (defaultPassword != null) loginPassword = defaultPassword;
		    else loginPassword = crtReadPassword("Lösenord: ");
		    loginUser = names[0].getNo();
		} else {
		    names = foo.lookupName(loginName, true, false);
		    if (loginUser != names[0].getNo()){
			throw new RuntimeException("Fel: loginUser " + loginUser + " != " + names[0].getNo());
		    }
		}
		
		consoleWriteLn("Loggar in som " +
			       bytesToString(names[0].confName) + " (" +
			       loginUser + ")");
		if (!foo.login(names[0].confNo, loginPassword, false)) {
		    consoleWriteLn("Inloggningen misslyckades!");
		    loginPassword = null;
		    names = new ConfInfo[0];
		    loginUser = 0;
		}		
	    }

	    consoleWriteLn("Inloggad. Välkommen till LysKOM! Skriv \"?\" och tryck <Enter> för hjälp.");
	    consoleWrite("Vänta lite medan jag hämtar information om olästa möten...");
	    foo.updateUnreads();
	    consoleWriteLn("klart.");
	    if (useGui) {
		
		
	    }
	    if (fixedWhatIAmDoing != null) {
		foo.doChangeWhatIAmDoing(fixedWhatIAmDoing);
	    }

	    if (doKeepActive) {
		Thread t = new Thread(new Runnable() {
			public void run() {
			    Debug.println("Keep-Active thread start");
			    try {
				while (true) {
				    Thread.sleep(10*1000);
				    Debug.println("sending user-active");
				    foo.doUserActive();
				}
			    } catch (Exception e1) {
				Debug.println("Exception in keep-active thread");
			    }
			    Debug.println("Keep-Active thread ended");

			}
		    });
		t.setName("KeepActiveThread");
		t.start();
	    }

	    foo.setLatteVersion("T2", "$Version$");
	    //4303588, 100035, 4257987, 4244657
	    int me = foo.getMyPerson().getNo();
	    int rc = 1;
	    while (!shutdown) {
		int currentConference = foo.getCurrentConference();
		if (lastTextNo != 0) {
		    while (rc == 1) {
			handleMessages();
			try {
			    likesAsynchMessages = true;
			    String myCmd = crtReadLine(genericPrompt());
			    likesAsynchMessages = false;
			    rc = doCommand(myCmd);
			} catch (CmdErrException ex) {
			    consoleWriteLn("%Fel: " + ex.getMessage());
			    rc = 1;
			}
		    }
		}
		if (rc == 0) { // default-action (read next text)
		    int textNo = foo.nextUnreadText(false);
		    if (textNo == -1 && toread.empty() && toreview.empty()) { // if no unread text in current conference,
			int nextConf = foo.nextUnreadConference(true); // change conference
			if (nextConf == -1) {			    
			    //foo.sendMessage(0, "Skriv nå't då!");
			    consoleWriteLn("Det finns inget att läsa.");
			}
			else {
			    int unr = foo.getUnreadCount(nextConf);
			    String unrS;
			    if (unr < 1) unrS = "inga olästa";
			    else if (unr == 1) unrS = "ett oläst";
			    else unrS = unr + " olästa";
			    consoleWriteLn("Gick till möte: " + confNoToName(nextConf) + " - " + unrS);
			}
			rc = 1;
			continue;
		    }
		    
		    Text text;
		    if (!toreview.empty()) {
			textNo = ((Integer) toreview.pop()).intValue();
		    }

		    if (toreview.empty() && !toread.empty()) {
			textNo = ((Integer) toread.pop()).intValue();
		    }

		    text = foo.getText(textNo);
		    rc = 1;
		    
		    if (text != null) {
			displayText(text, true); noRead++;
			
			lastTextNo = textNo;
			lastText = text;
		    } else {
			lastTextNo = 0;
			lastText = null;
		    }
		    
		}
		if (rc == -1) shutdown = true;
	    }
	    
	    consoleWrite("\n\n-- Read " + noRead + " texts.\n");
	    
	    foo.logout(true);
	    foo.disconnect(false);
	    if (!embedded) System.exit(0);
	    consoleWriteLn("Logged out");
	} catch (ProtocolException ex) {
	    System.err.println("Protokollfel: "+ex);
	} catch (IOException ex) {
	    System.err.println("I/O-fel: "+ex);
	}
    }

    String genericPrompt()
    throws IOException {
	int conf = foo.getCurrentConference();
	String curConf = "";
	if (Boolean.getBoolean("lattekom.show-conf-prompt")) {
	    curConf = (conf == -1 ? "Ej närvarande i något möte" : "Du är i möte " + confNoToName(conf) + 
		       " med " + foo.getUnreadCount(conf) + " olästa.") + "\n";
	}
	if (!toreview.empty()) {
	    curConf += "(Återse nästa text) ";
	    setStatus("Återser");
	} else if (!toread.empty()) {
	    curConf += "(Läsa nästa kommentar) ";
	    setStatus("Läser kommentarer");
	} else if (foo.nextUnreadText(false) == -1) {
	    if (foo.nextUnreadConference(false) == -1) {
		curConf += "(Slut på inlägg) ";
		setStatus("Väntar på inlägg");
	    } else {
		curConf += "(Gå till nästa möte) ";
		if (conf > 0)
		    setStatus("Har just läst ut ett möte");
		else
		    setStatus(randomStatus());
	    }
	} else {
	    curConf += "(Läsa nästa inlägg) ";
	    setStatus("Läser inlägg");
	}
	Debug.println("toread.size(): " + toread.size() + ", toreview.size(): " + toreview.size());
	Debug.println("nextUnreadText: " + foo.nextUnreadText(false) +
		      ", nextUnreadConference: " + foo.nextUnreadConference(false));
	return curConf + "> ";
    }


    Random random = new Random();
    public int nextRandInt(int n) {
	if (n<=0)
	    throw new IllegalArgumentException("n must be positive");
	
	if ((n & -n) == n)  // i.e., n is a power of 2
	    return (int)((n * (long) random.nextInt()) >> 32);
	
	int bits, val;
	do {
	    bits = random.nextInt();
	    val = bits % n;
	} while(bits - val + (n-1) < 0);
	return val;
    }


    String[] randomStatus = {
	"Kliar sig i huvudet.",
	"Väntar på att himlen ska falla ner.",
	"Leker pixelleken.",
	"Funderar.",
	"Är.",
	"Äter banan." };
    String randomStatus() {
	return randomStatus[nextRandInt(randomStatus.length)];
    }


    public String pad(String in, int length) {
	if (in.length() > length) {
	    return in.substring(0, length -1);
	} else if (in.length() < length) {
	    int diff = length - in.length() - 1;
	    char[] padding = new char[diff];
	    for (int i = 0; i < diff; i++)
		padding[i] = ' ';
	    return (new StringBuffer(in).append(padding)).toString();
	} else {
	    return in;
	}
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
    int doCommand(String s)
    throws CmdErrException, IOException {
	Text t = getLastText();
	if (s == null) return -1;
	if (s.equals("")) return 0;
	StringTokenizer st = new StringTokenizer(s);
	String cmd = st.nextToken();
	Command command = commands.getCommand(cmd);
	if (command != null) {
	    Debug.println("Executing command " + command.toString());
	    String parameters = null;
	    try {
		parameters = st.nextToken("").substring(1);
	    } catch (NoSuchElementException ex1) {}
	    return command.doCommand(cmd, parameters);
	}
	if (cmd.equals("?")) {
	    StringBuffer help = new StringBuffer();
	    Command[] allCommands = commands.getAllCommands();
	    for (int i=0; i < allCommands.length; i++) {
		consoleWriteLn("-- " + allCommands[i].getDescription() + ":");
		String[] commandNames = allCommands[i].getCommands();
		String prevCommand = "";
		for (int j=0; j < commandNames.length; j++) {
		    String description = allCommands[i].getCommandDescription(j);
		    if (description.equals(prevCommand)) continue; // XXX: hack to get around
		    prevCommand = description;                     // commands with many names 
		                                                   // (this assumes they are 
		                                                   // all after each other)
		    consoleWriteLn("\t" + pad(commandNames[j], 10) + " -- " +
				   description);
		}
	    }
	    consoleWriteLn("-- övriga kommandon");
	    consoleWriteLn("\tln                -- lista nyheter");
	    consoleWriteLn("\tq                 -- avsluta TestKOM");
	    consoleWriteLn("\trt <text>         -- radera text");
	    consoleWriteLn("\ts [mötesnamn]     -- skicka meddelande");
	    consoleWriteLn("\tss <sessionsnr.>  -- visa sessions- och klientinformation");
	    consoleWriteLn("\tuo                -- uppdatera olästa (typ omstart)");
	    consoleWriteLn("\tv                 -- lista inloggade aktiva användare");
	    consoleWriteLn("\tåk                -- återse (första) kommentaren");
	    consoleWriteLn("\trpc <#> [data]    -- skicka RPC-kommando (svaret ignoreras) (farligt)");
	    consoleWriteLn("");
	    consoleWriteLn("  Mötesnamn kan för det mesta bytas ut mot \"m <nummer>\"");
	    return 1;
	}
	if (cmd.equals("uo")) {
	    foo.updateUnreads();
	    return 1;
	}
	if (cmd.equals("rpc")) { // skicka RPC-kommando till servern
	    int rno = 0;
	    String params = null;
	    try {
		rno = Integer.parseInt(st.nextToken());
		params = st.nextToken("").substring(1);
	    } catch (NoSuchElementException ex1) {
		if (rno == 0) throw new CmdErrException("parameter saknas");
	    }
	    foo.writeRaw(rno, params);
	    return 1;
	    
	}

	if (cmd.equals("v")) {
	    DynamicSessionInfo[] vilka = foo.whoIsOnDynamic(true, false, 30*60);
	    consoleWriteLn("Listar " + vilka.length + " aktiva användare:");
	    consoleWriteLn("----------------------------------------------------------------");
	    for (int i=0; i < vilka.length; i++) {
		String personName = confNoToName(vilka[i].getPerson());
		String conferenceName = confNoToName(vilka[i].getWorkingConference());

		consoleWrite(vilka[i].getSession() + " " + personName);
		if (vilka[i].getWorkingConference() != 0) {
		    consoleWrite(" i möte " + conferenceName);
		}
		consoleWriteLn("");
		consoleWriteLn("\t(" + bytesToString(vilka[i].getWhatAmIDoing()) + ")");
	    }
	    consoleWriteLn("----------------------------------------------------------------");
	    return 1;

	}
	if (cmd.equals("ss")) { // status (för) session
	    try {
		int sessionNo = Integer.parseInt(st.nextToken());
		SessionInfo si = foo.getStaticSessionInfo(sessionNo);
		consoleWriteLn("Status för session " + sessionNo + ":");
		consoleWriteLn(" Användarnamn : " + bytesToString(si.getUsername()));
		consoleWriteLn(" Värddator    : " + bytesToString(si.getHostname()));
		String ident = bytesToString(si.getIdentUser());
		if (!ident.equals("unknown")) {
		    consoleWriteLn(" Ident        : " + ident);
		}
		consoleWriteLn(" Starttid     : " + komTimeFormat(si.getConnectionTime().getTime()));
		String clientName = bytesToString(foo.getClientName(sessionNo));
		String clientVersion = bytesToString(foo.getClientVersion(sessionNo));
		if (!clientName.equals(""))
		    consoleWriteLn(" Klientnamn   : " + clientName);
		if (!clientVersion.equals(""))
		    consoleWriteLn(" Klientversion: " + clientVersion);
		
	    } catch (RpcFailure ex0) {
		throw new CmdErrException(ex0.getMessage());
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("Du måste ange ett sessionsnummer");
	    } catch (NumberFormatException ex2) {
		throw new CmdErrException("Kunde inte tolka sessionsnummer " + ex2.getMessage());
	    }
	    return 1;
	}
	if (cmd.equals("åk") || cmd.equals("}k")) { // återse (det) kommenterade
	    int[] commented = t.getCommented();
	    if (commented.length == 0) {
		commented = t.getFootnoted();
		if (commented.length == 0)
		    throw(new CmdErrException("Texten är ingen kommentar"));
	    }
	    consoleWriteLn("återse det kommenterade (text " + commented[0] + ")");
	    lastText = foo.getText(commented[0]);
	    displayText(commented[0]);
	    return 1;
	}
	if (cmd.equals("e")) { // endast
	    String antal = st.nextToken();
	    try {
		foo.endast(Integer.parseInt(antal));
		foo.updateUnreads();
	    } catch (NumberFormatException ex) {
		throw new CmdErrException("Kunde inte parsa " + antal);
	    }
	    return 1;

	}
	if (cmd.equals("s")) { // sända meddelande
	    int confNo = 0;
	    try {
		confNo = parseNameArgs(st.nextToken("").substring(1), true, true);
	    } catch (NoSuchElementException ex1) {
		if (!crtReadLine("Vill du skicka ett alarmmeddelande? (j/N) ").equals("j")) return 1;
	    }
	    if (confNo > 0) {
		consoleWriteLn("Skicka meddelande till " + confNoToName(confNo));
	    } else if (confNo == -1) {
		return 1;
	    }
	    String message = crtReadLine("Text att skicka> ");
	    if (message == null || message.trim().equals("")) {
		consoleWriteLn("Avbruten");
	    }
	    try {
		foo.sendMessage(confNo, message);
		if (confNo == 0) consoleWriteLn("Ditt alarmmeddelande har skickats.");
		else consoleWriteLn("Ditt meddelande har skickats till " + confNoToName(confNo));
	    } catch (RpcFailure ex1) {
		consoleWriteLn("Det gick inte att skicka meddelandet. Felkod: " + ex1.getError());
	    }
	    return 1;
	}
	if (cmd.equals("q") || cmd.equals(".")) return -1; // avsluta
	throw(new CmdErrException("Förstod inte \"" + cmd + "\""));
		      
    }

    void consoleWriteLn(String s) {
	linesSinceLastPrompt++;
	if (linesSinceLastPrompt > linesPerScreen) {
	    crtReadLine("(-- tryck Enter för att fortsätta --)", true);
	}
	consoleWrite(s + lineSeparator);	
	if (useGui && macBreak) consoleWrite("\r\n");
    }

    void consoleWrite(String s) {
	consoleWrite(s, false);
    }

    void consoleWrite(String s, boolean isTransient) {
	try {
	    if (useGui) {
		if (isTransient) {
		    console.appendTransient(s);
		} else {
		    console.append(s, false);
		}
		console.setCaretPosition(console.getText().length());
		return;
	    }
	    ByteArrayOutputStream consByteStream = new ByteArrayOutputStream();
	    byte[] sBytes = s.getBytes(encoding);
	    consByteStream.write(sBytes, 0, sBytes.length);
	    consByteStream.writeTo(System.out);
	} catch (UnsupportedEncodingException ex1) {
	    throw new RuntimeException("Unsupported console encoding: " + ex1.getMessage());
	} catch (IOException ex2) {
	    throw new RuntimeException("I/O exception while writing to stdout: " + ex2.getMessage());
	}
    }

    class T2KeyAdapter extends KeyAdapter {
	Object locker = null;
	public T2KeyAdapter(Object _locker) {
	    setLocker(_locker);
	}
	public void setLocker(Object o) {
	    locker = o;
	}
	public void keyReleased(KeyEvent e) {
	    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
		synchronized (locker) {
		    locker.notifyAll();
		}
	    }
	}
    }

    /**
     * Reads one line of input, perhaps without character echo
     * if the terminal type supports it.
     */
    String crtReadPassword(String prompt) {
	if (!useGui) return crtReadLine(prompt);
	Object waitLock = new Object();

	JFrame pFrame = new JFrame("Lösenord");
	Container cPane = pFrame.getContentPane();
	cPane.setLayout(new FlowLayout());
	cPane.add(new JLabel("Ange lösenord: "));
	JPasswordField pField = new JPasswordField(20);
	cPane.add(pField);
	pField.addKeyListener(new T2KeyAdapter(waitLock));
	pFrame.pack();
	pFrame.setVisible(true);
	pField.requestFocus();
	pFrame.toFront();
	try {
	    synchronized (waitLock) {
		waitLock.wait();
	    }
	} catch (InterruptedException ex1) {
	}
	pFrame.dispose();
	return new String(pField.getPassword());
	
    }

    /**
     * Reads one line of input from STDIN.
     * Returns null if EOF is encountered (eg, user presses ^D on an empty line).
     * Otherwise, returns a String with the user input, without trailing '\n'.
     */
    String crtReadLine(String prompt) {
	return crtReadLine(prompt, false);
    }

    String crtReadLine(String prompt, boolean isTransient) {
	linesSinceLastPrompt = 0;
	lastPromptShown = prompt;
	if (prompt != null) {
	    consoleWrite(prompt, isTransient);
	}

	if (!useGui) {
	    try {	    
		int b = System.in.read();
		ByteArrayOutputStream inByteStream = new ByteArrayOutputStream();
		while (b != -1 && b != '\n') {
		    inByteStream.write(b);
		    b = System.in.read();
		}
		if (b == -1) return null;
		return inByteStream.toString(encoding).trim();
	    } catch (UnsupportedEncodingException ex1) {
		throw new RuntimeException("Unsupported console encoding: " + ex1.getMessage());
	    } catch (IOException ex0) {
		throw new RuntimeException("Error reading from stdin");
	    }
	} else {
	    synchronized (guiInput) {
		try {
		    guiInput.wait();
		} catch (InterruptedException ex1) {
		    throw new RuntimeException("Interrupted while waiting for user input");
		}
		return guiInput.getString();
	    }
	}
    }
    /**
     * Read one row from user input, if the resulting row is the
     * empty string, return a default string instead.
     * @param prompt The prompt to be used when asking user for input
     * @param default The default string to be returned if user presses enter
     * @see nu.dll.app.test.Test2#crtReadLine(String)
     */
    public String crtReadLine(String prompt, String defaultValue) {
	String s = crtReadLine(prompt);
	if (s == null || s.trim().length() == 0) return defaultValue;
	return s;
    }

    Text editText(Text t)
    throws IOException {
	String subject = bytesToString(t.getSubject());

	int[] rcpts = t.getRecipients();
	for (int i=0; i < rcpts.length; i++) {
	    consoleWriteLn("Mottagare: " + confNoToName(rcpts[i]));
	}
	int[] ccRcpts = t.getCcRecipients();
	for (int i=0; i < ccRcpts.length; i++) {
	    consoleWriteLn("Extra kopiemottagare: " + confNoToName(ccRcpts[i]));
	}

	if (subject.length() == 0) {
	    subject = crtReadLine("Ärende: ");
	} else {
	    consoleWriteLn("Ärende: " + subject);
	}

	consoleWriteLn("--- skriv \"!?\" på tom rad för hjälp ----------------------------------");
	if (subject == null) return null;
	int rowCount = 1;
	int currentRow = 1;

	List rows = t.getBodyList();

	if (rows.size() > 0) {
	    for (int i=0; i < rows.size(); i++) {
		consoleWriteLn((i+1) + ": " + rows.get(i));
	    }
	    consoleWriteLn("");
	}

	String row = crtReadLine(rows.size()+1 + ": ");
	while (row != null && !row.equals(".")) {
	    if (row.startsWith("!")) {
		StringTokenizer rst = new StringTokenizer(row);
		String icmd = rst.nextToken();
		if (icmd.startsWith("!f")) {
		    String confs = rst.nextToken("").substring(1);
		    int newConf = parseNameArgs(confs, true, true);
		    if (newConf > 0) {
			consoleWriteLn("** Byter mottagare till " + confNoToName(newConf));
			t.addCcRecipients(t.getRecipients());
			t.clearRecipients();
			t.addRecipient(newConf);
		    } 
		} else if (icmd.startsWith("!am")) {
		    String confs = rst.nextToken("").substring(1);
		    int newConf = parseNameArgs(confs, true, true);
		    if (newConf > 0) {			
			consoleWriteLn("** Adderar " + confNoToName(newConf) + " som mottagare");
			t.addRecipient(newConf);
		    }		    
		} else if (icmd.startsWith("!sk")) {
		    String confs = rst.nextToken("").substring(1);
		    int newConf = parseNameArgs(confs, true, true);
		    if (newConf > 0) {
			consoleWriteLn("** Subtraherar " + confNoToName(newConf) + " från kopiemottagarlista");
			t.removeCcRecipient(newConf);
		    }
		} else if (icmd.startsWith("!sm")) {
		    String confs = rst.nextToken("").substring(1);
		    int newConf = parseNameArgs(confs, true, true);
		    if (newConf > 0) {
			consoleWriteLn("** Subtraherar " + confNoToName(newConf) + " från mottagarlista");
			t.removeRecipient(newConf); 
		    } 
		} else if (icmd.startsWith("!ak")) {
		    String confs = rst.nextToken("").substring(1);
		    int newConf = parseNameArgs(confs, true, true);
		    if (newConf > 0) {
			consoleWriteLn("** Adderar " + confNoToName(newConf) + " som kopiemottagare");
			t.addCcRecipient(newConf);
		    }
		} else if (icmd.startsWith("!är")) {
		    int changeRow = Integer.parseInt(rst.nextToken());
		    String thisRow = (String) rows.get(changeRow-1);
		    consoleWriteLn("** Skriv om rad " + changeRow + ":");
		    consoleWriteLn(changeRow + ": " + thisRow);
		    rows.set(changeRow-1, crtReadLine(changeRow + ": "));
		    consoleWriteLn("");
		} else if (icmd.startsWith("!rr")) {
		    if (rst.hasMoreTokens()) {
			int delRow = Integer.parseInt(rst.nextToken());
			String theRow = (String) rows.get(delRow-1);
			consoleWriteLn("** Ta bort rad " + delRow);
			consoleWriteLn(delRow + ": " + theRow);
			if (crtReadLine("Vill du ta bort denna rad? (j/N) ").equals("j")) {
			    rows.remove(delRow-1);
			}
		    } else {
			consoleWriteLn("** Du måste ange ett radnummer.");
		    }
		    consoleWriteLn("");
		} else if (icmd.startsWith("!ää")) {
		    if (rst.hasMoreElements()) {
			subject = rst.nextToken("").substring(1);
			consoleWriteLn("** Ändrade ärende till: " + subject);
		    } else {
			consoleWriteLn("** Ändra ärende:");
			subject = crtReadLine("Ärende: ");
		    }
		} else if (icmd.startsWith("!v")) {
		    consoleWriteLn("** Hela texten:");
		    for (int i=0; i < rows.size(); i++) {
			consoleWriteLn((i+1) + ": " + rows.get(i));
		    }
		    consoleWriteLn("");
		} else if (icmd.startsWith("!q")) {
		    return null;
		} else if (icmd.startsWith("!?")) {
		    consoleWriteLn("** Kommandon i editorläge:");
		    consoleWriteLn("  !f <möte>     Flytta texten till <möte>");
		    consoleWriteLn("  !am <möte>    Addera <möte> som mottagare");
		    consoleWriteLn("  !ak <möte>    Addera <möte> som kopiemottagare");
		    consoleWriteLn("  !rr <radnr.>  Radera <radnr.>");
		    consoleWriteLn("  !sm <möte>    Subtrahera <möte> från mottagarlista");
		    consoleWriteLn("  !sk <möte>    Subtrahera <möte> från kopiemottagarlista");
		    consoleWriteLn("  !v            Visa hela texten");
		    consoleWriteLn("  !q            Avsluta utan att spara");
		    consoleWriteLn("  !är <radnr.>  Skriv om rad <radnr.>");
		    consoleWriteLn("  !ää           Ändra ärende");
		    consoleWriteLn("  .             Spara och avsluta");
		} else {
		    consoleWriteLn("** Okänt editorkommando");
		}
	    } else {
		if (row.length() > 73) {
		    consoleWriteLn("** Varning: rad " + (rows.size()+1) + " är över 73 tecken lång.");
		}
		rows.add(row);
		rowCount++;
	    }
	    row = crtReadLine(rows.size()+1 + ": ");
	}

	StringBuffer textb = new StringBuffer();
	Iterator i = rows.iterator();
	while (i.hasNext()) {
	    textb.append(i.next().toString() + "\n");
	}
	
	t.setContents((subject + "\n" + textb.toString().trim()).getBytes(Session.serverEncoding));
	return t;
    }
    void displayText(int tn) throws IOException {
	Text t = foo.getText(tn);
        if (t == null) {
            System.err.println("%Fel: text " + tn + " finns inte.");
            return;
        }
	displayText(t);
    }

    void saveText(Text text) throws IOException {
        ObjectOutputStream objStream =
            new ObjectOutputStream(new FileOutputStream(new File("texts" +
                              File.separator + text.getNo() + ".txt")));
        objStream.writeObject(text);
        objStream.close();
    }

    void markAsRead(int textNo) throws IOException, RpcFailure {
	Debug.println("markAsRead(" + textNo + ")");
	foo.markAsRead(textNo);
    }

    void displayText(Text text, boolean markAsRead) throws IOException {
	if (text == null) {
	    throw new RuntimeException("text is null");
	}
	displayText(text);
	if (markAsRead && text != null) {
	    markAsRead(text.getNo());

	    // retreive all comments of the texts, push them onto
	    // the 'toread' stack for later viewing (in reverse)
	    int[] comments = text.getComments();
	    for (int i=comments.length-1; i >= 0; i--) {
		if (!foo.getReadTexts().contains(comments[i]))
		    toread.push((Object) new Integer(comments[i]));
	    }
	}
    }

    String textDescription(int textNo)
    throws IOException {
	TextStat stat = foo.getTextStat(textNo);
	if (stat == null) {
	    return "text " + textNo + " (osynlig)";
	} else {
	    return "text " + textNo + " av " + confNoToName(stat.getAuthor());
	}
    }

    /**
     * Displays a text in elisp-client-style.
     */
    void displayText(Text text) throws IOException {
        if (false) {
            saveText(text);   
        }
	consoleWriteLn(text.getNo()+" " + komTimeFormat(text.getCreationTime()) + " /" + text.getRows() + " " +
			   (text.getRows() > 1 ? "rader" : "rad") + "/ " +
			   (text.getAuthor() > 0 ? confNoToName(text.getAuthor()) : "anonym person"));

	int[] rcpts = text.getStatInts(TextStat.miscRecpt);
	int[] ccRcpts = text.getCcRecipients();
	int[] sentBy = text.getStatInts(TextStat.miscSentBy);
	int[] footnotes = text.getFootnotes();

	int[] locals = text.getStatInts(TextStat.miscLocNo);
	int[] commented = text.getCommented();
	int[] footnoted = text.getFootnoted();
	Enumeration sentAt = null;

	for (int i=0; i < commented.length; i++)
	    consoleWriteLn("Kommentar till " + textDescription(commented[i]));

	for (int i=0; i < footnoted.length; i++)
	    consoleWriteLn("Fotnot till " + textDescription(footnoted[i]));


	for (int i=0; i<rcpts.length; i++) { // recipients
	    consoleWriteLn("Mottagare: " +
			       confNoToName(rcpts[i]) + " <" + text.getLocal(rcpts[i]) + ">");
	    
	}
	for (int i=0; i<ccRcpts.length; i++) { // recipients
	    consoleWriteLn("Extra kopia: " + confNoToName(ccRcpts[i]) + " <" + text.getLocal(ccRcpts[i]) + ">");
	}



	consoleWriteLn("Ärende: " + bytesToString(text.getSubject()));
	consoleWriteLn("------------------------------------------------------------");
	String textBody = bytesToString(text.getBody());
	StringTokenizer rows = new StringTokenizer(textBody, "\n");
	while (rows.hasMoreElements()) {
	    consoleWriteLn(rows.nextToken());
	}
	consoleWriteLn("(" + text.getNo() + ") /" + confNoToName(text.getAuthor()) +  "/--------------------");

	int[] comments = text.getComments();
	for (int i=0; i < comments.length; i++) {
	    consoleWriteLn("Kommentar i " + textDescription(comments[i]));
	}

	if (showAux) {
	    AuxItem[] auxs = text.getStat().getAuxItems();
	    //if (text.getStat().countAuxItems() > 0) consoleWriteLn("\t** Aux-saker:");
	    for (int i=0; i<text.getStat().countAuxItems(); i++)
		consoleWriteLn("\tAux-Item["+i+"]: typnummer: "+auxs[i].getNo() + ", data: " +
				   bytesToString(auxs[i].getData().getContents()));
	}
	for (int i=0; i < footnotes.length; i++) {
	    TextStat ts = foo.getTextStat(footnotes[i]);
	    consoleWrite("Det finns en fotnot i text " + footnotes[i]);
	    if (ts != null) {
		consoleWrite(" av " + confNoToName(ts.getAuthor()) + ", läsa? (J/n) ");
		if (crtReadLine("").equals("n")) continue;
		displayText(foo.getText(footnotes[i]), true);
	    } else {
		consoleWriteLn("som du inte får läsa.");
	    }

	}
    }

    static JFrame createConsoleFrame() {
	    JFrame frame = new JFrame();
	    frame.setSize(800,600);
	    frame.setVisible(true);
	    return frame;
    }

    public void consoleAction(ConsoleEvent event) {
	Debug.println("consoleAction(): " + event.getCommunique());
	if (event.getType() == ConsoleEvent.COMMAND_ENTERED) {
	    synchronized (guiInput) {
	        guiInput.setString((String) event.getCommunique());
		guiInput.notify();
	    }
	}
    }

	
    public static void main(String[] argv) {
	Test2 t2 = new Test2();
	
	if (t2.parseArgs(argv))	{
	    t2.consoleWriteLn("IJKLKOM (c) 1999 Rasmus Sten");
	    t2.run();
	}
    }

    boolean shutdown = false;
    class GuiFrameActionListener extends WindowAdapter {
	public void windowClosing(WindowEvent e) {
	    synchronized (guiInput) {
		shutdown = true;
		useGui = false;
		guiInput.setString("q"); // cphack. :-)
		guiInput.notify();
	    }
	}
    }

    
    boolean guiInited = false;
    void initGui() {
	initGui(false);
    }
    void initGui(boolean embedded) {
	if (guiInited) {
	    System.err.println("Konsoll-GUI är redan initialiserat.");
	    return;
	}
	guiInput = new GuiInput();
	Debug.println("Initialiserar GUI-konsoll.");

	if (!embedded) consoleFrame = createConsoleFrame();

	console = new Console(25, 80);
	console.setConsoleThingies("");

	if (!embedded) {
	    consoleFrame.getContentPane().add(new JScrollPane(console));
	    consoleFrame.setTitle("LatteKOM/T2");
	    consoleFrame.setVisible(true);
	    consoleFrame.addWindowListener(new GuiFrameActionListener());
	    consoleFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    console.requestFocus();
	}

	console.setFont(new Font(System.getProperty("lattekom.gui-font", "monospaced"), Font.PLAIN, 14));
	System.setOut(new PrintStream(new LogOutputStream(console)));
	console.addConsoleListener(this);
	console.addComponentListener(new ComponentAdapter() {
		public void componentResized(ComponentEvent e) {
		    int rows = console.getRows();
		    Debug.println("Changed console rows to " + rows);
		    linesPerScreen = rows; // has no effect, never changes 
		}
	    });
	if (!embedded && !Debug.ENABLED) {
	    System.setErr(new PrintStream(new LogOutputStream(console)));
	}
	guiInited = false;
    }

    public Console getConsole() {
	return console;
    }

    public Test2(boolean embedded, String server) {
	this.server = server;
	init(embedded);
    }

    public Test2(boolean embedded) {
	init(embedded);
    }

    public Test2() {
	init(false);
    }

    private void init(boolean embedded) {
	this.embedded = embedded;
	useGui = embedded;
	if (useGui) {
	    initGui(embedded);
	}
	asynchInvoker = new AsynchInvoker();
	asynchInvoker.setName("T2AsynchHandler-" + (++handlerCount));
	asynchInvoker.start();
    }
    static int handlerCount = 0;

    /**
     * Changed this to be a little more stable: messages are enqueued
     * into a single thread. It's particularly nifty, especially not
     * the prompt handling.
     */ 
    public void asynchMessage(AsynchMessage m) {
	synchronized (messages) {
	    messages.addLast(m);
	}
	if (likesAsynchMessages) {
	    asynchInvoker.enqueue(new Runnable() {
		    public void run() {
			consoleWriteLn("");
			handleMessages();
			try {
			    consoleWrite(genericPrompt());
			    linesSinceLastPrompt = 0;
			} catch (IOException ex1) {
			    throw new RuntimeException("I/O error: " + ex1.getMessage());
			}
		    }
		});
	    Debug.println("Enqueued asynch runner for " + m.toString());
	} else {
	    Debug.println("Did not enqueue a runner for " + m.toString());
	}
    }

}









