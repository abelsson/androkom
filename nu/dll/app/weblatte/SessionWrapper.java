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
