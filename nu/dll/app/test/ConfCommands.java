package nu.dll.app.test;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.text.MessageFormat;

import nu.dll.lyskom.*;

public class ConfCommands extends AbstractCommand {
    String[] myCommands = { "åp", "}p", "äp", "åf", "}f", "äf", "g", "ln", "bn", "nm", "sm", "lm" };

    // descriptions according to commandIndices
    String[] myDescriptions = {
	"återse presentation",
	"ändra presentation",
	"återse FAQ",
	"ändra FAQ",
	"gå till möte",
	"lista nyheter",
	"byta namn",
	"n{sta m|te",
	"skapa m|te",
	"lista m|ten"
    };

    int[] commandIndices = { 0, 0, 1, 2, 2, 3, 4, 5, 6, 7, 8, 9 };

    public ConfCommands() {
	setCommands(myCommands);
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
	if (parameters != null) confNo = application.parseNameArgs(parameters, true, true);

	switch (getCommandIndex(s)) {
	case 0: // review presentation
	    if (confNo < 1) throw new CmdErrException("Hittade inte mötet eller personen");
	    reviewPresentation(confNo);
	    break;
	case 1: // change presentation
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte mötet eller personen");
	    changePresentation(confNo);
	    break;
	case 2: // review FAQ
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte mötet eller personen");
	    reviewFaq(confNo);
	    break;
	case 3: // change FAQ
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte mötet eller personen");
	    changeFaq(confNo);
	    break;
	case 4: // change conference
	    if (parameters == null) throw new CmdErrException("Du måste ange ett möte att gå till.");
	    if (confNo < 1) throw new CmdErrException("Hittade inte mötet eller personen");
	    changeConference(confNo);
	    break;
	case 5: // list news
	    listNews();
	    break;
	case 6: // change name
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte mötet eller personen");
	    changeName(confNo);
	    break;
	case 7: // next unread conference
	    nextUnreadConference();
	    break;
	case 8: // create conference
	    if (parameters == null) throw new CmdErrException("Du m}ste ange ett m|tesnamn");
	    createConference(parameters);
	    break;
	case 9: // list conferences
	    listConferences(parameters != null ? parameters : "");
	    break;
	default: 
	    throw new RuntimeException("Unknown command " + s);
	}
	return OK;

    }

    public void listConferences(String substring) throws IOException, CmdErrException {
	ConfInfo[] confs = session.lookupName(substring, false, true);
	application.consoleWriteLn("Hittade " + confs.length + " möten");
	MessageFormat form = new MessageFormat(" {0,number}\t{2} {1}");

	for (int i=0; i < confs.length; i++) {
	    boolean memberOf = session.isMemberOf(confs[i].getNo());
	    application.consoleWriteLn(form.format(new Object[] {new Integer(confs[i].getNo()),
								 confs[i].getNameString(),
								 (memberOf ? " " : "*")
								 
	    }));
	}
	application.consoleWriteLn("-- Slut på listningen");
    }

    public void createConference(String name) throws CmdErrException, IOException {
	application.consoleWrite("Försöker skapa möte \"" + name + "\"...");
	int confNo = session.createConf(name, false, false, false);
	if (confNo > 0) application.consoleWriteLn(" lyckades: mötet fick nummer " + confNo);
	else application.consoleWriteLn(" det gick inte.");	
    }


    public void nextUnreadConference() throws CmdErrException, IOException {
	int nextConf = session.nextUnreadConference(true);
	if (nextConf > 0) {
	    while (!application.toread.isEmpty())
		application.toread.pop(); // töm att-läsa-listan
	    application.consoleWriteLn("nästa möte - " + application.confNoToName(nextConf));
	} else {
	    application.consoleWriteLn("Det finns inga fler olästa möten.");
	}
    }

