package nu.dll.app.weblatte;

import java.io.IOException;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;
import nu.dll.lyskom.Session;
import nu.dll.lyskom.Debug;
import nu.dll.lyskom.RpcFailure;
import nu.dll.lyskom.AsynchMessageReceiver;
import nu.dll.lyskom.AsynchMessage;
import nu.dll.lyskom.Asynch;

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
					    lyskom.updateUnreads();
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
