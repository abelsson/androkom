package nu.dll.app.weblatte;

import nu.dll.lyskom.*;
import java.util.List;
import java.util.LinkedList;

public class Servers {
    public static List list  = new LinkedList();
    public static KomServer defaultServer;
    static {
	if (Debug.ENABLED) list.add(defaultServer = new KomServer("localhost", "RasmusKOM"));
	list.add(new KomServer("sno.pp.se", "SnoppKOM"));
	list.add(new KomServer("kom.lysator.liu.se", "LysLysKOM"));
	list.add(new KomServer("plutten.dnsalias.org", "PluttenKOM"));
	list.add(new KomServer("kom.update.uu.se", "UppKOM"));
	defaultServer = (KomServer) list.get(0);
    }
}

