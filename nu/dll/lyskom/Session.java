/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */

package nu.dll.lyskom;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.ref.*;
import java.lang.reflect.*;
/**
 * <p>
 * This is the main interface to the LysKOM server and the LatteKOM library.
 * </p><p>
 * An instance of the Session class represents a single connection to a LysKOM
 * server. Access methods are provided for both asynchronous and synchronous
 * operations with the LysKOM server. 
 * </p><p>
 * All methods starting with <tt>do...</tt> are asynchronous methods that takes
 * care of constructing the RPC call, putting it on the send queue, and then
 * returns immediately. The call will be written to the server by another thread.
 * </p><p>
 * An application may register itself as an <tt>RpcEventListener</tt>, and send
 * commands to the server by calling the <tt>do...()</tt> methods. LatteKOM
 * will then call the the <tt>rpcEvent(RpcEvent)</tt> method when a reply
 * arrives from the server. The application may also use the <tt>waitFor(int)</tt>,
 * which will cause the thread to block until a reply has been received with the
 * supplied reference number (an RPC call's reference is automatically assigned
 * by LatteKOM in the <tt>do...()</tt> methods, and can be retreived by the
 * <tt>getId()</tt> method in an <tt>RpcCall</tt> object).
 * </p><p>
 * LatteKOM also provides synchronous method for all RPC calls. The synchronous
 * methods have the same names as the asynchronous methods, but without the
 * "<tt>do</tt>" prefix. They also add the extra convenience of interpreting
 * the RPC reply and returning objects that are easier to deal with. For example,
 * the <tt>whoIsOnDynamic(boolean, boolean, boolean)</tt> returns an array of
 * <tt>DynamicSessionInfo</tt> objects containing all the information returned
 * by the server. If a synchronous call fails, a <tt>RpcFailure</tt>
 * exception should be thrown containing information about the error that occured.
 * </p><p>
 * Asynchronous messages can be received by clients by registering
 * <tt>AsynchMessageReceiver</tt> objects with the <tt>addAsynchMessageReceiver</tt>
 * method. The objects' <tt>asynchMessage()</tt> method will be called by the network
 * read thread at the moment a message has been received and parsed into an
 * <tt>AsynchMessage</tt> object. As the <tt>asynchMessage()</tt> method will be
 * executed by the thread responsible for reading server messages, no server
 * messages will be read during the execution of the receiver's method. Most notably,
 * the receiving method must not send an RPC call and then call the waitFor() method
 * to wait for the reply (this will cause an <tt>IOException</tt> to be thrown by
 * the waitFor() method).
 * </p>
 * <p>
 * The LatteKOM library keeps caches of texts, conferences, persons and membership
 * information. The caches are refreshed or purged when the server indicates that
 * it might be needed. At the moment, much of that functionality is still in
 * development and might therefore be inefficient, incomplete or buggy (that goes
 * for large parts of LatteKOM in general). 
 * </p>
 * <p>
 * A very simple program to login and read a specific LysKOM text could look like this:
 * <pre>
 *   // Invoke with command: "java SimpleSample serverHost userName password textNo" 
 *   import nu.dll.lyskom.Session;
 *   import nu.dll.lyskom.Text;
 *   Import nu.dll.lyskom.ConfInfo;
 *   class SimpleSample {
 *       public static void main(String[] argv) throws Exception {
 *           Session session = new Session();
 *           session.connect(argv[0], 4894);
 *           System.out.println("Connected.");
 *           ConfInfo[] names = session.lookupName(argv[1], true, false);
 *           session.login(names[0].getNo(), argv[2], false);
 *           System.out.println("Logged in.");
 *
 *           Text t = session.getText(Integer.parseInt(argv[3]));
 *           System.out.println("Retreived text " + argv[3]);
 *           System.out.println("Subject: " + new String(t.getSubject()));
 *           System.out.println("Text body:");
 *           System.out.println(new String(t.getBody()));
 *       }
 *   }
 * </pre>
 * </p>
 *
 * @author rasmus@sno.pp.se
 * @version $Id: Session.java,v 1.87 2004/11/12 03:22:26 pajp Exp $
 * @see nu.dll.lyskom.Session#addRpcEventListener(RpcEventListener)
 * @see nu.dll.lyskom.RpcEvent
 * @see nu.dll.lyskom.RpcCall
 * @see nu.dll.lyskom.RpcFailure
 * @see nu.dll.lyskom.Session#waitFor(int)
 * @see nu.dll.lyskom.Session#addAsynchMessageReceiver(AsynchMessageReceiver)
 * @see nu.dll.lyskom.AsynchMessage
 */


