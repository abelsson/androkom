package nu.dll.app.weblatte;

import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;
import nu.dll.lyskom.Session;
import nu.dll.lyskom.Debug;

public class SessionWrapper implements HttpSessionBindingListener {
    Session lyskom;
    public SessionWrapper(Session lyskom) {
	this.lyskom = lyskom;
    }

    public void valueBound(HttpSessionBindingEvent event) {
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
	if (event.getName().equals("lyskom")) {
	    lyskom.shutdown();
	}
    }

    public Session getSession() {
	return lyskom;
    }
}
