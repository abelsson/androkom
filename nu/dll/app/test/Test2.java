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

import java.text.SimpleDateFormat;
import java.text.MessageFormat;

import org.leen.java.awt.console.*;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import nu.dll.lyskom.*;

/**
 * Test2 - LysKOM client test program. Can read texts and post comments.
 * Takes two optional arguments: the username and the password.
 *
 */
public class Test2 implements AsynchMessageReceiver, ConsoleListener {

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

    Stack toread = new Stack(); // used to store unread comments
    Stack toreview = new Stack(); // �terse-stack

    static String encoding = System.getProperty("lattekom.encoding");
    static String lineSeparator = System.getProperty("line.separator");

    static boolean showAux = Boolean.getBoolean("lattekom.showaux");
    static boolean dontMarkOwnTextsAsRead = Boolean.getBoolean("lattekom.dont-mark-own-texts-as-read");
    static boolean useAnsiColors = Boolean.getBoolean("lattekom.use-ansi");
    static boolean doKeepActive = Boolean.getBoolean("lattekom.keep-active");
    static boolean useGui = Boolean.getBoolean("lattekom.use-gui");

    static String fixedWhatIAmDoing = System.getProperty("lattekom.whatiamdoing");

    GuiInput guiInput = null;

    static {
	if (encoding == null) {
	    if (System.getProperty("os.name").startsWith("Windows") && !useGui) encoding = "Cp437";
	    else encoding = "ISO-8859-1";
	}
    }

    public void setStatus(String s) 
    throws IOException {
	if (fixedWhatIAmDoing == null) foo.changeWhatIAmDoing(s);
	    
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
	    return "M�te " + ex1.getErrorStatus() + " (fel " + ex1.getError() + ")";
	}
	if (name == null)
	    return "Person "+n+" (N/A)";
	if (useAnsiColors)
	    return "\u001b[01;34m" + new String(name) + "\u001b[0m";

