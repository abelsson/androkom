package nu.dll.app.kombiff;
import nu.dll.lyskom.*;
import java.text.*;
public class KomBiff {
    private Session s;
    private String username;
    private String password;
    private String server;

    // 0 = globalNo
    // 1 = localNo
    // 2 = Author
    // 3 = Subject
    // 4 = No. of rows
    private MessageFormat summary= new MessageFormat("    [{0}] {4} {2}: {3}");

    public KomBiff (String[] argv) {
	if (argv.length != 3) {
	    System.out.println("Usage: KomBiff server username password");
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
	    s.updateUnreads();
 	    Membership[] m = s.getUnreadMembership();
	    TextMapping[] tm = s.getUnreadTexts();
	    for (int i=0; i < m.length; i++) {
		String name = new String(s.getConfName(m[i].conference));
		System.out.println("Confname: " + name + "<"+m[i].conference+">: " + (tm[i].size()));

		while(tm[i].hasMoreElements()) {
		    Text t = s.getText(((Integer)tm[i].nextElement()).intValue());
		    Object[] args = {
			new Integer(t.getNo()),
			new Integer(t.getLocal(m[i].conference)),
			new String(s.getConfName(t.getAuthor())),
			new String(t.getSubject()),
			new Integer(t.getRows())
			    };
		    System.out.println(summary.format(args));
		}
	    }
	    s.logout(true);
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
	System.out.println("Kombiff 2000.nu");
	new KomBiff(argv);
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
		if (!s.login(usernames[0].confNo, password, true)) {
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







