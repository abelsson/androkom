package nu.dll.app.weblatte;

import javax.servlet.http.*;
import nu.dll.lyskom.Session;

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