    public void changeName(int confNo) throws CmdErrException, IOException {
	application.consoleWriteLn("Byta namn på " + application.confNoToName(confNo));
	try {
	    session.changeName(confNo, application.crtReadLine("Nytt namn> "));
	} catch (RpcFailure ex1) {
	    application.consoleWrite("Det gick inte att byta namn: ");
	    switch (ex1.getError()) {
	    case Rpc.E_permission_denied:
		throw new CmdErrException("du saknar behörighet för operationen");
	    case Rpc.E_conference_exists:
		throw new CmdErrException("det finns redan ett möte med detta namn");
	    case Rpc.E_string_too_long:
		throw new CmdErrException("det nya namnet är för långt");
	    case Rpc.E_bad_name:
		throw new CmdErrException("det nya namnet innehåller ogiltiga tecken");
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
	    application.consoleWriteLn("Du har " + unreads + " " + (unreads > 1 ? "olästa" : "oläst") +
				       " i " + application.confNoToName(conferences[i]));
	}
	if (confsum == 0) {
	    application.consoleWriteLn("Du har läst alla inlägg.");
	} else {
	    application.consoleWriteLn("");
	    application.consoleWriteLn("Du har totalt " + sum + " " + (sum > 1 ? "olästa" : "oläst") +
				       " inlägg i " + confsum + " " + (confsum > 1 ? "möten" : "möte"));
	}
    }

    public void changeConference(int confNo) throws CmdErrException, IOException {
	application.consoleWriteLn("-- gå till möte: " + application.confNoToName(confNo));
	try {
	    session.changeConference(confNo);
	    session.updateUnreads();

	    // töm att-läsa-listan
	    synchronized (application.toread) {
		while (!application.toread.isEmpty()) application.toread.pop();
	    }
	} catch (RpcFailure failed) {
	    application.consoleWrite("-- mötesbytet misslyckades: ");
	    switch (failed.getError()) {
	    case Rpc.E_not_member:
		application.consoleWriteLn("du är inte med i det mötet");
		String wantsChange = application.crtReadLine("Vill du gå med i mötet? (j/N) ");
		if (wantsChange.equals("j")) {
		    try {
			session.joinConference(confNo);
			changeConference(confNo);
		    } catch (RpcFailure failed2) {
			application.consoleWrite("Det gick inte att gå med i mötet: ");
			switch (failed2.getError()) {
			case Rpc.E_access_denied:
			    application.consoleWriteLn("du fick inte");
			    break;
			case Rpc.E_permission_denied:
			    application.consoleWriteLn("du får inte ändra på detta medlemskap");
			    break;
			default:
			    application.consoleWriteLn("okänd anledning " + failed2.getError());
			}
		    }
		}
		break;
	    default:
		application.consoleWriteLn("okänd anledning " + failed.getError());
	    }
	}
    }


    public void changeFaq(int confNo) throws CmdErrException, IOException {
	application.consoleWriteLn("Ändra FAQ för möte " + application.confNoToName(confNo));
	AuxItem[] confAuxItems = session.getConfStat(confNo).getAuxItems();
	List oldFaqItems = new LinkedList();
	for (int i=0; i < confAuxItems.length; i++) {
	    if (confAuxItems[i].getTag() == AuxItem.tagFaqText)
		oldFaqItems.add(new Integer(confAuxItems[i].getNo()));
	}
	int[] oldFaqAux = new int[oldFaqItems.size()];
	for (int i=0; i < oldFaqAux.length; i++) oldFaqAux[i] = ((Integer) oldFaqItems.get(i)).intValue();
	
	String faqText = application.crtReadLine("Ange textnummer för FAQ> ");
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
	    application.consoleWrite("%Fel: gick inte att ändra FAQ: ");
	    switch (ex1.getError()) {
	    case Rpc.E_aux_item_permission:
		application.consoleWriteLn("otillräcklig behörighet");
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
	application.consoleWriteLn("Återse FAQ för möte " + application.confNoToName(confNo));
	AuxItem[] confAuxItems = session.getConfStat(confNo).getAuxItems();
	for (int i=0; i < confAuxItems.length; i++) {
	    if (confAuxItems[i].getTag() == AuxItem.tagFaqText) {
		application.displayText(session.getText(confAuxItems[i].getData().intValue()));
	    }
	}
    }

    public void changePresentation(int confNo) throws CmdErrException, IOException {
	application.consoleWriteLn("Ändrar presentation för " + application.confNoToName(confNo));
	Conference myConf = session.getConfStat(confNo);
	int myPresentationNo = myConf.getPresentation();
	Text myPresentation = null;
	if (myPresentationNo > 0) {
	    Text oldPresentation = session.getText(myPresentationNo);
	    myPresentation = (Text) oldPresentation.clone();
	} else {
	    myPresentation = new Text(application.confNoToName(confNo),
				      "");
	    myPresentation.addRecipient(2); // pres memb
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
		application.consoleWriteLn("OK, text " + newText + " är ny presentation för " + 
					   application.confNoToName(confNo));
		} catch (RpcFailure ex1) {
		    application.consoleWriteLn("%Fel: kunde inte sätta presentation för " + 
					       application.confNoToName(confNo) + ": " + ex1.getMessage());
		}
	}
    }

    public void reviewPresentation(int confNo) throws CmdErrException, IOException {
	int presNo = session.getConfStat(confNo).getPresentation();
	if (presNo < 1) {
	    throw new CmdErrException("Hittade ingen presentation för " + application.confNoToName(confNo));
	}
	
	Text presText = session.getText(presNo);
	if (presText == null) throw new CmdErrException("Kunde inte hämta text " + presNo);
	application.displayText(presText);
    }
}
