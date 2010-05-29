<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*, nu.dll.app.weblatte.*, java.net.*, java.io.*, java.text.*,java.util.regex.*, java.nio.*, java.nio.charset.*, ii.ImageInfo' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<!-- %@ page errorPage='fubar.jsp' % -->
<%@ include file='kom.jsp' %>
<%@ page import='javax.mail.BodyPart, javax.mail.internet.*' %>
<%!
    public void addComments(Session lyskom, TextStat stat, List textNumbers, int maxTextsToShow)
    throws IOException, RpcFailure {
	int[] comments = stat.getComments();
	for (int i=0; textNumbers.size() < maxTextsToShow && i < comments.length; i++) {
	    TextStat commentStat;
	    try {
		commentStat = lyskom.getTextStat(comments[i]);
	    } catch (RpcFailure ex1) {
		if (ex1.getError() == Rpc.E_no_such_text) {
		    continue;
		}
		throw ex1;
	    }
	    for (Iterator iter = commentStat.getAllRecipients().iterator();
		 iter.hasNext();) {
		if (lyskom.isMemberOf(((Integer)iter.next()).intValue())) {
		    Integer commentObj = new Integer(comments[i]);
		    if (!textNumbers.contains(commentObj) && 
			!lyskom.getReadTexts().contains(comments[i])) {
			textNumbers.add(commentObj);
			addComments(lyskom, commentStat, textNumbers, maxTextsToShow);
		    }
		}
	    }
	}
    }
%>
<%
    Map parameters = parseQueryString(request.getQueryString(), "iso-8859-1");
    Enumeration penum = request.getParameterNames();
    while (penum.hasMoreElements()) {
	String name = (String) penum.nextElement();
	String[] values = request.getParameterValues(name);
	for (int i=0; i < values.length; i++) {
	    byte[] b = values[i].getBytes("iso-8859-1");
	    String s = new String(b, "utf-8");
	    if (!s.equals(values[i])) {
	       // means that the parameter probably was incorrectly
	       // decoded using iso-8859-1 but sent as utf-8 by the
	       // browser
	       values[i] = s;
	    }
	}
	if (values == null || values.length == 0) {
	    parameters.put(name, "");
	} else if (values.length == 1) {
	    parameters.put(name, values[0]);
	} else {
	    parameters.put(name, values);
	}
    }
    if (request.getAttribute("parsed-parameters") != null) {
	parameters.putAll((Map) request.getAttribute("parsed-parameters"));
    }
    request.setAttribute("parsed-parameters", parameters);

    if (request.getQueryString() != null) {
	Debug.println("query: " + request.getQueryString());
    } else {
	Debug.println("no query string");
    }


    String server = parameter(parameters, "server") != null ? 
	parameter(parameters, "server") : Servers.defaultServer.hostname;
    Boolean authenticated = (Boolean) session.getAttribute("LysKOMauthenticated");
    if (authenticated == null) authenticated = Boolean.FALSE;
    String error = null;
    boolean justLoggedIn = false;

    if (lyskom != null && parameter(parameters, "suspend") != null) {
	List suspendedSessions = (List) session.getAttribute("lyskom.suspended");
	if (suspendedSessions == null) {
	    suspendedSessions = new SuspendedSessionList();
	    session.setAttribute("lyskom.suspended", suspendedSessions);
	}
	synchronized (suspendedSessions) {
	    lyskom.changeWhatIAmDoing("Pausar Weblatte");
	    lyskomWrapper.setSuspended(true);
	    suspendedSessions.add(lyskomWrapper);
	}
	session.removeAttribute("lyskom");
	session.removeAttribute("LysKOMauthenticated");
	String target = basePath;
	if (request.getServerName().startsWith("s-")) {
	    target = "http://" + baseHost + basePath;
	}
	response.sendRedirect(target + "?r=" + Integer.toHexString(rnd.nextInt()));
	return;
    }

    try {
	if (parameter(parameters, "lyskomNamn") != null ||
	    (lyskom == null && session.getAttribute("lyskomPersonNo") != null)) {

	    if (lyskom == null) {
		lyskom = new Session();
	    }
	    int port = 4894;
	    if (!lyskom.getConnected()) {
		lyskom.setClientHost(request.getRemoteAddr());
		lyskom.setClientUser("www");
	    	Debug.println("Connecting to " + server + ":" + port + "...");
		lyskom.connect(server, port);
		Debug.println("Connected.");
		lyskom.setBigTextEnabled(true);
		LinkedList messages = new LinkedList();
		lyskom.setAttribute("weblatte.messages", messages);
		lyskom.addAsynchMessageReceiver(new MessageReceiver(messages));
	    }
	    ConfInfo[] names = null;
	    int person = 0;
	    String password = (String) session.getAttribute("lyskomPassword");
	    if (password == null) password = parameter(parameters, "lyskomLosen");

	    if (parameter(parameters, "lyskomNamn") != null) {
		String namnParam = parameter(parameters, "lyskomNamn");
		if (parameter(parameters, "createPerson") != null) {
		    try {
			lyskom.createPerson(namnParam, password, new Bitstring("00000000"),
					    new AuxItem[0]);
		    } catch (RpcFailure ex1) {
			person = -1;
			switch (ex1.getError()) {
			case Rpc.E_login_first:
			    error = "Du måste vara inloggad för att skapa en ny person.";
			    break;
			case Rpc.E_permission_denied:
			    error = "Du har inte tillräckligt med rättighet att skapa en ny person";
			    break;
			case Rpc.E_person_exists:
			    error = "Angivet namn är upptaget.";
			    break;
			case Rpc.E_illegal_aux_item:
			    error = "...";
			    break;
			case Rpc.E_index_out_of_range:
			    error = "KOM-servern har nått sin max-gräns för antalet skapade möten/personer.";
			    break;
			default:
			    throw ex1;
			}
		    }
		}
		if (namnParam.startsWith("#")) namnParam = "Person " + namnParam.substring(1);
		if (namnParam.toLowerCase().startsWith("person ")) {
		    StringTokenizer st = new StringTokenizer(namnParam);
		    st.nextToken();
		    person = Integer.parseInt(st.nextToken());
		}
		if (person == 0) {
		    names = lyskom.lookupName(parameter(parameters, "lyskomNamn"), true, false);
		    if (names.length == 1) person = names[0].getNo();
		}
	    } else {
		person = ((Integer) session.getAttribute("lyskomPersonNo")).intValue();
	    }
	    if (person > 0) {
		if (!lyskom.login(person, password,
				  parameter(parameters, "lyskomDold") != null, false)) {
		    error = "Felaktigt lösenord!";
		    lyskom.shutdown();
		} else {
		    Debug.println("User " + person + " logged on to " + lyskom.getServerName());
		    session.setAttribute("lyskom", new SessionWrapper(lyskom));
		    Cookie serverCookie = new Cookie("kom-server", server);
		    serverCookie.setMaxAge(31536000);
		    response.addCookie(serverCookie);
		    authenticated = Boolean.TRUE;
                    justLoggedIn = true;
		    if (parameters.containsKey("mini")) {
                        lyskom.setAttribute("weblatte.minimalistic", Boolean.TRUE);
			minimalistic = true;
		    }
		    lyskom.setLatteName("Weblatte " + Version.getVersion());
		    lyskom.setClientVersion("Weblatte", Version.getVersion() +
					    (Debug.ENABLED ? " (debug)" : ""));
		    lyskom.changeWhatIAmDoing("kör web-latte");
		}
	    } else if (names != null && names.length == 0) {
		error = "Namnet du angav (\"" + htmlize(parameter(parameters, "lyskomNamn"), false) + "\") " +
		    "finns inte. Välj \"Registrera ny användare\" för att skapa en ny KOM-person.";
	    } else if (names != null && names.length > 1) {
                request.setAttribute("ambiguous-name", names);
                /*
		StringBuffer buf = new StringBuffer("Flertydigt namn, följande matchar:<br/>\n<ul>");
		for (int i=0; i < names.length; i++) 
		    buf.append("<li>").append(lookupName(lyskom, names[i].getNo(), true)).append("\n");
		error = buf.append("</ul>\n").toString();
                */
                lyskom.shutdown();
                lyskom = null;
	    } else {
		error = "Ett fel uppstod.";
	    }
	}
    } catch (ConnectException ex1) {
	lyskom.shutdown();
	lyskom = null;
	error = "Det gick inte att ansluta till servern: " + ex1.getMessage();
    } catch (UnknownHostException ex2) {
	lyskom.shutdown();
	lyskom = null;
	error = "Det gick inte att slå upp serverns DNS-namn: " + ex2.getMessage();
    }

    try {
 	session.setAttribute("LysKOMauthenticated", authenticated);
        if (authenticated.booleanValue()) {
	    String gotoURL = (String) session.getAttribute("goto");
    	    if (gotoURL != null) {
		if (justLoggedIn) {
		    session.setAttribute("lyskom.justLoggedIn", Boolean.TRUE);
		}
	        session.removeAttribute("goto");
		Debug.println("redirecting according to goto-url: " + gotoURL);
	        response.sendRedirect(gotoURL);
	        return;
            }
	    Boolean justLoggedInObj = (Boolean) session.getAttribute("lyskom.justLoggedIn");
	    justLoggedIn = justLoggedIn || (justLoggedInObj != null && justLoggedInObj.booleanValue());
	    session.removeAttribute("lyskom.justLoggedIn");
        }
    } catch (IllegalStateException ex1) {}
    List messages = null;
    LinkedList reviewList = null;
    int interval = 120; // seconds
    if (authenticated.booleanValue()) {

	if (parameters.containsKey("markAsRead")) {
	    String[] values = request.getParameterValues("markAsRead");
	    Map conferences = new HashMap();
	    for (int i=0; i < values.length; i++) {
		TextStat readText = lyskom.getTextStat(Integer.parseInt(values[i]));
		lyskom.getReadTexts().add(readText.getNo());

		int[] rcpts = readText.getRecipients();
		int[] ccs = readText.getCcRecipients();
		int[] tmp = new int[rcpts.length+ccs.length];
		System.arraycopy(rcpts, 0, tmp, 0, rcpts.length);
		System.arraycopy(ccs, 0, tmp, rcpts.length, ccs.length);
		for (int j=0; j < tmp.length; j++) {
		    try {
			List locals = (List) conferences.get(new Integer(tmp[j]));
			if (locals == null) {
			    locals = new LinkedList();
			    conferences.put(new Integer(tmp[j]), locals);
			}
			locals.add(new Integer(readText.getLocal(tmp[j])));
		    
		    } catch (RpcFailure ex1) {
			if (ex1.getError() != Rpc.E_not_member)
			    throw ex1;
		    }
		}
	    }
	    for (Iterator i = conferences.entrySet().iterator(); i.hasNext();) {
		Map.Entry entry = (Map.Entry) i.next();
		int conf = ((Integer) entry.getKey()).intValue();
		List locals = (List) entry.getValue();
		int[] localTexts = new int[locals.size()];
		int k=0;
		for (Iterator j=locals.iterator(); j.hasNext(); k++) {
		    localTexts[k] = ((Integer) j.next()).intValue();
		}
		try {
		    lyskom.markAsRead(conf, localTexts);
		} catch (RpcFailure ex1) {
		    if (ex1.getError() != Rpc.E_not_member) throw ex1;
		}
	    }
	}

    	if (parameter(parameters, "privateReply") != null) {
	    request.setAttribute("set-uri", makeAbsoluteURL("composer.jsp"));
	    RequestDispatcher d = getServletContext().getRequestDispatcher(appPath + "/composer.jsp");
	    d.forward(request, response);
	    return;	
    	}
    	if (parameter(parameters, "changePresentation") != null &&
	    parameter(parameters, "createText") == null) {
	    request.setAttribute("set-uri", makeAbsoluteURL("composer.jsp"));
	    RequestDispatcher d = getServletContext().getRequestDispatcher(appPath + "/composer.jsp");
	    d.forward(request, response);
	    return;		
    	}
	if (parameter(parameters, "dispatchToComposer") != null) {
	    request.setAttribute("set-uri", makeAbsoluteURL("composer.jsp"));
	    RequestDispatcher d = getServletContext().getRequestDispatcher(appPath + "/composer.jsp");
	    d.forward(request, response);
	    return;
    	}
    }
