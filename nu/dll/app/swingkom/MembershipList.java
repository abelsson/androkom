package nu.dll.app.swingkom;

import nu.dll.lyskom.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;


/** this one might be useful to move to the lyskom package, 
 *  in some variation.
 */
public class MembershipList implements ListModel, Runnable {

    Membership[] list;
    String[] names;
    Vector listeners;
    Session session;

    boolean updatingNames = false;

    public MembershipList(Session session) {
	this.session = session;
	this.list = new Membership[0];
	listeners = new Vector(1);
    }

    public void run() {
	updatingNames = true;
	try {
	    for (int i=0;i<names.length;i++) {
		names[i] = new String(session.getConfName(list[i].conference));
	    }
	    notifyListeners();
	} catch (java.io.IOException ex) {
	    System.err.println("MembershipList.run(): "+ex);
	}
	updatingNames = false;
    }
    public Membership[] getList() {
	return list;
    }

    public void setList(Membership[] list) {
	this.list = list;
	if (names == null)
	    names = new String[list.length];
	if (!updatingNames)
	    (new Thread(this)).start();
	notifyListeners();
    }

    private void notifyListeners() {
	for(Enumeration e = listeners.elements();e.hasMoreElements();) {
	    ListDataListener l = (ListDataListener) e.nextElement();
	    l.contentsChanged(new ListDataEvent(this,
			      ListDataEvent.CONTENTS_CHANGED,
			      0, list.length));
	}
    }

    public Object getElementAt(int index) {
	String confRep = null;
	if (names[index] == null) {
	    confRep = "Conference "+list[index].conference;
	    if (!updatingNames) {
		new Thread(this).start();
	    }
	} else {
	    confRep = names[index] +
		" (#" + list[index].conference + ")";
	}
		
	return confRep;
    }

    public int getSize() {
	return list.length;
    }

    public void addListDataListener(ListDataListener l) {
	listeners.add((Object) l);
    }

    public void removeListDataListener(ListDataListener l) {
	listeners.removeElement((Object) l);
    }
}
