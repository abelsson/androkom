package nu.dll.app.test;

import javax.swing.*;
import java.io.*;
import java.util.StringTokenizer;
import nu.dll.lyskom.*;


public class TabClientCommands extends AbstractCommand {
    TabClient tabClient;
    JTabbedPane tabPane;
    public TabClientCommands(TabClient tc) {
	tabClient = tc;
	tabPane = tabClient.tabPane;
    }

    static String COMMENT_CMD = "_gui kommentera", FOOTNOTE_CMD = "_gui fotnotera", COMPOSE_CMD = "_gui inlägg";

    public int doCommand(String s, String parameters)
	throws IOException, CmdErrException {
	if (s.equals(COMMENT_CMD) || s.equals(FOOTNOTE_CMD)) {
	    int textNo = 0;
	    Text text = null;
	    boolean footnote = s.equals("f");
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
	    if (textNo < 1) {
		if (footnote && application.getLastSavedText() != null)
		    textNo = application.getLastSavedText().getNo();
		else
		    if (application.getLastText() != null)
			textNo = application.getLastText().getNo();
	    }
		    
	    if (textNo < 1) {
		throw new CmdErrException("Du måste ange ett giltigt textnummer eller läsa/skriva en text först");
	    }

		    
	    text = session.getText(textNo);
		    
	    if (text == null) {
		text = session.getText(textNo);
		if (text == null) throw new CmdErrException("Hittade inget inlägg");
	    }

	    TextComposer composer = new TextComposer(session, text, true, footnote);
	    String tabName = "edit";
	    tabClient.editCount++;
	    if (tabPane.indexOfTab(tabName) != -1) {
		tabName = "edit-" + tabClient.editCount;
	    }
	    int preSelectedIndex = tabPane.getSelectedIndex();
	    application.consoleWriteLn("Editorläge");
	    tabPane.addTab(tabName, composer);
	    tabPane.setSelectedIndex(tabPane.getTabCount()-1);
	    composer.waitForAction();
	    if (composer.isAborted()) {
		application.consoleWriteLn("Avbruten.");
		tabPane.setSelectedIndex(preSelectedIndex);
		tabPane.remove(composer);
		return Command.OK;
	    }
	    int newTextNo = 0;
	    try {
		newTextNo = session.createText(composer.getNewText());
		application.setLastSavedText(session.getText(newTextNo));
	    } catch (RpcFailure ex1) {
		application.consoleWrite("%Fel: kunde inte skapa kommentar/fotnot: ");
		switch (ex1.getError()) {
		case Rpc.E_not_author:
		    application.consoleWriteLn("du är inte författare till text " + ex1.getErrorStatus());
		    break;
		default:
		    throw new CmdErrException("Okänt fel: " + ex1.getMessage());
		}
		tabPane.setSelectedIndex(preSelectedIndex);
		tabPane.remove(composer);

		return Command.ERROR;
	    }
	    if (newTextNo > 0) {
		application.consoleWriteLn("text nummer " + newTextNo + " skapad.");
		tabPane.setSelectedIndex(preSelectedIndex);
		tabPane.remove(composer);
		if (!application.dontMarkOwnTextsAsRead) {
		    application.markAsRead(newTextNo);
		}
	    }

	    tabClient.editCount--;
	    return Command.OK;
	}
	if (s.equals(COMPOSE_CMD)) {
	    if (session.getCurrentConference() == -1) {
		throw new CmdErrException("Du måste vara i ett möte för att kunna skriva ett inlägg.");
	    }
	    
	    Text text = new Text();
	    text.addRecipient(session.getCurrentConference());
	    TextComposer composer = new TextComposer(session, text, false, false);
	    String tabName = "edit";
	    tabClient.editCount++;
	    if (tabPane.indexOfTab(tabName) != -1) {
		tabName = "edit-" + tabClient.editCount;
	    }
	    int preSelectedIndex = tabPane.getSelectedIndex();
	    application.consoleWriteLn("Editorläge");
	    tabPane.addTab(tabName, composer);
	    tabPane.setSelectedIndex(tabPane.getTabCount()-1);
	    composer.waitForAction();
	    if (composer.isAborted()) {
		application.consoleWriteLn("Avbruten.");
		tabPane.setSelectedIndex(preSelectedIndex);
		tabPane.remove(composer);
		return Command.OK;
	    }
	    int newTextNo = 0;
	    try {
		newTextNo = session.createText(composer.getNewText());
		application.setLastSavedText(session.getText(newTextNo));
	    } catch (RpcFailure ex1) {
		tabPane.setSelectedIndex(preSelectedIndex);
		tabPane.remove(composer);

		application.consoleWrite("%Fel: kunde inte skapa inlägg: ");
		switch (ex1.getError()) {
		default:
		    throw new CmdErrException("Okänt fel: " + ex1.getMessage());
		}
	    }
	    if (newTextNo > 0) {
		application.consoleWriteLn("text nummer " + newTextNo + " skapad.");

		if (!application.dontMarkOwnTextsAsRead) {
		    application.markAsRead(newTextNo);
		}
	    }
	    tabPane.setSelectedIndex(preSelectedIndex);
	    tabPane.remove(composer);

	    tabClient.editCount--;
	    return Command.OK;
	}
	return Command.ERROR;
    }
    public String[] getCommands() {
	return new String[] { "_gui fotnotera", "_gui kommentera", "_gui inlägg" };
    }
    public String[] getCommandDescriptions() {
	return new String[] { "Fotnotera [textnummer]", "Kommentera [textnummer]", "(Skriv) inlägg" };
    }
    public String getDescription() {
	return "Kommandon implementerade utav TabClient";
    }

}
