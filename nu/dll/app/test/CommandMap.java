package nu.dll.app.test;

import java.util.Map;
import java.util.HashMap;


import nu.dll.lyskom.Session;


public class CommandMap {
    Map commands = new HashMap();
    Session session = null;
    Test2 application = null;
    public CommandMap(Session session, Test2 application) {
	this.session = session;
	this.application = application;	
    }

    public void addCommand(Class cmd) {
	Command command = initCommand(cmd);
	String[] strings = command.getCommands();
	for (int i=0; i < strings.length; i++) {
	    addCommand(strings[i], command);
	}
    }

    public void addCommands(String[] str, Class cmd) {
	Command command = initCommand(cmd);
	command.setEnvironment(session, application);
	for (int i=0; i < str.length; i++) {
	    addCommand(str[i], command);
	}
    }

    private Command initCommand(Class cmd) {
	try {
	    Command command = (Command) cmd.newInstance();
	    command.setEnvironment(session, application);
	    return command;
	} catch (IllegalAccessException ex0) {
	    throw new RuntimeException("Error instantiating command class " + cmd.getName() + ": " + ex0.getMessage());
	} catch (InstantiationException ex1) {
	    throw new RuntimeException("Error instantiating command class " + cmd.getName() + ": " + ex1.getMessage());
	}
    }

    public void addCommand(String str, Class cmd) {
	commands.put(str, initCommand(cmd));
    }
    public void addCommand(String str, Command cmd) {
	cmd.setEnvironment(session, application);
	commands.put(str, cmd);
    }

    public Command getCommand(String str) {
	return (Command) commands.get(str);
    }

}
