package nu.dll.app.test;

import nu.dll.lyskom.Session;
import nu.dll.lyskom.CmdErrException;
import java.io.IOException;

interface Command {
    public static int OK = 1;
    public static int ERROR = 2;
    public static int USER_1 = 11;
    public static int USER_2 = 12;
    public static int USER_3 = 13;
    public static int USER_4 = 14;

    public void setEnvironment(Session session, Test2 t2);

    public int doCommand(String command, String parameters) throws CmdErrException, IOException;

    public String[] getCommands();

    public String getCommandDescription(int i);
    public String[] getCommandDescriptions();
    public String getCommandDescription(String command);
    public String getDescription();
    public String getParameters(Match command, String userInput);
}
