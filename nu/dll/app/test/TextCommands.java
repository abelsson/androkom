package nu.dll.app.test;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.text.MessageFormat;

import nu.dll.lyskom.*;

public class TextCommands extends AbstractCommand {
    String[] myCommands = { "å", "}", "f", "k", "i" };
    int[] commandIndices = { 0, 0, 1, 2, 3 };
    String[] myDescriptions = {
	"återse text", // 0
	"fotnotera text", // 1
	"kommentera text", // 2
	"skriv inl{gg" // 3
    };

    public TextCommands() {
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
	int textNo = 0;
	Text text = null;
	boolean footnote = false;
	if (parameters != null) {
	    try {
		textNo = Integer.parseInt(parameters);
	    } catch (NumberFormatException ex1) {
		textNo = -1;
	    }
	}

	switch (getCommandIndex(s)) {
	case 0: // review text
	    if (textNo < 1) throw new CmdErrException("Du m}ste ange ett textnummer");
	    text = session.getText(textNo, true);
	    application.displayText(textNo);
	    application.setLastText(text);
	    break;	    
	case 1: // footnote text
	    footnote = true;
	case 2: // comment text
	    if (textNo < 1 && application.getLastText() == null && application.getLastSavedText() == null) {
		throw new CmdErrException("Du m}ste ange ett giltigt textnummer eller l{sa/skriva en text f|rst");
	    }
	    if (footnote && application.getLastSavedText() != null)
		text = application.getLastSavedText();
	    else
		text = application.getLastText();
	    
	    if (text == null) {
		text = session.getText(textNo);
		if (text == null) throw new CmdErrException("Hittade inget inl{gg");
	    }
	    commentOrFootnote(text, footnote);
	    break;

	case 3:  // write text
	    writeText();
	    break;
	}
	return 1;
    }

    public void writeText() throws IOException, CmdErrException {
	if (session.getCurrentConference() == -1) {
	    throw new CmdErrException("Du måste vara i ett möte för att kunna skriva ett inlägg.");
	}
	application.setStatus("Skriver ett inlägg");
	StringBuffer textb = new StringBuffer();
	application.consoleWriteLn("-- Inlägg i möte " + application.confNoToName(session.getCurrentConference()));
	application.consoleWriteLn("Avsluta med \".\" på tom rad.");
	Text nText = new Text();
	nText.addRecipient(session.getCurrentConference());
	nText = application.editText(nText);
		
	int newText = session.createText(nText);
	if (newText > 0) {
	    application.consoleWriteLn("text nummer " + newText + " skapad.");
	    application.setLastSavedText(session.getText(newText));
	    if (!application.dontMarkOwnTextsAsRead) application.markAsRead(newText);
	} else application.consoleWriteLn("misslyckades att skapa inlägg.");

    }
    
    public void commentOrFootnote(Text text, boolean footnote) throws IOException, CmdErrException {
	application.setStatus("Skriver en " + (footnote ? "fotnot" : "kommentar"));
	application.consoleWriteLn("-- " + (footnote ? "Fotnot" : "Kommentar") + " till text " + text.getNo() + ".");
	application.consoleWriteLn("-- Skriv din " + (footnote ? "fotnot" : "kommentar") +  ", avsluta med \".\" på tom rad.");
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
		    application.consoleWriteLn("Du får inte skriva kommentarer i " +
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
		application.consoleWriteLn("du är inte författare till text " + ex1.getErrorStatus());
		break;
	    default:
		throw new CmdErrException("Ok{nt fel: " + ex1.getMessage());
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
