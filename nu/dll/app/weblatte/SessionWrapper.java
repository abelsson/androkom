package nu.dll.app.weblatte;

import java.io.IOException;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;
import nu.dll.lyskom.*;

public class SessionWrapper implements HttpSessionBindingListener {
    Session lyskom;
    boolean suspended = false;
    AsynchMessageReceiver amr = null;
    public SessionWrapper(Session lyskom) {
	this.lyskom = lyskom;
    }

    public void setSuspended(boolean b) {
	suspended = b;
	if (suspended) {
	    amr = new AsynchMessageReceiver() {
		    public void asynchMessage(AsynchMessage m) {
			if (m.getNumber() == Asynch.new_text ||
			    m.getNumber() == Asynch.new_text_old) {

			    lyskom.invokeLater(new Runnable() {
				    public void run() {
					try {
					    KomPreferences prefs =
						(KomPreferences)
						lyskom.getAttribute("weblatte.preferences.weblatte");
					    
					    if (prefs == null) {
						UserArea ua = lyskom.getUserArea();
						Hollerith data = ua.getBlock("weblatte");
					        prefs = new KomPreferences(new HollerithMap(data),
									   "weblatte");
					    }
					    if (prefs.getBoolean("many-memberships")) {
						lyskom.getUnreadConfsList(lyskom.getMyPerson().
									  getNo());
					    } else {
						lyskom.updateUnreads();
					    }
					    lyskom.setAttribute("mbInited", Boolean.TRUE);
					} catch (IOException ex1) {
					    Debug.println("I/O error: " + ex1);
					} catch (RpcFailure ex2) {
					    Debug.println("RPC failed: " + ex2);
					}
				    }
				});

			}
		    }
		};
	    lyskom.addAsynchMessageReceiver(amr);
	} else {
	    if (amr != null) lyskom.removeAsynchMessageReceiver(amr);
	    amr = null;
	}
    }

    public boolean isSuspended() {
	return suspended;
    }

    public void valueBound(HttpSessionBindingEvent event) {
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
	if (event.getName().equals("lyskom") && !suspended) {
	    lyskom.shutdown();
	}
    }

    public Session getSession() {
	return lyskom;
    }
}
