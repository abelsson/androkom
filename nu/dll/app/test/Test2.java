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
		System.out.println("%Fel: kunde inte tolka mötesnummer " + ex1.getMessage());
	    }
	}

	ConfInfo[] names = foo.lookupName(arg, wantPersons, wantConfs);
	if (names.length == 0) {
	    System.out.println("%Fel: hittar inget sådant möte eller sådan person");
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
			System.err.println("Det gick inte att utföra get-conf-name: " + ex.getMessage());
		    }
		    System.out.print("\007\007"); // beep!
		    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		    if (params[0].intValue() == foo.getMyPerson().getNo()) {
			System.out.println("Personling meddelande från " + sender + 
					   " (" + timestampFormat.format(m.getArrivalTime()) + "):");
		    } else if (params[0].intValue() == 0) {
			System.out.println("Alarmmeddelande från " + sender + " (" + timestampFormat.format(m.getArrivalTime()) + "):");
		    } else {
			System.out.println("Meddelande till " + recipient);
			System.out.println("från " + sender +
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
			System.err.println("Det gick inte att utföra get-conf-name: " + ex.getMessage());			
		    }
		    break;
		case Asynch.logout:
		    try {
			System.out.println(confNoToName(params[0].intValue()) + " loggade ut ur LysKOM (" +
 					   timestampFormat.format(m.getArrivalTime()) + ")");

		    } catch (IOException ex) {
			System.err.println("Det gick inte att utföra get-conf-name: " + ex.getMessage());			
		    }
		    break;
		case Asynch.sync_db:
		    serverSynch = !serverSynch;
		    if (serverSynch) {
			System.out.println("** Servern synkroniserar just nu databasen.");
		    } else {
			System.out.println("** Servern är klar med synkroniseringen.");
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
	    if (!foo.login(names[0].confNo, argv.length > 1 ? argv[1] : crtReadLine("lösenord> "), false)) {
		System.err.println("Inloggningen misslyckades!");
		System.exit(-32);
	    }
	    System.out.println("Inloggad. Välkommen till LysKOM!");	    
	    //4303588, 100035, 4257987, 4244657
	    foo.changeWhatIAmDoing("Petar sig i näsan");
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
	likesAsynchMessages = false;
	StringTokenizer st = new StringTokenizer(s);
	String cmd = st.nextToken();
	if (cmd.equals("?")) {
	    System.out.println("-- kommandon\n" +
			       "\tå <inläggsnummer> -- återse text\n" +
			       "\tk [inläggsnummer] -- kommentera text\n" +
			       "\tg <mötesnamn>     -- gå till möte\n" +
			       "\ti                 -- skriv inlägg\n" +
			       "\tåk                -- återse (första) kommentaren\n" +
			       "\tåu                -- återse (första) urinlägget\n" +
			       "\tsm <mötesnamn>    -- skapa (publikt) möte\n" +
			       "\tlm [substräng]    -- lista möten\n" +
			       "\ts [mötesnamn]     -- skicka meddelande\n" + 
			       "\tbn <mötesnamn>    -- byt namn\n" + 
			       "\tp [mötesnamn]     -- skriv presentation (*)\n" + 
			       "\trt <text>         -- radera text\n" +
			       "\trm <mötesnamn>    -- radera möte\n" +
			       "\tv                 -- lista inloggade användare\n" + 
			       "\tq                 -- avsluta TestKOM\n" +
			       "\trpc <#> [data]    -- skicka RPC-kommando (svaret ignoreras)\n" + 
			       "\n  Mötesnamn kan för det mesta bytas ut mot \"m <nummber>\"\n");
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
	if (cmd.equals("rm")) { // radera möte
	    System.out.println("-- Den här funktionen är inte implementerad ännu.");
	    return 1;	    
	}
	if (cmd.equals("rt")) { // radera inlägg
	    int textNo = 0;
	    try {
		textNo = Integer.parseInt(st.nextToken());
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("du måste ange ett inläggsnummer");
	    } catch (NumberFormatException ex2) {
		throw new CmdErrException("kunde inte tolka inläggsnummer");
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
		    System.out.println("%Fel: du har inte rätt att ta bort text " + textNo);
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
	if (cmd.equals("v")) {
	    DynamicSessionInfo[] vilka = foo.whoIsOnDynamic(true, false, 30*60);
	    System.out.println("Listar " + vilka.length + " aktiva användare:");
	    System.out.println("----------------------------------------------------------------");
	    for (int i=0; i < vilka.length; i++) {
		System.out.print(vilka[i].getSession() + " " + confNoToName(vilka[i].getPerson()));
		if (vilka[i].getWorkingConference() != 0) {
		    System.out.print(" i möte " + confNoToName(vilka[i].getWorkingConference()));
		}
		System.out.println("\n\t(" + vilka[i].getWhatAmIDoingString() + ")");
	    }
	    System.out.println("----------------------------------------------------------------");
	    return 1;

	}
	if (cmd.equals("p")) { // skriv presentation
	    System.out.println("-- Den här funktionen är inte implementerad ännu.");
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
	if (cmd.equals("bn")) { // byt namn
	    int confNo = 0;
	    try {
		confNo = parseNameArgs(st.nextToken("").substring(1), true, true);
	    } catch (NoSuchElementException ex1) {
		confNo = foo.getMyPerson().getNo();
	    }
	    if (confNo == 0) return 1;
	    System.out.println("Byta namn på " + confNoToName(confNo));
	    try {
		foo.changeName(confNo, crtReadLine("Nytt namn> "));
	    } catch (RpcFailure ex1) {
		System.out.print("Det gick inte att byta namn: ");
		switch (ex1.getError()) {
		case Rpc.E_permission_denied:
		    System.out.println("du saknar behörighet för operationen");
		    break;
		case Rpc.E_conference_exists:
		    System.out.println("det finns redan ett möte med detta namn");
		    break;
		case Rpc.E_string_too_long:
		    System.out.println("det nya namnet är för långt");
		    break;
		case Rpc.E_bad_name:
		    System.out.println("det nya namnet innehåller ogiltiga tecken");
		    break;
		default:
		    System.out.println("felkod " + ex1.getError());
		    break;
		}
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

	if (cmd.equals("g")) { // gå (till möte) (g <mötesnamn>)
	    int confNo = 0;
	    try {
		confNo = parseNameArgs(st.nextToken("").substring(1), true, true);
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("Du måste ange ett möte att gå till.");
	    }
	    if (confNo == 0) return 1;

	    System.out.println("-- gå till möte: " + confNoToName(confNo));
	    try {
		foo.changeConference(confNo);
		foo.updateUnreads();
		return 1;
	    } catch (RpcFailure failed) {
		System.out.print("-- mötesbytet misslyckades: ");
		switch (failed.getError()) {
		case Rpc.E_not_member:
		    System.out.println("du är inte med i det mötet");
		    String wantsChange = crtReadLine("Vill du gå med i mötet? (j/N) ");
		    if (wantsChange.equals("j")) {
			try {
			    foo.joinConference(confNo);
			    doCommand(t, "g m " + confNo);
			} catch (RpcFailure failed2) {
			    System.out.print("Det gick inte att gå med i mötet: ");
			    switch (failed2.getError()) {
			    case Rpc.E_access_denied:
				System.out.println("du fick inte");
				break;
			    case Rpc.E_permission_denied:
				System.out.println("du får inte ändra på detta medlemskap");
				break;
			    default:
				System.out.println("okänd anledning " + failed2.getError());
			    }
			}
		    }
		    break;
		default:
		    System.out.println("okänd anledning " + failed.getError());
		}
	    }

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
	if (cmd.equals("sm")) { // skapa möte
	    String name = null;
	    try {
		name = st.nextToken("").substring(1);
	    } catch (NoSuchElementException ex1) {
		throw new CmdErrException("Du måste ange ett namn på mötet.");
	    }
	    System.out.print("Försöker skapa möte \"" + name + "\"...");
	    int confNo = foo.createConf(name, false, false, false);
	    if (confNo > 0) System.out.println(" lyckades: mötet fick nummer " + confNo);
	    else System.out.println(" det gick inte.");
	    return 1;
	}
	if (cmd.equals("lm")) { // lista möten
	    String substring = null;
	    try {
		substring = st.nextToken("").substring(1);
	    } catch (NoSuchElementException ex1) {
		substring = "";
	    }
	    ConfInfo[] confs = foo.lookupName(substring, false, true);
	    System.out.println("Hittade " + confs.length + " möten");
	    MessageFormat form = new MessageFormat(" {0,number}\t-- {1}");
	    for (int i=0; i < confs.length; i++) {
		System.out.println(form.format(new Object[] {new Integer(confs[i].getNo()),
							     confs[i].getNameString()}));
	    }
	    System.out.println("-- Slut på listningen");
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









