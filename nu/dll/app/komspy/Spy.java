package nu.dll.app.komspy;

import nu.dll.lyskom.*;
import java.io.*;
import java.util.*;
public class Spy {

    public static void main(String[] argv) throws Exception {
	int sleepTime = Integer.getInteger("spy.sleeptime", 10000).intValue();
	String filename = System.getProperty("spy.file");
	String server = System.getProperty("spy.server", "kom.sno.pp.se");
	int loginUser = Integer.getInteger("spy.loginuser", 0).intValue();
	String loginPassword = System.getProperty("spy.loginpassword", "");
	Map confMap = null;
	try {
	    ObjectInputStream objStream = new ObjectInputStream(new FileInputStream(new File(filename)));
	    confMap = (Map) objStream.readObject();
	    objStream.close();
	    System.out.println("Read confmap file.");
	} catch (FileNotFoundException ex1) {
	    System.out.println("No confmap found, starting from scratch");
	    confMap = new HashMap();
	}

	Session session = new Session();
	session.connect(server);
	if (loginUser > 0) {
	    System.out.println("Logging in as user " + loginUser);
	    if (!session.login(loginUser, loginPassword, true))
		System.err.println("Login failed (continuing anyway).");
	}
	while (true) {
	    DynamicSessionInfo[] who = session.whoIsOnDynamic(true, true, 86400);
	    for (int i=0; i < who.length; i++) {
		int conf = 0;
		try {
		    conf = who[i].getWorkingConference();
		    if (conf > 0)
			session.getConfName(conf);
		    
		} catch (RpcFailure ex1) {
		    if (ex1.getError() == Rpc.E_undefined_conference) {
			int person = who[i].getPerson();
			List members = (List) confMap.get(new Integer(conf));
			boolean news = false;
			if (members == null) {
			    news = true;
			    System.out.println("New secret conference found: " + conf);
			    members = new LinkedList();
			    confMap.put(new Integer(conf), members);
			}
			if (!members.contains(new Integer(person))) {
			    news = true;
			    System.out.println("Registering " + person + " as a member of " + conf);
			    members.add(new Integer(person));
			}
			if (news) {
			    System.out.print("saving...");
			    ObjectOutputStream objStream = new ObjectOutputStream(new FileOutputStream(new File(filename)));
			    objStream.writeObject(confMap);
			    objStream.close();
			    System.out.println("done.");
			}
		    } else {
			System.out.println("RPC error: " + ex1.toString());
		    }
		}
	    }
	    try {
		Thread.sleep(sleepTime);
	    } catch (InterruptedException ex1) {
		System.out.println("sleep() interrupted");
	    }
	    
	}
    }   
}
