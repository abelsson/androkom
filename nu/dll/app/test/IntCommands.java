package nu.dll.app.test;

import nu.dll.lyskom.*;
import java.io.IOException;

public class IntCommands extends AbstractCommand {
    String[] myCommands = { "sända meddelande" };
    String[] myDescriptions = { "sända meddelande" };
    int[] commandIndices = { 0 };

    public IntCommands() {
	setCommands(myCommands);
    }

    public String getDescription() {
	return "Kommandon för interaktivitet";
    }

    public String[] getCommandDescriptions() {
	return myDescriptions;
    }

    public int doCommand(String s, String parameters) throws CmdErrException, IOException {
	switch (getCommandIndex(s)) {
	case 0:
	    int confNo = 0;
	    if (parameters != null && !parameters.equals("")) {
		confNo = application.parseNameArgs(parameters, true, true);
	    } else {
		if (!application.crtReadLine("Vill du skicka ett alarmmeddelande? (j/N) ").equals("j")) return 1;
	    }
	    if (confNo > 0) {
		application.consoleWriteLn("Skicka meddelande till " + application.confNoToName(confNo));
	    } else if (confNo == -1) {
		return Command.OK;
	    }
	    String message = application.crtReadLine("Text att skicka> ");
	    if (message == null || message.trim().equals("")) {
		application.consoleWriteLn("Avbruten");
	    }
	    try {
		session.sendMessage(confNo, message);
		if (confNo == 0) application.consoleWriteLn("Ditt alarmmeddelande har skickats.");
		else application.consoleWriteLn("Ditt meddelande har skickats till " + application.confNoToName(confNo));
		return Command.OK;
	    } catch (RpcFailure ex1) {
		application.consoleWriteLn("Det gick inte att skicka meddelandet. Felkod: " + ex1.getError());
		return Command.ERROR;
	    }
	    //break;
	}
	return Command.UNHANDLED;
    }

}
