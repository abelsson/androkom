package nu.dll.app.kombot;
import nu.dll.lyskom.*;
import java.text.*;
                               
public class KomBot implements AsynchMessageReceiver {
    private Session s;
    private String username;
    private String password;
    private String server;

    public void asynchMessage(AsynchMessage m) {
	System.out.println("got AsynchMessage: " + m.toString());
	try {
	    s.sendMessage(m.getParameters()[1].intValue(), "Det ska du skita i");
	    System.out.println("sent msg to #" + m.getParameters()[1].intValue());
	} catch (Exception e) {
	    System.err.println("fuckup");
	}
    }

    public KomBot (String[] argv) {
	if (argv.length != 3) {
	    System.out.println("Usage: KomBot server username password");
	    System.exit(-1);
	} else {
	    server = argv[0];
	    username = argv[1];
	    password = argv[2];
	}
	s = new Session();
	try {
	    s.connect(server);
	    login(s, username, password);
	    System.out.println("logged in to " + server + " just fine");
	    s.addAsynchMessageReceiver(this);
	    boolean finish = false;
	    while (!finish) {
		System.out.println("sleeping 10s");
		Thread.sleep(10*1000);
		System.out.println("waking up");
	    }
	    System.out.println("logged out");
	    s.disconnect(false);
	    System.out.println("disconnected");
	    System.exit(0);
	} catch (Exception e) {
	    System.err.println("Caught " + e.getClass().getName());
	    e.printStackTrace();
	}
    }	
    public static void main(String[] argv) {
	System.out.println("Kombot 2002.nu");
	new KomBot(argv);
    }

    void login (Session s, String username, String password) {
	ConfInfo usernames[] = new ConfInfo[0];
	try {
	    usernames = s.lookupName(username, true, false);
	    if (usernames.length != 1) {
		System.out.println("Invalid/ambigious username");
		System.exit(-1);
	    } else {
		// login as hidden 
		if (!s.login(usernames[0].confNo, password, false)) {
		    System.out.println("Login failed");
		    System.exit(-1);
		}
	    }
	} catch (Exception e) {
	    System.err.println("Caught " + e.getClass().getName());
	    e.printStackTrace();
	}
    }
    
}







