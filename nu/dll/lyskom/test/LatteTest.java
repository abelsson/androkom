package nu.dll.lyskom.test;

import java.io.IOException;
import junit.framework.*;

import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Random;
import java.util.Iterator;

import java.util.Calendar;
import java.util.GregorianCalendar;


import nu.dll.lyskom.*;

public class LatteTest extends TestCase {
    String testServer = "halosm025";
    int testUser = 27;
    String testPassword = "seven11";
    int testText = 100;
    Random random = new Random();
	

    protected Session session;
    protected Text text;
    
    public LatteTest(String name) {
	super(name);
    }

    protected void setUp() {
	if (session == null) session = startSession(testUser, testPassword);
	try {
	    if (!session.getConnected()) session.connect(testServer, 4894);
	    if (!session.getLoggedIn()) session.login(testUser, testPassword, false);
	    text = session.getText(testText);
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.getMessage());
	}
    }

    protected Session startSession(int user, String password) {
	try {
	    Session s = new Session();
	    s.connect(testServer);
	    s.login(user, password, false);
	    s.changeWhatIAmDoing("TestSession " + s);
	    return s;
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.getMessage());
	}
    }

    public void testChangeName() {
	try {
	    byte[] myname = session.getMyPerson().getUConference().getName();
	    String newName = "LatteTest (åäö) " + random.nextInt(99999);
	    session.changeName(session.getMyPerson().getNo(), newName);
	    assertTrue("ChangeName", Arrays.equals(newName.getBytes(), session.getMyPerson().getUConference().getName()));
	    session.changeName(session.getMyPerson().getNo(), new String(myname));
	    assertTrue("ChangeNameBack", Arrays.equals(myname, session.getMyPerson().getUConference().getName()));
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.getMessage());
	}	
    }

    public void testGetText() {
	try {
	    text = session.getText(testText, true);
	    assertEquals(42, text.getLocal(14));
	    assertEquals(-1, text.getLocal(0));
	    assertNotNull("TextNotNull", text);
	    assertEquals("TextNumber", testText, text.getNo());
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.getMessage());
	}	
    }

    public void testMiscInfo() {
	TextStat ts = text.getStat();
	assertNotNull("TextNotNull", text);
	assertNotNull("TextStatNotNull", ts);
	List miscInfo = ts.getMiscInfo();
	assertTrue("MiscInfoSize", miscInfo.size() > 0);

	Text t = new Text();
	t.addRecipients(new int[] { 4711, 4712, 4713 });
	t.addCcRecipients(new int[] { 5811, 5812, 5813, 5814 });
	assertEquals(3, t.getStat().getMiscInfoSelections(TextStat.miscRecpt).size());
	assertEquals(4, t.getStat().getMiscInfoSelections(TextStat.miscCcRecpt).size());
	List rcptSelections = t.getStat().getMiscInfoSelections(TextStat.miscRecpt);
	for (int i=0; i <  rcptSelections.size(); i++) {
	    Selection s = (Selection) rcptSelections.get(i);
	    s.add(TextStat.miscLocNo, i+10);
	}
	assertEquals(10, t.getLocal(4711));
	assertEquals(11, t.getLocal(4712));
	assertEquals(12, t.getLocal(4713));
	assertEquals(-1, t.getLocal(-1));
    }

    public void testKomTime() {
	KomTime t1 = new KomTime();
	KomTime t2 = new KomTime();
	KomTime t3 = new KomTime(0, 0, 0, 0, 0, 0, 0, 0, 0);
	assertTrue(t1.equals(t2));
	assertTrue(!t1.equals(t3));

	t3 = new KomTime(47, 11, 14, 14, 11, 79, 0, 199, 0);

	Calendar cal = new GregorianCalendar();
	cal.set(Calendar.DST_OFFSET, 0);
	cal.set(Calendar.YEAR, 1979);
	cal.set(Calendar.DAY_OF_YEAR, 200);
	cal.set(Calendar.HOUR_OF_DAY, 14);
	cal.set(Calendar.MINUTE, 11);
	cal.set(Calendar.SECOND, 47);
	cal.set(Calendar.MILLISECOND, 0);
	assertEquals(cal.getTime(), t3.getTime());
    }

    public void testKomToken() {
	KomToken t1 = new KomToken(new byte[] { 'a', 'b', 'c', 'd', 'e', (byte) 0xe5, (byte) 0xe4, (byte) 0xf6 });
	KomToken t2 = new KomToken("abcdeåäö");
	assertEquals(t1, t2);
	assertTrue(Arrays.equals(t1.getContents(), t2.getContents()));
	KomToken t3 = new KomToken("4711");
	assertEquals(4711, t3.intValue());

    }

    public void testReadTextsMap() {
	int count = 1000;
	List mylist = new LinkedList();
	ReadTextsMap map = new ReadTextsMap();
	int starttext = random.nextInt(2000000);
	for (int i=0; i < count; i++) {
	    int no = starttext + i;
	    map.add(no);
	    mylist.add(new Integer(no));
	}
	assertEquals(count, map.count());
	Iterator i = mylist.iterator();
	while (i.hasNext()) {
	    assertTrue(map.exists(((Integer) i.next()).intValue()));
	}
	i = mylist.iterator();
	while (i.hasNext()) {
	    map.remove(((Integer) i.next()).intValue());
	}

	assertEquals(0, map.count());
	
    }
    abstract class TestThread extends Thread {
	Exception exception = null;
	public Exception getLastException() {
	    return exception;
	}
	public Session[] sessions = null;
	int sid = 0;
    }

    public void testWaitConcurrency() {
	int count = 50;
	TestThread[] threads = new TestThread[count*2];
	Session session2 = startSession(testUser, testPassword);
	Session[] _sessions = new Session[] { session, session2 };
	for (int i=0; i < count/2; i++) {
	    threads[i] = new TestThread() {
		    public void run() {
			try {
			    sid++;
			    if (sid > sessions.length) sid = 0;
			    Session s = sessions[sid];
			    s.changeWhatIAmDoing("Test [s[" + sid + "] " + s + "] " + random.nextInt(99999));
			    assertEquals(100, s.getText(100, true).getNo());
			    s.getConfStat(19, true);
			    s.getPersonStat(19, true);
			    s.getUConfStat(19, true);
			} catch (IOException ex1) {
			    exception = ex1;
			}
		    }
		};
	    threads[i].sessions = _sessions;
	    threads[i].setName("TestThread-" + i);
	}

	for (int i=count/2; i < count; i++) {
	    threads[i] = new TestThread() {
		    public void run() {
			try {
			    sid++;
			    if (sid > sessions.length) sid = 0;
			    Session s = sessions[sid];
			    s.changeWhatIAmDoing("Test [s[" + sid + "] " + s + "] " + random.nextInt(99999));
			    s.getPersonStat(19, true);
			    s.getConfStat(19, true);
			    s.getUConfStat(19, true);			    
			    assertEquals(100, s.getText(100, true).getNo());
			} catch (IOException ex1) {
			    exception=ex1;
			}
		    }
		};
	    threads[i].sessions = _sessions;
	    threads[i].setName("TestThread-" + i);
	}

	for (int i=0; i < count; i++) {
	    threads[i].start();
	}
	
	for (int i=0; i < count; i++) {
	    try {
		threads[i].join();
		Debug.println("joined " + threads[i].getName());
		if (threads[i].getLastException() != null) 
		    throw new RuntimeException("Exception in thread " + i + ": " +
					       threads[i].getLastException().getMessage());

	    } catch (InterruptedException ex1) {
		throw new RuntimeException("Interrupted during join(): " + ex1.getMessage());
	    }
	}
	for (int i=0; i < _sessions.length; i++) {
	    try {
		_sessions[i].logout(true);
		_sessions[i].disconnect(true);
	    } catch (IOException ex1) {
		throw new RuntimeException(ex1.getMessage());
	    }
	}
    }
	


    public void testTextClone() {
	Text t2 = (Text) text.clone();
	
	assertEquals(text.getStat().getMiscInfoSelections(TextStat.miscRecpt).size(),
		     t2.getStat().getMiscInfoSelections(TextStat.miscRecpt).size());
	
	assertEquals(text.getStat().getMiscInfoSelections(TextStat.miscCcRecpt).size(),
		     t2.getStat().getMiscInfoSelections(TextStat.miscCcRecpt).size());
	
	assertEquals(text.getStat().getMiscInfoSelections(TextStat.miscBccRecpt).size(),
		     t2.getStat().getMiscInfoSelections(TextStat.miscBccRecpt).size());
	
	assertTrue(Arrays.equals(text.getBody(), t2.getBody()));
	assertTrue(Arrays.equals(text.getSubject(), t2.getSubject()));
	assertEquals(text.getContents().length, t2.getContents().length);
    }

    public void testTextComment() {

    }

    protected void tearDown() {
	try {
	    if (session.getLoggedIn()) session.logout(true);
	    if (session.getConnected()) session.disconnect(true);
	    session = null;
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.getMessage());
	}	
    }
}
