package nu.dll.app.test;

import nu.dll.lyskom.Session;
import nu.dll.lyskom.CmdErrException;
import java.io.IOException;

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

    public abstract String[] getCommandDescriptions();

}