%>
<html>
<head>
<% if (authenticated.booleanValue()) { %>
<title>Weblatte: <%= serverShort(lyskom) %></title>
<% } else { %>
<title>Weblatte LysKOM-klient</title>
<% } %>
<link rel="stylesheet" href="lattekom.css">
</head>
<body>
<%
    if (error != null) {
%>
<div class="statusError"><%= error %></div>
<%
    }
    if (parameter(parameters, "debug") != null) {
	debug = true;
	out.print("<pre>");
	Map info = lyskom != null ? lyskom.getInfo() : new HashMap();
	for (Iterator i = info.entrySet().iterator(); i.hasNext();) {
	    Map.Entry entry = (Map.Entry) i.next();
	    out.println("info-key \"" + entry.getKey() + "\", type: " +
		entry.getValue().getClass().getName() + ", value: &lt;" + 
		entry.getValue().toString() + "&gt;");
	}
	out.println("------");
	if (lyskom != null) {
	    Person me = lyskom.getMyPerson();
	    out.println("User area: " + me.getUserArea());
	    userArea = lyskom.getUserArea();
	    if (userArea != null) {
		String[] blocks = userArea.getBlockNames();
		for (int i=0; i < blocks.length; i++) {
		    String blockName = blocks[i];
		    Hollerith block = userArea.getBlock(blockName);
		    out.println("User-Area block <a href=\"?debug&uablock=" +
			blockName + "\">\"" + blockName + "\"</a>, " +
			block.getContents().length + " bytes");
		}

		if (parameter(parameters, "uablock") != null) {
		    String blockName = parameter(parameters, "uablock");
		    Hollerith block = userArea.getBlock(blockName);
		    out.println("------ Block \"" + blockName + "\" contents:");
		    out.println(block.getContentString());
		    out.println("------");
		}
	    }
	}
	
	out.println("</pre>");
    }
    if (parameter(parameters, "invalidate") != null) {
	session.invalidate();
	authenticated = Boolean.FALSE;
	%>
	<h2>utloggad.</h2>
	<div>
	[ <a href="<%= basePath %>">logga in</a> ]
	</div>
	<%
    }
    if (parameter(parameters, "logout") != null) {
	if (lyskom != null) {
	    lyskom.shutdown();
	}
	List suspendedSessions = (LinkedList) session.getAttribute("lyskom.suspended");
	if (suspendedSessions != null && suspendedSessions.size() > 0) {
	    session.removeAttribute("lyskom");    
	    session.removeAttribute("LysKOMauthenticated");

	    if (redirectHack(response, out, basePath + "sessions.jsp?loggedOut")) return;

	} else {
	    if (session != null) session.invalidate();
	}
	authenticated = Boolean.FALSE;
	%>
	<h2>utloggad.</h2>
	<div>
	[ <a href="<%= basePath %>">logga in</a> ]
	</div>
	<%
    } else if (authenticated.booleanValue()) {
	%><%@ include file='prefs_inc.jsp' %><%
	if (justLoggedIn && preferences.getBoolean("start-in-frames-mode")) {
	    if (redirectHack(response, out, basePath + "frames.jsp?conference=0")) return;

	}
        reviewList = (LinkedList) lyskom.getAttribute("lyskom.review-list");
        if (reviewList == null || parameter(parameters, "conference") == null) {
	    Debug.println("*** creating new review-list");	
	    reviewList = new LinkedList();
	    lyskom.setAttribute("lyskom.review-list", reviewList);
    	}
    }
    if (parameter(parameters, "pom") != null) {
	session.setAttribute("pom", new Boolean(parameter(parameters, "pom").equals("true")));
    }

    boolean showPOM = preferences != null && preferences.getBoolean("show-plain-old-menu");
    boolean showWelcome = parameter(parameters, "hw") == null &&
		(preferences != null && preferences.getBoolean("always-show-welcome"));

    boolean popupComment = request.getParameter("popupComment") != null;
    request.setAttribute("popupComment", popupComment ? Boolean.TRUE : null);

    boolean showStandardBoxes = !minimalistic && parameter(parameters, "hs") == null
	  && preferences != null && !preferences.getBoolean("hide-standard-boxes"); // "hide standard boxes"
    try {
	showPOM = session.getAttribute("pom") != null ? 
	    ((Boolean) session.getAttribute("pom")).booleanValue() : showPOM;
    } catch (IllegalStateException ex1) {}

    if (authenticated.booleanValue()) {
	if (true) {
	    if (debug) {
		out.print("<pre style=\"color: blue;\">DEBUGLÄGE: ");
		out.print("Person: " + lyskom.getMyPerson().getNo());
		out.print(", User-Area: " + lyskom.getMyPerson().getUserArea());
		out.print(", Session: " + lyskom.whoAmI());
		out.print(", Wrapper: " + Integer.toHexString(System.identityHashCode(lyskomWrapper)));
		out.println("</pre>");
	    }
	    if (justLoggedIn || showWelcome) {
%>
    	<div class="welcome">Välkommen till LysKOM, <%= lookupName(lyskom, lyskom.getMyPerson().getNo(), true) %>!<br>
	    Din LysKOM-server är <%= lyskom.getServerName() %>.
	</div>
<%
		if (!minimalistic) {
%>
		<div class="intro">Högerklicka på en tom yta för att visa menyn. Du kan även högerklicka på personnamn,
		mötesnamn och textnummer för att få fram menyer specifika för objektet i fråga.</div>
<%
	    	}
	    }
    if (Debug.ENABLED) Debug.println("unreadconfslist: " + lyskom.getMyUnreadConfsList());
    if (parameter(parameters, "changeName") != null) {
	String oldName = parameter(parameters, "changeName");
	String newName = parameter(parameters, "newName");
	int confNo = 0;
	ConfInfo ci = null;
	try {
	    ci = lookupName(lyskom, oldName, true, true);
	    if (ci != null) {
 	    	confNo = ci.getNo();
	    	lyskom.changeName(confNo, newName);
	    	out.println("<div class=\"statusSuccess\">OK: \"" +
		    htmlize(ci.getNameString(), false) + "\" har bytt namn till " +
		    lookupName(lyskom, confNo, true) + "</div>");
	    } else {
		out.println("<div class=\"statusError\">Fel: namnet \"" + 
			htmlize(oldName, false) + "\" finns inte.</div>");
	    }
	} catch (RpcFailure ex1) {
	    switch (ex1.getError()) {
		case Rpc.E_permission_denied:
		out.println("<div class=\"statusError\">Fel: du har inte rättighet att " +
			"ändra namn på \"" + lookupName(lyskom, confNo, true) + "\"</div>");
		break;
		case Rpc.E_conference_exists:
		out.println("<div class=\"statusError\">Fel: det angivna namnet är upptaget.</div>");
		break;
	    }
	} catch (AmbiguousNameException ex2) {
	    out.println(ambiguousNameMsg(lyskom, oldName, ex2));
	}
    }

    if (parameters.containsKey("delete")) {
	int textNo = Integer.parseInt(parameter(parameters, "delete"));
	try {
	    lyskom.deleteText(textNo);
	    out.println("<div class=\"statusSuccess\">OK: text " + 
			textLink(request, lyskom, textNo) + " är borttagen.</div>");
	} catch (RpcFailure ex1) {
	    switch (ex1.getError()) {
	    case Rpc.E_not_author:
		out.println("<div class=\"statusError\">Fel: du är inte " +
			    "textens skapare.</div>");
		break;
	    case Rpc.E_no_such_text:
		out.println("<div class=\"statusError\">Fel: det finns ingen " +
			    "sådan text (" + ex1.getErrorStatus() + ").</div>");
		break;
	    default:
		throw ex1;

	    }
	}
    }
    if (parameter(parameters, "mark") != null) {
	lyskom.markText(Integer.parseInt(parameter(parameters, "mark")), commonPreferences.getInt("default-mark"));
	out.println("<div class=\"statusSuccess\">Text " +
		textLink(request, lyskom,
		Integer.parseInt(parameter(parameters, "mark")))
		+ " har markerats.</div>");
	session.setAttribute("weblatte.marks", null);
    }
    if (parameter(parameters, "unmark") != null) {
	lyskom.unmarkText(Integer.parseInt(parameter(parameters, "unmark")));
	out.println("<div class=\"statusSuccess\">Text " +
		parameter(parameters, "unmark") + " har avmarkerats.</div>");
	session.setAttribute("weblatte.marks", null);
    }

    if (parameter(parameters, "endast") != null) {
	int textcount = Integer.parseInt(parameter(parameters, "endast"));
	ConfInfo conf = null;
	try {
	    conf = lookupName(lyskom, parameter(parameters, "endastConferenceName"), true, true);
	    if (conf != null) {
	    	out.print("<div>Endast " + textcount + " inlägg i möte " +
			lookupName(lyskom, conf.getNo(), true) + "...");
	    	out.flush();
	    	lyskom.endast(conf.getNo(), textcount);
	    	out.println(" ok.</div>");
		//lyskom.removeAttribute("mbInited");
	    } else {
	    	%><div class="statusError">Fel: mötet finns inte.</div><%
	    }
            

	} catch (AmbiguousNameException ex1) {
	    %><div class="statusError">Fel: mötesnamnet är flertydigt. Följande mötesnamn matchar:<%
	    out.println("<ul>");
	    ConfInfo[] names = ex1.getPossibleNames();
	    for (int i=0; i < names.length; i++) 
		out.println("<li>" + lookupName(lyskom, names[i].getNo(), true));
	    out.println("</ul>");
	    %>
	    </div>
	    <%
	}
    }

    if (parameter(parameters, "join") != null || parameter(parameters, "joinNo") != null) {
	int confNo = parameter(parameters, "joinNo") != null ? 
		Integer.parseInt(parameter(parameters, "joinNo")) : 0;

	try {
	    ConfInfo conf = null;
	    if (parameter(parameters, "join") != null) {
		conf = lookupName(lyskom, parameter(parameters, "join"), false, true);
		if (conf != null) confNo = conf.getNo();
	    }
	    if (confNo > 0) {
		out.print("<div>Bli medlem i " + lookupName(lyskom, confNo, true) + "...");
		out.flush();
		lyskom.joinConference(confNo);
		out.print("OK!</div>");
		out.flush();
		lyskom.setAttribute("mbInited", Boolean.FALSE);
	    } else {
		out.println("<div class=\"statusError\">Fel: hittar inget sådant möte</div>");
	    }
	} catch (AmbiguousNameException ex1) {
	    out.println(ambiguousNameMsg(lyskom, ex1));
	} catch (RpcFailure ex2) {
	  if (ex2.getError() == Rpc.E_access_denied) {
	      out.println("misslyckades.</div><div class=\"statusError\">Fel: du får inte gå med i mötet.");
	      Conference conf = lyskom.getConfStat(ex2.getErrorStatus());
	      out.println("Administratör för mötet är " +
	          lookupName(lyskom, conf.getSuperConf(), true) + " - vänd dig dit för mer information.</div>");
	  }
	}
    }
 
    if (parameter(parameters, "leave") != null || parameter(parameters, "leaveNo") != null) {
	try {
	    int confNo = parameter(parameters, "leaveNo") != null ?
		Integer.parseInt(parameter(parameters, "leaveNo")) : 0;

	    if (parameter(parameters, "leave") != null) {
	        ConfInfo conf = lookupName(lyskom, parameter(parameters, "leave"), false, true);
		if (conf != null) confNo = conf.getNo();
	    }
	    if (confNo > 0) {
		out.print("<div>Utträda ur möte " + lookupName(lyskom, confNo, true) + "...");
		out.flush();
		lyskom.subMember(confNo, lyskom.getMyPerson().getNo());
		out.println("OK!</div>");
		out.flush();
		lyskom.setAttribute("mbInited", Boolean.FALSE);
	    } else {
		out.println("<div class=\"statusError\">Fel: hittar inget sådant möte</div>");
	    }
	} catch (AmbiguousNameException ex1) {
	    out.println(ambiguousNameMsg(lyskom, ex1));
 	} catch (RpcFailure ex2) {
	    out.println("misslyckades.</div><div class=\"statusError\">Fel: du är inte medlem i mötet.</div>");
	}
    }

    int me = lyskom.getMyPerson().getNo();
    Boolean mbInitedObj = (Boolean) lyskom.getAttribute("mbInited");
    if (mbInitedObj == null) mbInitedObj = Boolean.FALSE;

    if (parameters.containsKey("refresh")) {
	mbInitedObj = Boolean.FALSE;
	lyskom.clearCaches();
    }
    boolean manyMemberships = lyskom.getAttribute("many-memberships") != null &&
		((Boolean) lyskom.getAttribute("many-memberships")).booleanValue();

    manyMemberships = manyMemberships || preferences.getBoolean("many-memberships");
    lyskom.setAttribute("many-memberships", new Boolean(manyMemberships));

    if (!mbInitedObj.booleanValue()) {
	out.print("<div>Läser in medlemskapsinformation...");
	out.flush();
	List unreadConferencesList = lyskom.getMyUnreadConfsList();
	int unreadConferences = unreadConferencesList.size();

	if (unreadConferences > preferences.getInt("force-many-memberships")
	    && !manyMemberships) {
	    lyskom.setAttribute("many-memberships", Boolean.TRUE);
	    out.println("<b>OBS</b>: Du verkar ha olästa " +
		"i många möten (" + unreadConferences + "). " +
		"Funktionen \"många möten\" har automatiskt aktiverats, vilket " +
		"innebär att komplett medlemsskap aldrig läses in, samt att " +
		"nyhetslistan aldrig visar fler än fem möten åt gången. " +
	  	"För att aktivera \"många möten\" permanent, ändra dina " + 
		"<a href=\"prefs.jsp\">inställningar</a>.</div>");
	    out.flush();
	    manyMemberships = true;
	} else {
	    if (!manyMemberships) {
		lyskom.updateUnreads(preferences.getBoolean("prequery-local-texts"),
	  			     preferences.getInt("min-conference-priority"));
	    } else {
		out.println("(\"många möten\" aktiverad)...");
	    }
	    out.println("klart.</div>");
	    out.flush();
	}
	mbInitedObj = Boolean.TRUE;
    }    
    lyskom.setAttribute("mbInited", mbInitedObj);

    if (parameter(parameters, "autoRefresh") == null) {
	lyskom.doUserActive();
    }
    String lastReceivedOrSent = null;


    if (parameter(parameters, "sendToName") != null) {
	String stn = parameter(parameters, "sendToName");
	try {
	    if (parameter(parameters, "chat") != null) {
		if (!stn.trim().equals("")) {
	            ConfInfo conf = lookupName(lyskom, stn, true, true);
	            response.sendRedirect(basePath + "chat.jsp?default=" + conf.getNo());
	            return;
		} else {
		    response.sendRedirect(basePath + "chat.jsp");
		    return;
		}
	    } 
	    String _text = parameter(parameters, "sendText");
	    if (stn.trim().equals("")) {
		lyskom.sendMessage(0, _text);
		%><div class="statusSuccess">Alarmmeddelande skickat.</div><%
	    } else {
	    	ConfInfo recipient = lookupName(lyskom, stn, true, true);
	    	if (recipient != null) {
		    lyskom.sendMessage(recipient.getNo(), _text);
		    lastReceivedOrSent = lookupName(lyskom, recipient.getNo());
		    %><div class="statusSuccess">Meddelande skickat till <%=lookupName(lyskom, recipient.getNo(), true)%>.</div><%
	    	} else {
		    %><div class="statusError">Hittade ingen mottagare som matchade "<%=htmlize(stn, false)%>".</div><%
	    	}
	    }
	} catch (RpcFailure ex2) {
	    if (ex2.getError() == Rpc.E_message_not_sent) {
		%><div class="statusError">Meddelandet gick inte att skicka.</div><%
	    } else {
		throw ex2;
	    }
	} catch (AmbiguousNameException ex1) {
	    out.println(ambiguousNameMsg(lyskom, ex1));
	}
    }

    messages = (List) lyskom.getAttribute("weblatte.messages");
    if (messages != null && messages.size() > 0) {
	synchronized (messages) {
	    Iterator i = messages.iterator();
	    while (i.hasNext()) {
		AsynchMessage m = (AsynchMessage) i.next();
		if (m.getNumber() == Asynch.send_message) {
		    int recipient = m.getParameters()[0].intValue();
		    int sender    = m.getParameters()[1].intValue();
		    String text   = lyskom.toString(m.getParameters()[2].getContents());
		    if (recipient == lyskom.getMyPerson().getNo()) {
			lastReceivedOrSent = lookupName(lyskom, sender);
		    } else if (recipient != 0) {
			lastReceivedOrSent = lookupName(lyskom, recipient);
		    }
%>
		<div class="asynchMessage">
		<i>Meddelande från <%=lookupName(lyskom, sender, true)%> till
                      <%= recipient != 0 ? lookupName(lyskom, recipient, true) : "alla"%>:</i><br>
		<tt class="asynchMessage-text"><%=htmlize(text).replaceAll("\n", "<br/>")%></tt><br/>
		<small>(mottaget <%= df.format(m.getArrivalTime()) %>)</small>
		</div>
<%
		    out.flush();
    		    if (parameter(parameters, "saveMessages") == null) {
			i.remove();
		     }
		} else {
		    i.remove();
		}
	    }
	}
    }

    if (parameter(parameters, "stopChat") != null) {
        session.setAttribute("lyskom.chat-running", Boolean.FALSE);
    }

    if (parameter(parameters, "sendTo") != null) {
	lastReceivedOrSent = lookupName(lyskom, Integer.parseInt(parameter(parameters, "sendTo")));
    }

    List textNumbers = new LinkedList();
    int textNumber = 0;
    int conferenceNumber = 0;
    int newTextNo = 0;
    if (parameter(parameters, "purgeOtherSessions") != null) {
	out.println("<div><pre>Listar sessioner...");
	out.flush();
	int mySession = lyskom.whoAmI();
	DynamicSessionInfo[] sessions = lyskom.whoIsOnDynamic(true, true, 0);
	for (int i=0; i < sessions.length; i++) {
	    if (sessions[i].getPerson() == lyskom.getMyPerson().getNo() &&
		sessions[i].getSession() != mySession) {
		SessionInfo _session = lyskom.getStaticSessionInfo(sessions[i].getSession());
		out.print("Avslutar session nummer " + sessions[i].getSession()
			  + " från " + lyskom.toString(_session.getHostname())
			  + "...");
		out.flush();
		lyskom.disconnect(sessions[i].getSession());
		out.println(" OK.");
	    }
	}
	out.println("Klar.");
	out.println("</pre></div>");
	out.flush();
    }
    if (parameter(parameters, "conference") != null) {
	conferenceNumber = Integer.parseInt(parameter(parameters, "conference"));
    }
    if (parameters.containsKey("markAsReadHash")) {
	String checkHashStr = parameter(parameters, "markAsReadHash");
	//int checkHash = Integer.parseInt(parameter(parameters, "markAsReadHash"), 16);
	List markAsReadTexts = (List) session.getAttribute("mark-as-read-list");
	// XXX XXX: very duplicated code from markAsRead code above
	// with the exception that this code breaks up into 
	// multiple mark-as-read if no of texts to mark for a conf
	// is > 100
	int listHash = markAsReadTexts != null ? markAsReadTexts.hashCode() : 0;
	if (checkHashStr.equals(Integer.toHexString(listHash))) {
	    Map conferences = new HashMap();
	    for (Iterator i = markAsReadTexts.iterator(); i.hasNext();) {
		TextStat readText = lyskom.getTextStat(((Integer) i.next()).intValue());
		lyskom.getReadTexts().add(readText.getNo());
		
		int[] rcpts = readText.getRecipients();
		int[] ccs = readText.getCcRecipients();
		int[] tmp = new int[rcpts.length+ccs.length];
		System.arraycopy(rcpts, 0, tmp, 0, rcpts.length);
		System.arraycopy(ccs, 0, tmp, rcpts.length, ccs.length);
		for (int j=0; j < tmp.length; j++) {
		    try {
			List locals = (List) conferences.get(new Integer(tmp[j]));
			if (locals == null) {
			    locals = new LinkedList();
			    conferences.put(new Integer(tmp[j]), locals);
			}
			locals.add(new Integer(readText.getLocal(tmp[j])));
			
		    } catch (RpcFailure ex1) {
			if (ex1.getError() != Rpc.E_not_member)
			    throw ex1;
		    }
		}
	       
	    }
	    for (Iterator i = conferences.entrySet().iterator(); i.hasNext();) {
		Map.Entry entry = (Map.Entry) i.next();
		int conf = ((Integer) entry.getKey()).intValue();
		List _locals = (List) entry.getValue();
		Iterator _li = _locals.iterator();
		while (_li.hasNext()) {
		    List locals = new LinkedList();
		    for (int j=0; j < 100 && _li.hasNext(); j++) {
			locals.add(_li.next());
			_li.remove();
		    }
		    int[] localTexts = new int[locals.size()];
		    int k=0;
		    for (Iterator j=locals.iterator(); j.hasNext(); k++) {
			localTexts[k] = ((Integer) j.next()).intValue();
		    }
		    try {
			lyskom.markAsRead(conf, localTexts);
		    } catch (RpcFailure ex1) {
			if (ex1.getError() != Rpc.E_not_member) throw ex1;
		    }		    
		}
	    }
	} else {
	    log("Warning: invalid markAsReadHash: query was " + checkHashStr + " while " +
		"object hash was " + Integer.toHexString(listHash));
	}
    }
    session.setAttribute("mark-as-read-list", null);


    if (parameter(parameters, "createText") != null) {
	out.flush();
	Debug.println("index.jsp: dispatching to savetext.jsp...");
	RequestDispatcher d = getServletContext().getRequestDispatcher(appPath + "/savetext.jsp");
	d.include(request, response);
	newTextNo = ((Integer) request.getAttribute("new-text-no")).intValue();
    }
    if (parameters.containsKey("addRecipient")) {
	int rtype = Integer.parseInt(parameter(parameters, "recipientType"));
	int _textNo = Integer.parseInt(parameter(parameters, "toText"));
	ConfInfo lbx = lookupName(lyskom, (String) parameters.get("addRecipient"), true, true);
	if (lbx == null) {
	    out.println("<div class=\"statusError\">Inget möte matchade det angivna namnet.</div>");
	} else {
	    int confNo = lbx.getNo();
	    lyskom.addRecipient(_textNo, confNo, rtype);
	    out.println("<div class=\"statusSuccess\">OK: ny mottagare för " + textLink(request, lyskom, _textNo) + ": " +
		lookupName(lyskom, confNo, true) + ".</div>");
	}
    }

    if (parameter(parameters, "setPassword") != null) {
	ConfInfo lbx = lookupName(lyskom, (String) parameters.get("setPasswordPerson"), true, false);
	if (lbx == null) {
	    out.println("<div class=\"statusError\">Ingen person har det angivna namnet.</div>");
	} else {
	    if (parameters.get("setPasswordNewPassword").
		equals(parameters.get("setPasswordNewPasswordVerify"))) {
	    	lyskom.setPassword(lbx.getNo(), (String) parameters.get("setPasswordUserPassword"),
			           (String) parameters.get("setPasswordNewPassword"));
	    	out.println("<div class=\"statusSuccess\">Person " +
			    lookupName(lyskom, lbx.getNo(), true) + " har bytt lösenord.</div>");
	    } else {
		out.println("<div class=\"statusError\">Lösenorden stämde inte överens.</div>");
	    }
	}
	
    }

    if (!minimalistic) {
    	if (request.getHeader("User-Agent").indexOf("MSIE") >= 0) {

%>
<script language="JavaScript1.2">
<%@ include file='stuff.jsp' %>
</script>

<%
	} else {
%>
	<script language="JavaScript1.2" src="stuff.jsp?pleasecache"></script>
<%
 	}
%>
<%@ include file='dhtmlMenu.jsp' %>
<%
    }

    boolean listNews = false;
    Person person = lyskom.getMyPerson();
    Conference letterbox = lyskom.getConfStat(person.getNo());
    if (letterbox.getPresentation() == 0) {
%>
	<div class="notice">Du har ingen presentation.
	Varför inte <a href="<%=basePath%>?changePresentation=<%=person.getNo()%>">skriva en</a>?
	</div>
<%
    }
    if (showPOM) {
%>
   	<div class="nav">
	    [ <a href="<%=basePath%>?logout">logga ut</a>
              (<a title="Logga ut mina andra sessioner" href="<%=myURI(request)%>?purgeOtherSessions">övriga</a>) |
	      <a href="<%=basePath%>?listnews">lista nyheter</a> |
	      <a href="composer.jsp">skriv inlägg</a> |
              <% if (vemArVem) { %>
	      <a href="<%=basePath%>?uploadForm">ladda upp bild</a> | 
              <% } %>
              <a href="<%=basePath%>?reviewMarked">lista markerade</a> ]
	    <br/>
	    [ <a href="<%=basePath%>?setPasswordForm">ändra lösenord</a> ]
	    [ <a href="<%=basePath%>prefs.jsp">inställningar</a> ]
	    [ <a href="<%=basePath%>?suspend">pausa</a> ]
   	</div>
<%
    }
    if (parameter(parameters, "reviewMarked") != null) {
	out.println("<div><table><tr><td>Typ</td><td>text</td><td>författare</td><td>ärende</td></tr>");
	Mark[] marks = lyskom.getMarks();
	boolean pyjamas = false;
	for (int i=0; i < marks.length; i++) {
	    pyjamas = !pyjamas;
	    Text t = null;
	    try {
		t = lyskom.getText(marks[i].getText(), false, true);
	    } catch (RpcFailure ex1) {
		if (ex1.getError() == Rpc.E_no_such_text) continue;
		throw ex1;
	    }

	    if (pyjamas) out.print("<tr bgcolor=\"#ccffff\">");
	    else out.print("<tr>");
	    out.println("<td>" + marks[i].getType() + "</td><td>" + textLink(request, lyskom, t.getNo(), false) +
		"</td><td>" + lookupName(lyskom, t.getAuthor(), true) + 
		"</td><td>" + htmlize(t.getSubjectString()) + "</td></tr>");
	    out.flush();
	}
	out.println("</table></p");
	out.flush();
    }
    if (conferenceNumber > 0 && !parameters.containsKey("listSubjects") &&
	!parameters.containsKey("comment") && !parameters.containsKey("text")) {
	int nextUnreadText = 0;
	try {
	    lyskom.changeConference(conferenceNumber);
	    Membership ms = lyskom.queryReadTexts(me, conferenceNumber);

	    UConference uconf = lyskom.getUConfStat(conferenceNumber);
	    int unreads = 0;
	    if (uconf.getHighestLocalNo() > ms.getLastTextRead()) {
	    	unreads = uconf.getHighestLocalNo() - ms.getLastTextRead();
		unreads -= ms.getReadTexts().length;
	    }
%>
	    <div>
	    Läser i <%= lookupName(lyskom, conferenceNumber, true) %> - <%= unreads %>
	    <%= unreads > 1 || unreads == 0 ? "olästa" : "oläst" %>.<br/>
<%
	    int maxTextsToShow = preferences.getInt("show-multiple-texts");
	    nextUnreadText = -1;	    

	    lyskom.changeWhatIAmDoing("Läser");
	    if (Debug.ENABLED) Debug.println("*** review-list: " + reviewList);

	    int textsAdded = 0;
   	    List unreadTexts = lyskom.nextUnreadTexts(conferenceNumber, false, maxTextsToShow);
	    boolean exhausted = false;
	    while (!exhausted && textNumbers.size() < maxTextsToShow &&
		   (!unreadTexts.isEmpty() ||
		    !reviewList.isEmpty())) {
		Integer txtNoObj = null;
		if (!reviewList.isEmpty()) {
		    txtNoObj = (Integer) reviewList.removeFirst();
		} else if (!unreadTexts.isEmpty()) {
		    txtNoObj = (Integer) unreadTexts.remove(0);
		    if (nextUnreadText == -1) nextUnreadText = txtNoObj.intValue();
		} else {
		    exhausted = true;
		    continue;
		}

		TextStat stat = lyskom.getTextStat(txtNoObj.intValue());
		if (!textNumbers.contains(txtNoObj)) textNumbers.add(txtNoObj);
		addComments(lyskom, stat, textNumbers, maxTextsToShow);
	    }

	} catch (RpcFailure ex1) {
	    if (ex1.getError() == Rpc.E_not_member) {
		out.println("<div class=\"statusError\">Fel: du är inte medlem i " +
			lookupName(lyskom, conferenceNumber, true) + "</div>");
	    } else if (ex1.getError() == Rpc.E_undefined_conference) {
		out.println("<div class=\"statusError\">Fel: möte " +
			ex1.getErrorStatus() + " finns inte.</div>");
	    } else {
		throw ex1;
	    }
	}
	if (nextUnreadText > 0) {
	    if (!parameters.containsKey("text")) textNumber = nextUnreadText;
	} else if (nextUnreadText == -1) {
%>
	Det finns inte fler olästa i <%= lookupName(lyskom, conferenceNumber, true) %>.
<%
	}


%>
	</div>
<%
    }

    if (textNumber == 0 && !parameters.containsKey("text") &&
	parameter(parameters, "comment") == null && newTextNo == 0 &&
        textNumbers.size() == 0) {
	listNews = true;
    }
    if (newTextNo > 0) {
	listNews = true;
    }

    if (parameters.containsKey("reviewFaq")) {
	try {
	    Conference conf = lyskom.getConfStat(Integer.parseInt(parameter(parameters, "reviewFaq")));
	    AuxItem[] confAuxs = conf.getAuxItems();
	    boolean foundFaq = false;
	    for (int i=0; i < confAuxs.length; i++) {
		if (confAuxs[i].getTag() == AuxItem.tagFaqText) {
		    textNumbers.add(new Integer(confAuxs[i].getData().intValue()));
		    foundFaq = true;
		}
	    }
	    if (foundFaq) {
		out.println("<div>Återser FAQ för " + lookupName(lyskom, conf.getNo(), true) + ".</div>");
	    } else {
		out.println("<div class=\"statusError\">Fel: mötet " + lookupName(lyskom, conf.getNo(), true) + 
			" har ingen FAQ.</div>");
	    }
	} catch (RpcFailure ex1) {
	    out.println("<div class=\"statusError\">Fel: felkod " + ex1.getError() +
		", status " + ex1.getErrorStatus() + "</div>");
	}
    }

    if (parameters.containsKey("reviewPresentation")) {
	Conference conf = null;
	int pres = 0;
	try {
	    conf = lyskom.getConfStat(Integer.parseInt(parameter(parameters, "reviewPresentation")));
	    pres = conf.getPresentation();

	} catch (NumberFormatException ex1) {
	    ConfInfo[] confs = lyskom.lookupName((String) parameters.get("reviewPresentation"), true, true);
	    if (confs.length == 0) {
		out.println("<div class=\"statusError\">Hittade inget möte eller person som matchade " 
		+ "\"" + htmlize((String) parameters.get("reviewPresentation")) + "\"</div>");
	    } else if (confs.length > 1) {
		out.println(ambiguousNameMsg(lyskom, new AmbiguousNameException(confs)));
	    } else {
		conf = lyskom.getConfStat(confs[0].getNo());
		pres = conf.getPresentation();
	    }
	}
	if (pres > 0) {
	    textNumber = conf.getPresentation();
	    out.println("<div class=\"statusSuccess\">Återser presentation för " +
	                lookupName(lyskom, conf.getNo(), true) + ".</div>");
	} else {
	    out.println("<div class=\"statusError\">" + lookupName(lyskom, conf.getNo(), true) + " har ingen presentation.</div>");
	}
    }
    if (parameter(parameters, "reviewOriginal") != null) {
	int startTextNo = Integer.parseInt(parameter(parameters, "reviewOriginal"));
	Text t = lyskom.getText(startTextNo);
	while (t.getCommented() != null &&
	       t.getCommented().length > 0) {
	    t = lyskom.getText(t.getCommented()[0]);
	}
	textNumber = t.getNo();
	
    }

    if (parameters.containsKey("treeId")) {
	List treeTexts = (List) 
	   session.getAttribute("weblatte.tree." + parameters.get("treeId"));
	Debug.println("treeId list: " + treeTexts);

	textNumbers.addAll(treeTexts);
    }


    List viewedTexts = (List) request.getAttribute("viewedTexts");
    if (viewedTexts == null) {
	viewedTexts = new LinkedList();
	request.setAttribute("viewedTexts", viewedTexts);
    }
    if (textNumber != 0 || parameters.containsKey("text") ||
	textNumbers.size() > 0) {
	// xxx: catch NFE for more graceful error handling
	if (textNumber > 0) textNumbers.add(new Integer(textNumber));
	if (parameters.containsKey("text")) {
	    String[] textNumberParams = request.getParameterValues("text");
	    for (int i=0; i < textNumberParams.length; i++) {
	        textNumbers.add(new Integer(textNumberParams[i].trim()));
	    }
        }
        for (Iterator i = textNumbers.iterator(); i.hasNext();) {
	    textNumber = ((Integer) i.next()).intValue();
	    if (viewedTexts.contains(new Integer(textNumber))) continue;
	    try {
		request.setAttribute("text-numbers", textNumbers);
		request.setAttribute("text", new Integer(textNumber));
		request.setAttribute("conferenceNumber", new Integer(conferenceNumber));
		out.flush();
		RequestDispatcher d = getServletContext().getRequestDispatcher(appPath + "/text.jsp");
		d.include(request, response);
	    } catch (RpcFailure ex1) {
		if (ex1.getError() == Rpc.E_no_such_text) {
		    %><div class="statusError">Fel: text <%= textNumber %> existerar inte.</div><%
		} else {
		    throw ex1;
		}
	    }
	}
    }
    if (viewedTexts != null && viewedTexts.size() > 1) {
	StringBuffer linkText = new StringBuffer();
	StringBuffer queryStr = new StringBuffer();
	linkText.append("Läsmarkera ");
	linkText.append(viewedTexts.size() == 1 ? "text " : "texterna ");
	int listhash = viewedTexts.hashCode();
	if (viewedTexts.size() > 5) {
	    queryStr.append("markAsReadHash=" + Integer.toHexString(listhash));
	    session.setAttribute("mark-as-read-list", viewedTexts);
	} else {
	    for (Iterator i = viewedTexts.iterator(); i.hasNext();) {
		Integer textNo = (Integer) i.next();
		linkText.append(textNo.toString());
		queryStr.append("markAsRead=").append(textNo.toString());
		if (i.hasNext()) {
		    if (viewedTexts.size() == 2)
			linkText.append(" och ");
		    else 
			linkText.append(", ");
		    queryStr.append("&");
		}
	    }
	}
	if (viewedTexts.size() > 4)
	    linkText = new StringBuffer("Läsmarkera alla " + viewedTexts.size() + " texter");

	if (conferenceNumber > 0) {
	    linkText.append(" (och läs nästa)");
	    queryStr.append("&conference=").append(conferenceNumber);
	}
	linkText.append(".");
	out.println("<div><a accesskey=\"N\" href=\"?" + queryStr.toString() + "\">" +
		linkText.toString() + "</a></div>");
    }

    if (parameters.containsKey("lookup")) {
	String str = (String) parameters.get("lookup");
	StringBuffer buf = new StringBuffer();
	for (int i=0; i < str.length(); i++) {
	    char c = str.charAt(i);
	    if (Character.isLetter(c)) {
		buf.append("[").append(Character.toLowerCase(c)).
		    append(Character.toUpperCase(c)).
	   	    append("]");
	    } else if (c == '\\') {
	        buf.append("\\\\");
	    } else {
		buf.append(c);
	    }
	}

	String regex = buf.toString();
	ConfInfo[] confs = lyskom.reLookup(regex, true, true);
	out.println("Följande möten och personer matchar \"" + htmlize(regex) + "\":<br>");
	out.println("<ul>");
	for (int i=0; i < confs.length; i++) {
	    out.print("<li>");
	    out.println(lookupName(lyskom, confs[i].getNo(), true));
	    out.flush();
	}
	out.println("</ul>");
	out.flush();
    }

    if (parameter(parameters, "listSubjects") != null && conferenceNumber > 0) {
	Membership membership = lyskom.queryReadTexts(me, conferenceNumber);
	UConference uconf = lyskom.getUConfStat(conferenceNumber);
	TextMapping mapping = lyskom.localToGlobal(conferenceNumber,
						   membership.getLastTextRead()+1, 255);
	out.println("<div><table><tr><td>Nummer</td><td width=\"200\">författare</td><td width=\"200\">ärende</td><td>tecken</td></tr>");
	boolean pyjamas = true;
	while (mapping.hasMoreElements()) {
	    int textNo = ((Integer) mapping.nextElement()).intValue();
	    Text text = lyskom.getText(textNo, false, true);
	    String charset = text.getCharset();
	    if ("us-ascii".equals(charset)) charset = "iso-8859-1";
	    if (pyjamas) out.print("<tr bgcolor=\"#ccffff\">");
	    else out.print("<tr>");
	    out.print("<td>");
	    out.print(textLink(request, lyskom, textNo, false));
	    out.print("</td><td>");
	    out.print(lookupName(lyskom, text.getAuthor(), true));
	    out.print("</td><td>");
	    out.print(htmlize(text.getSubjectString()));
	    out.print("</td><td>");
	    out.print(""+ text.getStat().getSize());
	    out.println("</td></tr>");
	    out.flush();
	    pyjamas = !pyjamas;
	}
	out.println("</table></div>");
    }
    if (parameter(parameters, "comment") != null && textNumber > 0) {
	lyskom.changeWhatIAmDoing("Skriver en kommentar");
	Text commented = lyskom.getText(textNumber);
	String ccharset = commented.getCharset();
	if (ccharset.equals("us-ascii")) ccharset = "iso-8859-1";
	String subjectString = commented.getSubjectString();
	try {
	    subjectString = new String(commented.getSubject(), ccharset);
	} catch (UnsupportedEncodingException ex1) {
	}
%>
	<form class="boxed" method="post" action="<%=myURI(request)%><%=conferenceNumber>0?"?conference="+conferenceNumber:""%>">
	<input type="hidden" name="inCommentTo" value="<%=textNumber%>">
	Skriver en kommentar till text <%= textNumber %> av <%= lookupName(lyskom, lyskom.getTextStat(textNumber).getAuthor(), true) %><br/>
	<input size="50" type="text" name="subject" value="<%= htmlize(subjectString, false) %>"><br/>
	<textarea name="body" cols="71" rows="10"></textarea><br/>
	<input type="submit" value="skicka!" name="createText">
	<input type="submit" name="dispatchToComposer" value="avancerat läge">
	</form>
<%
    }

	    listNews = (listNews || (parameter(parameters, "listnews") != null ||
	        (justLoggedIn && preferences.getBoolean("list-news-on-login")))) &&
	        viewedTexts.size() == 0;

	    if (listNews) {
		if (!minimalistic && preferences.getBoolean("auto-refresh-news") &&
		    (request.getHeader("User-Agent").indexOf("MSIE") >= 0 ||
	    	     request.getHeader("User-Agent").indexOf("Gecko") >= 0)) {
%>
	    <script language="JavaScript1.2">
		var interval = <%= interval %>*1000;
		var timeLeft = interval;
	        var refreshInProgress = false;
	        var countdownAborted = false;
		var ivref;
		function countdown() {
		    var div = document.getElementById("countdown");
	            if (countdownAborted) {
	                if (div != null) {
			    div.innerHTML = "(avbruten)";
			}
			return;
	            }
		    timeLeft -= 1000;
		    var s = timeLeft / 1000;
		    if (div != null && timeLeft > 0) {
			div.innerHTML = "(uppdaterar om " + s + 
			    (s > 1 ? " sekunder" : " sekund") + ")";
	                
		    } else if (div != null && !countdownAborted) {
			if (!refreshInProgress) {
	                    div.innerHTML = "(uppdaterar...)";
	                    refresh();
	                }
		    }
		}
		function refresh() {
	            refreshInProgress = true;
		    document.location.href = "<%=basePath%>?listnews&saveMessages&autoRefresh";
		}
	        function abortCountdown() {
		    countdownAborted = true;
		    window.clearInterval(ivref);
		    countdown();
		}
		function startCountdown() {
		    ivref = window.setInterval(countdown, 1000);
		}
		startCountdown();
	    </script>

<%		} 
%>
        <div class="news">
	<ul class="news">
<%	
		List unreadConfsList = lyskom.getMyUnreadConfsList();
		Iterator confIter = unreadConfsList.iterator();
		int sum = 0, confsum = 0;
		int lastconf = 0;
		int skipTo = 0;
		int skipped = 0;
		if (parameters.containsKey("skipTo")) {
		    skipTo = Integer.parseInt(parameter(parameters, "skipTo"));
		}
		Debug.println("list-news: unreadConfsList: " + unreadConfsList);
		synchronized (unreadConfsList) {
		    boolean abort = false;
		    while (confIter.hasNext() && !abort) {
			int conf = ((Integer) confIter.next()).intValue();
			if (skipTo > 0 && skipTo != conf) {
			    skipped++;
			    continue;
			} else if (skipTo == conf) {
			    skipped++;
			    skipTo = 0;
			    continue;
			}

			lastconf = conf;


			Membership membership;
			try {
		  	    membership = lyskom.queryReadTexts(me, conf);
			    Debug.println("membership: " + membership);
			} catch (RpcFailure ex1) {
			    if (ex1.getError() == Rpc.E_not_member) {
				Debug.println("warning: index.jsp: listnews: removing non-member conf " + conf);
				confIter.remove();
				continue;
			    } else {
				throw ex1;
			    }
			}

			if (preferences.getInt("min-conference-priority") > membership.getPriority()) {
			    confIter.remove();
			    continue;
			}

			UConference uconf = lyskom.getUConfStat(conf);
			int unreads = 0, highestLocalNo = uconf.getHighestLocalNo();
			List ranges = membership.getReadRanges();
			Debug.println("read-ranges: " + ranges);
			Membership.Range lastRange = null;
			boolean fastUnreads = preferences.getBoolean("fast-unreads");
			for (Object _range : ranges) {
			    Membership.Range range = (Membership.Range) _range;
			    if (lastRange != null) {
			    	if (fastUnreads) {
			    	    unreads += range.first - lastRange.last;
			    	} else {
			    	    int diff = range.first - lastRange.last;
			    	    TextMapping tm = lyskom.localToGlobal(conf, lastRange.last+1, diff);
			    	    for (int localno = lastRange.last+1; localno < range.first; localno++) {
				    	if (tm.search(localno)) unreads++;
				    }
				}
			    }
			    lastRange = range;
			}
			if (lastRange != null) {
			   if (!fastUnreads && highestLocalNo > lastRange.last) {
				TextMapping tm = lyskom.localToGlobal(conf, lastRange.last+1, highestLocalNo-lastRange.last);
				for (int localno = lastRange.last+1; localno <= highestLocalNo; localno++) {
				    if (tm.search(localno)) unreads++;
				}
			    } else {
			    	unreads += highestLocalNo-lastRange.last;
			    }
						   
			}
			sum += unreads;
			
			confsum++;
			out.print("<li> <a href=\"" + myURI(request) + "?conference=" +
				  conf + "\">" + 
				  lookupName(lyskom, conf, true, true) + "</a>: " +
				  unreads + " " + (unreads > 1 ? "olästa" : "oläst"));
			out.println(" [ <a href=\"" + myURI(request) + "?conference=" +
				    conf + "&listSubjects\">lista ärenden</a> ]");
			out.flush();
			if (manyMemberships && confsum >= 5) abort = true;
		    }
		}
		lyskom.changeWhatIAmDoing("Väntar");
%>
	</ul>
<%
		    if (skipped > 0) {
		        out.println("<div>Hoppade över " + skipped + " möten.</div>");
		    }
		if (manyMemberships && confIter.hasNext()) {
%>
		<div>Många möten: det finns troligen fler olästa i de
	           <%= unreadConfsList.size()-confsum %> möten
		   som inte visas i denna lista (<a href="<%= basePath %>?listnews&skipTo=<%= lastconf %>">lista nästa 5</a>).</div>
<%
		}
%>
		<div class="unread-summary"><%= confsum == 0 ? "<b>inga olästa i något möte</b>" : sum + " oläst(a) i " + confsum + " möte(n)" %><br>
<%
	        if (confsum == 0) {
%>
	        Tips: om du är osäker på vilka möten du ska gå med i, <a href="active.jsp">lista mötesaktivitet</a>.
<%
	        }
%>
		<div class="countdown" id="countdown"></div>
	        </div>
<%		if (sum > 0) {
		    out.println(jsTitle(serverShort(lyskom) + ": " + 
			(sum == 1 ? "ett oläst" : sum + " olästa")));
		}
%>
	</div>
<%
	    }

	    MultipartParser multip = null;
	    if (parameter(parameters, "upload") != null) {
		multip = new MultipartParser(request, 1024*1024);
		com.oreilly.servlet.multipart.Part nextPart = null;
		boolean imageOK = false;
		String imageFileName = null;
		File target = null;
	    	while (!imageOK && null != (nextPart = multip.readNextPart())) {
		    if (nextPart.isFile()) {
			FilePart fpart = (FilePart) nextPart;
			if (fpart == null || fpart.getFileName() == null) continue;
			String fileExt = fpart.getFileName().substring(fpart.getFileName().
								lastIndexOf(".")).toLowerCase();
			File imagef = new File(dir, lyskom.getMyPerson().getNo() + fileExt);
			fpart.writeTo(imagef);
			
			imageFileName = imagef.getName();
			imageOK = true;
			target = imagef;
		    } else {
			ParamPart ppart = (ParamPart) nextPart;
			String url = ppart.getStringValue();
			if (url == null || url.trim().equals("")) continue;

			URLConnection con = new URL(url).openConnection();
			String fileExt = url.substring(url.lastIndexOf(".")).toLowerCase();
			imageFileName = lyskom.getMyPerson().getNo() + fileExt;
			target = new File(dir, imageFileName);
			FileOutputStream targetStream = new FileOutputStream(target);
			InputStream input = (InputStream) con.getInputStream();
			byte[] loadBuf = new byte[2048];
			int read = 0;
			while ((read = input.read(loadBuf)) > 0) {
			    targetStream.write(loadBuf, 0, read);
			}
			imageOK = true;
		    }

		    if (imageOK) {
			ImageInfo ii = new ImageInfo();
			FileInputStream is = new FileInputStream(target);
			ii.setInput(is);
			if (!ii.check()) {
			    out.println("<p class=\"statusError\">Felaktigt filformat!</p>");
			    imageOK = false;
			}
			is.close();
			if (!imageOK) target.delete();
		    }

		    if (imageOK) {
			File f = new File(dir, lyskom.getMyPerson().getNo() + ".txt");
			
			FileOutputStream descf = new FileOutputStream(f);
			descf.write(lyskom.getConfName(lyskom.getMyPerson().getNo()));
			descf.write('\n');
			descf.write(imageFileName.getBytes());
			descf.close();

			out.println("<h3>OK! Din bild har blivit lagrad.</h3>");
		    }
	    	}
	    }
	    if (parameter(parameters, "uploadForm") != null) {
%>
	<form enctype="multipart/form-data" method="post" action="<%=myURI(request)%>?upload" class="boxed">
	    skriv en bild-URL här: <input type="text" size="50" name="urlsubmitter"> <br/>
	    eller ladda upp en bild: <input type="file" name="uploader"> <br/>
	    <input type="submit" value="skicka"><br/>
	</form>
<%
	    }
	    if (authenticated.booleanValue() && parameter(parameters, "setPasswordForm") != null) {
%>
	<form method="post" action="<%=basePath%>?setPassword" class="boxed">
	    <table>
	    <tr><td>Person att byta lösenord för:</td><td>
		<input type="text" size="40" name="setPasswordPerson" value="<%= lookupName(lyskom, person.getNo()) %>" /></td></tr>
	    <tr><td>Ditt (nuvarande) lösenord:</td><td><input type="password" name="setPasswordUserPassword" size="8" /></td></tr>
	    <tr><td>Nytt lösenord:</td><td><input type="password" name="setPasswordNewPassword" size="8" /></td></tr>
	    <tr><td>Upprepa det nya lösenordet:</td><td> <input type="password" name="setPasswordNewPasswordVerify" size="8" /></td></tr>
	    <tr><td>&nbsp;</td><td><input type="submit" value="ok!" /></td></tr>
	    </table>
	</form>
<%
	}

	if (showStandardBoxes) {
%>
    <div class="standard-boxes">
    <div id="read-text-box">
    <form method="get" action="<%=myURI(request)%>" class="boxed">
    Läs ett inlägg: <input type="text" size="10" name="text">
    <input type="submit" value="ok!">
    </form>
    </div>

    <div id="only-box">
    <form action="<%=myURI(request)%>" class="boxed" method="post">
    Endast: <input type="text" size="3" name="endast"> inlägg i möte
    <input type="text" size="40" name="endastConferenceName" value="<%= conferenceNumber > 0 ? entitize(lookupNameComplete(lyskom, conferenceNumber)) : "" %>">
    <input type="submit" value="ok!">
<%  if (listNews) { %>
    <input type="hidden" name="listnews" value="<%=parameter(parameters, "listnews")%>">
<%  } %>
    </form>
    </div>

    <div id="send-message-box">
    <form action="<%=myURI(request)%>" class="boxed" method="post">
    <a name="sendMessage"></a>
    Skicka ett meddelande till:<br/>
<%  if (listNews) { %>
    <input type="hidden" name="listnews" value="<%=parameter(parameters, "listnews")%>">
<%  } %>
    <input type="text" size="40" name="sendToName" value="<%=lastReceivedOrSent!=null?lastReceivedOrSent:""%>">
<br/>
    Text:<br/>
    <input onFocus="abortCountdown();" onChange="abortCountdown();" type="text" name="sendText" size="60"><input type="submit" value="ok">
    </form>
    </div>
    </div>
<%
	    }
	}
    }
