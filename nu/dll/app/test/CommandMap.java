package nu.dll.app.test;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import nu.dll.lyskom.Session;


public class CommandMap {
    Map commands = new HashMap();
    List commandList = new LinkedList();
    Session session = null;
    Test2 application = null;
    public CommandMap(Session session, Test2 application) {
	this.session = session;
	this.application = application;	
    }

    public Command[] getAllCommands() {
	Object[] objects = commandList.toArray();
	Command[] allcommands = new Command[objects.length];
	for (int i=0; i < objects.length; i++) {
	    allcommands[i] = (Command) objects[i];
	}
	return allcommands;
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
	if (!commandList.contains(cmd)) commandList.add(cmd);
    }
    public void addCommand(String str, Command cmd) {
	cmd.setEnvironment(session, application);
	commands.put(str, cmd);
	if (!commandList.contains(cmd)) commandList.add(cmd);
    }

    public Command getCommand(String str) {
	return (Command) commands.get(str);
    }

}
