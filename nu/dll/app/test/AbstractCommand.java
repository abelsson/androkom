package nu.dll.app.test;

import nu.dll.lyskom.Session;
import nu.dll.lyskom.CmdErrException;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

abstract class AbstractCommand implements Command {

    Session session = null;
    Test2 application = null;
    String[] commands = null;

    public void setEnvironment(Session s, Test2 t) {
	session = s;
	application = t;
    }

    public Session getSession() {
	return session;
    }

    public Test2 getApplication() {
	return application;
    }

    public abstract int doCommand(String command, String parameters) throws CmdErrException, IOException;

    public void setCommands(String[] commands) {
	this.commands = commands;
    }

    public String[] getCommands() {
	return commands;
    }
    public String getCommandDescription(int i) {
	return getCommandDescriptions()[i];
    }
    public String getCommandDescription(String command) {
	return getCommandDescription(getCommandIndex(command));
    }

    int getCommandIndex(String command) {
	String[] commands = getCommands();
	for (int i=0; i < commands.length; i++) {
	    if (commands[i].equals(command)) return i; // commandIndices[i];
	}
	return -1;
    }



    public abstract String[] getCommandDescriptions();    
    public String getDescription() {
	return toString();
    }

    //"g latte"
    public String getParameters(Match command, String userInput) {
	int i=0;
	StringTokenizer tok = new StringTokenizer(command.command);
	StringTokenizer uTok = new StringTokenizer(userInput);
	Debug.println("getParameters(): match: " + command.toString());
	try {
	    while (i < command.paramOffset && tok.hasMoreTokens()) {
		tok.nextToken();
		uTok.nextToken();
		i++;
	    }
	    String result = uTok.nextToken("").substring(1).trim();
	    if (result.equals("")) return null;
	    Debug.println("getParameters(): returning parameters: " + result);
	    return result;
	} catch (NoSuchElementException ex1) {
	    Debug.println("getParameters(): returning null");
	    return null;
	}
    }


}
