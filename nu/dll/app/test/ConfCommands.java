package nu.dll.app.test;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.text.MessageFormat;

import nu.dll.lyskom.*;

public class ConfCommands extends AbstractCommand {
    String[] myCommands = { "�terse presentation", "}terse presentation", "�ndra presentation",
			    "�terse FAQ", "}terse FAQ", "�ndra FAQ", "g� m�te", "lista nyheter",
			    "byta namn", "n�sta m�te", "skapa m�te", "lista m�ten", "uttr�da m�te",
                            "lista �renden", "s�k m�te regexp"};

    // descriptions according to commandIndices
    String[] myDescriptions = {
	"�terse presentation [m�te/person]",
	"�ndra presentation [m�te/person]",
	"�terse FAQ [m�te/person]",
	"�ndra FAQ [m�te/person]",
	"g� (till m�te) [m�te/brevl�da]",
	"lista nyheter",
	"byta namn [m�te/person]",
	"(g� till) n�sta m�te",
	"skapa m�te <namn p� m�tet>",
	"lista m�ten [substr�ng]",
	"uttr�da (ur) m�te <namn p� m�tet>",
	"lista �renden (ol�sta i nuvarande m�te)",
	"s�k m�te (med) regexp [regexp]"
    };

    int[] commandIndices = { 0, 0, 1, 2, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

    public ConfCommands() {
	setCommands(myCommands);
    }

    public String getDescription() {
	return "Kommandon f�r m�ten och brevl�dor";
    }

    public String getCommandDescription(int i) {
	return myDescriptions[commandIndices[i]];
    }


    int getCommandIndex(String command) {
	String[] commands = getCommands();
	for (int i=0; i < commands.length; i++) {
	    if (commands[i].equals(command)) return commandIndices[i];
	}
	return -1;
    }

    public String[] getCommandDescriptions() {
	return myDescriptions;
    }

    public int doCommand(String s, String parameters) throws CmdErrException, IOException {
	int confNo = 0;

	switch (getCommandIndex(s)) {
	case 0: // review presentation
	    if (parameters != null) confNo = application.parseNameArgs(parameters, true, true);
	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    reviewPresentation(confNo);
	    break;
	case 1: // change presentation
	    if (parameters != null) confNo = application.parseNameArgs(parameters, true, true);
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    changePresentation(confNo);
	    break;
	case 2: // review FAQ
	    if (parameters != null) confNo = application.parseNameArgs(parameters, true, true);
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    reviewFaq(confNo);
	    break;
	case 3: // change FAQ
	    if (parameters != null) confNo = application.parseNameArgs(parameters, true, true);
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    changeFaq(confNo);
	    break;
	case 4: // change conference
	    if (parameters == null) throw new CmdErrException("Du m�ste ange ett m�te att g� till.");
	    confNo = application.parseNameArgs(parameters, true, true);

	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    changeConference(confNo);
	    break;
	case 5: // list news
	    listNews();
	    break;
	case 6: // change name
	    if (parameters != null) confNo = application.parseNameArgs(parameters, true, true);
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    changeName(confNo);
	    break;
	case 7: // next unread conference
	    nextUnreadConference();
	    break;
	case 8: // create conference
	    if (parameters == null) throw new CmdErrException("Du m�ste ange ett m�tesnamn");
	    createConference(parameters);
	    break;
	case 9: // list conferences
	    listConferences(parameters != null ? parameters : "");
	    break;
	case 10:
	    if (parameters == null) throw new CmdErrException("Du m�ste ange ett m�te att g� till.");
	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    leaveConference(confNo);
	    break;
	case 11:
	    if (session.getCurrentConference() < 1)
		throw new CmdErrException("Du m�ste vara i ett m�te f�r att kunna lista �renden");
	    listSubjects();
	    break;
	case 12:
	    if (parameters == null)
		throw new CmdErrException("Du m�ste ange ett s�kuttryck");
	    searchConferences(parameters);
	    break;

	default: 
	    throw new RuntimeException("Unknown command " + s);
	}
	return OK;

    }

    public void listSubjects() throws IOException, CmdErrException {
	// somewhat crude.
	UConference uconf = session.getUConfStat(session.getCurrentConference());
	Membership membership = session.queryReadTexts(session.getMyPerson().getNo(),
						       session.getCurrentConference(),
						       true);
	int localTextNo = membership.getLastTextRead()+1;
	int count = 0;
	application.consoleWriteLn("  " + application.pad("nummer", 8) + 
				   "  " + application.pad("f�rfattare", 25) +
				   "  " + application.pad("�rende", 35));
	application.consoleWriteLn("----------------------------------------------------------------------");
	while (uconf.getHighestLocalNo() >= localTextNo) {
	    TextMapping map = session.localToGlobal(uconf.getNo(), localTextNo, 10);
	    while (map.hasMoreElements()) {
		localTextNo++;
		int globalTextNo = ((Integer) map.nextElement()).intValue();
		if (globalTextNo == 0) continue;
		Text text = session.getText(globalTextNo);
		String author = application.confNoToName(text.getAuthor());
		String subject = application.bytesToString(text.getSubject());
		application.consoleWriteLn(" #" + application.pad(""+text.getNo(), 8) +
					   "  " + application.pad(author, 25) +
					   "  " + application.pad(subject, 35));
		count++;
	    }
	}
	application.consoleWriteLn("Listade " + count + " inl�gg.");
	application.consoleWriteLn("----------------------------------------------------------------------");


    }

    
    /**
     * note: should clear toread stack too.
     */
    public void leaveConference(int confNo) throws IOException, CmdErrException {
	if (application.crtReadLine("Vill du verkligen uttr�da ur " +
				    application.confNoToName(confNo) + " (j/N)? ", "n").equals("j")) {
	    try {
		session.subMember(confNo, session.getMyPerson().getNo());
	    } catch (RpcFailure ex1) {
		throw new CmdErrException("Misslyckades: " + ex1.getMessage());
	    }
	}
    }

    public void searchConferences(String regexp) throws IOException, CmdErrException {
	listConferences(regexp, true);
    }
    public void listConferences(String substring) throws IOException, CmdErrException {
	listConferences(substring, false);
    }
    public void listConferences(String substring, boolean useRegexp) throws IOException, CmdErrException {
	ConfInfo[] confs = useRegexp ? session.reLookup(substring, false, true) :
	    session.lookupName(substring, false, true);
	application.consoleWriteLn("Hittade " + confs.length + " m�ten");
	MessageFormat form = new MessageFormat(" {0,number}\t{2} {1}");

	for (int i=0; i < confs.length; i++) {
	    boolean memberOf = session.isMemberOf(confs[i].getNo());
	    application.consoleWriteLn(form.format(new Object[] {new Integer(confs[i].getNo()),
								 confs[i].getNameString(),
								 (memberOf ? " " : "*")
								 
	    }));
	}
	application.consoleWriteLn("-- Slut p� listningen");
    }

    public void createConference(String name) throws CmdErrException, IOException {
	application.consoleWrite("F�rs�ker skapa m�te \"" + name + "\"...");
	int confNo = session.createConf(name, false, false, false);
	if (confNo > 0) application.consoleWriteLn(" lyckades: m�tet fick nummer " + confNo);
	else application.consoleWriteLn(" det gick inte.");	
    }


    public void nextUnreadConference() throws CmdErrException, IOException {
	int nextConf = session.nextUnreadConference(true);
	if (nextConf > 0) {
	    while (!application.toread.isEmpty())
		application.toread.pop(); // t�m att-l�sa-listan
	    application.consoleWriteLn("n�sta m�te - " + application.confNoToName(nextConf));
	} else {
	    application.consoleWriteLn("Det finns inga fler ol�sta m�ten.");
	}
    }

    public void changeName(int confNo) throws CmdErrException, IOException {
	application.consoleWriteLn("Byta namn p� " + application.confNoToName(confNo));
	try {
	    session.changeName(confNo, application.crtReadLine("Nytt namn> "));
	} catch (RpcFailure ex1) {
	    application.consoleWrite("Det gick inte att byta namn: ");
	    switch (ex1.getError()) {
	    case Rpc.E_permission_denied:
		throw new CmdErrException("du saknar beh�righet f�r operationen");
	    case Rpc.E_conference_exists:
		throw new CmdErrException("det finns redan ett m�te med detta namn");
	    case Rpc.E_string_too_long:
		throw new CmdErrException("det nya namnet �r f�r l�ngt");
	    case Rpc.E_bad_name:
		throw new CmdErrException("det nya namnet inneh�ller ogiltiga tecken");
	    default:
		throw new CmdErrException("felkod " + ex1.getError());
	    }
	}
    }

    public void listNews() throws CmdErrException, IOException {
	int[] conferences = session.getUnreadConfs(session.getMyPerson().getNo());
	int sum = 0;
	int confsum = 0;
	for (int i=0; i < conferences.length; i++) {
	    int unreads = session.getUnreadCount(conferences[i]);
	    sum += unreads;
	    confsum++;
	    application.consoleWriteLn("Du har " + unreads + " " + (unreads > 1 ? "ol�sta" : "ol�st") +
				       " i " + application.confNoToName(conferences[i]));
	}
	if (confsum == 0) {
	    application.consoleWriteLn("Du har l�st alla inl�gg.");
	} else {
	    application.consoleWriteLn("");
	    application.consoleWriteLn("Du har totalt " + sum + " " + (sum > 1 ? "ol�sta" : "ol�st") +
				       " inl�gg i " + confsum + " " + (confsum > 1 ? "m�ten" : "m�te"));
	}
    }

    public void changeConference(int confNo) throws CmdErrException, IOException {
	application.consoleWriteLn("-- g� till m�te: " + application.confNoToName(confNo));
	try {
	    session.changeConference(confNo);
	    session.updateUnreads();

	    // t�m att-l�sa-listan
	    synchronized (application.toread) {
		while (!application.toread.isEmpty()) application.toread.pop();
	    }
	} catch (RpcFailure failed) {
	    application.consoleWrite("-- m�tesbytet misslyckades: ");
	    switch (failed.getError()) {
	    case Rpc.E_not_member:
		application.consoleWriteLn("du �r inte med i det m�tet");
		String wantsChange = application.crtReadLine("Vill du g� med i m�tet? (j/N) ");
		if (wantsChange.equals("j")) {
		    try {
			session.joinConference(confNo);
			changeConference(confNo);
		    } catch (RpcFailure failed2) {
			application.consoleWrite("Det gick inte att g� med i m�tet: ");
			switch (failed2.getError()) {
			case Rpc.E_access_denied:
			    application.consoleWriteLn("du fick inte");
			    break;
			case Rpc.E_permission_denied:
			    application.consoleWriteLn("du f�r inte �ndra p� detta medlemskap");
			    break;
			default:
			    application.consoleWriteLn("ok�nd anledning " + failed2.getError());
			}
		    }
		}
		break;
	    default:
		application.consoleWriteLn("ok�nd anledning " + failed.getError());
	    }
	}
    }


    public void changeFaq(int confNo) throws CmdErrException, IOException {
	application.consoleWriteLn("�ndra FAQ f�r m�te " + application.confNoToName(confNo));
	AuxItem[] confAuxItems = session.getConfStat(confNo).getAuxItems();
	List oldFaqItems = new LinkedList();
	for (int i=0; i < confAuxItems.length; i++) {
	    if (confAuxItems[i].getTag() == AuxItem.tagFaqText)
		oldFaqItems.add(new Integer(confAuxItems[i].getNo()));
	}
	int[] oldFaqAux = new int[oldFaqItems.size()];
	for (int i=0; i < oldFaqAux.length; i++) oldFaqAux[i] = ((Integer) oldFaqItems.get(i)).intValue();
	
	String faqText = application.crtReadLine("Ange textnummer f�r FAQ> ");
	int faq;
	try {
	    faq = Integer.parseInt(faqText);
	} catch (NumberFormatException e) {
	    throw new CmdErrException("Felaktigt textnummer");
	}
	try {
	    session.modifyAuxInfo(true, confNo, oldFaqAux,
			      new AuxItem[] { new AuxItem(AuxItem.tagFaqText,
							  new Bitstring("00000000"), 0,
							  new Hollerith(""+faq)) });
	    application.consoleWriteLn("OK: FAQ satt till text " + faq);
	} catch (RpcFailure ex1) {
	    application.consoleWrite("%Fel: gick inte att �ndra FAQ: ");
	    switch (ex1.getError()) {
	    case Rpc.E_aux_item_permission:
		application.consoleWriteLn("otillr�cklig beh�righet");
		break;
	    case Rpc.E_illegal_aux_item:
		application.consoleWriteLn("felaktigt aux-item");
		break;
	    default:
		application.consoleWriteLn("felkod " + ex1.getError());
	    }
	}

    }
    public void reviewFaq(int confNo) throws CmdErrException, IOException {
	application.consoleWriteLn("�terse FAQ f�r m�te " + application.confNoToName(confNo));
	AuxItem[] confAuxItems = session.getConfStat(confNo).getAuxItems();
	for (int i=0; i < confAuxItems.length; i++) {
	    if (confAuxItems[i].getTag() == AuxItem.tagFaqText) {
		application.displayText(session.getText(confAuxItems[i].getData().intValue()));
	    }
	}
    }

    public void changePresentation(int confNo) throws CmdErrException, IOException {
	application.consoleWriteLn("�ndrar presentation f�r " + application.confNoToName(confNo));
	Conference myConf = session.getConfStat(confNo);
	int myPresentationNo = myConf.getPresentation();
	Text myPresentation = null;
	if (myPresentationNo > 0) {
	    Text oldPresentation = session.getText(myPresentationNo);
	    myPresentation = (Text) oldPresentation.clone();
	} else {
	    myPresentation = new Text(application.confNoToName(confNo),
				      "");
	    if (myConf.getType().letterbox()) {
		myPresentation.addRecipient(2); // pres memb
	    } else {
		myPresentation.addRecipient(1); // pres conf
	    }
	}
	myPresentation = application.editText(myPresentation);
	if (myPresentation == null) {
	    throw new CmdErrException("Editeringen avbruten");
	}
	int newText = session.createText(myPresentation);
	if (newText > 0) {
	    application.consoleWriteLn("text nummer " + newText + " skapad.");
	    application.markAsRead(newText);
	    try {
		session.setPresentation(confNo, newText);
		application.consoleWriteLn("OK, text " + newText + " �r ny presentation f�r " + 
					   application.confNoToName(confNo));
		} catch (RpcFailure ex1) {
		    application.consoleWriteLn("%Fel: kunde inte s�tta presentation f�r " + 
					       application.confNoToName(confNo) + ": " + ex1.getMessage());
		}
	}
    }

    public void reviewPresentation(int confNo) throws CmdErrException, IOException {
	int presNo = session.getConfStat(confNo).getPresentation();
	if (presNo < 1) {
	    throw new CmdErrException("Hittade ingen presentation f�r " + application.confNoToName(confNo));
	}
	
	Text presText = session.getText(presNo);
	if (presText == null) throw new CmdErrException("Kunde inte h�mta text " + presNo);
	application.displayText(presText);
    }
}
