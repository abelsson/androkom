package nu.dll.app.test;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.text.MessageFormat;

import nu.dll.lyskom.*;

public class TextCommands extends AbstractCommand {

    // "�terse" renamed to "�terse text" due to a conflict
    String[] myCommands = { "�terse text", "}terse text", "fotnotera", "kommentera", "inl�gg", "radera text",
			    "markera text", "lista markerade texter", "�terse urinkl�gg", "}terse urinl{gg" };
    int[] commandIndices = { 0, 0, 1, 2, 3, 4, 5, 6, 7, 7 };
    String[] myDescriptions = {
	"�terse text <textnummer>", // 0
	"fotnotera (text) [textnummer]", // 1
	"kommentera (text)", // 2
	"(skriv) inl�gg", // 3
	"radera text", // 4
	"markera text [textnummer] [markeringstyp]", // 5
	"lista markerade texter", // 6
	"�terse urinl�gg [textnummer]" // 7
    };

    public TextCommands() {
	setCommands(myCommands);
    }

    public String getDescription() {
	return "Kommandon f�r l�sning, skrivning och annan texthantering";
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
	Debug.println("--> TextCommands.doCommand(" + s + ", " + parameters + ")");
	int textNo = 0;
	Text text = null;
	boolean footnote = false;
	StringTokenizer st = null;
	
	if (parameters != null) {
	    st = new StringTokenizer(parameters, " ");
	    try {
		if (st.hasMoreTokens())
		    textNo = Integer.parseInt(st.nextToken());
	    } catch (NumberFormatException ex1) {
		textNo = -1;
	    }
	}

	switch (getCommandIndex(s)) {
	case 0: // review text
	    if (textNo < 1) throw new CmdErrException("Du m�ste ange ett textnummer");
	    text = session.getText(textNo, true);
	    application.displayText(textNo);
	    application.setLastText(text);
	    break;	    
	case 1: // footnote text
	    footnote = true;
	case 2: // comment text
	    Debug.println("--> TextCommands.doCommand(): comment text");
	    if (textNo < 1) {
		if (footnote && application.getLastSavedText() != null)
		    textNo = application.getLastSavedText().getNo();
		else
		    if (application.getLastText() != null)
			textNo = application.getLastText().getNo();
	    }

	    if (textNo < 1) {
		throw new CmdErrException("Du m�ste ange ett giltigt " +
					  "textnummer eller l�sa/skriva " +
					  "en text f�rst")�;
	    }

	    text = session.getText(textNo);
	    
	    if (text == null) {
		text = session.getText(textNo);
		if (text == null) throw new CmdErrException("Hittade inget inl�gg");
	    }
	    commentOrFootnote(text, footnote);
	    break;

	case 3:  // write text
	    writeText();
	    break;
	case 4:
	    if (parameters == null || textNo < 1)
		throw new CmdErrException("Du m�ste ange ett giltigt textnummer");
	    deleteText(textNo);
	    break;
	case 5:
	    int markType = -1;
	    if (parameters == null) { // EWW.
		try {
		    textNo = Integer.parseInt(application.crtReadLine("Ange textnummer att markera: "));
		    markType = Integer.parseInt(application.crtReadLine("Ange markeringstype (1-255, 100)",
									"100"));
		} catch (NumberFormatException ex1) {}
	    } else {
		if (textNo > 1) {
		    if (st.hasMoreTokens()) {
			try {
			    markType = Integer.parseInt(st.nextToken());
			} catch (NumberFormatException ex2) {}
		    }
		}
	    }
	    if (textNo < 1) throw new CmdErrException("Du m�ste ange ett giltigt textnummer");
	    markText(textNo, markType > 0 ? markType : 100);
	    break;
	case 6: // list marked texts
	    listMarkedTexts();
	    break;
	case 7: // review original
	    reviewOriginal(textNo);
	    break;
	default:
	    throw new CmdErrException("Internfel: ok�nt kommando \"" + s + "\".");
	}

	return Command.OK;
    }

    public void reviewOriginal(int textNo) throws IOException, CmdErrException {
	// * does not treat footnotes as comments
	// * does not handle multiple original texts (only looks at forst comm-to)
	// * bugs out if original text is secret
	Text t = null;
	if (textNo < 1) {
	    t = application.getLastText();
	} else {
	    t = session.getText(textNo); 
	}
	if (t == null) {
	    throw new CmdErrException("Du m�ste ha en text att b�rja med.");
	}

	application.consoleWrite("S�ker efter urinl�gg f�r text " + t.getNo() + "... ");
	TextStat ts = session.getTextStat(t.getNo());
	while (ts.getStatInts(TextStat.miscCommTo).length > 0) {
	    ts = session.getTextStat(ts.getStatInts(TextStat.miscCommTo)[0]);
	}
	application.consoleWriteLn("hittade text " + ts.getNo());
	t = session.getText(ts.getNo());
	application.setLastText(t);
	application.displayText(t);
    }