public class Session
implements AsynchMessageReceiver, RpcReplyReceiver, RpcEventListener {

    /**
     * Time-out used by the waitFor() method in milliseconds. Can be set (in 
     * seconds) through the system property "lyskom.rpc-timeout". Default
     * is 30 seconds.
     */
    public static int rpcTimeout;

    /**
     * This is the "soft" timeout, in milliseconds. If larger than zero, the waitFor()
     * call will only block for this long, and then check if a reply has been received
     * even if the receiver thread hasn't notified yet. If none found, it will repeat
     * the process until a reply is found or rpcTimeout has been reached.
     * <br>
     * Default is zero, i.e., no soft timeout will be used, and the waitFor() method
     * will block until it is notified by the receiving thread. It can be set (in 
     * seconds) by the system property "lyskom.rpc-soft-timeout".
     */
    public static int rpcSoftTimeout;

    /**
     * The encoding used by the LysKOM server. All conversions from bytes to String (and vice versa)
     * in server I/O should respect this setting. It can be changed by setting the
     * system property "lyskom.encoding". Default is "iso-8859-1".
     */
    public static String defaultServerEncoding = null;
    
    /**
     * Value of system property <tt>lyskom.big-text</tt> (default false)
     */
    public static boolean defaultEnabledBigText;
    /**
     * Value of system property <tt>lyskom.big-text-limit</tt> (default 20480)
     */
    public static int defaultBigTextLimit;
    /**
     * Value of system property <tt>lyskom.big-text-head</tt> (default 100)
     */
    public static int defaultBigTextHead;
    /**
     * Value of system property <tt>lyskom.lazy-text-limit</tt> (default 80)
     */
    public static int defaultLazyTextLimit;

    // set property values
    static {
	defaultServerEncoding = System.getProperty("lyskom.encoding", "iso-8859-1");
	rpcTimeout = Integer.getInteger("lyskom.rpc-timeout", 60).intValue() * 1000;
	rpcSoftTimeout = Integer.getInteger("lyskom.rpc-soft-timeout", 0).intValue() * 1000;
	defaultBigTextLimit = Integer.getInteger("lyskom.big-text-limit", 150*1024).intValue();
	defaultBigTextHead = Integer.getInteger("lyskom.big-text-head", 100).intValue();
	defaultEnabledBigText = Boolean.getBoolean("lyskom.big-text");
	defaultLazyTextLimit = Integer.getInteger("lyskom.lazy-text-limit", 80).intValue();
    }


    /**
     * Not connected to a LysKOM server
     */
    public final static int STATE_DISCONNECTED = 0;
    /**
     * Connected to a LysKOM server, but not logged in. In this state
     * only a few calls can be made to the server (lookupName(),
     * login(), create(), etc.) */
    public final static int STATE_CONNECTED = 1;
    /**
     * Connected, and logged in as a specific user
     */
    public final static int STATE_LOGIN = 2;

    int loginRpcId;
    boolean loggedIn = false;
    String server = null;
    int port = 4894;

    int rpcCount = 0;
    int lastRpcCall = 0;

    Random random = new Random();


    private int state = STATE_DISCONNECTED;

    Connection connection;
    KomTokenReader reader;
    MessageListener listener;

    AsynchInvoker invoker;

    String password;

    //int wakeOnReplyFrom = -1;

    // Conferences in which we might have unread texts
    List unreads = null;
    List membership = null;
    
    // Membership for the corresponding conferences
    List unreadMembership;
    // Vector for all unread texts. It'd be nice if we could provide a
    // TreeMap iterator to get all unread texts for each conference in
    // comment, depth-first order...
    List unreadTexts;

    boolean prefetch = Boolean.getBoolean("lattekom.enable-prefetch");
    List textPrefetchQueue;
    
    int currentConference = -1;

    TextCache textCache;
    PersonCache personCache;
    ConferenceCache conferenceCache;
    MembershipCache membershipCache;
    TextStatCache textStatCache;
    Map sessionCache;

    Map sessionAttributes = new HashMap();
    
    ReadTextsMap readTexts;
    
    RpcHeap rpcHeap;

    Vector rpcEventListeners;

    Person myPerson = null;
    int myPersonNo = 0;
    int mySessionNo = 0;

    UserArea userArea = null;
    
    boolean connected = false;

    int lastText = 0;

    KomTokenArray[] emptyKomTokens = { };

    String clientHost = null;
    String clientUser = System.getProperty("user.name", "");

    String latteVersion = "$Version$";
    String latteName = "LatteKOM";

    String serverEncoding = defaultServerEncoding;
    boolean enableBigText = defaultEnabledBigText;
    int bigTextLimit = defaultBigTextLimit;
    int bigTextHead = defaultBigTextHead;
    int lazyTextLimit = defaultLazyTextLimit;

    List pendingAsynchMessages = new LinkedList();
    boolean storeAsynchMessages = false;
    
    Map serverInfo = null;

    boolean proto_10_membership = false;

    private void init() {
	textCache = new TextCache();
	personCache = new PersonCache();
	conferenceCache = new ConferenceCache();
	membershipCache = new MembershipCache();
	textStatCache = new TextStatCache();
	sessionCache = new HashMap();
	readTexts = new ReadTextsMap();
	rpcHeap = new RpcHeap();
	rpcEventListeners = new Vector(1);
	mainThread = Thread.currentThread();
	textPrefetchQueue = new LinkedList();
	invoker = new AsynchInvoker();
	invoker.setDaemon(true);
    }

    Thread mainThread;
    public Session() {
	init();
    }

    public void setBigTextEnabled(boolean b) {
	enableBigText = b;
    }

    public void setStoreAsynchMessages(boolean b) {
	storeAsynchMessages = b;
    }

    boolean dontCacheBinaries = Boolean.getBoolean("lattekom.dont-cache-binaries");
    protected boolean isCachableType(String contentType) {
	if (dontCacheBinaries) {
	    if (contentType.startsWith("x-kom/"))
		return true;
	    
	    if (contentType.startsWith("text/"))
		return true;
	    return false;
	} else {
	    return true;
	}
    }

    public void clearCaches() {
	textCache.clear();
	textStatCache.clear();
	membershipCache.clear();
	synchronized (unreads) {
	    unreads.clear();
	}
	conferenceCache.clear();
	sessionCache.clear();
	personCache.clear();
	readTexts.clear();
    }

    /**
     * Removes any evidence of the given text in the caches
     * (Text cache and Text-Stat cache)
     */
    public void purgeTextCache(int textNo) {
	textCache.remove(textNo);
	textStatCache.remove(textNo);
    }

    public void invokeLater(Runnable r) {
	invoker.enqueue(r);
    }

    /**
     * Sets the client host name that is reported during the initial connection
     * handshake with the LysKOM server. This method must be called before
     * <tt>connect()</tt>.
     *
     * @see nu.dll.lyskom.Session#connect(String, int)
     */
    public void setClientHost(String s) {
	clientHost = s;
    }

    /**
     * Sets the client user name that is reported during the intial handshake.
     * Must be called before connect().
     */
    public void setClientUser(String s) {
	clientUser = s;
    }
    

    /**
     * Adds RPC reply listeners. These will be called
     * through the RpcEventListener (rpcEvent()) interface when
     * RPC replies are received.
     *
     * @see nu.dll.lyskom.RpcEventListener
     * @see nu.dll.lyskom.RpcEvent
     */
    public void addRpcEventListener(RpcEventListener l) {
	rpcEventListeners.addElement((Object) l);
    }

    /**
     * Removes an RPC reply listener.
     */
    public void removeRpcEventListener(RpcEventListener l) {
	rpcEventListeners.removeElement((Object) l);
    }

    /**
     * Traverses through the rpcEventListeners vector
     * and calls all registered event listeners.
     *
     * This method could probably be private.
     */    
    protected void notifyRpcEventListeners(RpcEvent ev) {
 	for (Enumeration e = rpcEventListeners.elements();
	     e.hasMoreElements();)
	    ((RpcEventListener) e.nextElement()).rpcEvent(ev);
    }

    /**
     * Disconnects the given session from the server,
     * given appropriate permissions.
     */
    public RpcCall doDisconnect(int sessionNo, boolean discardReply)
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_disconnect).
			    add(new KomToken(sessionNo)), !discardReply);
    }

    /**
     * Disconnects the given session from the server,
     * given appropriate permissions.
     */
    public void disconnect(int sessionNo)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doDisconnect(sessionNo, false));
	if (!reply.getSuccess()) throw reply.getException();
    }

    /**
     * Disconnects from the server
     *
     * @param force (unused) Do not wait for the server to reply to the disconnect command
     */
    public void disconnect(boolean force)
    throws IOException {
	mySessionNo = 0;

	// if we're not connected, then state could never be anything
	// but STATE_DISCONNECTED, right? So we can return without
	// explicitly setting state to that.
	if (!connected) return;
	// remove listeners/recievers
	removeRpcEventListener(this);
	if (listener != null) {
	    listener.setAsynch(false);
	    listener.removeAsynchMessageReceiver(this);
	    listener.removeRpcReplyReceiver(this);
	    listener.disconnect();
	}
	invoker.quit();
	if (connection != null) {
	    connection.close();
	}
	connection = null;
	connected = false;
	state = STATE_DISCONNECTED;
    }

    public void finalize() {
	if (Debug.ENABLED) System.err.println("--> " + this + "::finalize().");
	try {
	    shutdown();
	} catch (Throwable t1) {
	    t1.printStackTrace();
	    System.err.println("Exception in " + this + "::finalize(): " + t1.toString());
	}
	if (Debug.ENABLED) System.err.println("<-- " + this + "::finalize().");
    }

    /**
     * Connect to specified server/port number and do initial handshake
     *
     * @param server The host name of the server
     * @param port Port number to use (normally 4894)
     * @return <tt>true</tt> if the connection was successful
     * @see nu.dll.lyskom.Session#connect(String)
     */     
    public boolean connect(String server, int port)
    throws IOException, ProtocolException {
	this.server = server;
	this.port = port;

	connection = new Connection(this);
	reader = new KomTokenReader(connection.getInputStream(), this);

	byte[] userdata = new Hollerith(clientUser +
					(clientHost != null ? "%" + 
					 clientHost : "")).toNetwork();
	byte[] handshake = new byte[userdata.length+1];
	handshake[0] = (byte) 'A';
	System.arraycopy(userdata, 0, handshake, 1, userdata.length);
    	connection.writeLine(handshake);

	 // "LysKOM\n"
	String serverResponse = new String(reader.readToken().getContents());
	if (!serverResponse.equals("LysKOM"))
	    throw(new KomProtocolException("Bad handshake: "+serverResponse));

	listener = new MessageListener(this);
	listener.addRpcReplyReceiver(this);
	listener.addAsynchMessageReceiver(this);
	listener.setAsynch(true);
	addRpcEventListener(this);
	state = STATE_CONNECTED;
	return connected = true;
    }

    public String toString(byte[] buf) throws UnsupportedEncodingException {
	return new String(buf, getServerEncoding());
    }

    public byte[] toByteArray(String s) throws UnsupportedEncodingException {
	return s.getBytes(getServerEncoding());
    }

    public String getServerEncoding() {
	return serverEncoding;
    }

    public String getCanonicalName()
    throws IOException, RpcFailure {
	Map info = getInfo();
	AuxItem item = AuxItem.getFirst(AuxItem.tagCanonicalName,
					(List) info.get("aux-item-list"));
	return item != null ? item.getDataString() : null;
    }

    public String getServerName()
    throws IOException, RpcFailure {
	String name = getCanonicalName();
	if (name != null) 
	    return name;
	else
	    return getServer() + ":" + getPort();
    }

    public String getServer() {
	return server;
    }

    public int getPort() {
	return port;
    }

    /**
     * Connect to specified server on the default port (4894) and do initial handshake
     *
     * @param server The host name of the server
     * @see nu.dll.lyskom.Session#connect(String, int)
     * @return <tt>true</tt> if the connection was successful
     */
    public boolean connect(String server) 
    throws IOException, ProtocolException {
	return connect(server, port);
    }

    /**
     * Adds a listener for asynchronous messages.
     *
     * At the moment, registered receivers must return quickly, and
     * specifically, may not call the waitFor() method. 
     *
     * Must not be called before connect()
     *
     * @see nu.dll.lyskom.AsynchMessageReceiver
     */
    public void addAsynchMessageReceiver(AsynchMessageReceiver a) {
	listener.addAsynchMessageReceiver(a);
    }

    /**
     * Removes an AsyncMessageReceiver.
     *
     * @see nu.dll.lyskom.Session#addAsynchMessageReceiver(AsynchMessageReceiver)
     */
    public void removeAsynchMessageReceiver(AsynchMessageReceiver a) {
	listener.removeAsynchMessageReceiver(a);
    }

    /**
     * Return true if connected to a LysKOM server.
     */    
    public boolean getConnected() {
	return connected;
    }

    /**
     * Logs on to the LysKOM server and retrieves the user's membership list..
     * LysKOM call: login
     *
     * @param id ID number of the person to log in as
     * @param password corresponding password, will be converted to bytes according to server encoding
     * @param hidden if true, session will not be broadcasted on LysKOM
     *
     */
    public boolean login(int id, String password, boolean hidden)
    throws IOException {
	return login(id, password, hidden, true);
    }


    /**
     * Logs on to the LysKOM server.
     * LysKOM call: login
     *
     * @param id ID number of the person to log in as
     * @param password corresponding password, will be converted to bytes according to server encoding
     * @param hidden if true, session will not be broadcasted on LysKOM
     * @param getMembership if true, the users membership will be queried upon login
     *
     */
    public boolean login(int id, String password, boolean hidden,
			 boolean getMembership) 
    throws IOException {
	return login(id, password.getBytes(serverEncoding), hidden, getMembership);
    }

    public boolean login(String name, String password,
			 boolean hidden, boolean getMembership)
    throws IOException {
	ConfInfo[] names = lookupName(name, true, false);
	if (names.length != 1) return false;

	return login(names[0].getNo(), password, hidden, getMembership);
    }
       

    /**
     * Logs on to the LysKOM server.
     * LysKOM call: login
     *
     * @param id ID number of the person to log in as
     * @param password corresponding password
     * @param hidden if true, session will not be broadcasted on LysKOM
     * @param getMembership if true, the users membership will be queried upon login
     *
     */
    public boolean login(int id, byte[] password, boolean hidden,
			 boolean getMembership)
    throws IOException {
	int rpcid = count();
	if (password == null) throw new IOException("Null password not allowed.");
	RpcCall loginCall = new RpcCall(rpcid, Rpc.C_login).
	    add(new KomToken(id)).add(new Hollerith(password)).
	    add(hidden ? "1" : "0"); // invisibility
	
	writeRpcCall(loginCall);

	RpcReply reply = waitFor(rpcid);
	loggedIn = reply.getSuccess();
	if (loggedIn) {
	    myPerson = getPersonStat(id);
	    myPersonNo = myPerson.getNo();
	    myPerson.uconf = getUConfStat(id);
	    acceptAsynchAll();
	    if (getMembership) {
		membership = getMyMembershipList(false);
	    }
	}
	state = STATE_LOGIN;
	return loggedIn = reply.getSuccess();
    }

    /**
     * Returns the Person object of the currently registered user.
     */
    public Person getMyPerson()
    throws IOException, RpcFailure {
	if (myPerson == null) {
	    myPerson = getPersonStat(myPersonNo, true);
	}
	return myPerson;
    }

    /**
     * Returns the current cached list of unread conferences.
     * If neither getUnreadConfsList() nor updateUnreads() has
     * been called, this list will be empty. 
     *
     */
    public List getUnreadConfsListCached() {
	return unreads;
    }

    /**
     * Returns the current cached Membership of the given conference.
     * If the Membership for the given conference hasn't yet been
     * queries, <tt>null</tt> is returned. Calling updateUnreads()
     * before guarantees that Membership has been queried for all
     * conferences in the Unread-Conferences list returned by 
     * getUnreadConfsList[Cached]().
     *
     */
    public Membership queryReadTextsCached(int confNo) {
	return membershipCache.get(confNo);
    }

    /**
     * Updates the unreadMembership array with Membership objects for
     * conferences that may contain unreads.
     */
    public void updateUnreads()
    throws IOException {
	updateUnreads(null, false, 0);
    }

    public void updateUnreads(boolean getReadTexts, int minPrio)
    throws IOException {
	updateUnreads(null, getReadTexts, minPrio);
    }

    /**
     * Updates the unreadMembership array with Membership objects for
     * conferences that may contain unreads.
     * If the given parameter is non-null, it will be used as the
     * starting point for which conferences are queries, and the
     * same list will be returned by subsequent calls to
     * getUnreadConfsListCached(). If null, getUnreadConfsList()
     * will be called to retrieve that data instead.
     */
    public void updateUnreads(List _unreads, boolean getReadTexts, int minPrio)
    throws IOException {
	Debug.println("--> updateUnreads()");
	if (membership == null) getMyMembershipList(getReadTexts);
	int persNo = myPerson.getNo();
	if (_unreads == null) {
	    getUnreadConfsList(persNo, true);
	} else {
	    unreads = _unreads;
	}
	
	unreadMembership = new LinkedList();
	unreadTexts = new LinkedList();

        // first, bulkly send all get-uconf-stat and query-read-texts
	// calls to the server

	List pendingCalls = new LinkedList();
	synchronized (unreads) {
	    for (Iterator i = unreads.iterator(); i.hasNext();) {
		int conf = ((Integer) i.next()).intValue();
		pendingCalls.add(new Integer(doGetUConfStat(conf).getId()));
		pendingCalls.add(new Integer(doQueryReadTexts(persNo, conf).getId()));
	    }
	}

	// then just wait for them all to finish and put their results
	// into the caches
	while (pendingCalls.size() > 0) {
	    RpcCall rc = waitForCall(pendingCalls);
	    if (rc.getOp() == Rpc.C_get_uconf_stat) {
		conferenceCache.add(new UConference(rc.getParameter(0).intValue(), 
						    rc.getReply().getParameters()));
	    } else if (rc.getOp() == Rpc.C_query_read_texts ||
		       rc.getOp() == Rpc.C_query_read_texts_10) {
		membershipCache.add(Membership.createFrom(0, rc.getReply().getParameters(),
							  rc.getOp() == Rpc.C_query_read_texts_10));
	    } else {
		throw new RuntimeException("Unexpected RPC reply " + 
					   rc.getOp());
	    }

	    pendingCalls.remove(new Integer(rc.getId()));
	}

	// by now, we know for sure that the caches are filled with 
	// fresh data about all the conferences we are interested in.
	// theoretically, assuming the caches aren't purged, the
	// remaining code should never result in another server call.
	synchronized (unreads) {
	    List unreadsCopy = new LinkedList();
	    unreadsCopy.addAll(unreads);
	    unreads.clear();
	    for (int i=0; i < unreadsCopy.size(); i++) {
		int conf = ((Integer) unreadsCopy.get(i)).intValue();
		Membership m = queryReadTexts(persNo, conf);
		if (m.getPriority() < minPrio) {
		    Debug.println("updateUnreads(): skipping conf " +
				  conf + " with prio " +
				  m.getPriority());
		    continue;
		}
		
		int possibleUnreads = getUConfStat(m.getNo()).getHighestLocalNo() - m.getLastTextRead();
		possibleUnreads -= m.getReadTexts().length;
		
		if (possibleUnreads > 0 ) {
		    unreads.add(new Integer(conf));
		    unreadMembership.add(m);
		} else {
		    Debug.println("updateUnreads(): skipping empty conf " + conf);
		}
	    }
	}
	Debug.println("<-- updateUnreads()");
    }


    /**
     * Marks the text as read in all recipient conferences which the
     * user is a member of.
     *
     */
    // note: a better idea is to queue the text numbers up and then
    // send them in batches to the server with mark-as-read.
    public void markAsRead(int textNo) throws IOException, RpcFailure {
	TextStat stat = getTextStat(textNo, true);
	Debug.println("markAsRead(" + textNo + "): text-stat is " + stat);
	List recipientSelections = new LinkedList();
	List recipientNumbers = new LinkedList();
	int[] tags = { TextStat.miscRecpt, TextStat.miscCcRecpt, TextStat.miscBccRecpt };
	for (int i=0; i < tags.length; i++) {
	    recipientSelections.addAll(stat.getMiscInfoSelections(tags[i]));
	}
	
	Iterator recipientIterator = recipientSelections.iterator();
	while (recipientIterator.hasNext()) {	    
	    Selection selection = (Selection) recipientIterator.next();
	    int rcpt = 0;
	    for (int i=0; i < tags.length; i++) {
		if (selection.contains(tags[i])) 
		    rcpt = selection.getIntValue(tags[i]);		    
	    }
	    if (rcpt > 0 && isMemberOf(rcpt)) {
		int local = selection.getIntValue(TextStat.miscLocNo);
		Debug.println("markAsRead: global " + textNo + " rcpt " + rcpt + " local " + local);
		markAsRead(rcpt, new int[] { local });
	    }
	}
	// add the text to the ReadTextsMap
	getReadTexts().add(textNo);
    }


    /**
     * Returns the current conference (as entered by changeConference())
     */
    public int getCurrentConference() {
	return currentConference;
    }

    /**
     * Returns the conference number of the next conference that _may_ contain
     * an unread text, or -1 if no unread conference is found.
     *
     * @param change if true, this method also calls changeConference()
     */
    public int nextUnreadConference(boolean change)
    throws IOException {
	int oldCurrent = currentConference;

	if (unreads.size() == 0) return -1;
	Iterator iter = unreads.iterator();
	while (iter.hasNext()) {
	    int conf = ((Integer) iter.next()).intValue();
	    if (currentConference == conf) {
		if (iter.hasNext()) {
		    int nextConf = ((Integer) iter.next()).intValue();
		    
		    if (change) {
			changeConference(nextConf);
			// keep changing until nextUnreadText() returns a text
			// or unreads is empty
			if (nextUnreadText(false) == -1) {
			    unreads.remove(new Integer(currentConference));
			    return nextUnreadConference(true);
			}
			return currentConference = nextConf;			
		    }
		}
	    }
	}
	if (change) {
	    changeConference(((Integer) unreads.get(0)).intValue());
	    if (oldCurrent == currentConference) // prolly buggy
		return -1;
	    return currentConference;
	} else {
	    return ((Integer) unreads.get(0)).intValue();
	}
    }

    public int nextUnreadText(int conference, boolean updateUnread)
    throws IOException {
	List list = nextUnreadTexts(conference, updateUnread, 1);
	if (list.size() == 0) return -1;
	return ((Integer) list.get(0)).intValue();
    }

    /**
     * Returns the global text number of the next unread text in the
     * current conference. Returns -1 if there are no unread texts.
     *
     * Implementation note: still inefficient.
     *
     *
     * @param updateUnread if true, also marks the returned text as read
     * @see nu.dll.lyskom.Session#nextUnreadText(boolean)
     */   
    public List nextUnreadTexts(int conference, boolean updateUnread, int maxTexts)
    throws IOException {
	return nextUnreadTexts(conference, updateUnread, maxTexts, true);
    }
    public List nextUnreadTexts(int conference, boolean updateUnread, int maxTexts, boolean modifyUnreadList)
    throws IOException {
	if (unreads.size() == 0) return new LinkedList();
	
	UConference c = getUConfStat(conference);
	Membership m = queryReadTexts(myPerson.getNo(), conference, true);
	if (c.getHighestLocalNo() > m.getLastTextRead()) {
	    int localNo = m.getLastTextRead() + 1;	    
	    TextMapping tm = localToGlobal(conference, localNo, maxTexts);

	    if (!tm.hasMoreElements()) {
		if (modifyUnreadList) {
		    synchronized (unreads) {
			unreads.remove(new Integer(conference));
		    }
		    setLastRead(conference, c.getHighestLocalNo());
		}
		return new LinkedList();
	    }
	    List returnList = new LinkedList();
	    while (tm.hasMoreElements()) {
		Integer txtNoObj = (Integer) tm.nextElement();
		int txtNo = txtNoObj.intValue();
		if (m.isRead(tm.local()) || readTexts.contains(txtNo))
		    continue;

		returnList.add(txtNoObj);
	    }

	    if (returnList.size() == 0) {
		if (modifyUnreadList) {
		    synchronized (unreads) {
			unreads.remove(new Integer(conference));
		    }
		    setLastRead(conference, c.getHighestLocalNo());
		}
		Debug.println("no unread texts found");
		return new LinkedList();
	    }
	    
	    if (updateUnread) {
		// XXX: should be optimized for lesser server calls
		// through mark-as-read grouping
		for (Iterator i = returnList.iterator();i.hasNext();) {
		    int[] ur = { tm.localToGlobal(((Integer) i.next()).intValue()) };
		    markAsRead(conference, ur);
		}
	    }
	    lastText = ((Integer) returnList.get(returnList.size()-1)).intValue();
	    return returnList;
	} else {
	    if (modifyUnreadList) unreads.remove(new Integer(conference));
	    return new LinkedList();
	}
    }

    /** 
     * Same as <tt>nextUnreadText(session.getCurrentConference(), <i>updateUnread</i>)</tt>.
     *
     * @param updateUnread if true, marks the returned text as unread
     * @see nu.dll.lyskom.Session#nextUnreadText(int, boolean)
     */
    public int nextUnreadText(boolean updateUnread)
    throws IOException {
	if (currentConference == -1) {
	    return -1;
	}

	return nextUnreadText(currentConference, updateUnread);

    }
    public boolean isMemberOf(int confNo) throws IOException {
	return isMemberOf(confNo, true);
    }

    /**
     * Returns <tt>true</tt> if the current user is a member of confNo,
     * otherwise <tt>false</tt>.
     *
     * If the membership list hasn't been read from the server, it will
     * be fetched if "fetchMembership" is true. If it is false,
     * this method will return <tt>false</tt>.
     *
     * @param confNo Conference number
     */
    public boolean isMemberOf(int confNo, boolean fetchMembership) throws IOException {
	if (membership == null) {
	    if (fetchMembership) {
		Debug.println("Warning: isMemberOf(" + confNo + ") " +
			      "called before membership has been queried " +
			      "from server (querying now)");
		getMyMembershipList(false);
	    } else {
		return false;
	    }
	}
	Iterator i = membership.iterator();
	while (i.hasNext()){
	    Membership m = (Membership) i.next();
	    if (m.getNo() == confNo) {
		return !m.getType().getBitAt(MembershipType.passive);
	    }
	}
	return false;
    }

    /**
     * Returns the highest possible number of unreads for a specific conference.
     *
     * @param confNo Conference number
     */
    public int getUnreadCount(int confNo)
    throws IOException {
	if (unreads.size() == 0 || !unreads.contains(new Integer(confNo)))
	    return 0;	

	UConference c = getUConfStat(confNo);
	Membership m = queryReadTexts(myPerson.getNo(), confNo, true);
	if (c.getHighestLocalNo() > m.getLastTextRead()) {
	    return c.getHighestLocalNo() - m.getLastTextRead();
	} else {
	    return 0;
	}
    }

    /**
     * Returns a ReadTextsMap object.
     * The ReadTextsMap is a set containing all texts read during this session.
     */
    public ReadTextsMap getReadTexts() { return readTexts; }

    /**
     * Marks a text as read on the LysKOM server. Note that
     * the text number is local.
     *
     * @param confNo conference number
     * @param localTextNo local text number
     *
     * LysKOM call: mark-as-read
     */
    public void markAsRead(int confNo, int[] localTextNo)
    throws IOException {

	RpcReply r = waitFor(doMarkAsRead(confNo, localTextNo).getId());
	if (!r.getSuccess()) throw r.getException();
	for (int i=0; i < localTextNo.length; i++) {
	    // update caches
	    Membership ms = queryReadTextsCached(confNo);
	    if (ms != null) {
		ms.markAsRead(localTextNo[i]);
	    }
	    UConference cuconf = conferenceCache.getUConference(confNo);
	    if (cuconf != null) {
		if (localTextNo[i] > cuconf.getHighestLocalNo()) {
		    cuconf.setHighestLocalNo(localTextNo[i]);
		}
	    }
	    Debug.println("marked local " + localTextNo[i] + " in conf " + confNo + " as read");
	}
    }

    /**
     * Returns the client version (as a byte array) for a given session. Returns a zero length
     * array of the client has no version string set.
     *
     * @param sessionNo The session for which to retreive the client version. 
     * @see nu.dll.lyskom.Session#getClientName(int)
     */
    public byte[] getClientVersion(int sessionNo)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doGetClientVersion(sessionNo));
	if (!reply.getSuccess()) throw reply.getException();
	return reply.getParameters()[0].getContents();
    }

    /**
     * Sends the RPC call get-client-version to the server.
     *
     * @param sessionNo The session for which to retreive the client version. 
     * @see nu.dll.lyskom.Session#getClientVersion(int)
     */
    public RpcCall doGetClientVersion(int sessionNo) 
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_get_client_version).
	    add(new KomToken(sessionNo)));
    }

    /**
     * Returns the client name (as a byte array) for a given session. Returns a zero length
     * array of the client has no name string set.
     *
     * @param sessionNo The session for which to retreive the client name. 
     * @see nu.dll.lyskom.Session#getClientVersion(int)
     */
    public byte[] getClientName(int sessionNo)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doGetClientName(sessionNo));
	if (!reply.getSuccess()) throw reply.getException();
	return reply.getParameters()[0].getContents();
    }

    /**
     * Sends the RPC call get-client-name to the server.
     * @param sessionNo The session for which to retreive the client name.
     * @see nu.dll.lyskom.Session#getClientName(int)
     */
    public RpcCall doGetClientName(int sessionNo)
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_get_client_name).
	    add(new KomToken(sessionNo)));
    }

    /**
     * Returns a Map containing server-information.
     *
     * The keys in the returned map corresponds to the fields specified
     * in the `Info' data type in the LysKOM protocol A specification.
     *
     * All values in the map are stores as KomToken object, with the
     * exception of "aux-item-list", which is a List of AuxItem objects.
     *
     * This information is persistently cached for the duration of the
     * session.
     *
     */
    public Map getInfo()
    throws IOException, RpcFailure {
	if (serverInfo != null) return serverInfo;

	RpcReply reply = waitFor(doGetInfo());
	if (!reply.getSuccess()) throw reply.getException();
	KomToken[] data = reply.getParameters();
	
	Map info = new HashMap();
	int offset = 0;
	info.put("version", data[offset++]);
	info.put("conf-pres-conf", data[offset++]);
	info.put("pers-pres-conf", data[offset++]);
	info.put("motd-conf", data[offset++]);
	info.put("kom-news-conf", data[offset++]);
	info.put("motd-of-lyskom", data[offset++]);
	int auxListLength = data[offset++].intValue();
	List auxItems = new LinkedList();
	for (int i=0; i < auxListLength; i++) {
	    AuxItem item = new AuxItem(((KomTokenArray) data[offset]).getTokens());
	    if (Debug.ENABLED) Debug.println("Server aux-item: " + item);
	    auxItems.add(item);
	    offset += AuxItem.ITEM_SIZE;
	}
	info.put("aux-item-list", auxItems);

	return serverInfo = Collections.unmodifiableMap(info);
    }

    

    public List getAllowedContentTypes()
    throws IOException, RpcFailure {
	List auxItems = (List) getInfo().get("aux-item-list");
	List allowedContentTypes = new LinkedList();
	for (Iterator i = auxItems.iterator(); i.hasNext();) {
	    AuxItem item = (AuxItem) i.next();
	    if (item.getTag() == AuxItem.tagAllowedContentType) {
		String data = item.getDataString();
		allowedContentTypes.add(data.substring(data.indexOf(" ")+1));
	    }
	}
	return allowedContentTypes;
    }
    
    public RpcCall doGetInfo()
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_get_info));
    }


    /**
     * Returns static session information for a given session number. This is guaranteed by
     * the LysKOM protocol to never change for a session number during a server's life time.
     *
     * @param sessionNo The session for which to retreive information
     * @return A <tt>SessionInfo</tt> object containing static session information
     * @see nu.dll.lyskom.SessionInfo
     */
    public SessionInfo getStaticSessionInfo(int sessionNo)
    throws IOException, RpcFailure {
	SessionInfo session;
	synchronized (sessionCache) {
	    session = (SessionInfo) sessionCache.get(new Integer(sessionNo));
	    if (session != null) return session;
	}
	RpcReply reply = waitFor(doGetStaticSessionInfo(sessionNo));
	if (!reply.getSuccess()) throw reply.getException();

	session = new SessionInfo(0, reply.getParameters());
	synchronized (sessionCache) {
	    sessionCache.put(new Integer(sessionNo), session);
	}
	return session;
    }

    /**
     * Sends the RPC call get-static-session-info to the server.
     *
     * @param sessionNo The session for which to retreive information
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doGetStaticSessionInfo(int sessionNo)
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_get_static_session_info).
	    add(new KomToken(sessionNo)));
    }

    /**
     * Sets client name and version, prepending the supplied strings with
     * a slash and the name and version of the LatteKOM library, respectively.
     * The resulting name and version would then be something like
     * <tt>"LatteKOM/Swing"</tt>, <tt>"0.1/0.9"</tt>
     *
     * @param clientName The name of the LysKOM client
     * @param clientVersion The version of the LysKOM client
     * @see nu.dll.lyskom.Session#setClientVersion(String, String)
     */

    public void setLatteVersion(String clientName, String clientVersion)
    throws IOException, RpcFailure {
	setClientVersion(latteName + "/" + clientName, latteVersion + "/" + clientVersion);
    }

    /**
     * Sets the name used for the Aux-Item creating-software and elsewhere.
     */
    public void setLatteName(String clientName) {
	latteName = clientName;
    }

    /**
     * Reports the name and version of this client to the server. These can
     * be retreived by other clients by the calls <tt>get-client-name</tt>
     * and <tt>get-client-version</tt>. I recommend using the 
     * <tt>setLatteVersion(String, String)</tt> method instead, to also
     * report name and version of the LatteKOM library.
     *
     * @param clientName The name of the LysKOM client
     * @param clientVersion The version of the LysKOM client
     * @see nu.dll.lyskom.Session#setLatteVersion(String, String)
     */
    public void setClientVersion(String clientName, String clientVersion)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doSetClientVersion(clientName, clientVersion));
	if (!reply.getSuccess()) throw reply.getException();
    }

    /**
     * Sends the RPC call set-client-version to the server.
     *
     * @param clientName The name of the LysKOM client
     * @param clientVersion The version of the LysKOM client
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doSetClientVersion(String clientName, String clientVersion)
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_set_client_version).
	    add(new Hollerith(clientName)).add(new Hollerith(clientVersion)));
    }

    /**
     * Sends the RPC call mark-as-read to the server.
     *
     * @param confNo the conference in which to mark texts as read
     * @param localTextNo an <tt>int</tt> array containing the local text numbers to mark as read
     * @return An RpcCall object representing this specific RPC call
     */
    public synchronized RpcCall doMarkAsRead(int confNo, int[] localTextNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_mark_as_read);
	req.add(new KomToken(confNo)).add(new KomTokenArray(localTextNo.length, localTextNo));
	writeRpcCall(req);
	return req;
    }
    
    public synchronized RpcCall doGetTime()
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_time);
        writeRpcCall(req);
	return req;
    }

    public KomTime getTime()
    throws IOException {
	RpcReply reply = waitFor(doGetTime());
	return KomTime.createFrom(0, reply.getParameters());
    }


    /**
     * Returns a Text object corresponding to the specified global
     * text number. If a cached copy of the text exists, it will be returned
     * instead.
     *
     * @param textNo Global text number
     * @see nu.dll.lyskom.Session#getText(int, boolean)
     */
    public Text getText(int textNo) 
    throws IOException, RpcFailure { 
	return getText(textNo, false);
    }

    /**
     * Returns a Text object corresponding to the specified global
     * text number.
     *
     * @param textNo Global text number
     * @param refreshCache If <tt>true</tt>, this call will never return a cached copy of a text.
     * @see nu.dll.lyskom.Session#getText(int)
     *
     */
    public synchronized Text getText(int textNo, boolean refreshCache)
    throws IOException, RpcFailure { 
	return getText(textNo, refreshCache, true);
    }

    /**
     * Returns a Text object corresponding to the specified global
     * text number.
     *
     * @param textNo Global text number
     * @param refreshCache If <tt>true</tt>, this call will never return a cached copy of a text.
     * @param useLazyText If <tt>true</tt> the text contents exceeding the lazy-text-limit will not be read until getContents() is called
     * @see nu.dll.lyskom.Session#getText(int)
     *
     */
    public synchronized Text getText(int textNo, boolean refreshCache, boolean useLazyText)
    throws IOException, RpcFailure { 
	if (textNo == 0) throw new RuntimeException("attempt to retreive text zero");

	Debug.println("** getText(): getting text " + textNo + "; refreshCache: " + refreshCache);

	Text text = refreshCache ? null : textCache.get(textNo);
	if (text != null) {
	    text.setCached(true);
	    Debug.println("** getText(): Returning cached text " + textNo);
	    return text;
	}

	TextStat textStat = getTextStat(textNo, refreshCache);
	if (textStat == null) return null;
	if (refreshCache || !textStatCache.contains(textNo)) textStatCache.add(text.getStat());

	boolean textIsBig = textStat.getSize() > bigTextLimit;
	int contentLimit = textStat.getSize();

	if (enableBigText && textIsBig) {
	    Debug.println("Creating a BigText");
	    text = new BigText(this, textNo);
	    contentLimit = bigTextHead;
	} else {
	    if (useLazyText) {
		Debug.println("Creating a LazyText");
		text = new LazyText(this, textNo);
		contentLimit = textStat.getSize() > lazyTextLimit ? lazyTextLimit : textStat.getSize();
	    } else {
		text = new Text(textNo);
	    }
	}
	text.setStat(textStat);
	
	RpcCall textReq = new RpcCall(count(), Rpc.C_get_text).
	    add(new KomToken(textNo)).add("0").
	    add(new KomToken(contentLimit));

	writeRpcCall(textReq);
	
	RpcReply reply = waitFor(textReq.getId());
	if (!reply.getSuccess()) throw reply.getException();

	text.setContents(reply.getParameters()[0].getContents());
	if (isCachableType(text.getContentType())) {
	    textCache.add(text);
	} else {
	    if (Debug.ENABLED) {
		Debug.println("Not caching " + text + " (" + text.getContentType() + ")");
	    }
	}
	return text;
    }

    /**
     * Returns a text's contents as a HollerithStream.
     *
     * @see nu.dll.lyskom.HollerithStream
     */
    public HollerithStream getTextStream(int textNo, int startChar, int endChar)
    throws IOException, RpcFailure { 
	int rpcId = count();
	listener.addHollerithStreamReceiver(rpcId, 0);

	RpcCall textReq = new RpcCall(rpcId, Rpc.C_get_text).
	    add(new KomToken(textNo)).add(new KomToken(startChar)).
	    add(new KomToken(endChar));

	writeRpcCall(textReq);
	
	RpcReply reply = waitFor(textReq.getId());
	if (!reply.getSuccess()) throw reply.getException();
	
	return (HollerithStream) reply.getParameters()[0];
    }

    /**
     * Sends the RPC call mark-text to the server.
     *
     * @param textNo the text number to mark
     * @param markType the type of mark to set
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doMarkText(int textNo, int markType)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_mark_text);
	req.add(new KomToken(textNo));
	req.add(new KomToken(markType));
	writeRpcCall(req);
	return req;
    }

    /**
     * Place a personal mark on a LysKOM text.
     * Blocks until the server has replied.
     *
     * @param textNo the text number to mark
     * @param markType the type of mark to set
     */
    public void markText(int textNo, int markType)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doMarkText(textNo, markType));
	if (!reply.getSuccess()) throw reply.getException();
    }

    /**
     * Sends the RPC call mark-text to the server.
     *
     * @param textNo the text number to unmark
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doUnmarkText(int textNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_unmark_text);
	req.add(new KomToken(textNo));
	writeRpcCall(req);
	return req;
    }

    /**
     * Unmarks a text.
     *
     * @param textNo the text number to unmark
     */
    public void unmarkText(int textNo) 
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doUnmarkText(textNo));
	if (!reply.getSuccess()) throw reply.getException();
    }


    /**
     * Sends the RPC call get-marks to the server.
     *
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doGetMarks()
    throws IOException, RpcFailure {
	RpcCall call = new RpcCall(count(), Rpc.C_get_marks);
	writeRpcCall(call);
	return call;
    }

    /**
     * Returns an array of Mark objects with all text-marks for this user.
     */
    public Mark[] getMarks()
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doGetMarks());
	if (!reply.getSuccess()) throw reply.getException();
	KomToken[] parameters = reply.getParameters();
	int numMarks = parameters[0].intValue();
	Debug.println("numMarks: " + numMarks);
	Mark[] marks = new Mark[numMarks];
	KomToken[] array = ((KomTokenArray) parameters[1]).getTokens();
	Debug.println("mark array length: " + array.length);
	int j=0, i=0;
	while (j < array.length) {
	    marks[i++] = new Mark(array[j++].intValue(), array[j++].intValue());
	}
	return marks;

    }

    /**
     * Sends the RPC call local-to-global to the server.
     *
     * @param confNo The conference number in which to map text numbers
     * @param firstLocalNo The first local number to map
     * @param noOfExistingTexts The maximum number of texts that the returned mappnig should contain
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doLocalToGlobal(int confNo, int firstLocalNo,
						int noOfExistingTexts)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_local_to_global);
	req.add(new KomToken(confNo)).add(new KomToken(firstLocalNo)).
	    add(new KomToken(noOfExistingTexts));
	writeRpcCall(req);
	return req;
    }

    /**
     * Returns a TextMapping that can be used to convert local text
     * number in a conference to global text numbers.
     *
     * See the "<tt>local-to-global</tt>" RPC call and the "<tt>Text-Mapping</tt>" data
     * structure in the LysKOM specification for more information.
     *
     * LysKOM call: local-to-global, but without the limitation that
     * noOfExistingTexts must be between 1-255 inclusive
     *
     * @param confNo The conference number in which to map text numbers
     * @param firstLocalNo The first local number to map
     * @param noOfExistingTexts The maximum number of texts that the returned mapping should contain
     * @see nu.dll.lyskom.Session#doLocalToGlobal(int, int, int)
     */
    Map ltgCache = new HashMap();
    public  TextMapping localToGlobal(int confNo, int firstLocalNo,
						  int noOfExistingTexts)
    throws IOException, RpcFailure {
	if (firstLocalNo == 0)
	    throw new IllegalArgumentException("First local text number cannot be zero.");
	String key = confNo + "-" + firstLocalNo + "-" + noOfExistingTexts;
	Reference ref;
	TextMapping m;
	synchronized (ltgCache) {
	    ref = (Reference) ltgCache.get(key);
	    m = (TextMapping) (ref != null ? ref.get() : null);
	}
	
	if (m != null) {
	    Debug.println("returning cached TextMapping " + m);
	    m.first();
	    return m;
	}

	m = new TextMapping();

	int offset = 0;
	int existingTextsLeft;
	//if noOfExistingTexts is larger than 255, break up in several calls
	// this code could probably be a lot more legible
	do {
	    existingTextsLeft = noOfExistingTexts - offset;
	    RpcReply r = waitFor(doLocalToGlobal(confNo, firstLocalNo+offset,
						 ((existingTextsLeft > 255) ? 255 : existingTextsLeft)).getId());
	    if (!r.getSuccess()) throw new RpcFailure(r, "in localToGloal(" + confNo + 
						      ", " + firstLocalNo + ", " + noOfExistingTexts + ")");

	    m.update(0, r.getParameters(), false);
	    offset += 255; 
	} while ((noOfExistingTexts - offset) > 0);

	synchronized (ltgCache) {
	    ltgCache.put(key, new WeakReference(m));
	}
	return m;
		 
    }


    /**
     * Sends the RPC call query-read-texts to the server.
     *
     * @param persNo The person number for which to query information
     * @param confNo The conference number in which to query information about read texts
     * @return An RpcCall object representing this specific RPC call
     */
    public  RpcCall doQueryReadTexts(int persNo, int confNo)
    throws IOException {
	RpcCall req;
	if (!proto_10_membership) {
	    req = new RpcCall(count(), Rpc.C_query_read_texts);
	    req.add(new KomToken(persNo)).add(new KomToken(confNo));
	    req.add(new KomToken(true)).add(new KomToken(0));
	} else {
	    req = new RpcCall(count(), Rpc.C_query_read_texts_10);
	    req.add(new KomToken(persNo)).add(new KomToken(confNo));
	}
	writeRpcCall(req);
	return req;
    }

    /**
     * Returns a Membership object containing information about read/unread texts
     * for a given person in a given conference.
     *
     * @param persNo The person number for which to query information
     * @param confNo The conference number in which to query information about read texts
     *
     * @see nu.dll.lyskom.Membership
     * @see nu.dll.lyskom.Session#queryReadTexts(int, int, boolean)
     */
    public Membership queryReadTexts(int persNo, int confNo)
    throws IOException {
	return queryReadTexts(persNo, confNo, false);
    }

    /**
     * Returns a Membership object containing information about read/unread texts
     * for a given person in a given conference.
     *
     * @param persNo The person number for which to query information
     * @param confNo The conference number in which to query information about read texts
     * @param refresh If <tt>true</tt>, the membership cache will be refreshed
     * @see nu.dll.lyskom.Membership
     * @see nu.dll.lyskom.Session#queryReadTexts(int, int, boolean)
     */
    public Membership queryReadTexts(int persNo, int confNo,
				     boolean refresh)
    throws IOException {
	Membership m = null;
	if (!refresh && persNo == myPerson.getNo()) {
	    m = membershipCache.get(confNo);
	    if (m != null && m.hasReadTexts()) {
		return m;
	    }
	}
	RpcCall call = waitForCall(doQueryReadTexts(persNo, confNo));
	RpcReply reply = call.getReply();
	if (!reply.getSuccess()) throw reply.getException();

	if (call.getOp() == Rpc.C_query_read_texts ||
	    call.getOp() == Rpc.C_query_read_texts_10) {
	    m = Membership.createFrom(0, reply.getParameters(),
				      call.getOp() == Rpc.C_query_read_texts_10);
	}

	if (persNo == myPerson.getNo()) membershipCache.add(m);
	return m;
    }

    /**
     * Sends the RPC call get-unread-confs to the server.
     *
     * @param persNo The person number for which to query information
     * @return An RpcCall object representing this specific RPC call
     * @see nu.dll.lyskom.Session#getUnreadConfsList(int)
     * @see nu.dll.lyskom.Session#getUnreadConfs(int)
     */
    public  RpcCall doGetUnreadConfs(int persNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_unread_confs).
	    add(new KomToken(persNo));
	writeRpcCall(req);
	return req;
    }

    /**
     * Returns a List containing all conferences which may contain unread
     * texts.
     *
     * @param persNo The person number for which to query information
     * @see nu.dll.lyskom.Session#getUnreadConfs(int)
     */
    public List getUnreadConfsList(int persNo)
    throws IOException {
	return getUnreadConfsList(persNo, false);
    }

    public List getMyUnreadConfsList() 
    throws IOException {
	return getMyUnreadConfsList(false);
    }

    public List getMyUnreadConfsList(boolean askServer) 
    throws IOException {
	return getUnreadConfsList(myPersonNo, askServer);
    }

    public List getUnreadConfsList(int persNo, boolean askServer)
    throws IOException {
	if (persNo == myPersonNo && !askServer && unreads != null) {
	    return unreads;
	}
	LinkedList confList = new LinkedList();
	int[] confs = getUnreadConfs(persNo);
	for (int i=0; i < confs.length; i++) confList.add(new Integer(confs[i]));
	if (persNo == myPersonNo) unreads = confList;
	return confList;
    }

    /**
     * Returns an <tt>int</tt> array containing all conferences which may contain unread
     * texts.
     *
     * @param persNo The person number for which to query information
     * @see nu.dll.lyskom.Session#getUnreadConfsList(int)
     */
    public  int[] getUnreadConfs(int persNo)
    throws IOException {
	KomToken[] parameters = waitFor(doGetUnreadConfs(persNo).getId()).
	    getParameters();
	KomToken[] confs = ((KomTokenArray) parameters[1]).getTokens();
	int[] iconfs = new int[confs.length];
	for (int i=0; i<iconfs.length; i++)
	    iconfs[i] = confs[i].intValue();
	return iconfs;
    }
	
	    
    /**
     * Sends the RPC call get-membership to the server.
     *
     * @param persNo The person number for which to query information
     * @param first The first membership position in list to retreive
     * @param no The number of conferences to retreive
     * @param wantReadTexts If true, the server will return a read-texts array with the membership information
     * @return An RpcCall object representing this specific RPC call
     */
    public  RpcCall doGetMembership(int persNo, int first,
				    int no, boolean wantReadTexts)
    throws IOException {
	RpcCall req;
	if (proto_10_membership) {
	    req = new RpcCall(count(), Rpc.C_get_membership_10).
		add(new KomToken(persNo)).add(new KomToken(first)).
		add(new KomToken(no)).
		add(new KomToken(wantReadTexts));
	} else {
	    req = new RpcCall(count(), Rpc.C_get_membership).
		add(new KomToken(persNo)).add(new KomToken(first)).
		add(new KomToken(no)).
		add(new KomToken(wantReadTexts)).
		add(new KomToken(0));

	}
	writeRpcCall(req);
	return req;
    }
    /**
     * Equal to <tt>doGetMembership(<i>persNo</i>, 0, 1000, false)</tt>.
     * @see nu.dll.lyskom.Session#doGetMembership(int, int, int, boolean)
     */
    public RpcCall doGetMembership(int persNo)
    throws IOException {
	return doGetMembership(persNo, 0, 1000, false);
    }

    /**
     * Equal to <tt>getMembershipList(myPerson.getNo(), 0, myPerson.noOfConfs+1, new Bitstring("0"))</tt>
     * @see nu.dll.lyskom.Session#getMembershipList(int, int, int, Bitstring)
     */
    public List getMyMembershipList(boolean wantReadTexts)
    throws IOException {
	return membership = getMembershipList(myPerson.getNo(), 0, myPerson.noOfConfs+1, wantReadTexts);
    }

    /**
     * Returns a List of Membership objects representing the membership information
     * for a given person.
     * @param persNo The person number for which to query information
     * @param first The first membership position in list to retreive
     * @param no The number of conferences to retreive
     * @param wantReadTexts If true, the server will return a read-texts array with the membership information
     *
     */
    public List getMembershipList(int persNo, int first, int no, boolean wantReadTexts)
    throws IOException {
	Debug.println("getMembershipList(" + persNo + ", " + first + ", " + no + ", " + wantReadTexts + ")");
	Membership[] m = getMembership(persNo, first, no, wantReadTexts);
	LinkedList l = new LinkedList();
	for (int i=0; i < m.length; i++) {
	    if (m[i] != null) {
		l.add(m[i]);
		if (persNo == myPerson.getNo())
		    membershipCache.add(m[i]);
	    }
	}
	Debug.println("getMembershipList(persNo:" + persNo +
		      "): returning " + l.size() + " confererences");
	return l;
    }
    /**
     * Returns an array of Membership objects representing the membership information
     * for a given person.
     *
     * @param persNo The person number for which to query information
     * @param first The first membership position in list to retreive
     * @param no The number of conferences to retreive
     * @param wantReadTexts If the first bit is set, the server will not return a read-texts array with the membership information
     */    
    public  Membership[] getMembership(int persNo, int first,
				       int no, boolean wantReadTexts)
    throws IOException {
	RpcCall call = waitForCall(doGetMembership(persNo, first,
						   no, wantReadTexts));
	if (!call.getReply().getSuccess()) throw call.getReply().getException();
	KomToken[] parameters = call.getReply().getParameters();
	return Membership.createFromArray(0, parameters,
					  call.getOp() == Rpc.C_get_membership_10);
    }

    /**
     * Sends the RPC call create-person to the server.
     *
     * @param name The requested name of the person
     * @param password The requested password for this new person
     * @param flags A Bitstring of flags
     * @param auxItems an array of AuxItem objects that should be set for this person
     * @return An RpcCall object representing this specific RPC call
     * @see nu.dll.lyskom.Session#createPerson(String, String, Bitstring, AuxItem[])
     */
    public RpcCall doCreatePerson(String name, String password, Bitstring flags, AuxItem[] auxItems)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_create_person);
	req.add(new Hollerith(name));
	req.add(new Hollerith(password));
	req.add(flags);
	req.add(new KomTokenArray(auxItems.length, auxItems));
	writeRpcCall(req);
	return req;
    }

    /**
     * Asks the server to create a new person.
     * @return The number of the newly created person
     * @param name The requested name of the person
     * @param password The requested password for this new person 
     * @param flags A Bitstring of flags 
     * @param auxItems an array of AuxItem objects that should be set for this person
     * @return An RpcCall object representing this specific RPC call 
     * @see nu.dll.lyskom.Session#createPerson(String, String, Bitstring, AuxItem[])
     */
    public int createPerson(String name, String password, Bitstring flags, AuxItem[] auxItems)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doCreatePerson(name, password, flags, auxItems));

	if (!reply.getSuccess()) throw reply.getException();
	return reply.getParameters()[0].intValue();
    }

    public RpcCall doFindPreviousTextNo(int text)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_find_previous_text_no);
	req.add(text);
	writeRpcCall(req);
	return req;
    }

    public int findPreviousTextNo(int text) {
	RpcReply r = waitFor(doFindPreviousTextNo(text));
	if (!r.getSuccess()) throw r.getException();
	return r.getParameters()[0].intValue();
    }
    /**
     * Sends the RPC call create-conf to the server.
     *
     * @param name The requested name of the confernece
     * @param flags A Bitstring of flags
     * @param auxItems an array of AuxItem objects that should be set for this conference
     * @return An RpcCall object representing this specific RPC call
     * @see nu.dll.lyskom.Session#createConf(String, boolean, boolean, boolean)
     */
    public  RpcCall doCreateConf(String name, Bitstring type, AuxItem[] auxItems)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_create_conf);
	req.add(new Hollerith(name));
	req.add(type);
	req.add(new KomTokenArray(auxItems.length, auxItems));
	writeRpcCall(req);
	return req;
    }


    /**
     * Ask the server to create a new conference
     *
     * @return The number of the newly created conference
     * @param name The requested name of the conference
     * @param readProt if <tt>true</tt>, the new conference should be read protected
     * @param original if <tt>true</tt>, the new conference should not allow comments
     * @param secret if <tt>true</tt>, the new conference should be secret
     */

    public  int createConf(String name, boolean readProt, boolean original, boolean secret)
    throws IOException {
	Bitstring confType = new Bitstring(new boolean[] { readProt, original, secret, false });
	RpcReply r = waitFor(doCreateConf(name, confType, new AuxItem[] {}).getId());
	if (r.getSuccess()) {
	    return r.getParameters()[0].intValue();
	}
	return -1;
    }
					     
    /**
     * Returns an array of Membership objects for a given person, representing
     * the persons full membership list.
     * @param persNo The person for which to request membership information
     * @see nu.dll.lyskom.Session#doGetMembership(int)
     */
    public  Membership[] getMembership(int persNo)
    throws IOException {
	RpcCall call = waitForCall(doGetMembership(persNo));
	return Membership.createFromArray(0, call.getReply().getParameters(),
					  call.getOp() == Rpc.C_get_membership_10);
    }

    /**
     * Returns an array of Membership objects, representing the conferences
     * in which the currently logged in person might have unread texts.
     */
    public  Membership[] getUnreadMembership() {
	Membership[] m = new Membership[unreadMembership.size()];
	for (int i=0; i < m.length; i++) {
	    m[i] = (Membership) unreadMembership.get(i);
	}
	return m;
    }
    /**
     * return array of unread texts (in TextMapping for) for all
     * conferences. You should call updateUnreads before this.
     */
    public  TextMapping[] getUnreadTexts() {
	TextMapping[] map = new TextMapping[unreadTexts.size()];
	Iterator iter = unreadTexts.iterator();
	for (int i=0; i < unreadTexts.size(); i++) {
	    map[i] = (TextMapping) iter.next();
	}
	return map;
    }
    
    /**
     * Sends the RPC call get-uconf-stat to the server.
     *
     * @param confNo The conference to request information about
     * @return An RpcCall object representing this specific RPC call
     * @see nu.dll.lyskom.Session#getUConfStat(int)
     */
    public  RpcCall doGetUConfStat(int confNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_uconf_stat).
	    add(new KomToken(confNo));

	writeRpcCall(req);
	return req;
    }

    /**
     * Returns an <tt>UConference</tt> object containing information about
     * a specific conference.
     *
     * @param confNo The conference to request information about
     * @param refreshCache if <tt>true</tt>, don't look in the cache
     * @see nu.dll.lyskom.UConference
     */
    public UConference getUConfStat(int confNo, boolean refreshCache)
    throws IOException, RpcFailure {
	if (confNo == 0)
	    throw new IllegalArgumentException("Attempt to use conference zero.");

	UConference cc = refreshCache ? null : conferenceCache.getUConference(confNo);
	if (cc != null) return cc;
	RpcCall req = doGetUConfStat(confNo);	
	if (Debug.ENABLED) Debug.println("uconf-stat for " + confNo + " not in cache, asking server");
	RpcReply rep = waitFor(req.getId());
	if (rep.getSuccess()) {	    
	    cc = new UConference(confNo, rep.getParameters());	
	    conferenceCache.add(cc);
	    return cc;
	} else {
	    throw rep.getException();
	}
    }

    /**
     * Returns an <tt>UConference</tt> object containing information about
     * a specific conference. Returns a cached copy if possible.
     *
     * @param confNo The conference to request information about
     * @see nu.dll.lyskom.UConference
     */
    public UConference getUConfStat(int confNo)
    throws IOException, RpcFailure {
	return getUConfStat(confNo, false);
    }

    public void saveUserArea(UserArea userArea)
    throws RpcFailure, IOException {
	saveUserArea(myPersonNo, userArea);
    }

    public void saveUserArea(int persNo, UserArea userArea)
    throws RpcFailure, IOException {
	Text userDataText = new Text(userArea.toNetwork());
	userDataText.setCharset(userArea.getCharset());
	userDataText.getStat().setAuxItem(new AuxItem(AuxItem.tagContentType, UserArea.contentType));

	int textNo = createText(userDataText);
	setUserArea(persNo, textNo);
    }

    public RpcCall doSetUserArea(int persNo, int userAreaTextNo) 
    throws RpcFailure, IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_set_user_area).
			    add(new KomToken(persNo)).
			    add(new KomToken(userAreaTextNo)));
    }

    public void setUserArea(int persNo, int userAreaText) 
    throws RpcFailure, IOException {
	RpcReply r = waitFor(doSetUserArea(persNo, userAreaText));
	if (!r.getSuccess()) throw r.getException();

	if (persNo == myPersonNo) {
	    Debug.println("setUserArea(): clearing myPerson.");
	    myPerson = null;
	}
	personCache.remove(persNo);
    }

    public void setUserArea(int userAreaText)
    throws IOException, RpcFailure {
	setUserArea(myPersonNo, userAreaText);
    }

    public UserArea getUserArea()
    throws RpcFailure, IOException {
	int userAreaTextNo = getMyPerson().getUserArea();
	if (userAreaTextNo == 0) return new UserArea(getServerEncoding());
	if (userArea != null && userAreaTextNo == userArea.getTextNo()) {
	    return userArea;
	}
	Text t = getText(userAreaTextNo);
	if (t == null) return null; // ?!
	userArea = new UserArea(t);
	return userArea;
    }

    /**
     * Sets the maximum number of unread texts in the current conference.
     * Note that this call will fail if the user is not currently in a conference.
     * 
     * @param no The number of unread texts to request
     * @see nu.dll.lyskom.Session#endast(int, int)
     */
    public void endast(int no) 
    throws IOException, RpcFailure {
	endast(currentConference, no);
    }

    /**
     * Sets the maximum number of unread texts in a conference.
     *
     * @param confNo The conference in which to set the number of unread texts
     * @param no The number of unread texts to request
     * @see nu.dll.lyskom.Session#endast(int)
     */
    public  void endast(int confNo, int no)
    throws IOException, RpcFailure {
	UConference uconf = getUConfStat(confNo);
	int highest = uconf.getHighestLocalNo();
	int lastRead = highest-no;
	if (lastRead < 0) lastRead = 0;
	setLastRead(confNo, lastRead);
	Membership ms = membershipCache.get(confNo);
	if (ms != null) {
	    ms.setLastTextRead(lastRead);
	}
	if (unreads != null) {
	    synchronized (unreads) {
		if (!unreads.contains(new Integer(confNo))) {
		    if (lastRead < highest) unreads.add(new Integer(confNo));
		}
	    }
	}
	readTexts.clear();
    }


    /**
     * Sends the RPC call set-last-read to the server.
     *
     * @param confNo The conference to set last-read information in
     * @param textNo the text number to be the highest read text
     * @return An RpcCall object representing this specific RPC call
     * @see nu.dll.lyskom.Session#endast(int, int)
     */
    public  RpcCall doSetLastRead(int confNo, int textNo) 
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_set_last_read).
			    add(new KomToken(confNo)).
			    add(new KomToken(textNo)));
    }

    /**
     * Sets the highest local text number that has been read in a given conference.
     *
     * @param confNo The conference to set last-read information in
     * @param textNo the text number to be the highest read text
     * @see nu.dll.lyskom.Session#endast(int, int)
     *
     */ 
    public  void setLastRead(int confNo, int textNo)
    throws IOException {
	waitFor(doSetLastRead(confNo, textNo).getId());
	Membership ms = membershipCache.get(confNo);
	if (ms != null) {
	    ms.setLastTextRead(textNo);
	}
    }


    /**
     * Sends the RPC call get-conf-stat to the server.
     *
     * @param confNo The conference to retreive conf-stat information about
     * @return An RpcCall object representing this specific RPC call
     */
    public  RpcCall doGetConfStat(int confNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_conf_stat).
	    add(new KomToken(confNo));

	writeRpcCall(req);
	return req;
    }

    /**
     * Returns a <tt>Conference</tt> object containing information about a given
     * conference. If the information has been cached previously, a cached copy
     * will be returned.
     *
     * @param confNo The confernece to retreive information about
     * @see nu.dll.lyskom.Session
     */
    public Conference getConfStat(int confNo) 
    throws IOException, RpcFailure {
	return getConfStat(confNo, false);
    }
	

    /**
     * Returns a <tt>Conference</tt> object containing information about a given
     * conference.
     *
     * @param confNo The confernece to retreive information about
     * @param refreshCache If <tt>true</tt>, don't go look into the cache before asking server
     * @see nu.dll.lyskom.Session
     */
    public Conference getConfStat(int confNo, boolean refreshCache)
    throws IOException, RpcFailure {
	Conference cc = refreshCache ? null: conferenceCache.getConference(confNo);
	if (cc != null) return cc;

	RpcReply reply = waitFor(doGetConfStat(confNo).getId());
	if (!reply.getSuccess()) throw reply.getException();
	Conference c = new Conference(confNo, reply.getParameters());
	conferenceCache.add(c);
	return c;
    }

    /**
     * Changes the user's current conference.
     *
     * @param confNo The conference to change to
     */
    public void changeConference(int confNo)
    throws IOException, RpcFailure {
	if (confNo == currentConference) return;

	RpcReply reply = waitFor(doChangeConference(confNo).getId());
	if (reply.getSuccess()) {
	    currentConference = confNo;
	} else {
	    throw reply.getException();
	}
    }

    /**
     * Sends the RPC call change-conference to the server.
     *
     * @param confNo The conference to change to
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doChangeConference(int confNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_change_conference)
	    .add(confNo);
	writeRpcCall(req);
	return req;
    }

    /**
     * Sends the RPC call sub-member to the server.
     *
     * @param confNo The conference to remove a member from
     * @param persNo The person to be removed from a converence
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doSubMember(int confNo, int persNo) throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_sub_member);
	req.add(new KomToken(confNo)).add(new KomToken(persNo));
	writeRpcCall(req);
	return req;
    }

    /**
     * Removes the person <tt>persNo</tt> from conference <tt>confNo</tt>.
     * Only the supervisor of <tt>confNo</tt> or <tt>persNo</tt> can do this.
     * Also sets the current conference to be -1 if the call succeeds.
     *
     * @param confNo The conference to remove a member from
     * @param persNo The person to be removed from a converence
     */
    public void subMember(int confNo, int persNo) throws RpcFailure, IOException {
	RpcReply reply = waitFor(doSubMember(confNo, persNo));
	if (!reply.getSuccess()) throw reply.getException();
	subConfMembership(confNo);
	currentConference = -1;
    }

    void subConfMembership(int confNo) {
	synchronized (unreads) {
	    for (Iterator i = unreads.iterator(); i.hasNext();)
		if (((Integer) i.next()).intValue() == confNo)
		    i.remove();
	    
	}
	
	if (membership == null) return;

	synchronized (membership) {
	    Iterator i = membership.iterator();
	    while (i.hasNext()) {
		Membership m = (Membership) i.next();
		if (m.getNo() == confNo) {
		    i.remove();
		    return;
		}
	    }
	}
    }

    /**
     * Sends the RPC call add-member to the server.
     *
     * @param confNo The conference to add to the membership list
     * @param persNo The person who's membership list will be updated
     * @param prio The priority of the membership
     * @param listPos The position of this conference in the membership list
     * @param type A Bitstring containing the membership type information
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doAddMember(int confNo, int persNo, int prio, int listPos, Bitstring type) throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_add_member);
	req.add(new KomToken(confNo));
	req.add(new KomToken(persNo));
	req.add(new KomToken(prio));
	req.add(new KomToken(listPos));
	req.add(type);
	writeRpcCall(req);
	return req;
    }

    /**
     * Adds a conference to a person's membership.
     *
     * @param confNo The conference to add to the membership list
     * @param persNo The person who's membership list will be updated
     * @param prio The priority of the membership
     * @param listPos The position of this conference in the membership list
     * @param invititation If <tt>true</tt>, this is an invitation to become a member
     * @param passive If <tt>true</tt>, this membership is passive
     * @param secret If <tt>true</tt>, this membership is secret
     * @see nu.dll.lyskom.Session#joinConference(int)
     */
    public void addMember(int confNo, int persNo, int prio, int listPos, boolean invitation,
			  boolean passive, boolean secret)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doAddMember(confNo, persNo, prio, listPos,
					     new MembershipType(new boolean[] { invitation, passive, secret })).getId());
	if (!reply.getSuccess()) {
	    throw reply.getException();
	}
	membership = getMyMembershipList(false); // XXX
    }

    /**
     * Sends the RPC call change-name to the server.
     *
     * @param confNo The conference (or letterbox/person) to change the name of
     * @param newName The new name of the conference
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doChangeName(int confNo, String newName)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_change_name);
	req.add(new KomToken(confNo));
	req.add(new Hollerith(newName));
	writeRpcCall(req);
	return req;
    }

    /**
     * Changes the name of a conference or a person.
     *
     * @param confNo The conference (or letterbox/person) to change the name of
     * @param newName The new name of the conference
     */
    public void changeName(int confNo, String newName)
    throws IOException, RpcFailure {
	RpcCall call = null;
	RpcReply reply = waitFor(call = doChangeName(confNo, newName));
	if (!reply.getSuccess()) throw reply.getException();
	myPerson.uconf.name = call.getParameter(1).getContents();
    }

    /**
     * Sets the text number that contains a conference's or a person's presentation.
     *
     * @param confNo The conference for which to set the presentation
     * @param textNo The global text number containing the presentation
     */
    public void setPresentation(int confNo, int textNo)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doSetPresentation(confNo, textNo));
	if (!reply.getSuccess()) throw reply.getException();
	conferenceCache.removeAll(confNo);
    }

    /**
     * Sends the RPC call set-presentation to the server.
     *
     * @param confNo The conference for which to set the presentation
     * @param textNo The global text number containing the presentation
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doSetPresentation(int confNo, int textNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_set_presentation);
	req.add(new KomToken(confNo));
	req.add(new KomToken(textNo));
	writeRpcCall(req);
	return req;
    }

    /**
     * Adds a conference with priority 100 at the last position of the currently
     * logged in person's membership list.
     *
     * @param confNo The conference to join
     * @see nu.dll.lyskom.Session#addMember(int, int, int, int, boolean, boolean, boolean)
     */
    public void joinConference(int confNo) throws IOException, RpcFailure {
	addMember(confNo, getMyPerson().getNo(), 100, getMyPerson().noOfConfs+1, false, false, false);
	membership = getMyMembershipList(false); // XXX
    }

    /**
     * Sends the RPC call get-person-stat to the server.
     *
     * @param persNo The person to request information about
     * @return An RpcCall object representing this specific RPC call
     */
    public  RpcCall doGetPersonStat(int persNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_person_stat).
	    add(persNo);
	
	writeRpcCall(req);
	return req;
    }

    /**
     * Returns a <tt>Person</tt> object containing information about
     * a given person.
     *
     * @param persNo The person to request information about
     * @param refreshCache If <tt>true</tt> don't look in the cache first.
     * @see nu.dll.lyskom.Person
     */
    public Person getPersonStat(int persNo)
    throws IOException, RpcFailure {
	return getPersonStat(persNo, false);
    }

    /**
     * Returns a <tt>Person</tt> object containing information about
     * a given person.
     *
     * @param persNo The person to request information about
     * @param refreshCache If <tt>true</tt> don't look in the cache first.
     * @see nu.dll.lyskom.Person
     */
    public Person getPersonStat(int persNo, boolean refreshCache)
    throws IOException, RpcFailure {
	Person p = refreshCache ? null : personCache.get(persNo);
	if (p != null) return p;

	RpcReply reply = waitFor(doGetPersonStat(persNo).getId());
	if (!reply.getSuccess()) throw reply.getException();

	p = Person.createFrom(persNo, reply);
	personCache.add(p);

	return p;
    }
	

    /**
     * Returns a byte-array containing the name for a conference,
     * or null if the conference doesn't exist.
     *
     * @param confNo conference number to retreive the name for
     */
    public  byte[] getConfName(int confNo)
    throws IOException, RpcFailure {
	if (confNo == 0)
	    return "Conference 0".getBytes();
	UConference c = getUConfStat(confNo);
	
	return c == null ? null : c.getName();
    }

    /**
     * Sends the RPC call get-text-stat to the server.
     *
     * @param textNo The text number to request information about
     * @return An RpcCall object representing this specific RPC call
     */
    public  RpcCall doGetTextStat(int textNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_text_stat).
	    add(new KomToken(textNo));

	writeRpcCall(req);
	return req;
    }

    /**
     * Returns a <tt>TextStat</tt> object containing information about a
     * given text.
     *
     * @param textNo The text number to request information about
     * @see nu.dll.lyskom.TextStat
     */
    public TextStat getTextStat(int textNo)
    throws IOException {    
	return getTextStat(textNo, false);
    }
    /**
     * Returns a TextStat object for a given text number,
     * or null if the text doesn't exist or is inaccessible.
     *
     * @param textNo The text number to request information about
     * @param refreshCache If <tt>true</tt>, a cached copy will not be returned
     * @see nu.dll.lyskom.TextStat
     */
    public TextStat getTextStat(int textNo, boolean refreshCache)
    throws IOException, RpcFailure {

        TextStat ts = refreshCache ? null : textStatCache.get(textNo);
        if (ts != null) return ts;

	Text cachedText = textCache.get(textNo);
	if (!refreshCache && cachedText != null && cachedText.getStat() != null)
	    return cachedText.getStat();

	RpcReply reply = waitFor(doGetTextStat(textNo).getId());

	if (!reply.getSuccess()) throw reply.getException();

	ts = TextStat.createFrom(textNo, reply);

	if (cachedText != null) cachedText.setStat(ts);
	textStatCache.add(ts);
	return ts;

    }

    /**
     * Sends the RPC call user-active to the server.
     *
     * This method does not store the RpcCall for user with a later
     * call to waitFor(), since the call is always expected to succeed.
     *
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doUserActive()
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_user_active), false);
    }

    public RpcCall doWhoAmI()
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_who_am_i), true);
    }

    public int whoAmI()
    throws IOException, RpcFailure {
	if (mySessionNo > 0) return mySessionNo;

	RpcReply r = waitFor(doWhoAmI());
	if (!r.getSuccess()) throw r.getException();
	mySessionNo = r.getParameters()[0].intValue();
	return mySessionNo;
    }

    /**
     * Returns <tt>true</tt> if a user is currently logged in in this session.
     */   
    public boolean getLoggedIn() {
	return loggedIn;
    }

    /**
     * Logs out the currently logged in user.
     *
     * @param block If <tt>true</tt>, this method block until the server has confirmed the call
     */
    public void logout(boolean block)
    throws IOException, RpcFailure {
	RpcCall logoutCall = new RpcCall(count(), Rpc.C_logout);

	writeRpcCall(logoutCall);
	loggedIn = false;
	state = STATE_CONNECTED;
	if (!block) return;
	
	waitFor(logoutCall.getId());
    }

    /** 
     * Returns the state of this connection.
     *
     */
    public int getState() {
	return state;
    }
    
    /**
     * Forcibly logout, disconnect, and finish all threads
     */
    public void shutdown() {
	try {
	    if (loggedIn) logout(false);
	    if (connected) disconnect(false);
	} catch (IOException ex1) {

	} finally {
	    if (invoker != null) invoker.quit();
	    invoker = null;
	    listener = null;
	}
    }

    /**
     * Writes a raw (custom) RPC call to the server. The reference number will
     * be selected by this method.
     *
     * @param id The protocol request number to send to the server
     * @param s A String containing the parameters to this call
     *
     */
    public void writeRaw(int id, String s) throws IOException {
	int seq = count();
	OutputStream out = connection.getOutputStream();
	byte[] bytes = (seq + " " + id + ((s != null) ? " " + s : "") + "\n")
	    .getBytes();
	connection.queuedWrite(bytes);
		 
    }

    /**
     * Returns the next RPC reference number to use and increments the RPC
     * reference counter.
     */
    public synchronized int count() {
	rpcCount++;
	//return lastRpcCall += random.nextInt(63)+1;
	return rpcCount;
    }

    KomTokenReader getKomTokenReader() {
	return reader;
    }

    /**
     * Sends the RPC call create-text to the server.
     *
     * @param t A Text object containing the information needed to construct the RPC call
     * @return An RpcCall object representing this specific RPC call
     * @see nu.dll.lyskom.Text
     */
    public RpcCall doCreateText(Text t)
    throws IOException {
	t.trimContents();
	TextStat s = t.getStat();
	if (!s.containsAuxItem(AuxItem.tagCreatingSoftware)) {
	    s.addAuxItem(new AuxItem(AuxItem.tagCreatingSoftware, latteName));
	}
	if (!s.containsAuxItem(AuxItem.tagContentType)) {
	    s.addAuxItem(new AuxItem(AuxItem.tagContentType,
				     "text/x-kom-basic; charset=" +
				     getServerEncoding()));
	}
	return doCreateText(t.getContents(), s.getMiscInfo(),
			    s.getAuxItems());
    }


    /**
     * Very simple reply function. Application developers are encouraged to 
     * use createText() instead.
     */
    public int reply(int textNo, Text t)
    throws IOException {
	Text commented = getText(textNo);
	int[] rcpts = commented.getRecipients();
	for (int i=0; i < rcpts.length; i++) 
	    t.addRecipient(rcpts[i]);
	t.addCommented(textNo);
	return createText(t);
    }

    /**
     * Sends the RPC call create-text to the server.
     *
     * @param text byte-array containing the article subject and text
     * @param miscInfo List of Selections with Misc-Info stuff (recipients, etc)
     * @param auxItems Auxiallry items (Aux-Item) attached to this article
     * @return An RpcCall object representing this specific RPC call
     *
     */
    public  RpcCall doCreateText(byte[] text,
				 List miscInfo,
				 AuxItem[] auxItems)
    throws IOException {
	//KomToken[] ar = { miscInfo.toToken() };	
	KomTokenArray auxTokenArray;

	if (auxItems != null && AuxItem.countAuxItems(auxItems) > 0) {
	    KomToken[] auxar = null;
	    auxar = new KomToken[auxItems.length];
	    for (int i=0; i<auxItems.length; i++)
		auxar[i] = auxItems[i].toToken();
	    auxTokenArray = new KomTokenArray(auxItems.length, auxar);
	} else {
	    auxTokenArray = new KomTokenArray(0);
	}

	// XXX: the MiscInfo list must be sorted to prevent
	// eg. comm-to to appear before rcpt in the resulting
	// array!!

	RpcCall req = new RpcCall(count(), Rpc.C_create_text).
	    add(new Hollerith(text)).
	    add((KomToken) new KomTokenArray(miscInfo.size(), miscInfo)).
	    add((KomToken) auxTokenArray);
	writeRpcCall(req);
	return req;	
    }


    /**
     * Creates a text on the server, then returns the number of the newly created text.
     *
     * @param t A Text object containing the information needed to create the text
     * @return The number of the newly created text.
     * @see nu.dll.lyskom.Text
     */
    public int createText(Text t)
    throws IOException {
	RpcReply reply = waitFor(doCreateText(t).getId());

	if (!reply.getSuccess())
	    throw reply.getException();

	return reply.getParameters()[0].intValue();
    }

    /**
     * Sends the RPC call modify-conf-info or modify-text-info to the server.
     *
     * @param isConf If <tt>true</tt>, the refered object is a conference, otherwise a text
     * @param objNo If <tt>isConf</tt> is true: the conference to modify, otherwise the text number to modify
     * @param delAuxNo An array containing Aux-Info numbers to delete
     * @param addAux An array containing AuxItem objects to add
     * @see nu.dll.lyskom.AuxItem
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doModifyAuxInfo(boolean isConf, int objNo,
				   int[] delAuxNo, AuxItem[] addAux)
    throws IOException, RpcFailure {
	int call = isConf ? Rpc.C_modify_conf_info : Rpc.C_modify_text_info;
	RpcCall req = new RpcCall(count(), call);
	req.add(new KomToken(objNo));
	req.add(new KomTokenArray(delAuxNo.length, delAuxNo));
	KomToken[] auxs = new KomToken[addAux.length];
	for (int i=0; i < addAux.length; i++) {
	    auxs[i] = addAux[i].toToken();
	    Debug.println("doModifyAuxInfo(): adding aux-item " + i + ": " + auxs[i]);
	}
	req.add(new KomTokenArray(addAux.length, auxs));
	writeRpcCall(req);
	return req;
    }

    /**
     * Modifies Aux-Info for a conference or a text.
     *
     * @param isConf If <tt>true</tt>, the refered object is a conference, otherwise a text
     * @param objNo If <tt>isConf</tt> is true: the conference to modify, otherwise the text number to modify
     * @param delAuxNo An array containing Aux-Info numbers to delete
     * @param addAux An array containing AuxItem objects to add
     * @see nu.dll.lyskom.AuxItem
     */
    public void modifyAuxInfo(boolean isConf, int objNo,
			      int[] delAuxNo, AuxItem[] addAux)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doModifyAuxInfo(isConf, objNo,
						 delAuxNo, addAux));
	if (!reply.getSuccess()) throw reply.getException();
    }

    /**
     * Sends the RPC call change-what-i-am-doing to the server.
     *
     * @param s A String telling the server what you are doing
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doChangeWhatIAmDoing(String s) throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_change_what_i_am_doing);
	req.add(new Hollerith(s));
	writeRpcCall(req);
	return req;
    }

    /**
     * Tell the server what you are doing, which will be retreived by other
     * clients issuing the who-is-on-dynamic call.
     *
     * @param s A String telling the server what you are doing
     */
    String whatIAmDoing = null;
    public void changeWhatIAmDoing(String s) throws IOException {
	if (s.equals(whatIAmDoing)) return;

	RpcReply reply = waitFor(doChangeWhatIAmDoing(s));
	if (!reply.getSuccess()) throw reply.getException();
	whatIAmDoing = s;
    }

    public RpcCall doSetPassword(int person, String userPassword, String newPassword)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_set_passwd);
	req.add(new KomToken(person)).add(new Hollerith(userPassword)).
	    add(new Hollerith(newPassword));
	writeRpcCall(req);
	return req;
    }

    public void setPassword(int person, String userPassword, String newPassword)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doSetPassword(person, userPassword, newPassword));
	if (!reply.getSuccess()) throw reply.getException();
    }

    public RpcCall doAddRecipient(int textNo, int confNo, int type) 
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_add_recipient).add(new KomToken(textNo)).add(new KomToken(confNo));
	Selection info = new Selection();
	info.add(type, null);
	req.add(info.toToken());
	writeRpcCall(req);
	return req;
    }

    public void addRecipient(int textNo, int confNo, int type) 
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doAddRecipient(textNo, confNo, type));
	if (!reply.getSuccess()) throw reply.getException();
	purgeTextCache(textNo);
    }

    /**
     * Sends the RPC call who-is-on-dynamic to the server.
     *
     * @param wantVisible If <tt>true</tt>, the server will return sessions marked as visible
     * @param wantInvisible If <tt>true</tt>, the server will return sessions marked as invisible
     * @param activeLast Do not return sessions that has been idle for more than <tt>activeLast</tt> seconds
     * @return An RpcCall object representing this specific RPC call
     */
    public synchronized RpcCall doWhoIsOnDynamic (boolean wantVisible, boolean wantInvisible, int activeLast)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_who_is_on_dynamic);
	req.add(wantVisible ? "1" : "0").
	    add(wantInvisible ? "1" : "0").
	    add(activeLast);
	writeRpcCall(req);
	return req;
    }

    /**
     * Returns an array of DynamicSessionInfo objects containing information about online sessions
     *
     * @param wantVisible If <tt>true</tt>, the server will return sessions marked as visible
     * @param wantInvisible If <tt>true</tt>, the server will return sessions marked as invisible
     * @param activeLast Do not return sessions that has been idle for more than <tt>activeLast</tt> seconds
     * @see nu.dll.lyskom.DynamicSessionInfo
     */
    public DynamicSessionInfo[] whoIsOnDynamic (boolean wantVisible, boolean wantInvisible, int activeLast)
	throws IOException {
	Debug.println("whoIsOnDynamic called");
	RpcReply reply = waitFor(doWhoIsOnDynamic(wantVisible, wantInvisible, activeLast).getId());

	KomToken[] parameters = reply.getParameters();
	DynamicSessionInfo[] ids = new DynamicSessionInfo[parameters[0].intValue()];
	KomToken[] sessionData = ((KomTokenArray) parameters[1]).getTokens();


	for (int i=0, j=5 ; i < ids.length ; i++, j = j + 6)
	    ids[i] = new DynamicSessionInfo(sessionData[j-5].intValue(),
					    sessionData[j-4].intValue(),
					    sessionData[j-3].intValue(),
					    sessionData[j-2].intValue(),
					    new Bitstring(sessionData[j-1]),
					    sessionData[j].getContents());
	
	
	return ids;
    }


    /**
     * Sends the RPC call delete-text to the server.
     *
     * @param textNo The number of the text to delete
     * @return An RpcCall object representing this specific RPC call
     */
    public RpcCall doDeleteText(int textNo) throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_delete_text).add(new KomToken(textNo));
	writeRpcCall(req);
	return req;
    }
    
    /**
     * Deletes a text on the server.
     *
     * @param textNo The number of the text to delete
     */
    public void deleteText(int textNo) throws IOException, RpcFailure {
	RpcReply reply = waitFor(doDeleteText(textNo));
	if (!reply.getSuccess()) throw reply.getException();
	purgeTextCache(textNo);
    }

    /**
     * Lookup names of persons or conferences using regular expressions
     *
     * @param regexp A string containing the regexp that will be used for search
     * @param wantPersons Return persons matching <tt>name</tt>
     * @param wantConfs Return conferences matching <tt>name</tt>
     * @return An RpcCall object representing this specific RPC call
     * @see #lookupName(String, boolean, boolean)
     */
    public RpcCall doReLookup(String regexp, boolean wantPersons, boolean wantConfs)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_re_z_lookup);
	req.add(new Hollerith(regexp));
	req.add(new KomToken(wantPersons ? "1" : "0"));
	req.add(new KomToken(wantConfs ? "1" : "0"));
	writeRpcCall(req);
	return req;		
    }

    public ConfInfo[] reLookup(String regexp, boolean wantPersons, boolean wantConfs)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doReLookup(regexp, wantPersons, wantConfs));
	if (!reply.getSuccess()) throw reply.getException();
	KomToken[] parameters = reply.getParameters();
	ConfInfo[] ids = new ConfInfo[parameters[0].intValue()];
	KomToken[] confData = ((KomTokenArray) parameters[1]).getTokens();

	for (int i=0, j=2 ; i < ids.length ; i++, j = j + 3)
	    ids[i] = new ConfInfo((Hollerith) confData[j-2],
				  new ConfType(confData[j-1]),
				  confData[j].intValue());

	return ids;
    }


    /**
     * Lookup names of persons/conferences.
     * Returns a ConfInfo array of matches.
     *
     * @param name (sub)string naming the person(s)/conference(s)
     * @param wantPersons Return persons matching <tt>name</tt>
     * @param wantConfs Return conferences matching <tt>name</tt>
     * @see nu.dll.lyskom.ConfInfo
     */
    // TODO: return Conf-Z-Info-List (ConfInfo[]?)
    public ConfInfo[] lookupName(String name, boolean wantPersons,
					      boolean wantConfs)
    throws IOException, RpcFailure {
	int id = count();
	RpcCall lcall = new RpcCall(id, Rpc.C_lookup_z_name);
	lcall.add(new Hollerith(name)).
	    add(wantPersons ? "1" : "0").
	    add(wantConfs ? "1" : "0");


	writeRpcCall(lcall);

	RpcReply reply = waitFor(id);

	if (!reply.getSuccess()) {
	    throw reply.getException();
	}
	KomToken[] parameters = reply.getParameters();
	ConfInfo[] ids = new ConfInfo[parameters[0].intValue()];
	KomToken[] confData = ((KomTokenArray) parameters[1]).getTokens();

	for (int i=0, j=2 ; i < ids.length ; i++, j = j + 3)
	    ids[i] = new ConfInfo((Hollerith) confData[j-2],
				  new ConfType(confData[j-1]),
				  confData[j].intValue());

	return ids;
    }

    /**
     * Sends an asynchronous message with the <tt>send-message</tt> call.
     *
     * @param recipient The recipient conference, or <tt>0</tt> if it is a broadcast message
     * @param message The message to be sent to the server
     */
    public boolean sendMessage(int recipient, String message)
    throws IOException, RpcFailure {
	return sendMessage(recipient, message, true);
    }

    /**
     * Sends an asynchronous message with the <tt>send-message</tt> call.
     *
     * @param recipient The recipient conference, or <tt>0</tt> if it is a broadcast message
     * @param message The message to be sent to the server
     * @param block If true, wait until the server has replied to the RPC call
     */
    public boolean sendMessage(int recipient, String message, boolean block)
    throws IOException, RpcFailure {
	RpcCall msgCall = new RpcCall(count(), Rpc.C_send_message).
	    add(new KomToken(recipient)).add(new Hollerith(message));

	writeRpcCall(msgCall);
	if (block) {
	    RpcReply reply = waitFor(msgCall.getId());
	    if (!reply.getSuccess()) throw reply.getException();
	    return true;
	}
	return true;
    }

    /**
     * Writes an RPC call constructed from an RpcCall object to the
     * network output stream.
     *
     * @param c RpcCall object to be sent to the server
     * @param store if true, add it to the RPC call storage. This is
     *              required in order to use waitFor() etc
     */
    public RpcCall writeRpcCall(RpcCall c, boolean store)
    throws IOException {
	if (store) rpcHeap.addRpcCall(c);
	if (Debug.ENABLED) {
	    Debug.println("writeRpcCall(): id: " + c.getId() + "; op: " +
			  c.getOp() + "; store: " + store + 
			  " (" + c.getParameterCount() + " parameters)");
	}
	if (connection == null) {
	    throw new IOException("Connection has gone away (see SF bug ID 973278).");
	}
	connection.queuedWrite(c.toNetwork());
	return c; 
    }
    

    /**
     * Writes an RPC call constructed from an RpcCall object to the
     * network output stream and add it to the RPC call storage.
     *
     * @param c RpcCall object to be sent to the server
     */
    public RpcCall writeRpcCall(RpcCall c)
    throws IOException {
	return writeRpcCall(c, true);
    }



    public void removeAttributes(String pattern) {
	synchronized (sessionAttributes) {
	    for (Iterator i = sessionAttributes.keySet().iterator();
		 i.hasNext();) {

		String key = (String) i.next();
		if (key.matches(pattern)) {
		    if (Debug.ENABLED)
			Debug.println("Removing attribute \"" + key + "\"");

		    i.remove();
		}
	    }
	}
    }

    /**
     * Removes an attribute from the Session.
     */
    public Object removeAttribute(String key) {
	synchronized (sessionAttributes) {
	    return sessionAttributes.remove(key);
	}
    }

    /**
     * Stores an arbitrary attribute object in this Session.
     *
     * @see #getAttribute(String)
     */
    public Object setAttribute(String key, Object value) {
	synchronized (sessionAttributes) {
	    return sessionAttributes.put(key, value);
	}
    }

    /**
     * Retrieves an object previously stored with setAttribute()
     *
     * @see #setAttribute(String, Object)
     */
    public Object getAttribute(String key) {
	synchronized (sessionAttributes) {
	    return sessionAttributes.get(key);
	}
    }

    public RpcReply waitFor(int id)
    throws IOException {
	return waitFor(singleton(id));
    }

    public Collection singleton(int i) {
	HashSet h = new HashSet();
	h.add(new Integer(i));
	return h;
    }

    public RpcReply waitFor(Collection ids)
    throws IOException {
	RpcCall rc = waitForCall(ids);
	if (rc != null) return rc.getReply();
	return null;
    }

    public RpcCall waitForCall(RpcCall call)
    throws IOException {
	return waitForCall(call.getId());
    }
    public RpcCall waitForCall(int id)
    throws IOException {
	return waitForCall(singleton(id));
    }


    public AsynchMessage getAsynchMessage() {
	storeAsynchMessages = true;
	synchronized (pendingAsynchMessages) {
	    if (pendingAsynchMessages.size() == 0) {
		try {
		    pendingAsynchMessages.wait();
		} catch (InterruptedException ex1) {}
	    }
	    if (pendingAsynchMessages.size() > 0) {
		return (AsynchMessage) pendingAsynchMessages.remove(0);
	    }
	}
	return null;
    }

    /**
     * This methods provides a synchronous way of waiting for RPC
     * replies. Note that this method should never be called from
     * the MessageListener thread (for example, you may not call
     * this method from inside an AsynchMessageListener's
     * asynchMessage() method).
     * <br>
     * It blocks the current thread until a reply with the
     * specified reference ID has been read from the server, or the
     * rpcTimeout value has been reached.
     *
     * @param id The RPC call reference number to wait for
     * @see nu.dll.lyskom.Session#rpcTimeout
     *
     */
    public RpcCall waitForCall(Collection ids)
    throws IOException {

	if (Thread.currentThread() == listener.getThread()) {
	    throw new IOException("waitForCall() called from listener thread");
	}

	RpcCall call = null;
	RpcReply reply = null;
	long waited = 0;
	long waitStart = System.currentTimeMillis();
	int waitCount = 0;
	while (call == null) {
	    if (Debug.ENABLED) {
		Debug.println("waitForCall(" + ids + ")");
	    }

	    try {
		
		// do a check before the wait() in case the reply has arrived already
		if (null == (call = rpcHeap.getRpcCall(ids, true))) {
		    waitCount++;
		    if (!listener.isConnected()) {
			if (listener.getException() != null) {
			    throw new IOException("Exception in listener thread: " +
						  listener.getException().toString());
			} else {
			    throw new IOException("Listener is disconnected.");
			}
		    }
		    synchronized (rpcHeap) {
			rpcHeap.wait(rpcSoftTimeout > 0 ? rpcSoftTimeout : rpcTimeout);
		    }
		} else {
		    rpcHeap.purgeRpcCall(call);
		    if (fallbackCheck(ids, call)) {
			if (Debug.ENABLED) {
			    Debug.println("waitForCall(" + ids + ") returning after " +
					  (System.currentTimeMillis() - waitStart)
					  + " milliseconds (wait-count " + waitCount + ")");
			}
			return call;
		    }
		}
		
	    } catch (InterruptedException ex1) {
		Debug.println("RPC waitForCall() interrupted: " + ex1.getMessage());
		// ...?
	    }
	    call = rpcHeap.getRpcCall(ids, true);
	    if (call == null && listener != null && listener.getException() != null) {
		throw new IOException("Exception in listener: " + 
				      listener.getException());
	    } else if (listener == null) {
		throw new IOException("MessageListener has gone away!");
	    }

	    if (call == null) {
		waited = System.currentTimeMillis()-waitStart;
		if (waited > rpcTimeout) {
		    IOException e = new IOException("Timeout waiting for RPC reply #"+ids+" (" + waited + " ms)");
		    e.printStackTrace();
		    throw(e);
		}
	    } else {
		rpcHeap.purgeRpcCall(call);
		if (!fallbackCheck(ids, call)) {
		    call = null;
		} else {
		    if (Debug.ENABLED) {
			Debug.println("waitForCall(" + ids + ") returning after " +
				      (System.currentTimeMillis() - waitStart)
				      + " milliseconds (wait-count " + waitCount
				      + ")");
		    }
		}
	    }
	}
	return call;
    }

    public boolean fallbackCheck(Collection ids, RpcCall call)
    throws IOException {
	RpcReply reply = call.getReply();
	if (!reply.getSuccess() &&
	    reply.getException().getError() == Rpc.E_not_implemented) {
	    if (call.getOp() == Rpc.C_get_membership) {
		Debug.println("server says get-membership not implemented, " +
			      "falling back to get-membership-10");
		proto_10_membership = true;
		call.removeLast();
		int oldId = call.getId();
		call.setId(count());
		call.setOp(Rpc.C_get_membership_10);
		call.setReply(null);
		ids.remove(new Integer(oldId));
		ids.add(new Integer(call.getId()));
		writeRpcCall(call);
		return false;
	    }
	    if (call.getOp() == Rpc.C_query_read_texts) {
		Debug.println("server says query-read-texts not implemented, " +
			      "falling back to query-read-texts-10");
		proto_10_membership = true;
		call.removeLast();
		call.removeLast();
		int oldId = call.getId();
		call.setId(count());
		call.setOp(Rpc.C_query_read_texts_10);
		call.setReply(null);
		ids.remove(new Integer(oldId));
		ids.add(new Integer(call.getId()));
		writeRpcCall(call);
		return false;
	    }
	}
	return true;
    }

    /**
     * Convenience wrapper for <tt>waitFor(int)</tt>.
     * @see nu.dll.lyskom.Session#waitFor(int)
     */
    public RpcReply waitFor(RpcCall r) throws IOException {
	return waitFor(r.getId());
    }

    /**
     * Receiver of RPC events.
     */
    public void rpcEvent(RpcEvent e) {
    }

    /**
     * Receiver of RPC replies. This method and the RpcReplyReceiver are used
     * internally by LatteKOM.
     *
     * @see nu.dll.lyskom.RpcReplyReceiver
     */
    public void rpcReply(RpcReply r) {

	if (r == null) {
	    synchronized (rpcHeap) {
		rpcHeap.notifyAll();
	    }
	    return;
	}

	RpcCall originCall = rpcHeap.getRpcCall(r.getId(), false);
	if (originCall != null) {
	    originCall.setReply(r);
	}

	synchronized (rpcHeap) {
	    if (Debug.ENABLED) {
		Debug.println("notifying waiting threads: " + r.getId() + " " +
			      (r.getSuccess() ? ":-)" : ":-("));
	    }
	    rpcHeap.notifyAll();
	}

	if (originCall != null) {
	    notifyRpcEventListeners(new RpcEvent(this, originCall));
	}
	
    }
    
    void updateCachesNewText(TextStat textStat) {
	List miscInfo = textStat.getMiscInfo();
	for (int i=0; i < miscInfo.size(); i++) {
	    Selection misc = (Selection) miscInfo.get(i);
	    int key = misc.getKey();
	    if (key == TextStat.miscRecpt ||
		key == TextStat.miscCcRecpt ||
		key == TextStat.miscBccRecpt) {
		int recipient = misc.getIntValue();
		Integer recipientObj = new Integer(recipient);
		UConference cachedUConf = conferenceCache.getUConference(recipient);
		if (cachedUConf != null) {
		    int locNo = misc.getIntValue(TextStat.miscLocNo);
		    if (locNo > cachedUConf.getHighestLocalNo())
			cachedUConf.setHighestLocalNo(locNo);
		}
		Membership cachedMs = membershipCache.get(recipient);
		if (cachedMs != null) {
		    int locNo = misc.getIntValue(TextStat.miscLocNo);
		    if (readTexts.contains(textStat.getNo()) &&
			!cachedMs.isRead(locNo)) {
			cachedMs.markAsRead(locNo);
		    }
		}
		try {
		    if (unreads != null) {
			synchronized (unreads) {
			    if (!unreads.contains(recipientObj) &&
				!readTexts.contains(textStat.getNo()) &&
				isMemberOf(recipient, false)) {
				unreads.add(recipientObj);
			    }
			}
		    }
		} catch (IOException ex1) {}
	    }
	    if (key == TextStat.miscCommTo) {
		textStatCache.remove(misc.getIntValue());
	    }
	    if (key == TextStat.miscFootnTo) {
		textStatCache.remove(misc.getIntValue());
	    }
	}
    }

    void updateCachesSubRecipient(int textNo, int confNo, int recipientType) {
	purgeTextCache(textNo);
    }
    
    void updateCachesNewRecipient(int textNo, int confNo, int recipientType) {
	purgeTextCache(textNo);
	Integer textNoObj = new Integer(textNo);
	if (!readTexts.contains(textNo)) {
	    try {
		if (unreads != null) {
		    synchronized (unreads) {
			if (!unreads.contains(new Integer(confNo))
			    && isMemberOf(confNo, false)) {
			    unreads.add(new Integer(confNo));
			}
		    }
		}
	    } catch (IOException ex1) {}
	}
	if (membershipCache.contains(confNo))
	    membershipCache.remove(confNo);

	conferenceCache.removeUConference(confNo);
    }


    private void acceptAsynchAll()
    throws IOException, RpcFailure {
	acceptAsync(new int[] {
	    Asynch.new_name,
	    Asynch.sync_db,
	    Asynch.leave_conf,
	    Asynch.login,
	    Asynch.rejected_connection,
	    Asynch.send_message,
	    Asynch.logout,
	    Asynch.deleted_text,
	    Asynch.new_text,
	    Asynch.new_recipient,
	    Asynch.sub_recipient,
	    Asynch.new_membership
	});
    }
    
    public void acceptAsync(int[] requestList)
    throws IOException, RpcFailure {
	RpcReply r = waitFor(doAcceptAsync(requestList, false));
	if (!r.getSuccess()) throw r.getException();
    }

    public RpcCall doAcceptAsync(int[] requestList, boolean discardReply)
    throws IOException {
	RpcCall c = new RpcCall(count(), Rpc.C_accept_async);
	c.add(new KomTokenArray(requestList.length, requestList));
	return writeRpcCall(c, !discardReply);
    }


    /**
     * Receiver of asynchronous messages.
     * @see nu.dll.lyskom.AsynchMessageReceiver
     */
    public void asynchMessage(AsynchMessage m) {
	if (storeAsynchMessages) {
	    synchronized (pendingAsynchMessages) {
		pendingAsynchMessages.add(m);
		pendingAsynchMessages.notifyAll();
	    }
	}
	KomToken[] parameters = m.getParameters();

	int textNo = 0;
	TextStat textStat = null;

	switch (m.getNumber()) {
	case Asynch.login:
	    break;

	case Asynch.new_recipient:
 	    updateCachesNewRecipient(parameters[0].intValue(),
 				     parameters[1].intValue(),
 				     parameters[2].intValue());
	    break;
	case Asynch.sub_recipient:
 	    updateCachesSubRecipient(parameters[0].intValue(),
 				     parameters[1].intValue(),
 				     parameters[2].intValue());
	    break;

	case Asynch.new_text_old:
	    textStat = TextStat.createFrom(textNo, parameters, 1, true);
	case Asynch.new_text:
	    if (textStat == null)
		textStat = TextStat.createFrom(textNo, parameters, 1, false);

	    updateCachesNewText(textStat);
	    if (prefetch) {
		textNo = parameters[0].intValue();
		textPrefetchQueue.add(new Integer(textNo));
		invoker.enqueue(new TextPrefetcher(this, textPrefetchQueue));
	    }
	    break;
	    
	case Asynch.new_name:
	    conferenceCache.removeAll(parameters[0].intValue());
	    break;
	case Asynch.deleted_text:
	    textNo = parameters[0].intValue();
	    Debug.println("async-deleted-text for text " + textNo);
	    purgeTextCache(textNo);
	    break;

	}

    }

    

}