	return new String(name);
    }

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
		consoleWriteLn("%Fel: kunde inte tolka m�tesnummer " + ex1.getMessage());
	    }
	}

	ConfInfo[] names = foo.lookupName(arg, wantPersons, wantConfs);
	if (names.length == 0) {
	    consoleWriteLn("%Fel: hittar inget s�dant m�te eller s�dan person");
	    return -1;
	}
	if (names.length > 1) {
	    consoleWriteLn("%Fel: flertydigt namn");
	    for (int i=0; i < names.length; i++)
		consoleWriteLn("-- alternativ " + (i+1) + ": " + names[i].getNameString());
	    return 0;
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

    boolean serverSynch = false;
    Locale locale = new Locale("sv", "se");  // language and location
    SimpleDateFormat timestampFormat = new SimpleDateFormat("EEEE d MMMM kk:mm", new Locale("sv", "se"));
    void handleMessages() {
	synchronized (messages) {
	    while (messages.size() > 0) {
		AsynchMessage m = (AsynchMessage) messages.removeFirst();
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
			System.err.println("Det gick inte att utf�ra get-conf-name: " + ex.getMessage());
		    }
		    consoleWrite("\007\007"); // beep!
		    consoleWriteLn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		    if (params[0].intValue() == foo.getMyPerson().getNo()) {
			consoleWriteLn("Personling meddelande fr�n " + sender + 
					   " (" + timestampFormat.format(m.getArrivalTime()) + "):");
		    } else if (params[0].intValue() == 0) {
			consoleWriteLn("Alarmmeddelande fr�n " + sender + " (" + timestampFormat.format(m.getArrivalTime()) + "):");
		    } else {
			consoleWriteLn("Meddelande till " + recipient);
			consoleWriteLn("fr�n " + sender +
					   " (" + timestampFormat.format(m.getArrivalTime()) + "):");
		    }
		    consoleWriteLn("\n" + ((Hollerith) params[2]).getContentString());
		    consoleWriteLn("----------------------------------------------------------------");
		    break;
		case Asynch.login:
		    try {
			consoleWriteLn(confNoToName(params[0].intValue()) + " loggade in i LysKOM (" +
 					   timestampFormat.format(m.getArrivalTime()) + ")");

		    } catch (IOException ex) {
			System.err.println("Det gick inte att utf�ra get-conf-name: " + ex.getMessage());			
		    }
		    break;
		case Asynch.logout:
		    try {
			consoleWriteLn(confNoToName(params[0].intValue()) + " loggade ut ur LysKOM (" +
 					   timestampFormat.format(m.getArrivalTime()) + ")");

		    } catch (IOException ex) {
			System.err.println("Det gick inte att utf�ra get-conf-name: " + ex.getMessage());			
		    }
		    break;
		case Asynch.sync_db:
		    serverSynch = !serverSynch;
		    if (serverSynch) {
			consoleWriteLn("** Servern synkroniserar just nu databasen.");
		    } else {
			consoleWriteLn("** Servern �r klar med synkroniseringen.");
		    }
		    break;
		    //case Asynch.new_text:
		    //break;
		default: 
		    consoleWriteLn("Asynkront meddelande av typen " + m.getNumber());
		}
	    }
	}
    }

    public void run(String[] argv) {
	try {

	    String server = System.getProperty("lyskom.server") == null ? "sno.pp.se" :
		System.getProperty("lyskom.server");

	    foo = new Session();
	    foo.connect(server, 4894);
	    consoleWriteLn("Ansluten till " + server);
	    foo.addAsynchMessageReceiver(this);	    
	    
	    ConfInfo names[] = new ConfInfo[0];

	    int loginUser = 0;
	    String loginName = null;
	    String loginPassword = null;
	    while (loginUser == 0 && names.length != 1) {
		String enteredName = argv.length > 0 ? argv[0] : crtReadLine("Vad heter du? ");
		names = foo.lookupName(enteredName,
				       true, false);
		setStatus("Loggar in.");
		if (names.length > 1) {
		    System.err.println("Flertydigt namn.");
		    if (argv.length > 0) System.exit(-42);
		    for (int i=0;i<names.length;i++)
			consoleWriteLn("\tAlternativ "+i+": " +
					   names[i].getNameString());
		}
		if (names.length == 0) {
		    consoleWriteLn("Namnet finns inte.");
		    if (crtReadLine("Vill du skapa ny person med namnet \"" + enteredName + "\"? (j/N)").equals("j")) {
			consoleWriteLn("Du kan senare v�lja att byta namn, om du vill.");
			consoleWriteLn("");
			boolean passMatch = false;
			String newPassword = null;
			while (!passMatch) {
			    newPassword = crtReadLine("Ange ett l�senord: ");
			    
			    passMatch = newPassword.equals(crtReadLine("Upprepa: "));
			    if (!passMatch) {
				consoleWriteLn("Du angav inte samma l�senord b�da g�ngerna. F�rs�k igen.");
			    }
			}
			try {
			    int persNo = foo.createPerson(enteredName, newPassword, new Bitstring("0"), new AuxItem[] {});
			    consoleWriteLn("Du fick nummer " + persNo);
			    loginPassword = newPassword;
			    loginName = enteredName;
			    loginUser = persNo;			    
			} catch (RpcFailure e1) {
			    consoleWriteLn("%Fel: det gick inte att skapa n�gon person. Felkod " + e1.getError());
			}

		    }
		}
	    }

	    if (loginUser == 0) {
		loginName = names[0].getNameString();
		if (argv.length > 1) loginPassword = argv[1];
		else loginPassword = crtReadLine("L�senord: ");
		loginUser = names[0].getNo();
	    } else {
		names = foo.lookupName(loginName, true, false);
		if (loginUser != names[0].getNo()){
		    throw new RuntimeException("Fel: loginUser " + loginUser + " != " + names[0].getNo());
		}
	    }

	    consoleWriteLn("Loggar in som " +
			       new String(names[0].confName) + " (" +
			       loginUser + ")");
	    if (!foo.login(names[0].confNo, loginPassword, false)) {
		System.err.println("Inloggningen misslyckades!");
		System.exit(-32);
	    }
	    consoleWriteLn("Inloggad. V�lkommen till LysKOM!");
	    consoleWrite("V�nta lite medans jag h�mtar information om ol�sta m�ten...");
	    foo.updateUnreads();
	    consoleWriteLn("klart.");

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


	    //4303588, 100035, 4257987, 4244657
	    int me = foo.getMyPerson().getNo();
	    boolean go = true;
	    int rc = 1;
	    while (go) {
		int currentConference = foo.getCurrentConference();
		if (lastTextNo != 0) {
		    while (rc == 1) {
			handleMessages();
			try {
			    likesAsynchMessages = true;
			    rc = doCommand(lastText, crtReadLine(genericPrompt()));
			} catch (CmdErrException ex) {
			    consoleWriteLn("%Fel: " + ex.getMessage());
			    rc = 1;
			}
		    }
		}
		if (rc == 0) { // default-action (read next text)
		    int textNo = foo.nextUnreadText(false);
		    if (textNo == -1) { // if no unread text in current conference,
			int nextConf = foo.nextUnreadConference(true); // change conference
			if (nextConf == -1) {			    
			    //foo.sendMessage(0, "Skriv n�'t d�!");
			    consoleWriteLn("Det finns inget att l�sa.");
			}
			else {
			    int unr = foo.getUnreadCount(nextConf);
			    String unrS;
			    if (unr < 1) unrS = "inga ol�sta";
			    else if (unr == 1) unrS = "ett ol�st";
			    else unrS = unr + " ol�sta";
			    consoleWriteLn("Gick till m�te: " + confNoToName(nextConf) + " - " + unrS);
			}
			rc = 1;
			continue;
		    }
		    
		    Text text;
		    if (!toreview.empty()) {
			textNo = ((Integer) toreview.pop()).intValue();
		    }

		    // if we have an unread comment on the stack from earlier texts
		    // pop it off
		    if (toreview.empty() && !toread.empty()) {
			textNo = ((Integer) toread.pop()).intValue();
		    }
		    
		    text = foo.getText(textNo);
		    displayText(text, true); noRead++;
		    
		    rc = 1;
		    if (text != null) {
			lastTextNo = textNo;
			lastText = text;
		    } else {
			lastTextNo = 0;
			lastText = null;
		    }
		    
		}
		if (rc == -1) go = false;
	    }
	    
	    consoleWrite("\n\n-- Read " + noRead + " texts.\n");
	    
	    foo.logout(true);
	    consoleWriteLn("Logged out");
	    System.exit(0);
	} catch (ProtocolException ex) {
	    consoleWriteLn("Test: "+ex);
	} catch (IOException ex) {
	    consoleWriteLn("Test:  "+ex);
	}
	System.exit(0);
    }

    String genericPrompt()
    throws IOException {
	int conf = foo.getCurrentConference();
	String curConf = (conf == -1 ? "Ej n�rvarande i n�got m�te" : "Du �r i m�te " + confNoToName(conf) + 
			  " med " + foo.getUnreadCount(conf) + " ol�sta.") + "\n";
	if (!toreview.empty()) {
	    curConf += "(�terse n�sta text) ";
	    setStatus("�terser.");
	} else if (!toread.empty()) {
	    curConf += "(L�sa n�sta kommentar) ";
	    setStatus("L�ser kommentarer.");
	} else if (foo.nextUnreadText(false) == -1) {
	    if (foo.nextUnreadConference(false) == -1) {
		curConf += "(Slut p� inl�gg) ";
		setStatus("V�ntar p� inl�gg.");
	    } else {
		curConf += "(G� till n�sta m�te) ";
		setStatus("Har just l�st ut ett m�te");
	    }
	} else {
	    curConf += "(L�sa n�sta inl�gg) ";
	    setStatus("L�ser inl�gg.");
	}
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
	likesAsynchMessages = false;
	StringTokenizer st = new StringTokenizer(s);
	String cmd = st.nextToken();
	if (cmd.equals("?")) {
	    consoleWriteLn("-- kommandon\n" +
			   "\tbn <m�tesnamn>    -- byt namn\n" + 
			   "\tf [inl�ggsnummer] -- fotnotera inl�gg\n" + 
			   "\tg <m�tesnamn>     -- g� till m�te\n" +
			   "\ti                 -- skriv inl�gg\n" +
			   "\tk [inl�ggsnummer] -- kommentera text\n" +
			   "\tlm [substr�ng]    -- lista m�ten\n" +
			   "\tlmt               -- lista markerade texter\n" +
			   "\tln                -- lista nyheter\n" +
			   "\tmt                -- markera senast l�sta text\n" +
			   //"\tp [m�tesnamn]     -- skriv presentation (*)\n" + 
			   "\tq                 -- avsluta TestKOM\n" +
			   //"\trm <m�tesnamn>    -- radera m�te\n" +
			   "\trt <text>         -- radera text\n" +
			   "\ts [m�tesnamn]     -- skicka meddelande\n" + 
			   "\tsm <m�tesnamn>    -- skapa (publikt) m�te\n" +
			   "\tuo                -- uppdatera ol�sta (typ omstart)\n" + 
			   "\tv                 -- lista inloggade anv�ndare\n" + 
			   "\t� <inl�ggsnummer> -- �terse text\n" +
			   "\t�k                -- �terse (f�rsta) kommentaren\n" +
			   "\t�u                -- �terse (f�rsta) urinl�gget\n" +
			   "\t�f <m�tesnamn>    -- �ndra FAQ\n" +
			   "\trpc <#> [data]    -- skicka RPC-kommando (svaret ignoreras)\n" + 
			   "\n  M�tesnamn kan f�r det mesta bytas ut mot \"m <nummber>\"\n");
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
	    /*
	    RpcReply reply = foo.writeRaw(rno, params);
	    consoleWriteLn("Fick svar: " + reply);
	    KomToken[] reqparams = reply.getParameters();
	    for (int i=0; i < reqparams.length; i++) {
		consoleWriteLn("\t--> Parameter " + i + ": " +
				   reqparams[i]);
	    }
	    */
	    return 1;
	    
	}
	if (cmd.equals("�f")) { // �terse FAQ
	    int confNo = 0;
	    try {
		confNo = parseNameArgs(st.nextToken(), true, true);
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("Du m�ste ange ett m�te eller brevl�da");
	    }
	    if (confNo < 1) return 1;
	    consoleWriteLn("�terse FAQ f�r m�te " + confNoToName(confNo));
	    AuxItem[] confAuxItems = foo.getConfStat(confNo).getAuxItems();
	    for (int i=0; i < confAuxItems.length; i++) {
		if (confAuxItems[i].getTag() == AuxItem.tagFaqText) {
		    displayText(foo.getText(confAuxItems[i].getData().intValue()));
		}
	    }
	    return 1;

	}
	if (cmd.equals("�f")) { // �ndra FAQ
	    int confNo = 0;
	    try {
		confNo = parseNameArgs(st.nextToken(), true, true);
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("Du m�ste ange ett m�te eller brevl�da");
	    }
	    if (confNo < 1) return 1;
	    consoleWriteLn("�ndra FAQ f�r m�te " + confNoToName(confNo));
	    AuxItem[] confAuxItems = foo.getConfStat(confNo).getAuxItems();
	    List oldFaqItems = new LinkedList();
	    for (int i=0; i < confAuxItems.length; i++) {
		if (confAuxItems[i].getTag() == AuxItem.tagFaqText)
		    oldFaqItems.add(new Integer(confAuxItems[i].getNo()));
	    }
	    int[] oldFaqAux = new int[oldFaqItems.size()];
	    for (int i=0; i < oldFaqAux.length; i++) oldFaqAux[i] = ((Integer) oldFaqItems.get(i)).intValue();

	    String faqText = crtReadLine("Ange textnummer f�r FAQ> ");
	    int faq;
	    try {
		faq = Integer.parseInt(faqText);
	    } catch (NumberFormatException e) {
		throw new CmdErrException("Felaktigt textnummer");
	    }
	    try {
		foo.modifyAuxInfo(true, confNo, oldFaqAux,
				  new AuxItem[] { new AuxItem(AuxItem.tagFaqText,
							      new Bitstring("00000000"), 0,
							      new Hollerith(""+faq)) });
		consoleWriteLn("OK: FAQ satt till text " + faq);
	    } catch (RpcFailure ex1) {
		consoleWrite("%Fel: gick inte att �ndra FAQ: ");
		switch (ex1.getError()) {
		case Rpc.E_aux_item_permission:
		    consoleWriteLn("otillr�cklig beh�righet");
		    break;
		case Rpc.E_illegal_aux_item:
		    consoleWriteLn("felaktigt aux-item");
		    break;
		default:
		    consoleWriteLn("felkod " + ex1.getError());
		}
	    }


	    return 1;
	}
	if (cmd.equals("�")) { // �terse inl�gg (� <txtno>)
	    String txt = st.hasMoreTokens() ? st.nextToken() : null;
	    if (txt == null) throw new CmdErrException("du m�ste ange inl�ggsnummer");
	    int txtNo = 0;
	    try {
		txtNo = Integer.parseInt(txt);
	    } catch (NumberFormatException ex) {
		throw(new CmdErrException("trasigt inl�ggsnummer: " + txt));
	    }
	    lastText = foo.getText(txtNo);
	    displayText(txtNo);
	    return 1;
	}
	if (cmd.equals("ln")) { // lista nyheter
	    int[] conferences = foo.getUnreadConfs(foo.getMyPerson().getNo());
	    int sum = 0;
	    int confsum = 0;
	    for (int i=0; i < conferences.length; i++) {
		int unreads = foo.getUnreadCount(conferences[i]);
		sum += unreads;
		confsum++;
		consoleWriteLn("Du har " + unreads + " " + (unreads > 1 ? "ol�sta" : "ol�st") +
			       " i " + confNoToName(conferences[i]));
	    }
	    if (confsum == 0) {
		consoleWriteLn("Du har l�st alla inl�gg.");
	    } else {
		consoleWriteLn("");
		consoleWriteLn("Du har totalt " + sum + " " + (sum > 1 ? "ol�sta" : "ol�st") +
			       " inl�gg i " + confsum + " " + (confsum > 1 ? "m�ten" : "m�te"));
	    }
	    
	    return 1;
	}
	if (cmd.equals("rm")) { // radera m�te
	    consoleWriteLn("-- Den h�r funktionen �r inte implementerad �nnu.");
	    return 1;	    
	}
	if (cmd.equals("rt")) { // radera inl�gg
	    int textNo = 0;
	    try {
		textNo = Integer.parseInt(st.nextToken());
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("du m�ste ange ett inl�ggsnummer");
	    } catch (NumberFormatException ex2) {
		throw new CmdErrException("kunde inte tolka inl�ggsnummer");
	    }
	    if (textNo == 0) throw new CmdErrException("du kan inte ta bort text 0");
	    try {
		foo.deleteText(textNo);
		return 1;
	    } catch (RpcFailure ex1) {
		switch (ex1.getError()) {
		case Rpc.E_no_such_text:
		    consoleWriteLn("%Fel: det finns ingen text med nummer " + textNo);
		    break;
		case Rpc.E_not_author:
		    consoleWriteLn("%Fel: du har inte r�tt att ta bort text " + textNo);
		    break;
		default:
		    consoleWriteLn("%Fel: felkod " + ex1.getError());
		    break;
		}
	    }
	}
	if (cmd.equals("f")) { // fotnotera
	    String opt = null;
	    if (st.hasMoreTokens()) opt = st.nextToken();
	    if (opt != null) {
		try {
		    t = foo.getText(Integer.parseInt(opt));
		} catch (NumberFormatException ex) {
		    throw new CmdErrException("trasigt inl�ggsnummer: " + opt);
		}
	    }
	    if (t == null) consoleWriteLn("Hittar inget inl�gg att kommentera");
	    setStatus("Skriver en fotnot.");
	    consoleWriteLn("-- Fotnot till text " + t.getNo() + ".");
	    consoleWriteLn("-- Skriv din fotnot, avsluta med \".\" p� tom rad.");
	    Text nText = new Text(new String(t.getSubject()), "");
	    nText.addFootnoted(t.getNo());
	    nText.addRecipients(t.getRecipients());
	    nText.addCcRecipients(t.getCcRecipients());
	    nText = editText(nText);
	    if (nText == null) {
		consoleWriteLn("Editeringen avbruten.");
		return 1;
	    }
	    consoleWrite("\nSkapar fotnot... ");

	    int newText = foo.createText(nText);
	    
	    if (newText > 0) {
		consoleWriteLn("text nummer " + newText + " skapad.");
		if (!dontMarkOwnTextsAsRead) {
		    markAsRead(newText);
		}
	    }
	    return 1;
	}
	if (cmd.equals("k")) { // kommentera (k [txtno])
	    String opt = null;
	    if (st.hasMoreTokens()) opt = st.nextToken();
	    if (opt != null) {
		try {
		    t = foo.getText(Integer.parseInt(opt));
		} catch (NumberFormatException ex) {
		    throw new CmdErrException("trasigt inl�ggsnummer: " + opt);
		}
	    }
	    if (t == null) consoleWriteLn("Hittar inget inl�gg att kommentera");
	    setStatus("Skriver en kommentar");
	    consoleWriteLn("-- Kommentar till text " + t.getNo() + ".");
	    consoleWriteLn("-- Skriv din kommentar, avsluta med \".\" p� tom rad.");
	    Text nText = new Text(new String(t.getSubject()), "");
	    nText.addCommented(t.getNo());
	    nText.addRecipients(t.getRecipients());
	    nText = editText(nText);
	    if (nText == null) {
		consoleWrite("Avbruten.");
		return 1;
	    }
	    consoleWrite("\nSkapar kommentar... ");


	    int newText = 0;
	    try {
		newText = foo.createText(nText);
	    } catch (RpcFailure ex1) {
		consoleWrite("%Fel: kunde inte skapa fotnot: ");
		switch (ex1.getError()) {
		case Rpc.E_not_author:
		    consoleWriteLn("du �r inte f�rfattare till texten");
		    break;
		default:
		    consoleWriteLn(ex1.getMessage());
		}
		return 1;
	    }
	    if (newText > 0) {
		consoleWriteLn("text nummer " + newText + " skapad.");
		if (!dontMarkOwnTextsAsRead) {
		    markAsRead(newText);
		    t = foo.getText(newText);
		}
	    }
	    return 1;
	}
	if (cmd.equals("lmt")) {
	    consoleWriteLn("Markerade inl�gg:");
	    Mark[] marks = foo.getMarks();
	    for (int i=0; i < marks.length; i++) {
		consoleWriteLn("Markering " + (i+1) + ": " + marks[i].getText() + " (" + marks[i].getType() + ")");
	    }
	    consoleWriteLn("");
	    return 1;
	}
	if (cmd.equals("mt")) {
	    try {
		int textNo = (t != null ? t.getNo() : 0);
		if (st.hasMoreTokens()) {
		    textNo = Integer.parseInt(st.nextToken());
		} else if (textNo == 0) {
		    textNo = Integer.parseInt(crtReadLine("Ange textnummer att markera: "));
		}
		
		int markType = 0;
		if (st.hasMoreTokens()) {
		    markType = Integer.parseInt(st.nextToken());
		} else {
		    markType = 100;
		    markType = Integer.parseInt(crtReadLine("Ange markeringstyp (0-255): "));
		}

		foo.markText(textNo, markType);
		consoleWriteLn("OK, text " + textNo + " �r markerad");
	    } catch (NumberFormatException ex1) {
		consoleWriteLn("Ogiltigt v�rde: " + ex1.getMessage());
	    } catch (RpcFailure ex1) {
		throw new CmdErrException(ex1.getMessage());
	    }
	    return 1;
	}
	if (cmd.equals("v")) {
	    DynamicSessionInfo[] vilka = foo.whoIsOnDynamic(true, false, 30*60);
	    consoleWriteLn("Listar " + vilka.length + " aktiva anv�ndare:");
	    consoleWriteLn("----------------------------------------------------------------");
	    for (int i=0; i < vilka.length; i++) {
		consoleWrite(vilka[i].getSession() + " " + confNoToName(vilka[i].getPerson()));
		if (vilka[i].getWorkingConference() != 0) {
		    consoleWrite(" i m�te " + confNoToName(vilka[i].getWorkingConference()));
		}
		consoleWriteLn("\n\t(" + vilka[i].getWhatAmIDoingString() + ")");
	    }
	    consoleWriteLn("----------------------------------------------------------------");
	    return 1;

	}
	if (cmd.equals("p")) { // skriv presentation
	    consoleWriteLn("-- Den h�r funktionen �r inte implementerad �nnu.");
	    return 1;
	}
	if (cmd.equals("i")) { // skriv inl�gg i nuvarande m�te
	    if (foo.getCurrentConference() == -1) {
		throw new CmdErrException("Du m�ste vara i ett m�te f�r att kunna skriva ett inl�gg.");
	    }
	    setStatus("Skriver ett inl�gg");
	    StringBuffer textb = new StringBuffer();
	    consoleWriteLn("-- Inl�gg i m�te " + confNoToName(foo.getCurrentConference()));
	    consoleWriteLn("Avsluta med \".\" p� tom rad.");
	    Text nText = new Text();
	    nText.addRecipient(foo.getCurrentConference());
	    nText = editText(nText);

	    int newText = foo.createText(nText);
	    if (newText > 0) {
		consoleWriteLn("text nummer " + newText + " skapad.");
		markAsRead(newText);
	    } else consoleWriteLn("misslyckades att skapa inl�gg.");
	    return 1;
	}		 
	if (cmd.equals("�p")) {
	    int confNo = foo.getMyPerson().getNo();
	    if (st.hasMoreTokens()) {
		confNo = parseNameArgs(st.nextToken(), true, true);
	    }
	    if (confNo < 1) return 1;

	    Conference myConf = foo.getConfStat(confNo);
	    int myPresentationNo = myConf.getPresentation();
	    Text myPresentation = null;
	    if (myPresentationNo > 0) {
		Text oldPresentation = foo.getText(myPresentationNo);
		myPresentation = (Text) oldPresentation.clone();
	    } else {
		myPresentation = new Text(confNoToName(confNo),
					  "");
		myPresentation.addRecipient(2); // pres memb
	    }
	    myPresentation = editText(myPresentation);
	    if (myPresentation == null) {
		throw new CmdErrException("Editeringen avbruten");
	    }
	    int newText = foo.createText(myPresentation);
	    if (newText > 0) {
		consoleWriteLn("text nummer " + newText + " skapad.");
		markAsRead(newText);
		try {
		    foo.setPresentation(foo.getMyPerson().getNo(), newText);
		    consoleWriteLn("OK, text " + newText + " �r ny presentation f�r " + 
				   confNoToName(confNo));
		} catch (RpcFailure ex1) {
		    consoleWriteLn("%Fel: kunde inte s�tta presentation f�r " + 
				   confNoToName(confNo) + ": " + ex1.getMessage());
		}
	    }
	    return 1;
	}
	if (cmd.equals("�k")) { // �terse (det) kommenterade
	    int[] commented = t.getCommented();
	    if (commented.length == 0) {
		commented = t.getFootnoted();
		if (commented.length == 0)
		    throw(new CmdErrException("Texten �r ingen kommentar"));
	    }
	    consoleWriteLn("�terse det kommenterade (text " + commented[0] + ")");
	    lastText = foo.getText(commented[0]);
	    displayText(commented[0]);
	    return 1;
	}
	if (cmd.equals("�u")) { // �terse urinl�gg
	    // * does not treat footnotes as comments
	    // * does not handle multiple original texts (only looks at forst comm-to)
	    consoleWrite("S�ker efter urinl�gg f�r text " + t.getNo() + "... ");
	    TextStat ts = foo.getTextStat(t.getNo());
	    while (ts.getStatInts(TextStat.miscCommTo).length > 0) {
		ts = foo.getTextStat(ts.getStatInts(TextStat.miscCommTo)[0]);
	    }
	    consoleWriteLn("hittade text " + ts.getNo());
	    displayText(lastText = t = foo.getText(ts.getNo()));
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
	if (cmd.equals("bn")) { // byt namn
	    int confNo = 0;
	    try {
		confNo = parseNameArgs(st.nextToken("").substring(1), true, true);
	    } catch (NoSuchElementException ex1) {
		confNo = foo.getMyPerson().getNo();
	    }
	    if (confNo < 1) return 1;
	    consoleWriteLn("Byta namn p� " + confNoToName(confNo));
	    try {
		foo.changeName(confNo, crtReadLine("Nytt namn> "));
	    } catch (RpcFailure ex1) {
		consoleWrite("Det gick inte att byta namn: ");
		switch (ex1.getError()) {
		case Rpc.E_permission_denied:
		    consoleWriteLn("du saknar beh�righet f�r operationen");
		    break;
		case Rpc.E_conference_exists:
		    consoleWriteLn("det finns redan ett m�te med detta namn");
		    break;
		case Rpc.E_string_too_long:
		    consoleWriteLn("det nya namnet �r f�r l�ngt");
		    break;
		case Rpc.E_bad_name:
		    consoleWriteLn("det nya namnet inneh�ller ogiltiga tecken");
		    break;
		default:
		    consoleWriteLn("felkod " + ex1.getError());
		    break;
		}
	    }
	    return 1;

	}
	if (cmd.equals("s")) { // s�nda meddelande
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
	    try {
		foo.sendMessage(confNo, message);
		if (confNo == 0) consoleWriteLn("Ditt alarmmeddelande har skickats.");
		else consoleWriteLn("Ditt meddelande har skickats till " + confNoToName(confNo));
	    } catch (RpcFailure ex1) {
		consoleWriteLn("Det gick inte att skicka meddelandet. Felkod: " + ex1.getError());
	    }
	    return 1;
	}

	if (cmd.equals("g")) { // g� (till m�te) (g <m�tesnamn>)
	    int confNo = 0;
	    try {
		confNo = parseNameArgs(st.nextToken("").substring(1), true, true);
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("Du m�ste ange ett m�te att g� till.");
	    }
	    if (confNo == 0) return 1;

	    consoleWriteLn("-- g� till m�te: " + confNoToName(confNo));
	    try {
		foo.changeConference(confNo);
		foo.updateUnreads();
		return 1;
	    } catch (RpcFailure failed) {
		consoleWrite("-- m�tesbytet misslyckades: ");
		switch (failed.getError()) {
		case Rpc.E_not_member:
		    consoleWriteLn("du �r inte med i det m�tet");
		    String wantsChange = crtReadLine("Vill du g� med i m�tet? (j/N) ");
		    if (wantsChange.equals("j")) {
			try {
			    foo.joinConference(confNo);
			    doCommand(t, "g m " + confNo);
			} catch (RpcFailure failed2) {
			    consoleWrite("Det gick inte att g� med i m�tet: ");
			    switch (failed2.getError()) {
			    case Rpc.E_access_denied:
				consoleWriteLn("du fick inte");
				break;
			    case Rpc.E_permission_denied:
				consoleWriteLn("du f�r inte �ndra p� detta medlemskap");
				break;
			    default:
				consoleWriteLn("ok�nd anledning " + failed2.getError());
			    }
			}
		    }
		    break;
		default:
		    consoleWriteLn("ok�nd anledning " + failed.getError());
		}
	    }

	    return 1;
	}
	if (cmd.equals("nm")) { // n�sta m�te
	    int nextConf = foo.nextUnreadConference(true);
	    if (nextConf != -1)
		consoleWriteLn("n�sta m�te - " + confNoToName(nextConf));
	    else
		consoleWriteLn("Det finns inga fler ol�sta m�ten.");
	    return 1;
	}
	if (cmd.equals("sm")) { // skapa m�te
	    String name = null;
	    try {
		name = st.nextToken("").substring(1);
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("Du m�ste ange ett namn p� m�tet.");
	    }
	    consoleWrite("F�rs�ker skapa m�te \"" + name + "\"...");
	    int confNo = foo.createConf(name, false, false, false);
	    if (confNo > 0) consoleWriteLn(" lyckades: m�tet fick nummer " + confNo);
	    else consoleWriteLn(" det gick inte.");
	    return 1;
	}
	if (cmd.equals("lm")) { // lista m�ten
	    String substring = null;
	    try {
		substring = st.nextToken("").substring(1);
	    } catch (NoSuchElementException ex1) {
		substring = "";
	    }
	    ConfInfo[] confs = foo.lookupName(substring, false, true);
	    consoleWriteLn("Hittade " + confs.length + " m�ten");
	    MessageFormat form = new MessageFormat(" {0,number}\t{2} {1}");

	    for (int i=0; i < confs.length; i++) {
		boolean memberOf = foo.isMemberOf(confs[i].getNo());
		consoleWriteLn(form.format(new Object[] {new Integer(confs[i].getNo()),
							 confs[i].getNameString(),
							 (memberOf ? " " : "*")
					
		}));
	    }
	    consoleWriteLn("-- Slut p� listningen");
	    return 1;

	}
	if (cmd.equals("q") || cmd.equals(".")) return -1; // avsluta
	throw(new CmdErrException("F�rstod inte \"" + cmd + "\""));
		      
    }
    
    static void consoleWriteLn(String s) {
	consoleWrite(s + lineSeparator);
    }

    static void consoleWrite(String s) {
	try {
	    ByteArrayOutputStream consByteStream = new ByteArrayOutputStream();
	    byte[] sBytes = s.getBytes(encoding);
	    consByteStream.write(sBytes, 0, sBytes.length);
	    consByteStream.writeTo(System.out);
	    if (useGui) stdoutScroll.getVerticalScrollBar().setValue(stdoutScroll.getVerticalScrollBar().getMaximum());
	} catch (UnsupportedEncodingException ex1) {
	    throw new RuntimeException("Unsupported console encoding: " + ex1.getMessage());
	} catch (IOException ex2) {
	    throw new RuntimeException("I/O exception while writing to stdout: " + ex2.getMessage());
	}
    }

    /**
     * Reads one line of input from STDIN.
     * Returns null if EOF is encountered (eg, user presses ^D on an empty line).
     * Otherwise, returns a String with the user input, without trailing '\n'.
     */
    String crtReadLine(String prompt)
    throws IOException {
	lastPromptShown = prompt;
	if (prompt != null) {
	    consoleWrite(prompt);
	}

	if (!useGui) {
	    int b = System.in.read();
	    ByteArrayOutputStream inByteStream = new ByteArrayOutputStream();
	    while (b != -1 && b != '\n') {
		inByteStream.write(b);
		b = System.in.read();
	    }
	    if (b == -1) return null;
	    try {	    
		return inByteStream.toString(encoding).trim();
	    } catch (UnsupportedEncodingException ex1) {
		throw new RuntimeException("Unsupported console encoding: " + ex1.getMessage());
	    }
	} else {
	    synchronized (guiInput) {
		try {
		    guiInput.wait();
		} catch (InterruptedException ex1) {
		    System.err.println("Interrupted while waiting for user input");
		}
		return guiInput.getString();
	    }
	}
    }

    Text editText(Text t)
    throws IOException {
	String subject = new String(t.getSubject());
	Debug.println("pre subj: \"" + subject + "\"");
	if (subject.length() == 0) {
	    subject = crtReadLine("�rende: ");
	} else {
	    consoleWriteLn("�rende: " + subject);
	}
	int[] rcpts = t.getRecipients();
	for (int i=0; i < rcpts.length; i++) {
	    consoleWriteLn("Mottagare: " + confNoToName(rcpts[i]));
	}
	int[] ccRcpts = t.getCcRecipients();
	for (int i=0; i < ccRcpts.length; i++) {
	    consoleWriteLn("Extra kopiemottagare: " + confNoToName(ccRcpts[i]));
	}
	consoleWriteLn("--- skriv \"!?\" p� tom rad f�r hj�lp ----------------------------------");
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
			consoleWriteLn("** Subtraherar " + confNoToName(newConf) + " fr�n kopiemottagarlista");
			t.removeCcRecipient(newConf);
		    }
		} else if (icmd.startsWith("!sm")) {
		    String confs = rst.nextToken("").substring(1);
		    int newConf = parseNameArgs(confs, true, true);
		    if (newConf > 0) {
			consoleWriteLn("** Subtraherar " + confNoToName(newConf) + " fr�n mottagarlista");
			t.removeRecipient(newConf); 
		    } 
		} else if (icmd.startsWith("!ak")) {
		    String confs = rst.nextToken("").substring(1);
		    int newConf = parseNameArgs(confs, true, true);
		    if (newConf > 0) {
			consoleWriteLn("** Adderar " + confNoToName(newConf) + " som kopiemottagare");
			t.addCcRecipient(newConf);
		    }
		} else if (icmd.startsWith("!�r")) {
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
			consoleWriteLn("** Du m�ste ange ett radnummer.");
		    }
		    consoleWriteLn("");
		} else if (icmd.startsWith("!��")) {
		    consoleWriteLn("** �ndra �rende:");
		    subject = crtReadLine("�rende: ");
		} else if (icmd.startsWith("!v")) {
		    consoleWriteLn("** Hela texten:");
		    for (int i=0; i < rows.size(); i++) {
			consoleWriteLn((i+1) + ": " + rows.get(i));
		    }
		    consoleWriteLn("");
		} else if (icmd.startsWith("!q")) {
		    return null;
		} else if (icmd.startsWith("!?")) {
		    consoleWriteLn("** Kommandon i editorl�ge:");
		    consoleWriteLn("  !f <m�te>     Flytta texten till <m�te>");
		    consoleWriteLn("  !am <m�te>    Addera <m�te> som mottagare");
		    consoleWriteLn("  !ak <m�te>    Addera <m�te> som kopiemottagare");
		    consoleWriteLn("  !rr <radnr.>  Radera <radnr.>");
		    consoleWriteLn("  !sm <m�te>    Subtrahera <m�te> fr�n mottagarlista");
		    consoleWriteLn("  !sk <m�te>    Subtrahera <m�te> fr�n kopiemottagarlista");
		    consoleWriteLn("  !v            Visa hela texten");
		    consoleWriteLn("  !q            Avsluta utan att spara");
		    consoleWriteLn("  !�r <radnr.>  Skriv om rad <radnr.>");
		    consoleWriteLn("  !��           �ndra �rende");
		    consoleWriteLn("  .             Spara och avsluta");
		} else {
		    consoleWriteLn("** Ok�nt editorkommando");
		}
	    } else {
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
	
	t.setContents((subject + "\n" + textb.toString()).getBytes());
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

    /**
     * Marks the text as read in all recipient conferences which the
     * user is a member of.
     *
     */
    void markAsRead(int textNo) throws IOException {
	Text text = foo.getText(textNo);

	List miscInfo = text.getStat().getMiscInfo();
	List recipients = new LinkedList();
	Iterator si = miscInfo.iterator();
	while (si.hasNext()) {
	    Selection selection = (Selection) si.next();
	    int[] tags = { TextStat.miscRecpt, TextStat.miscCcRecpt };
	    for (int j=0; j < tags.length; j++) {
		if (selection.contains(tags[j])) {
		    Enumeration e = selection.get(tags[j]);
		    while (e.hasMoreElements()) recipients.add(e.nextElement());
		}
	    }
	}

	for (int i=0; i < recipients.size(); i++) {
	    int rcpt = ((Integer) recipients.get(i)).intValue();
	    Debug.println("about to mark " + text.getNo() + " as read for recipient #" + i + ": " + rcpt);
	    if (foo.isMemberOf(rcpt)) {
		Debug.println("mark-as-read: " + text.getNo() + ", local " +
			      text.getLocal(rcpt));
		
		foo.markAsRead(rcpt, new int[] { text.getLocal(rcpt) });
	    } else {
		Debug.println("not member of " + rcpt);
	    }
	}
	// add the text to the ReadTextsMap
	foo.getReadTexts().add(text.getNo());
    }

    void displayText(Text text, boolean markAsRead) throws IOException {
	displayText(text);
	if (markAsRead && text != null) {
	    markAsRead(text.getNo());

	    // retreive all comments of the texts, push them onto
	    // the 'toread' stack for later viewing (in reverse)
	    int[] comments = text.getComments();
	    for (int i=comments.length-1; i >= 0; i--) {
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
	consoleWriteLn(text.getNo()+" " + timestampFormat.format(text.getCreationTime()) + " /" + text.getRows() + " " +
			   (text.getRows() > 1 ? "rader" : "rad") + "/ " +
			   (text.getAuthor() > 0 ? new String(confNoToName(text.getAuthor())) : "anonym person"));

	int[] rcpts = text.getStatInts(TextStat.miscRecpt);
	int[] ccRcpts = text.getCcRecipients();
	int[] sentBy = text.getStatInts(TextStat.miscSentBy);
	int[] footnotes = text.getFootnotes();

	int[] locals = text.getStatInts(TextStat.miscLocNo);
	int[] commented = text.getCommented();
	int[] footnoted = text.getFootnoted();
	Enumeration sentAt = null;
	/*
	try {
	    sentAt = text.getStat().getMiscInfo().get(TextStat.miscSentAt);
	    while (sentAt.hasMoreElements()) {
		Debug.println("sent-at: " + sentAt.nextElement().toString());
	    }
	} catch (NoSuchKeyException ex1) {}
	*/
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



	consoleWriteLn("�rende: " + new String(text.getSubject()));
	consoleWriteLn("------------------------------------------------------------");
	consoleWriteLn(new String(text.getBody()));
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
				   new String(auxs[i].getDataString()));
	}
	for (int i=0; i < footnotes.length; i++) {
	    TextStat ts = foo.getTextStat(footnotes[i]);
	    consoleWrite("Det finns en fotnot i text " + footnotes[i]);
	    if (ts != null) {
		consoleWrite(" av " + confNoToName(ts.getAuthor()) + ", l�sa? (J/n) ");
		if (crtReadLine("").equals("n")) continue;
		displayText(foo.getText(footnotes[i]), true);
	    } else {
		consoleWriteLn("som du inte f�r l�sa.");
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
	Debug.println("consoleAction(" + event + ")");
	if (event.getType() == ConsoleEvent.COMMAND_ENTERED) {
	    synchronized (guiInput) {
	        guiInput.setString((String) event.getCommunique());
		guiInput.notify();
	    }
	}
    }

	
    public static void main(String[] argv) {
	Test2 t2 = new Test2();

	consoleWriteLn("IJKLKOM (c) 1999 Rasmus Sten");
	t2.run(argv);
    }

    static JScrollPane stdoutScroll;
    public Test2() {
	if (useGui) {
	    guiInput = new GuiInput();

	    System.out.println("Enabling console GUI");
	    JFrame stdout = createConsoleFrame();
	    Console stdoutConsole = new Console(80, 25);
	    stdoutScroll = new JScrollPane(stdoutConsole,
					   JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    stdout.getContentPane().add(new JScrollPane(stdoutConsole));
	    stdout.repaint();
	    stdout.setTitle("LatteKOM console");

	    System.setOut(new PrintStream(new LogOutputStream(stdoutConsole)));

	    stdoutConsole.addConsoleListener(this);
	    if (Debug.ENABLED) {
		Console stdErrConsole = new Console(80,25);
		JFrame stderr = createConsoleFrame();
		stderr.getContentPane().add(new JScrollPane(stdErrConsole));
		stderr.setVisible(true);
		stderr.setTitle("LatteKOM stderr console");
		stderr.repaint();
		System.setErr(new PrintStream(new LogOutputStream(stdErrConsole)));
	    } else {
		System.setErr(new PrintStream(new LogOutputStream(stdoutConsole)));
	    }
	}	
    }

    public void asynchMessage(AsynchMessage m) {
	synchronized (messages) {
	    messages.addLast(m);
	}
	if (likesAsynchMessages) {
	    new Thread(new Runnable() {
		    public void run() {
			consoleWriteLn("");
			handleMessages();
			try {
			    consoleWrite(genericPrompt());
			} catch (IOException ex1) {
			    throw new RuntimeException("I/O error: " + ex1.getMessage());
			}
		    }
		}, "AsynchMessageHandler").start();
	}
    }

}