    public void listMarkedTexts() throws IOException, CmdErrException {
	application.consoleWriteLn("Markerade inl�gg:");
	Mark[] marks = session.getMarks();
	for (int i=0; i < marks.length; i++) {
	    application.consoleWrite("Markering " + (i+1) + ": " + marks[i].getText() + " (" + marks[i].getType() + ")");
	    TextStat ts = session.getTextStat(marks[i].getText());
	    if (ts != null) {
		application.consoleWriteLn(" av " + application.confNoToName(ts.getAuthor()));
	    } else {
		application.consoleWriteLn("");
	    }
	}
	application.consoleWriteLn("");
    }

    public void markText(int textNo, int markType) throws IOException, CmdErrException {
	try {
	    session.markText(textNo, markType);
	    application.consoleWriteLn("OK, text " + textNo + " �r markerad");
	} catch (RpcFailure ex1) {
	    throw new CmdErrException(ex1.getMessage());
	}
    }

    public void deleteText(int textNo) throws IOException, CmdErrException {
	if (textNo == 0) throw new CmdErrException("du kan inte ta bort text 0");
	try {
	    session.deleteText(textNo);
	    application.consoleWriteLn("OK: text " + textNo + " raderad.");
	} catch (RpcFailure ex1) {
	    switch (ex1.getError()) {
	    case Rpc.E_no_such_text:
		application.consoleWriteLn("det finns ingen text med nummer " + textNo);
		break;
	    case Rpc.E_not_author:
		application.consoleWriteLn("du har inte r�tt att ta bort text " + textNo);
		break;
	    default:
		application.consoleWriteLn("ok�nt fel: " + ex1.getMessage());
		break;
	    }
	}
    }

    public void writeText() throws IOException, CmdErrException {
	if (session.getCurrentConference() == -1) {
	    throw new CmdErrException("Du m�ste vara i ett m�te f�r att kunna skriva ett inl�gg.");
	}
	application.setStatus("Skriver ett inl�gg");
	StringBuffer textb = new StringBuffer();
	application.consoleWriteLn("-- Inl�gg i m�te " + application.confNoToName(session.getCurrentConference()));
	application.consoleWriteLn("Avsluta med \".\" p� tom rad.");
	Text nText = new Text();
	nText.addRecipient(session.getCurrentConference());
	nText = application.editText(nText);
	if (nText == null) {
	    application.consoleWriteLn("Avbruten.");
	    return;
	}
	int newText = session.createText(nText);
	if (newText > 0) {
	    application.consoleWriteLn("text nummer " + newText + " skapad.");
	    application.setLastSavedText(session.getText(newText));
	    if (!application.dontMarkOwnTextsAsRead) application.markAsRead(newText);
	} else application.consoleWriteLn("misslyckades att skapa inl�gg.");

    }
    
    public void commentOrFootnote(Text text, boolean footnote) throws IOException, CmdErrException {
	application.setStatus("Skriver en " + (footnote ? "fotnot" : "kommentar"));
	application.consoleWriteLn("-- " + (footnote ? "Fotnot" : "Kommentar") + " till text " + text.getNo() + ".");
	application.consoleWriteLn("-- Skriv din " + (footnote ? "fotnot" : "kommentar") + 
				   ", avsluta med \".\" p� tom rad.");
	Text nText = new Text(application.bytesToString(text.getSubject()), "");

	if (!footnote)
	    nText.addCommented(text.getNo());
	else
	    nText.addFootnoted(text.getNo());

	int[] recipients = text.getRecipients();
	for (int i=0; i < recipients.length; i++) {
	    Conference conf = session.getConfStat(recipients[i]);
	    if ((!footnote) && conf.getType().original()) {
		int superconf = conf.getSuperConf();
		if (superconf > 0) {
		    nText.addRecipient(superconf);
		} else {
		    application.consoleWriteLn("Du f�r inte skriva kommentarer i " +
					       conf.getNameString());
		}
	    } else {
		nText.addRecipient(recipients[i]);
	    }
	}

	if (footnote) nText.addCcRecipients(text.getCcRecipients());

	nText = application.editText(nText);
	if (nText == null) {
	    application.consoleWrite("Avbruten.");
	    return;
	}
	application.consoleWrite("\nSkapar kommentar... ");


	int newText = 0;
	try {
	    newText = session.createText(nText);
	    application.setLastSavedText(session.getText(newText));
	} catch (RpcFailure ex1) {
	    application.consoleWrite("%Fel: kunde inte skapa kommentar/fotnot: ");
	    switch (ex1.getError()) {
	    case Rpc.E_not_author:
		application.consoleWriteLn("du �r inte f�rfattare till text " + ex1.getErrorStatus());
		break;
	    default:
		throw new CmdErrException("Ok�nt fel: " + ex1.getMessage());
	    }
	    return;
	}
	if (newText > 0) {
	    application.consoleWriteLn("text nummer " + newText + " skapad.");
	    if (!application.dontMarkOwnTextsAsRead) {
		application.markAsRead(newText);
		//t = foo.getText(newText);
	    }
	}	    
    }

	
}
