package nu.dll.app.test;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Iterator;

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

    public void addCommand(Command command) {
	command.setEnvironment(session, application);
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
	if (commands.containsKey(str)) {
	    Object oldCommand = commands.put(str, initCommand(cmd));
	    commands.put("!" + str, oldCommand);
	} else {
	    commands.put(str, initCommand(cmd));
	}

	if (!commandList.contains(cmd)) commandList.add(cmd);
    }
    public void addCommand(String str, Command cmd) {
	cmd.setEnvironment(session, application);

	if (commands.containsKey(str)) {
	    Object oldCommand = commands.put(str, cmd);
	    commands.put("!" + str, oldCommand);
	} else {
	    commands.put(str, cmd);
	}

	if (!commandList.contains(cmd)) commandList.add(cmd);
    }

    public Command getCommand(String str) {
	return (Command) commands.get(str);
    }

    // GROSS!
    public Match[] resolveCommand(String s) {
	List foundCommands = new LinkedList();
	Iterator i = commandList.iterator();
	int lastMatchCount = 0;
	Match bestMatch = null;
	boolean ambig = false;
	while (i.hasNext()) {
	    Command c = (Command) i.next();
	    String[] commandStrings = c.getCommands();
	    for (int j=0; j < commandStrings.length; j++) {
		StringTokenizer uiTokenizer = new StringTokenizer(s);
		StringTokenizer cmdTokenizer = new StringTokenizer(commandStrings[j]);
		int commandTokens = countTokens(commandStrings[j]);
		boolean match = false;
		int k=0, matchCount = 0;
		Debug.print("trying " + commandStrings[j] + ", ");
		while (k < commandTokens && uiTokenizer.hasMoreTokens()) {
		    k++;
		    String uiToken = uiTokenizer.nextToken();
		    //if (!cmdTokenizer.hasMoreTokens()) continue;
		    String cmdToken = cmdTokenizer.nextToken();
		    if (!cmdToken.toLowerCase().startsWith(uiToken.toLowerCase())) continue;		    
		    match = true; matchCount++;
		    Debug.print("(match) ");
		}
		if (match && matchCount <= commandTokens) {
		    Match m = new Match(commandStrings[j], matchCount, matchCount);
		    if (bestMatch == null) {
			bestMatch = m;
		    } else {
			if (m.score > bestMatch.score) {
			    bestMatch = m;
			    ambig = false;
			} else if (m.score == bestMatch.score) {
			    ambig = true;
			}
		    }
		    Debug.println(s + " matches " + commandStrings[j]);
		    foundCommands.add(m);
		} 
	    }
	}
	if (!ambig && bestMatch != null) {
	    foundCommands.clear();
	    foundCommands.add(bestMatch);
	}

	Match[] result = new Match[foundCommands.size()];
	i = foundCommands.iterator();
	int j=0;
	while (i.hasNext()) {
	    result[j++] = (Match) i.next();
	}
	return result;
	
    }

    int countTokens(String command) {
	int i=0;
	StringTokenizer tok = new StringTokenizer(command);
	while (tok.hasMoreTokens()) {
	    tok.nextToken();
	    i++;
	}
	return i;
    }


}