%>
<%
    if (!authenticated.booleanValue()) {
	try {
	    session.setAttribute("goto", myURI(request) + (request.getQueryString() != null ? "?"+request.getQueryString() : ""));
	} catch (IllegalStateException ex1) {}
%>
<div class="intro">
Du är inte inloggad.
</div>
<div class="news">
</div>
<form name="lyskomlogin" method="post" action="<%=myURI(request)%>">
<%
    String lyskomNamn = "";
    if (parameter(parameters, "lyskomNamn") != null) lyskomNamn = parameter(parameters, "lyskomNamn");
%>
<table class="boxed">
<%
    ConfInfo[] names = (ConfInfo[]) request.getAttribute("ambiguous-name");
    if (names == null) {
%>
<tr><td>namn eller <span title="Du kan ange t.ex. &quot;#4711&quot; för att logga in som person nummer 4711">#nummer</span>:</td><td><input type="text" name="lyskomNamn" value="<%= lyskomNamn %>" size="30"></td></tr>
<%
    } else {
%>
<tr><td colspan="2" class="statusError">Flertydigt namn, välj ett i listan</td></tr>
<tr><td>välj person:</td><td>
<select name="lyskomNamn">
<%
        Arrays.sort(names, new Comparator() {
		        public int compare(Object o1, Object o2) {
		            return ((ConfInfo) o1).getNameString().
		                   compareTo(((ConfInfo) o2).getNameString());
		        }
	});
        for (int i=0; i < names.length; i++) {
            ConfInfo conf = names[i];
            out.println("<option value=\"#" + conf.getNo() + "\">" + htmlize(conf.getNameString(), false) + " (person " +
		    conf.getNo() + ")");
        }
%>
</select>
</td></tr>
<%
    }
%>
<tr><td>lösenord:</td><td><input type="password" name="lyskomLosen" size="8" value="<%= request.getParameter("lyskomLosen") != null ? dqescHtml(request.getParameter("lyskomLosen")) : "" %>"></td></tr>
<tr><td>dold session:</td><td><input type="checkbox" name="lyskomDold"></td></tr>
<tr><td>sparsamt gränssnitt:</td><td><input type="checkbox" name="mini"></td></tr>
<tr><td>registrera ny användare:</td><td><input type="checkbox" name="createPerson"></td></tr>
<tr><td>server:</td><td>
<select name="server">
<%
    String selectedServer = parameter(parameters, "server");
    if (selectedServer == null) {
	Cookie[] cookies = request.getCookies();
	for (int i=0; cookies != null && i < cookies.length; i++) {
	    if (cookies[i].getName().equals("kom-server")) {
		selectedServer = cookies[i].getValue();
	    }
	}
    }
    if (selectedServer == null) {
	selectedServer = Servers.defaultServer.hostname;
    }
    for (Iterator i = Servers.list.iterator(); i.hasNext();) {
	KomServer ks = (KomServer) i.next();
	out.println("<option ");
	if (selectedServer.equals(ks.hostname)) {
	    out.println("selected ");
	}
	out.println("value=\"" + ks.hostname + "\">" + ks.name + "\n");
    }
%>
</select>
</td></tr>
<tr><td>&nbsp;</td><td><input type="submit" value="logga in"></td></tr>
</table>
</form>
<p class="banner">
Välkommen till Weblatte!
</p>
<%
    } else {
	try {
	    if (!authenticated.booleanValue() && lyskom != null) lyskom.shutdown();
	} catch (Throwable t1) {
	    System.err.println("oops: " + t1.getClass().getName() + ": " +
				t1.getMessage());
	    t1.printStackTrace(System.err);
	}
    }
