package nu.dll.app.weblatte;

import nu.dll.lyskom.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

/**
 * A simple static list of KOM servers.
 *
 * Used to provide the list of servers to be presented at the login screen.
 * The List provided is unmodifiable, making it suitable for iteration without
 * synchronization. The first server in the list is set as default.
 */
public class Servers {
    public static List list  = new LinkedList();
    public static KomServer defaultServer;
    static {
	if (Boolean.getBoolean("weblatte.debug")) list.add(new KomServer("localhost", "RasmusKOM"));
	//list.add(new KomServer("kom.sno.pp.se", "SnoppKOM"));
	list.add(new KomServer("kom.lysator.liu.se", "LysLysKOM"));
	list.add(new KomServer("city.dll.nu", "SnoppKOM"));
	list.add(new KomServer("myskom.kfib.org", "MysKOM"));
	list.add(new KomServer("plutten.dnsalias.org", "PluttenKOM"));
	list.add(new KomServer("kom.stacken.kth.se", "TokKOM"));
	list.add(new KomServer("kom.update.uu.se", "UppKOM"));
	list.add(new KomServer("kom.ludd.luth.se", "LuddKOM"));
	list.add(new KomServer("kom.ds.hj.se", "dskom"));
	list.add(new KomServer("kom.cd.chalmers.se", "CD-KOM"));
	list.add(new KomServer("com.helsinki.fi", "HesaKOM"));
	defaultServer = (KomServer) list.get(0);
	list = Collections.unmodifiableList(list);
    }
}

