package nu.dll.app.komwho;
import nu.dll.lyskom.*;
public class KomWho {
    private Session s;
    private String server;
    
    public KomWho (String[] argv) {
	if (argv.length != 1) {
	    System.out.println("Usage: KomWho server");
	    System.exit(-1);
	} else {
	    server = argv[0];
	}
	s = new Session();
	try {
	    s.connect(server);
	    //login(s, "Staffan Test", "hemligt");
	    //System.out.println("logged in to " + server + " just fine");

	    DynamicSessionInfo[] si = s.whoIsOnDynamic(true, false, 1800);

	    for (int i = 0; i < si.length; i++) {

		System.out.print(pad(si[i].session+"", 5) + " ");
		System.out.print(pad(new String(s.getConfName(si[i].person)), 20) + " ");
		System.out.print(pad(new String(s.getConfName(si[i].workingConference)), 20) + " ");
		System.out.print(pad(new String(si[i].whatAmIDoing), 20));
		System.out.println();
	    }
	    System.exit(-1);
	} catch (Exception e) {
	    System.err.println("Caught " + e.getClass().getName());
	    e.printStackTrace();
	}
    }
    public static void main(String[] argv) {
	new KomWho(argv);
    }
    public String pad(String in, int length) {
	if (in.length() > length) {
	    return in.substring(0, length -1);
	} else if (in.length() < length) {
	    int diff = length - in.length() - 1;
	    char[] padding = new char[diff];
	    for (int i = 0; i < diff; i++)
		padding[i] = ' ';
	    return (new StringBuffer(in).append(padding)).toString();
	} else {
	    return in;
	}
    }
}
	    