%>
<div class="nav">
<% if (showPOM) { %>
[ 
<%
        if (vemArVem) {
%>
<a href="bilder/">visa bilder</a> |
<%
	}
%>
<a href="vilka/">vilka är inloggade?</a> |
<a href="?pom=false">dölj menyer</a> ]
<%  } else { %>
[ <a href="?pom=true">visa menyer</a> ]
<%  }
    if (debug) { %>
	<div class="debug"><a href="<%= basePath%>?debug">debugdata</a>
	   <a href="prefs.jsp">inställningar</a></div>
<%  } %>
</div>
<div class="footer">
<%
    List suspendedSessions = null;
    try {
	suspendedSessions = (List) session.getAttribute("lyskom.suspended");
	List actSessions = (List) session.getAttribute("lyskom.active");
	if (actSessions != null) suspendedSessions.addAll(actSessions);
    } catch (IllegalStateException ex1) {}
    int suspSessCount = 0;
    if (suspendedSessions != null) {
	synchronized (suspendedSessions) {
	    suspSessCount = suspendedSessions.size();
	}
    }
    if (suspSessCount > 0) {
	if (suspSessCount > 1) {
	    out.print("<a title=\"föregående session\" href=\"sessions.jsp?previous\">&lt;&lt;</a> ");
	}
	out.print("<a href=\"sessions.jsp\"><b>OBS! Du har " +
		suspSessCount + " " + 
		(suspSessCount > 1 ? "andra LysKOM-sessioner" :
		 "till LysKOM-session") + "</b></a>");
	boolean unreads = false, unreadLetters = false;
	synchronized (suspendedSessions) {
	    for (Iterator i=suspendedSessions.iterator();!unreads && i.hasNext();) {
		Session _session = ((SessionWrapper) i.next()).getSession();
		List unreadConfs = _session.getMyUnreadConfsList();
		if (unreadConfs.size() > 0) unreads = true;
		if (unreadConfs.contains(new Integer(_session.getMyPerson().getNo()))) {
		    unreadLetters = true;
		}
	    }
	}
	if (unreads) out.print(" (olästa" + (unreadLetters ? " brev" : "") + ")");
	out.print(" <a title=\"nästa session\" href=\"sessions.jsp?next\">>></a>");
	out.println("<br/>");
    }
%>
<a href="about.jsp">Hjälp och information om Weblatte</a><br/>
$Revision: 1.103 $
</div>
</body>
</html>

