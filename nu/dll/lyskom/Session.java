/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */

package nu.dll.lyskom;

import java.net.*;
import java.io.*;
import java.util.*;
/**
 * Session class. This handles connection, logging in, querying
 * unreads, creating texts, and a whole bunch of other stuff
 * @author rasmus@sno.pp.se
 * @version 0.1 */


public class Session
implements AsynchMessageReceiver, RpcReplyReceiver, RpcEventListener {
    public final static int MAX_TIMEOUT = 20000;
    public final static int DEFAULT_TIMEOUT = 1000;

    /**
     * Not connected to a LysKOM server
     */
    public final static int STATE_DISCONNECTED = 0;
    /**
     * Connected to a LysKOM server, but not logged in. In this state
     * only a few calls can be made to the server (lookupName,
     * login, create-person (not impl.), etc.) */
    public final static int STATE_CONNECTED = 1;
    /**
     * Connected, and logged in as a specific user
     */
    public final static int STATE_LOGIN = 2;

    int loginRpcId;
    boolean loggedIn = false;
    
    int rpcCount = 0;
    int lastRpcCall = 0;

    Random random = new Random();


    private int state = STATE_DISCONNECTED;

    Connection connection;
    KomTokenReader reader;
    MessageListener listener;

    String password;

    //int wakeOnReplyFrom = -1;

    // Conferences in which we might have unread texts
    List unreads = new LinkedList();
    List membership = null;
    
    // Membership for the corresponding conferences
    List unreadMembership;
    // Vector for all unread texts. It'd be nice if we could provide a
    // TreeMap iterator to get all unread texts for each conference in
    // comment, depth-first order...
    List unreadTexts;
    
    int currentConference = -1;

    TextCache textCache;
    PersonCache personCache;
    ConferenceCache conferenceCache;
    MembershipCache membershipCache;
    TextStatCache textStatCache;
    
    ReadTextsMap readTexts;
    
    RpcHeap rpcHeap;

    Vector rpcEventListeners;

    Person myPerson = null;

    boolean connected = false;

    int lastText = 0;

    KomTokenArray[] emptyKomTokens = { };

    String clientHost = null;

    private void init() {
	textCache = new TextCache();
	personCache = new PersonCache();
	conferenceCache = new ConferenceCache();
	membershipCache = new MembershipCache();
	textStatCache = new TextStatCache();
	readTexts = new ReadTextsMap();
	rpcHeap = new RpcHeap();
	rpcEventListeners = new Vector(1);
	mainThread = Thread.currentThread();
    }

    Thread mainThread;
    public Session() {
	init();
    }

    public void setClientHost(String s) {
	clientHost = s;
    }
    

    /**
     * Adds and removes RPC reply listeners. These will be called
     * through the RpcEventListener (rpcEvent()) interface when
     * RPC replies are received.
     */
    public void addRpcEventListener(RpcEventListener l) {
	rpcEventListeners.addElement((Object) l);
    }
    public void removeRpcEventListener(RpcEventListener l) {
	rpcEventListeners.removeElement((Object) l);
    }

    /**
     * Traverses through the rpcEventListeners vector
     * and calls all registered event listeners.
     *
     * This method could probably be private.
     */    
    public void notifyRpcEventListeners(RpcEvent ev) {
 	for (Enumeration e = rpcEventListeners.elements();
	     e.hasMoreElements();)
	    ((RpcEventListener) e.nextElement()).rpcEvent(ev);
    }

    /**
     * Disconnects from the server
     *
     * @param force This parameter has no effect (currently)
     */
    public synchronized void disconnect(boolean force)
    throws IOException {
	
	// if we're not connected, then state could never be anything
	// but STATE_DISCONNECTED, right? So we can return without
	// explicitly setting state to that.
	if (!connected) return;
	// remove listeners/recievers
	removeRpcEventListener(this);
	listener.setAsynch(false);
	listener.removeAsynchMessageReceiver(this);
	listener.removeRpcReplyReceiver(this);
	connection.close();
	connection = null;
	connected = false;
	state = STATE_DISCONNECTED;
    }

    /**
     * Connect to specified server/port number and do initial handshake
     *
     * @param server The host name of the server
     * @param port Port number to use (normally 4894)
     */     
    public synchronized boolean connect(String server, int port)
    throws IOException, ProtocolException {
	connection = new Connection(server, port);
	reader = new KomTokenReader(connection.getInputStream());

	connection.write('A'); // protocol A
	connection.writeLine(new Hollerith("LatteKOM" + (clientHost != null ? "%" + clientHost : "")).toNetwork());

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

    /**
     * Connect to specified server on the default port (4894) and do initial handshake
     *
     * @param server The host name of the server
     */     
    public synchronized boolean connect(String server) 
    throws IOException, ProtocolException {
	return connect(server, 4894);
    }

    /**
     * Adds a listener for asynchronous messages (called through
     * the AsynchMessageReceiver interface).
     */
    public void addAsynchMessageReceiver(AsynchMessageReceiver a) {
	listener.addAsynchMessageReceiver(a);
    }


    /**
     * Return true if connected
     */    
    public boolean getConnected() {
	return connected;
    }

    /**
     * Supposed to return an array of global text number for all unreads
     * in a conference. Generally not a good idea. Not used.
     */
    public synchronized int[] getGlobalUnreadInConf(int conf)
    throws IOException {
	int pers = myPerson.getNo();
	Vector v = new Vector(100); // xxx

	for (int i=0; i<unreadMembership.size(); i++) {
	    if (unreadMembership == null) continue;
	    v.addElement((Object) new Integer(((Membership) unreadMembership.get(i)).conference));
	}

	int c[] = new int[v.size()];
	Enumeration e = v.elements();
	for (int i=0; i < v.size() ; i++)
	    c[i] = ((Integer) e.nextElement()).intValue();
	return c;
    }	


    /**
     * Logs on to the LysKOM server.
     *
     * @param id ID number of the person to log in as
     * @param password corresponding password
     * @param hidden if true, session will not be broadcasted on LysKOM
     *
     * LysKOM call: login
     */
    public synchronized boolean login(int id, String password, boolean hidden)
    throws IOException {
	int rpcid = count();
	RpcCall loginCall = new RpcCall(rpcid, Rpc.C_login).
	    add(new KomToken(id)).add(new Hollerith(password)).
	    add(hidden ? "1" : "0"); // invisibility
	
	writeRpcCall(loginCall);

	RpcReply reply = waitFor(rpcid);
	loggedIn = reply.getSuccess();
	if (loggedIn) {
	    myPerson = getPersonStat(id);
	    myPerson.uconf = getUConfStat(id);
	    membership = getMyMembershipList();
	}
	state = STATE_LOGIN;
	return loggedIn = reply.getSuccess();
    }

    /**
     * Returns the Person object of the currently registered user.
     */
    public Person getMyPerson() {
	return myPerson;
    }

    /**
     * Updates the unreadMembership array with Membership objects for
     * conferences that _may_ contain unreads.
     */
    public void updateUnreads()
    throws IOException {
	int persNo = myPerson.getNo();
	unreads = getUnreadConfsList(persNo);
	
	unreadMembership = new LinkedList();
	unreadTexts = new LinkedList();
        
	for (int i=0; i < unreads.size(); i++) {
	    int conf = ((Integer) unreads.get(i)).intValue();
	    conferenceCache.removeAll(conf);
	    membershipCache.remove(conf);

	    Membership m = queryReadTexts(persNo, conf);
	    membershipCache.add(m);
	    int possibleUnreads = getUConfStat(m.conference).getHighestLocalNo() - m.lastTextRead;
	    if (possibleUnreads > 0 ) {
		unreadMembership.add(queryReadTexts(persNo, ((Integer) unreads.get(i)).intValue()));
		TextMapping tm = localToGlobal(((Integer) unreads.get(i)).intValue(), m.lastTextRead+1, possibleUnreads);
		
		// ok, this textmapping may contain text numbers that
		// we've already read, lets purge.
		for (int j=0; j < m.readTexts.length; j++) {
		    if (tm.removePair(m.readTexts[j])) {
			Debug.println("Removed already read text " + m.readTexts[j]);
		    }
		}
		unreadTexts.add(tm);
		m.setTextMapping(tm);
	    }
	}
    }

    /**
     * Returns the current conference (as entered by changeConference())
     */
    public int getCurrentConference() {
	return currentConference;
    }

    /**
     * Returns the conference number of the next conference that _may_ contain
     * an unread text.
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
		    }
		    return currentConference = nextConf;			
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

    /**
     * Returns the global text number of the next unread text in the
     * current conference. Returns -1 if there are no unread texts.
     *
     * @param updateUnread if true, also marks the returned text as read
     *
     * NOTE: This method is _very_ inefficient and expensive on the
     * server, most notably the use of localToGlobal() for each text is
     * very stupid.
     */   
    public int nextUnreadText(int conference, boolean updateUnread)
    throws IOException {
	if (unreads.size() == 0) return -1;
	
	UConference c = getUConfStat(conference);
	Membership m = queryReadTexts(myPerson.getNo(), conference, true);
	if (c.getHighestLocalNo() > m.lastTextRead) {
	    int localNo = m.lastTextRead + 1;	    
	    TextMapping tm = localToGlobal(conference, localNo, 10);

	    if (!tm.hasMoreElements()) {
		unreads.remove(new Integer(conference));
		return -1;
	    }
	    int txtNo = ((Integer) tm.nextElement()).intValue();
	    while (tm.hasMoreElements() && (txtNo == 0 || readTexts.exists(txtNo))) {
		Debug.println("nextUnreadText(): not returning " + txtNo);
		txtNo = ((Integer) tm.nextElement()).intValue();
	    }
	    if (txtNo == 0 || readTexts.exists(txtNo)) {
		Debug.println("no unread texts found");
		return -1;
	    }
	    
	    if (updateUnread) {
		int[] ur = { tm.localToGlobal(txtNo) };
		markAsRead(conference, ur);
		readTexts.add(txtNo);
	    }
	    return lastText = txtNo;
	} else {
	    unreads.remove(new Integer(conference));
	    return -1;
	}
    }

    public int nextUnreadText(boolean updateUnread)
    throws IOException {
	if (currentConference == -1) {
	    return -1;
	}

	return nextUnreadText(currentConference, updateUnread);

    }

    public boolean isMemberOf(int confNo) {
	Iterator i = membership.iterator();
	while (i.hasNext()){
	    Membership m = (Membership) i.next();
	    if (m.getNo() == confNo) {
		Debug.println("IS member of " + confNo);
		return true;
	    }
	}
	Debug.println("is NOT member of " + confNo);
	return false;
    }

    public int getUnreadCount(int confNo)
    throws IOException {
	if (unreads.size() == 0 || !unreads.contains(new Integer(confNo)))
	    return 0;	

	UConference c = getUConfStat(confNo);
	Membership m = queryReadTexts(myPerson.getNo(), confNo, true);
	if (c.getHighestLocalNo() > m.lastTextRead) {
	    return c.getHighestLocalNo() - m.lastTextRead;
	} else {
	    return 0;
	}
    }

    /**
     * Returns a ReadTextsMap object.
     * The ReadTextsMap should be rethought.
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
	for (int i=0; i < localTextNo.length; i++) {
	    Debug.println("about to mark text " + localTextNo[i] + " in conf " + confNo + " as read");
	}

	RpcReply r = waitFor(doMarkAsRead(confNo, localTextNo).getId());
	if (!r.getSuccess()) throw r.getException();
	for (int i=0; i < localTextNo.length; i++) {
	    Debug.println("marked local " + localTextNo[i] + " in conf " + confNo + " as read");
	}
    }

    /**
     * Constructs and sends the RPC call for markAsRead().
     */
    public synchronized RpcCall doMarkAsRead(int confNo, int[] localTextNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_mark_as_read);
	req.add(new KomToken(confNo)).add(new KomTokenArray(localTextNo.length, localTextNo));
	writeRpcCall(req);
	return req;
    }


    /**
     * Returns a Text object corresponding to the specified global
     * text number. If the text has been cached earlier, the cached
     * copy will always be returned.
     *
     * @param textNo Global text number
     *
     * LysKOM call: get-text
     */
    public synchronized Text getText(int textNo)
    throws IOException, RpcFailure { // and NoSuchTextException (RpcException?)
	if (textNo == 0) throw new RuntimeException("attempt to retreive text zero");
	Text text = textCache.get(textNo);

	// need to make sure that the text has contents,
	// since we also store TextStat skeletons in the Text cache.
	// ** do we still? they should be in the textStatCache only...?
	if (text != null && text.getContents() != null)
	    return text.setCached(true);

	if (text == null) text = new Text(textNo);

	Debug.println("** getting text " + textNo);

	if (text.getStat() == null) text.setStat(getTextStat(textNo));
	if (text.getStat() == null) return null; // no such text

	RpcCall textReq = new RpcCall(count(), Rpc.C_get_text).
	    add(new KomToken(textNo)).add("0").
	    add(new KomToken(text.getStat().chars));

	writeRpcCall(textReq);

	RpcReply reply = waitFor(textReq.getId());
	text.setContents(reply.getParameters()[0].getContents());
	textCache.add(text);
	textStatCache.add(text.getStat());
	return text;

    }

    /**
     * Writes an RPC call constructed from an RpcCall object to the
     * network output stream.
     *
     * @param c RpcCall object to be sent to the server
     */
    public RpcCall writeRpcCall(RpcCall c)
    throws IOException {
	rpcHeap.addRpcCall(c);
	c.writeNetwork(connection.getOutputStream());
	return c;
    }

    public RpcCall doMarkText(int textNo, int markType)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_mark_text);
	req.add(new KomToken(textNo));
	req.add(new KomToken(markType));
	writeRpcCall(req);
	return req;
    }

    public void markText(int textNo, int markType)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doMarkText(textNo, markType));
	if (!reply.getSuccess()) throw reply.getException();
    }

    public RpcCall doUnmarkText(int textNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_unmark_text);
	req.add(new KomToken(textNo));
	writeRpcCall(req);
	return req;
    }

    public void unmarkText(int textNo) 
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doUnmarkText(textNo));
	if (!reply.getSuccess()) throw reply.getException();
    }

    public RpcCall doGetMarks()
    throws IOException, RpcFailure {
	RpcCall call = new RpcCall(count(), Rpc.C_get_marks);
	writeRpcCall(call);
	return call;
    }

    public Mark[] getMarks()
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doGetMarks());
	if (!reply.getSuccess()) throw reply.getException();
	KomToken[] parameters = reply.getParameters();
	int numMarks = parameters[0].intValue();
	Mark[] marks = new Mark[numMarks];
	KomToken[] array = ((KomTokenArray) parameters[1]).getTokens();
	for (int j=0; j < array.length; j += 2) {
	    marks[j] = new Mark(array[j].intValue(), array[j+1].intValue());
	}
	return marks;

    }

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
     * See the "local-to-global" RPC call and the "Text-Mapping" data
     * structure in the LysKOM specification for more information.
     *
     * See doLocalToGlobal()
     *
     * LysKOM call: local-to-global, but without the limitation that
     * noOfExistingTexts must be between 1-255 inclusive
     */
    public  TextMapping localToGlobal(int confNo, int firstLocalNo,
						  int noOfExistingTexts)
    throws IOException {
	TextMapping m;
	if (membershipCache.contains(confNo)) {
	    Membership membership = membershipCache.get(confNo);
	    if (membership.getTextMapping() != null) {
		m = membership.getTextMapping();
	    } else {
		m = new TextMapping();
		membership.setTextMapping(m);
	    }
	} else {
	    m = new TextMapping();
	}

	int offset = 0;
	int existingTextsLeft;
	//if noOfExistingTexts is larger than 255, break up in several calls
	// this code could probably be a lot more legible
	do {
	    existingTextsLeft = noOfExistingTexts - offset;
	    RpcReply r = waitFor(doLocalToGlobal(confNo, firstLocalNo+offset,
						 ((existingTextsLeft > 255) ? 255 : existingTextsLeft)).getId());
	    m.update(0, r.getParameters(), false);
	    offset += 255; 
	} while ((noOfExistingTexts - offset) > 0);
	return m;
		 
    }


    /** query-read-texts **/
    public  RpcCall doQueryReadTexts(int persNo, int confNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_query_read_texts);
	req.add(new KomToken(persNo)).add(new KomToken(confNo));
	writeRpcCall(req);
	return req;
    }

    public  Membership queryReadTexts(int persNo, int confNo)
    throws IOException {
	return queryReadTexts(persNo, confNo, false);
    }
    public  Membership queryReadTexts(int persNo, int confNo,
						  boolean refresh)
    throws IOException {
	Membership m = null;
	if (!refresh && persNo == myPerson.getNo()) {
	    m = membershipCache.get(confNo);
	    if (m != null) return m;
	}
	m = new Membership(0, 
			   waitFor(doQueryReadTexts(persNo, confNo).getId()).
			   getParameters());
	membershipCache.add(m);
	return m;
    }

    /** get-unread-confs **/
    public  RpcCall doGetUnreadConfs(int persNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_unread_confs).
	    add(new KomToken(persNo));
	writeRpcCall(req);
	return req;
    }

    public List getUnreadConfsList(int persNo)
    throws IOException {
	LinkedList confList = new LinkedList();
	int[] confs = getUnreadConfs(persNo);
	for (int i=0; i < confs.length; i++) confList.add(new Integer(confs[i]));
	return confList;
    }

    public  int[] getUnreadConfs(int persNo)
    throws IOException {
	KomToken[] parameters = waitFor(doGetUnreadConfs(persNo).getId()).
	    getParameters();
	KomToken[] confs = ((KomTokenArray) parameters[1]).getTokens();
	int[] iconfs = new int[confs.length];
	for (int i=0; i<iconfs.length; i++)
	    iconfs[i] = confs[i].toInteger();
	return iconfs;
    }
	
	    
    /** get-membership **/
    public  RpcCall doGetMembership(int persNo, int first,
						int no, Bitstring mask)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_membership).
	    add(new KomToken(persNo)).add(new KomToken(first)).
	    add(new KomToken(no)).
	    add((KomToken) mask);
	writeRpcCall(req);
	return req;
    }
    public RpcCall doGetMembership(int persNo)
    throws IOException {
	return doGetMembership(persNo, 0, 1000, new Bitstring("0"));
    }
    public List getMyMembershipList()
    throws IOException {
	return getMembershipList(myPerson.getNo(), 0, myPerson.noOfConfs+1, new Bitstring("0"));
    }
    public List getMembershipList(int persNo, int first, int no, Bitstring mask)
    throws IOException {
	Debug.println("getMembershipList(" + persNo + ", " + first + ", " + no + ", " + mask + ")");
	Membership[] m = getMembership(persNo, first, no, mask);
	LinkedList l = new LinkedList();
	for (int i=0; i < m.length; i++) {
	    if (m[i] != null) {
		l.add(m[i]);
		Debug.println("getMembershipList(): adding conf " + m[i].getNo());
		membershipCache.add(m[i]);
	    }
	}
	Debug.println("getMembershipList(): returning " + l.size() + " confererences");
	return l;
    }
    public  Membership[] getMembership(int persNo, int first,
				       int no, Bitstring mask)
    throws IOException {
	return Membership.createFrom(waitFor(doGetMembership(persNo, first,
							     no, mask)
					     .getId()));
    }

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

    public int createPerson(String name, String password, Bitstring flags, AuxItem[] auxItems)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doCreatePerson(name, password, flags, auxItems));

	if (!reply.getSuccess()) throw reply.getException();
	return reply.getParameters()[0].intValue();
    }

    public  RpcCall doCreateConf(String name, Bitstring type, AuxItem[] auxItems)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_create_conf);
	req.add(new Hollerith(name));
	req.add(type);
	req.add(new KomTokenArray(auxItems.length, auxItems));
	writeRpcCall(req);
	return req;
    }

    public  int createConf(String name, boolean readProt, boolean original, boolean secret)
    throws IOException {
	Bitstring confType = new Bitstring(new boolean[] { readProt, original, secret, false });
	RpcReply r = waitFor(doCreateConf(name, confType, new AuxItem[] {}).getId());
	if (r.getSuccess()) {
	    return r.getParameters()[0].intValue();
	}
	return -1;
    }
					     

    public  Membership[] getMembership(int persNo)
    throws IOException {
	return Membership.createFrom(waitFor(doGetMembership(persNo).getId()));
    }

    public  Membership[] getUnreadMembership() {
	Membership[] m = new Membership[unreadMembership.size()];
	for (int i=0; i < m.length; i++) {
	    m[i] = (Membership) unreadMembership.get(i);
	}
	return m;
    }
    /**
     * return array of unread texts (in TextMapping for) for all
     * conferences. You should call updateUnreads before this */
    public  TextMapping[] getUnreadTexts() {
	TextMapping[] map = new TextMapping[unreadTexts.size()];
	Iterator iter = unreadTexts.iterator();
	for (int i=0; i < unreadTexts.size(); i++) {
	    map[i] = (TextMapping) iter.next();
	}
	return map;
    }
    
    /** get-uconf-stat **/
    public  RpcCall doGetUConfStat(int confNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_uconf_stat).
	    add(new KomToken(confNo));

	writeRpcCall(req);
	return req;
    }
    public UConference getUConfStat(int confNo)
    throws IOException, RpcFailure {
	UConference cc = conferenceCache.getUConference(confNo);
	if (cc != null) return cc;
	RpcCall req = doGetUConfStat(confNo);	
	Debug.println("uconf-stat for " + confNo + " not in cache, asking server");
	RpcReply rep = waitFor(req.getId());
	if (rep.getSuccess()) {	    
	    cc = new UConference(confNo, rep.getParameters());	
	    conferenceCache.add(cc);
	    return cc;
	} else {
	    throw rep.getException();
	}
    }

    public  void endast(int no) 
    throws IOException {
	endast(currentConference, no);
    }

    public  void endast(int confNo, int no)
    throws IOException {
	int highest = getUConfStat(confNo).getHighestLocalNo();
	setLastRead(confNo, highest-no);
    }

    /** set-last-read **/
    public  RpcCall doSetLastRead(int confNo, int textNo) 
    throws IOException {
	return writeRpcCall(new RpcCall(count(), Rpc.C_set_last_read).
			    add(new KomToken(confNo)).
			    add(new KomToken(textNo)));
    }

    public  void setLastRead(int confNo, int textNo)
    throws IOException {
	waitFor(doSetLastRead(confNo, textNo).getId());
    }


    /** get-conf-stat **/
    public  RpcCall doGetConfStat(int confNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_conf_stat).
	    add(new KomToken(confNo));

	writeRpcCall(req);
	return req;
    }
    public  Conference getConfStat(int confNo)
    throws IOException {
	Conference cc = conferenceCache.getConference(confNo);
	if (cc != null) return cc;

	RpcReply reply = waitFor(doGetConfStat(confNo).getId());

	Conference c = new Conference(confNo, reply.getParameters());
	conferenceCache.add(c);
	return c;
    }

    /** change-conference **/
    public  void changeConference(int confNo)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doChangeConference(confNo).getId());
	if (reply.getSuccess()) {
	    currentConference = confNo;
	} else {
	    throw reply.getException();
	}
    }

    public  RpcCall doChangeConference(int confNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_change_conference)
	    .add(confNo);
	writeRpcCall(req);
	return req;
    }

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

    public void addMember(int confNo, int persNo, int prio, int listPos, boolean invitation,
			  boolean passive, boolean secret)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doAddMember(confNo, persNo, prio, listPos,
					     new MembershipType(new boolean[] { invitation, passive, secret })).getId());
	if (!reply.getSuccess()) {
	    throw reply.getException();
	}
    }

    public RpcCall doChangeName(int confNo, String newName)
    throws IOException, RpcFailure {
	RpcCall req = new RpcCall(count(), Rpc.C_change_name);
	req.add(new KomToken(confNo));
	req.add(new Hollerith(newName));
	writeRpcCall(req);
	return req;
    }

    public void changeName(int confNo, String newName)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doChangeName(confNo, newName));
	if (!reply.getSuccess()) throw reply.getException();
    }

    public void setPresentation(int confNo, int textNo)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doSetPresentation(confNo, textNo));
	if (!reply.getSuccess()) throw reply.getException();
	conferenceCache.removeAll(confNo);
    }

    public RpcCall doSetPresentation(int confNo, int textNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_set_presentation);
	req.add(new KomToken(confNo));
	req.add(new KomToken(textNo));
	writeRpcCall(req);
	return req;
    }

    public void joinConference(int confNo) throws IOException, RpcFailure {
	addMember(confNo, getMyPerson().getNo(), 100, getMyPerson().noOfConfs+1, false, false, false);
	membership = getMyMembershipList();
    }

    /** get-person-stat **/
    public  RpcCall doGetPersonStat(int persNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_person_stat).
	    add(persNo);
	
	writeRpcCall(req);
	return req;
    }
    public  Person getPersonStat(int persNo)
    throws IOException {
	Person p = personCache.get(persNo);
	if (p != null) return p;

	RpcReply reply = waitFor(doGetPersonStat(persNo).getId());

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
    throws IOException {
	if (confNo == 0)
	    return "Conference 0".getBytes();
	UConference c = getUConfStat(confNo);
	
	return c == null ? null : c.getName();
    }

    public  RpcCall doGetTextStat(int textNo)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_get_text_stat).
	    add(new KomToken(textNo));

	writeRpcCall(req);
	return req;
    }

    /**
     * Returns a TextStat object for a given text number,
     * or null if the text doesn't exist or is inaccessible.
     *
     * As with getText(), getTextStat() also currently always 
     * returns a cached object if available.
     *
     * LysKOM call: get-text-stat
     */
    public  TextStat getTextStat(int textNo)
    throws IOException {

        TextStat ts = textStatCache.get(textNo);
        if (ts != null) return ts;

	Text text = textCache.get(textNo);
	if (text != null && text.getStat() != null)
	    return text.getStat();

	RpcReply reply = waitFor(doGetTextStat(textNo).getId());
	if (reply.getSuccess()) {
	    ts = TextStat.createFrom(textNo, reply);
	    textStatCache.add(ts);
	    return ts;
	} else {
	    int error = reply.getException().getError();
	    if (error == Rpc.E_no_such_text) return null;
	    throw reply.getException();
	}

    }
    
    public boolean getLoggedIn() {
	return loggedIn;
    }

    public  void logout(boolean block)
    throws IOException {
	RpcCall logoutCall = new RpcCall(count(), Rpc.C_logout);

	writeRpcCall(logoutCall);
	loggedIn = false;
	state = STATE_CONNECTED;
	if (!block) return;
	
	waitFor(logoutCall.getId());
    }

    public void writeRaw(int id, String s) throws IOException {
	int seq = count();
	OutputStream out = connection.getOutputStream();
	byte[] bytes = (seq + " " + id + ((s != null) ? " " + s : "") + "\n")
	    .getBytes();
	    Debug.println("raw RPC call: " + new String(bytes));
	out.write(bytes);
		 
    }

    private synchronized int count() {
	rpcCount++;
	return lastRpcCall += random.nextInt(63)+1;
    }

    public KomTokenReader getKomTokenReader() {
	return reader;
    }

    public  RpcCall doCreateText(Text t)
    throws IOException {
	t.trimContents();
	t.getStat().addAuxItem(new AuxItem(AuxItem.tagCreatingSoftware,
					   new Bitstring("00000000"), 0,
					   new Hollerith("lattekom 0.1")));
	t.getStat().addAuxItem(new AuxItem(AuxItem.tagContentType,
					   new Bitstring("00000000"), 0,
					   new Hollerith("x-lattekom/basic")));
	return doCreateText(t.getContents(), t.getStat().getMiscInfo(),
			    t.getStat().getAuxItems());
    }


    /**
     * Very simple reply function. Application developers are encouraged to 
     * use createText() instead
     */
    public  int reply(int textNo, Text t)
    throws IOException {
	Text commented = getText(textNo);
	int[] rcpts = commented.getRecipients();
	for (int i=0; i < rcpts.length; i++) 
	    t.addRecipient(rcpts[i]);
	t.addCommented(textNo);
	return createText(t);
    }

    /**
     * Creates a text on the LysKOM server. Sort of works, actually.
     *
     * @param text byte-array containing the article text
     * @param miscInfo List of Selections with Misc-Info stuff (recipients, etc)
     * @param auxItems Auxiallry items (Aux-Item) attached to this article
     *
     * LysKOM call: create-text
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


	RpcCall req = new RpcCall(count(), Rpc.C_create_text).
	    add(new Hollerith(text)).
	    add((KomToken) new KomTokenArray(miscInfo.size(), miscInfo)).
	    add((KomToken) auxTokenArray);
	writeRpcCall(req);
	return req;	
    }

    /**
     * Wrapper for other createText()
     */
    public  int createText(Text t)
    throws IOException {
	RpcReply reply = waitFor(doCreateText(t).getId());

	if (!reply.getSuccess())
	    throw reply.getException();

	return reply.getParameters()[0].toInteger();
    }

    private RpcReply waitFor(RpcCall r) throws IOException {
	return waitFor(r.getId());
    }

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

    public void modifyAuxInfo(boolean isConf, int objNo,
			      int[] delAuxNo, AuxItem[] addAux)
    throws IOException, RpcFailure {
	RpcReply reply = waitFor(doModifyAuxInfo(isConf, objNo,
						 delAuxNo, addAux));
	if (!reply.getSuccess()) throw reply.getException();
    }

    public RpcCall doChangeWhatIAmDoing(String s) throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_change_what_i_am_doing);
	req.add(new Hollerith(s));
	writeRpcCall(req);
	return req;
    }

    public void changeWhatIAmDoing(String s) throws IOException {
	RpcReply reply = waitFor(doChangeWhatIAmDoing(s));
	if (!reply.getSuccess()) throw reply.getException();
    }


    /** who-is-on-dynamic **/
    public synchronized RpcCall doWhoIsOnDynamic (boolean wantVisible, boolean wantInvisible, int activeLast)
    throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_who_is_on_dynamic);
	req.add(wantVisible ? "1" : "0").
	    add(wantInvisible ? "1" : "0").
	    add(activeLast);
	writeRpcCall(req);
	return req;
    }

    public DynamicSessionInfo[] whoIsOnDynamic (boolean wantVisible, boolean wantInvisible, int activeLast)
	throws IOException {
	Debug.println("whoIsOnDynamic called");
	RpcReply reply = waitFor(doWhoIsOnDynamic(wantVisible, wantInvisible, activeLast).getId());
	Debug.println("waitFor returned");

	KomToken[] parameters = reply.getParameters();
	DynamicSessionInfo[] ids = new DynamicSessionInfo[parameters[0].toInteger()];
	KomToken[] sessionData = ((KomTokenArray) parameters[1]).getTokens();


	for (int i=0, j=5 ; i < ids.length ; i++, j = j + 6)
	    ids[i] = new DynamicSessionInfo(sessionData[j-5].toInteger(),
					    sessionData[j-4].toInteger(),
					    sessionData[j-3].toInteger(),
					    sessionData[j-2].toInteger(),
					    new Bitstring(sessionData[j-1]),
					    sessionData[j].getContents());
	
	
	return ids;
    }


    public RpcCall doDeleteText(int textNo) throws IOException {
	RpcCall req = new RpcCall(count(), Rpc.C_delete_text).add(new KomToken(textNo));
	writeRpcCall(req);
	return req;
    }

    public void deleteText(int textNo)throws IOException, RpcFailure {
	RpcReply reply = waitFor(doDeleteText(textNo));
	if (!reply.getSuccess()) throw reply.getException();
    }


    /**
     * lookup names of persons/conferences. 
     *
     * @param name (sub)string naming the person(s)/conference(s)
     * @param wantPersons Return persons matching @tt{name}
     * @param wantConfs Return conferences matching @tt{name}
     *
     * LysKOM call: lookup-z-name
     */

    
    // TODO: return Conf-Z-Info-List (ConfInfo[]?)
    public synchronized ConfInfo[] lookupName(String name, boolean wantPersons,
					      boolean wantConfs)
    throws IOException {
	int id = count();
	RpcCall lcall = new RpcCall(id, Rpc.C_lookup_z_name);
	lcall.add(new Hollerith(name)).
	    add(wantPersons ? "1" : "0").
	    add(wantConfs ? "1" : "0");


	writeRpcCall(lcall);

	RpcReply reply = waitFor(id);

	if (!reply.getSuccess()) {
	    return null;
	}
	KomToken[] parameters = reply.getParameters();
	ConfInfo[] ids = new ConfInfo[parameters[0].toInteger()];
	KomToken[] confData = ((KomTokenArray) parameters[1]).getTokens();

	for (int i=0, j=2 ; i < ids.length ; i++, j = j + 3)
	    ids[i] = new ConfInfo(confData[j-2].getContents(),
				  new ConfType(confData[j-1]),
				  confData[j].toInteger());

	return ids;
    }

    public boolean sendMessage(int recipient, String message)
    throws IOException, RpcFailure {
	RpcCall msgCall = new RpcCall(count(), Rpc.C_send_message).
	    add(new KomToken(recipient)).add(new Hollerith(message));

	writeRpcCall(msgCall);
	RpcReply reply = waitFor(msgCall.getId());
	if (!reply.getSuccess()) throw reply.getException();
	return true;
    }

    /**
     * This methods provides a synchronous way of waiting for RPC
     * replies. Note that this method should never be called from
     * the MessageListener thread (for example, you may not call
     * this method from inside an AsynchMessageListener's
     * asynchMessage() method.
     */
    private RpcReply waitFor(int id)
    throws IOException {
	RpcCall rcfjant = null;
	RpcReply reply = null;
	int waited = 0;
	while (rcfjant == null) {
	    synchronized (rpcHeap) {
		try {
		    Debug.println("Waiting for RPC reply to command #" + id);
		    if (Thread.currentThread() == listener.getThread()) {
			throw new IOException("waitFor() called from asynch dispatcher thread");
		    } else {
			if (null == (rcfjant = rpcHeap.getRpcCall(id, true)))
			    rpcHeap.wait(DEFAULT_TIMEOUT);
			else
			    return rcfjant.getReply();
		    }

		} catch (InterruptedException ex1) {
		    // ...?
		}
		rcfjant = rpcHeap.getRpcCall(id, true);
	    }

	    if (rcfjant == null) {
		//wakeOnReplyFrom = id;
		waited += DEFAULT_TIMEOUT;
		if (waited > MAX_TIMEOUT) {
		    IOException e = new IOException("Time out waiting for RPC reply #"+id);
		    e.printStackTrace();
		    throw(e);
		}
	    } else {
		reply = rcfjant.getReply();
	    }
	}
	//wakeOnReplyFrom = -1;
	return reply;
    }



    public void rpcEvent(RpcEvent e) {
	switch(e.getOp()) {
	case Rpc.C_query_read_texts:
	    int conf = e.getCall().getParameter(1).toInteger();
	    Membership m = membershipCache.get(conf);
	    if (m == null) {
		m = membershipCache.add(new Membership(0,
						       e.getReply().
						       getParameters()));
	    }
	    break;
	}
    }

    public void rpcReply(RpcReply r) {
	if (false) {
	    if (r.getSuccess())
		Debug.print("OK, ");
	    else
		Debug.print("ERROR, ");

	    Debug.print("ID="+r.getId());
	    KomToken[] foo = r.getParameters();
	    for (int i=0;i<foo.length;i++) {
		Debug.print(" ["+foo[i].toString()+"]");
	    }
	    Debug.println("!");
	}

	RpcCall originCall = rpcHeap.getRpcCall(r.getId(), false);
	if (originCall != null) {
	    originCall.setReply(r);
	}

	//if (wakeOnReplyFrom == r.getId())
	//mainThread.interrupt();

	if (originCall != null) {
	    notifyRpcEventListeners(new RpcEvent(this, originCall));
	}

    }

    /**
     * http://www.lysator.liu.se/lyskom/protocol/10.4/protocol-a.html#About%20Asynchronous%20Messages
     *
     * An asynchronous message is sent as a colon immediately followed by the
     * number of message parameters, the message number and the message
     * parameters. For example, message number 5 could be sent as:
     * <pre>   :3 5 119 11HDavid Byers 13HDavid C Byers</pre>
     *
     *
     */
    public void asynchMessage(AsynchMessage m) {
	Debug.println("Session.asynchMessage(): "+m.toString());

	KomToken[] parameters = m.getParameters();

	int textNo = 0;
	
	switch (m.getNumber()) {
	case Asynch.login:
	    Debug.println("asynch-login");
	    break;
	case Asynch.new_text_old:
	    /* if the new text is a comment to a previously read and cached text,
	     * the cached copy of the commented text's text-stat must be
	     * invalidated or refreshed
	     */
	    textNo = parameters[0].intValue();
	    textCache.remove(textNo);
	    textStatCache.remove(textNo);
	    Debug.println("async-new-text-old for text " + textNo);
	    break;
	case Asynch.new_name:
	    conferenceCache.removeAll(parameters[0].intValue());
	    break;
	case Asynch.deleted_text:
	    textNo = parameters[0].intValue();
	    Debug.println("async-deleted-text for text " + textNo);
	    textCache.remove(textNo);
	    textStatCache.remove(textNo);
	    break;

	case Asynch.new_recipient:
	    textNo = parameters[0].intValue();
	    Debug.println("async-new-recipient for text " + textNo);
	    textCache.remove(textNo);
	    break;

	case Asynch.sub_recipient:
	    textNo = parameters[0].intValue();
	    Debug.println("async-sub-recipient for text " + textNo);	    
	    textCache.remove(textNo);
	    break;	    
	}

    }

    

}

