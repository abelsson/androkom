package nu.dll.app.test;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import nu.dll.lyskom.*;

public class ConfCommands extends AbstractCommand {
    String[] myCommands = { "�p", "}p", "�p", "�f", "}f", "�f", "g" };
    String[] myDescriptions = {
	"�terse presentation",
	"�terse presentation",
	"�ndra presentation",
	"�terse FAQ",
	"�terse FAQ",
	"�ndra FAQ",
	"g� till m�te"
    };
    public ConfCommands() {
	setCommands(myCommands);
    }

    int getCommandIndex(String command) {
	String[] commands = getCommands();
	for (int i=0; i < commands.length; i++) {
	    if (commands[i].equals(command)) return i;
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
	case 0:
	case 1:
	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    reviewPresentation(confNo);
	    break;
	case 2:
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    changePresentation(confNo);
	    break;
	case 3:
	case 4:
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    reviewFaq(confNo);
	    break;
	case 5:
	    if (confNo < 1)
		confNo = session.getMyPerson().getNo();

	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    changeFaq(confNo);
	    break;
	case 6:
	    if (parameters == null) throw new CmdErrException("Du m�ste ange ett m�te att g� till.");
	    if (confNo < 1) throw new CmdErrException("Hittade inte m�tet eller personen");
	    changeConference(confNo);
	    break;
	    
	default: 
	    throw new RuntimeException("Unknown command " + s);
	}
	return OK;

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
