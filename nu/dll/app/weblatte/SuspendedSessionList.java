package nu.dll.app.weblatte;

import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;
import nu.dll.lyskom.Session;
import nu.dll.lyskom.Debug;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

public class SuspendedSessionList extends LinkedList implements HttpSessionBindingListener {
    public void valueBound(HttpSessionBindingEvent event) {}

    public void valueUnbound(HttpSessionBindingEvent event) {
	synchronized (this) {
	    Debug.println("Shutting down all suspended sessions...");
	    for (Iterator i = iterator(); i.hasNext();) {
		((SessionWrapper) i.next()).getSession().shutdown();
	    }
	}
    }

    public boolean add(Object o) {
	if (contains(o)) return false;
	return super.add(o);
    }
}
