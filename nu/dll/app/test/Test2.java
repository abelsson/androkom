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
    LinkedList messages = new LinkedList();
    boolean likesAsynchMessages = false;
    String lastPromptShown = null;

    Object consoleLock = new Object();

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
	byte[] name = foo.getConfName(n);
	if (name == null)
	    return "Person "+n+" (N/A)";
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
		System.out.println("%Fel: kunde inte tolka m�tesnummer " + ex1.getMessage());
	    }
	}

	ConfInfo[] names = foo.lookupName(arg, wantPersons, wantConfs);
	if (names.length == 0) {
	    System.out.println("%Fel: hittar inget s�dant m�te eller s�dan person");
	    return 0;
	}
	if (names.length > 1) {
	    System.out.println("%Fel: flertydigt namn");
	    for (int i=0; i < names.length; i++)
		System.out.println("-- alternativ " + (i+1) + ": " + names[i].getNameString());
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
		case Asynch.new_name:
		    System.out.println(((Hollerith) params[1]).getContentString() + " har bytt namn till " + 
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
		    System.out.print("\007\007"); // beep!
		    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		    if (params[0].intValue() == foo.getMyPerson().getNo()) {
			System.out.println("Personling meddelande fr�n " + sender + 
					   " (" + timestampFormat.format(m.getArrivalTime()) + "):");
		    } else if (params[0].intValue() == 0) {
			System.out.println("Alarmmeddelande fr�n " + sender + " (" + timestampFormat.format(m.getArrivalTime()) + "):");
		    } else {
			System.out.println("Meddelande till " + recipient);
			System.out.println("fr�n " + sender +
					   " (" + timestampFormat.format(m.getArrivalTime()) + "):");
		    }
		    System.out.println("\n" + ((Hollerith) params[2]).getContentString());
		    System.out.println("----------------------------------------------------------------");
		    break;
		case Asynch.login:
		    try {
			System.out.println(confNoToName(params[0].intValue()) + " loggade in i LysKOM (" +
 					   timestampFormat.format(m.getArrivalTime()) + ")");

		    } catch (IOException ex) {
			System.err.println("Det gick inte att utf�ra get-conf-name: " + ex.getMessage());			
		    }
		    break;
		case Asynch.logout:
		    try {
			System.out.println(confNoToName(params[0].intValue()) + " loggade ut ur LysKOM (" +
 					   timestampFormat.format(m.getArrivalTime()) + ")");

		    } catch (IOException ex) {
			System.err.println("Det gick inte att utf�ra get-conf-name: " + ex.getMessage());			
		    }
		    break;
		case Asynch.sync_db:
		    serverSynch = !serverSynch;
		    if (serverSynch) {
			System.out.println("** Servern synkroniserar just nu databasen.");
		    } else {
			System.out.println("** Servern �r klar med synkroniseringen.");
		    }
		    break;
		case Asynch.new_text_old:
		    
		    break;
		    //case Asynch.new_text:
		    //break;
		default: 
		    System.out.println("Asynkront meddelande av typen " + m.getNumber());
		}
	    }
	}
    }

    public Test2(String[] argv) {
	try {
	    String server = System.getProperty("lyskom.server") == null ? "sno.pp.se" :
		System.getProperty("lyskom.server");

	    foo = new Session();
	    foo.connect(server, 4894);
	    System.out.println("Ansluten till " + server);
	    foo.addAsynchMessageReceiver(this);	    
	    
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
		if (names.length == 0) {
		    System.err.println("Namnet finns inte.");
		    System.exit(-42);
		}
	    }

	    System.out.println("Loggar in som " +
			       new String(names[0].confName) + " (" +
			       names[0].confNo + ")");
	    if (!foo.login(names[0].confNo, argv.length > 1 ? argv[1] : crtReadLine("l�senord> "), false)) {
		System.err.println("Inloggningen misslyckades!");
		System.exit(-32);
	    }
	    System.out.println("Inloggad. V�lkommen till LysKOM!");	    
	    //4303588, 100035, 4257987, 4244657
	    foo.changeWhatIAmDoing("Petar sig i n�san");
	    int me = foo.getMyPerson().getNo();
	    boolean go = true;
	    Stack toread = new Stack(); // used to store unread comments
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
			    System.err.println("%Fel: " + ex.getMessage());
			    rc = 1;
			}
		    }
		}
		if (rc == 0) { // default-action (read next text)
		    int textNo = foo.nextUnreadText(false);
		    if (textNo == -1) { // if no unread text in current conference,
			int nextConf = foo.nextUnreadConference(false); // change conference
			if (nextConf == -1) System.out.println("Det finns inga fler ol�sta m�ten.");
			else {
			    System.out.println("G�r till m�te: " + confNoToName(nextConf));
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
	String curConf = conf == -1 ? "Ej n�rvarande i n�got m�te" : confNoToName(conf);
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
	    System.out.println("-- kommandon\n" +
			       "\t� <inl�ggsnummer> -- �terse text\n" +
			       "\tk [inl�ggsnummer] -- kommentera text\n" +
			       "\tg <m�tesnamn>     -- g� till m�te\n" +
			       "\ti                 -- skriv inl�gg\n" +
			       "\t�k                -- �terse (f�rsta) kommentaren\n" +
			       "\t�u                -- �terse (f�rsta) urinl�gget\n" +
			       "\tsm <m�tesnamn>    -- skapa (publikt) m�te\n" +
			       "\tlm [substr�ng]    -- lista m�ten\n" +
			       "\ts [m�tesnamn]     -- skicka meddelande\n" + 
			       "\tbn <m�tesnamn>    -- byt namn\n" + 
			       "\tp [m�tesnamn]     -- skriv presentation (*)\n" + 
			       "\trt <text>         -- radera text\n" +
			       "\trm <m�tesnamn>    -- radera m�te\n" +
			       "\tv                 -- lista inloggade anv�ndare\n" + 
			       "\tq                 -- avsluta TestKOM\n" +
			       "\trpc <#> [data]    -- skicka RPC-kommando (svaret ignoreras)\n" + 
			       "\n  M�tesnamn kan f�r det mesta bytas ut mot \"m <nummber>\"\n");
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
	    System.out.println("Fick svar: " + reply);
	    KomToken[] reqparams = reply.getParameters();
	    for (int i=0; i < reqparams.length; i++) {
		System.out.println("\t--> Parameter " + i + ": " +
				   reqparams[i]);
	    }
	    */
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
	if (cmd.equals("rm")) { // radera m�te
	    System.out.println("-- Den h�r funktionen �r inte implementerad �nnu.");
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
		    System.out.println("%Fel: det finns ingen text med nummer " + textNo);
		    break;
		case Rpc.E_not_author:
		    System.out.println("%Fel: du har inte r�tt att ta bort text " + textNo);
		    break;
		default:
		    System.out.println("%Fel: felkod " + ex1.getError());
		    break;
		}
	    }
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
	    StringBuffer textb = new StringBuffer();
	    System.out.println("-- Kommentar till text " + t.getNo() + ".");
	    System.out.println("-- Skriv din kommentar, avsluta med \".\" p� tom rad.");
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
	if (cmd.equals("v")) {
	    DynamicSessionInfo[] vilka = foo.whoIsOnDynamic(true, false, 30*60);
	    System.out.println("Listar " + vilka.length + " aktiva anv�ndare:");
	    System.out.println("----------------------------------------------------------------");
	    for (int i=0; i < vilka.length; i++) {
		System.out.print(vilka[i].getSession() + " " + confNoToName(vilka[i].getPerson()));
		if (vilka[i].getWorkingConference() != 0) {
		    System.out.print(" i m�te " + confNoToName(vilka[i].getWorkingConference()));
		}
		System.out.println("\n\t(" + vilka[i].getWhatAmIDoingString() + ")");
	    }
	    System.out.println("----------------------------------------------------------------");
	    return 1;

	}
	if (cmd.equals("p")) { // skriv presentation
	    System.out.println("-- Den h�r funktionen �r inte implementerad �nnu.");
	    return 1;
	}
	if (cmd.equals("i")) { // skriv inl�gg i nuvarande m�te
	    StringBuffer textb = new StringBuffer();
	    System.out.println("-- Inl�gg i m�te " + confNoToName(foo.getCurrentConference()));
	    System.out.println("Avsluta med \".\" p� tom rad.");
	    String subject = crtReadLine("�mne> ");
	    if (subject == null) return 1;
	    String row = crtReadLine("> ");
	    while (row != null && !row.equals(".")) {
		textb.append(row + "\n");
		row = crtReadLine("> ");
	    }
	    System.out.print("\nSkapar inl�gg... ");
	    int newText = foo.createText(new Text(subject, textb.toString()).addRecipient(foo.getCurrentConference()));
	    if (newText > 0) System.out.println("text nummer " + newText + " skapad.");
	    else System.out.println("misslyckades att skapa inl�gg.");
	    return 1;
	}		 
	if (cmd.equals("�k")) { // �terse (det) kommenterade
	    int[] commented = t.getCommented();
	    if (commented.length == 0)
		throw(new CmdErrException("Texten �r ingen kommentar"));
	    System.out.println("�terse det kommenterade (text " + commented[0] + ")");
	    lastText = foo.getText(commented[0]);
	    displayText(commented[0]);
	    return 1;
	}
	if (cmd.equals("�u")) { // �terse urinl�gg
	    // footnotes breaks the chain
	    System.out.print("S�ker efter urinl�gg f�r text " + t.getNo() + "... ");
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
	if (cmd.equals("bn")) { // byt namn
	    int confNo = 0;
	    try {
		confNo = parseNameArgs(st.nextToken("").substring(1), true, true);
	    } catch (NoSuchElementException ex1) {
		confNo = foo.getMyPerson().getNo();
	    }
	    if (confNo == 0) return 1;
	    System.out.println("Byta namn p� " + confNoToName(confNo));
	    try {
		foo.changeName(confNo, crtReadLine("Nytt namn> "));
	    } catch (RpcFailure ex1) {
		System.out.print("Det gick inte att byta namn: ");
		switch (ex1.getError()) {
		case Rpc.E_permission_denied:
		    System.out.println("du saknar beh�righet f�r operationen");
		    break;
		case Rpc.E_conference_exists:
		    System.out.println("det finns redan ett m�te med detta namn");
		    break;
		case Rpc.E_string_too_long:
		    System.out.println("det nya namnet �r f�r l�ngt");
		    break;
		case Rpc.E_bad_name:
		    System.out.println("det nya namnet inneh�ller ogiltiga tecken");
		    break;
		default:
		    System.out.println("felkod " + ex1.getError());
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
	    if (confNo != 0) {
		System.out.println("Skicka meddelande till " + confNoToName(confNo));
	    }
	    String message = crtReadLine("Text att skicka> ");
	    try {
		foo.sendMessage(confNo, message);
		if (confNo == 0) System.out.println("Ditt alarmmeddelande har skickats.");
		else System.out.println("Ditt meddelande har skickats till " + confNoToName(confNo));
	    } catch (RpcFailure ex1) {
		System.out.println("Det gick inte att skicka meddelandet. Felkod: " + ex1.getError());
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

	    System.out.println("-- g� till m�te: " + confNoToName(confNo));
	    try {
		foo.changeConference(confNo);
		foo.updateUnreads();
		return 1;
	    } catch (RpcFailure failed) {
		System.out.print("-- m�tesbytet misslyckades: ");
		switch (failed.getError()) {
		case Rpc.E_not_member:
		    System.out.println("du �r inte med i det m�tet");
		    String wantsChange = crtReadLine("Vill du g� med i m�tet? (j/N) ");
		    if (wantsChange.equals("j")) {
			try {
			    foo.joinConference(confNo);
			    doCommand(t, "g m " + confNo);
			} catch (RpcFailure failed2) {
			    System.out.print("Det gick inte att g� med i m�tet: ");
			    switch (failed2.getError()) {
			    case Rpc.E_access_denied:
				System.out.println("du fick inte");
				break;
			    case Rpc.E_permission_denied:
				System.out.println("du f�r inte �ndra p� detta medlemskap");
				break;
			    default:
				System.out.println("ok�nd anledning " + failed2.getError());
			    }
			}
		    }
		    break;
		default:
		    System.out.println("ok�nd anledning " + failed.getError());
		}
	    }

	    return 1;
	}
	if (cmd.equals("nm")) { // n�sta m�te
	    int nextConf = foo.nextUnreadConference(true);
	    if (nextConf != -1)
		System.out.println("n�sta m�te - " + confNoToName(nextConf));
	    else
		System.out.println("Det finns inga fler ol�sta m�ten.");
	    return 1;
	}
	if (cmd.equals("sm")) { // skapa m�te
	    String name = null;
	    try {
		name = st.nextToken("").substring(1);
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("Du m�ste ange ett namn p� m�tet.");
	    }
	    System.out.print("F�rs�ker skapa m�te \"" + name + "\"...");
	    int confNo = foo.createConf(name, false, false, false);
	    if (confNo > 0) System.out.println(" lyckades: m�tet fick nummer " + confNo);
	    else System.out.println(" det gick inte.");
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
	    System.out.println("Hittade " + confs.length + " m�ten");
	    MessageFormat form = new MessageFormat(" {0,number}\t-- {1}");
	    for (int i=0; i < confs.length; i++) {
		System.out.println(form.format(new Object[] {new Integer(confs[i].getNo()),
							     confs[i].getNameString()}));
	    }
	    System.out.println("-- Slut p� listningen");
	    return 1;

	}
	if (cmd.equals("q") || cmd.equals(".")) return -1; // avsluta
	throw(new CmdErrException("F�rstod inte \"" + cmd + "\""));
		      
    }

    /**
     * Reads one line of input from STDIN.
     * Returns null if EOF is encountered (eg, user presses ^D on an empty line).
     * Otherwise, returns a String with the user input, without trailing '\n'.
     */
    String crtReadLine(String prompt)
    throws IOException {
	lastPromptShown = prompt;
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

    void saveText(Text text) throws IOException {
        ObjectOutputStream objStream =
            new ObjectOutputStream(new FileOutputStream(new File("texts" +
                              File.separator + text.getNo() + ".txt")));
        objStream.writeObject(text);
        objStream.close();
    }

    /**
     * Displays a text in elisp-client-style.
     */
    void displayText(Text text) throws IOException {
        if (false) {
            saveText(text);   
        }
	System.out.println(text.getNo()+" " + timestampFormat.format(text.getCreationTime()) + " /" + text.getRows() + " " +
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

	System.out.println("�rende: " + new String(text.getSubject()));
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
	    //if (text.getStat().countAuxItems() > 0) System.out.println("\t** Aux-saker:");
	    for (int i=0; i<text.getStat().countAuxItems(); i++)
		System.out.println("\tAux-Item["+i+"]: typnummer: "+auxs[i].getNo() + ", data: " +
				   new String(auxs[i].getDataString()));
	}
    }
	
	
    public static void main(String[] argv) {
	System.out.println("IJKLKOM (c) 1999 Rasmus Sten");
	new Test2(argv);
    }

    public void asynchMessage(AsynchMessage m) {
	synchronized (messages) {
	    messages.addLast(m);
	}
	if (likesAsynchMessages) {
	    new Thread(new Runnable() {
		    public void run() {
			System.out.println("");
			handleMessages();
			System.out.print(lastPromptShown);
		    }
		}, "AsynchMessageHandler").start();
	}
    }

}









