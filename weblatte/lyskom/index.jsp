<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*, nu.dll.app.weblatte.*, java.net.*, java.io.*, java.text.*,java.util.regex.*, java.nio.*, java.nio.charset.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ page errorPage='fubar.jsp' %>
<%@ include file='kom.jsp' %>
<%@ page import='javax.mail.BodyPart, javax.mail.internet.*' %>
<%
    Map parameters = parseQueryString(request.getQueryString(), "iso-8859-1");
    Enumeration penum = request.getParameterNames();
    while (penum.hasMoreElements()) {
	String name = (String) penum.nextElement();
	String[] values = request.getParameterValues(name);
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
	    lyskom.doChangeWhatIAmDoing("Pausar Weblatte");
	    lyskomWrapper.setSuspended(true);
	    suspendedSessions.add(lyskomWrapper);
	}
	session.removeAttribute("lyskom");
	session.removeAttribute("LysKOMauthenticated");

	response.sendRedirect(basePath + "?r=" + Integer.toHexString(rnd.nextInt()));
	return;
    }

    try {
	if (parameter(parameters, "lyskomNamn") != null ||
	    (lyskom == null && session.getAttribute("lyskomPersonNo") != null)) {

	    if (lyskom == null) {
		lyskom = new Session();
	    }
	    if (!lyskom.getConnected()) {
		lyskom.setClientHost(request.getRemoteAddr());
		lyskom.setClientUser("www");
	    
		lyskom.connect(server, 4894);
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
		    session.setAttribute("lyskom", new SessionWrapper(lyskom));
		    Cookie serverCookie = new Cookie("kom-server", server);
		    serverCookie.setMaxAge(31536000);
		    response.addCookie(serverCookie);
		    authenticated = Boolean.TRUE;
                    justLoggedIn = true;
		    if (parameters.containsKey("mini"))
			lyskom.setAttribute("weblatte.minimalistic", Boolean.TRUE);
		    lyskom.setLatteName("Weblatte");
		    lyskom.setClientVersion("dll.nu/lyskom", "$Revision: 1.59 $" + 
					    (debug ? " (devel)" : ""));
		    lyskom.doChangeWhatIAmDoing("kör web-latte");
		}
	    } else if (names != null && names.length == 0) {
		error = "Namnet du angav (\"" + htmlize(parameter(parameters, "lyskomNamn")) + "\") " +
		    "finns inte. Välj \"Registrera ny användare\" för att skapa en ny KOM-person.";
	    } else if (names != null && names.length > 1) {
		StringBuffer buf = new StringBuffer("Flertydigt namn, följande matchar:<br/>\n<ul>");
		for (int i=0; i < names.length; i++) 
		    buf.append("<li>").append(lookupName(lyskom, names[i].getNo(), true)).append("\n");
		error = buf.append("</ul>\n").toString();
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
<html><head>
<% if (authenticated.booleanValue()) { %>
<title>Weblatte: <%= serverShort(lyskom) %></title>
<% } else { %>
<title>Weblatte LysKOM-klient</title>
<% } %>
</head>
<link rel="stylesheet" href="lattekom.css" />
<body>
<%
    if (error != null) {
%>
<p class="statusError"><%= error %></p>
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
	<p>
	[ <a href="<%= basePath %>">logga in</a> ]
	</p>
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
	<p>
	[ <a href="<%= basePath %>">logga in</a> ]
	</p>
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
    	<h2>välkommen till LysKOM, <%= lookupName(lyskom, lyskom.getMyPerson().getNo(), true) %>!</h2>
<%
		if (!minimalistic) {
%>
		<p class="intro">Högerklicka på en tom yta för att visa menyn. Du kan även högerklicka på personnamn,
		mötesnamn och textnummer för att få fram menyer specifika för objektet i fråga.</p>
<%
	    	}
	    }
    if (Debug.ENABLED) Debug.println("unreadconfslist: " + lyskom.getUnreadConfsListCached());
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
	    	out.println("<p class=\"statusSuccess\">OK: \"" +
		    htmlize(ci.getNameString()) + "\" har bytt namn till " +
		    lookupName(lyskom, confNo, true) + "</p>");
	    } else {
		out.println("<p class=\"statusError\">Fel: namnet \"" + 
			htmlize(oldName) + "\" finns inte.</p>");
	    }
	} catch (RpcFailure ex1) {
	    switch (ex1.getError()) {
		case Rpc.E_permission_denied:
		out.println("<p class=\"statusError\">Fel: du har inte rättighet att " +
			"ändra namn på \"" + lookupName(lyskom, confNo, true) + "\"</p>");
		break;
		case Rpc.E_conference_exists:
		out.println("<p class=\"statusError\">Fel: det angivna namnet är upptaget.</p>");
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
	    out.println("<p class=\"statusSuccess\">OK: text " + 
			textLink(request, lyskom, textNo) + " är borttagen.</p>");
	} catch (RpcFailure ex1) {
	    switch (ex1.getError()) {
	    case Rpc.E_not_author:
		out.println("<p class=\"statusError\">Fel: du är inte " +
			    "textens skapare.</p>");
		break;
	    case Rpc.E_no_such_text:
		out.println("<p class=\"statusError\">Fel: det finns ingen " +
			    "sådan text (" + ex1.getErrorStatus() + ").</p>");
		break;
	    default:
		throw ex1;

	    }
	}
    }
    if (parameter(parameters, "mark") != null) {
	lyskom.markText(Integer.parseInt(parameter(parameters, "mark")), commonPreferences.getInt("default-mark"));
	out.println("<p class=\"statusSuccess\">Text " +
		textLink(request, lyskom,
		Integer.parseInt(parameter(parameters, "mark")))
		+ " har markerats.</p>");
    }
    if (parameter(parameters, "unmark") != null) {
	lyskom.unmarkText(Integer.parseInt(parameter(parameters, "unmark")));
	out.println("<p class=\"statusSuccess\">Text " +
		parameter(parameters, "unmark") + " har avmarkerats.</p>");
    }

    if (parameter(parameters, "endast") != null) {
	int textcount = Integer.parseInt(parameter(parameters, "endast"));
	ConfInfo conf = null;
	try {
	    conf = lookupName(lyskom, parameter(parameters, "endastConferenceName"), true, true);
	    if (conf != null) {
	    	out.print("<p>Endast " + textcount + " inlägg i möte " +
			lookupName(lyskom, conf.getNo(), true) + "...");
	    	out.flush();
	    	lyskom.endast(conf.getNo(), textcount);
	    	out.println(" ok.</p>");
		//lyskom.removeAttribute("mbInited");
	    } else {
	    	%><p class="statusError">Fel: mötet finns inte.</p><%
	    }
            

	} catch (AmbiguousNameException ex1) {
	    %><p class="statusError">Fel: mötesnamnet är flertydigt. Följande mötesnamn matchar:<%
	    out.println("<ul>");
	    ConfInfo[] names = ex1.getPossibleNames();
	    for (int i=0; i < names.length; i++) 
		out.println("<li>" + lookupName(lyskom, names[i].getNo(), true));
	    out.println("</ul>");
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
		out.print("<p>Bli medlem i " + lookupName(lyskom, confNo, true) + "...");
		out.flush();
		lyskom.joinConference(confNo);
		out.print("OK!</p>");
		out.flush();
		lyskom.setAttribute("mbInited", Boolean.FALSE);
	    } else {
		out.println("<p class=\"statusError\">Fel: hittar inget sådant möte</p>");
	    }
	} catch (AmbiguousNameException ex1) {
	    out.println(ambiguousNameMsg(lyskom, ex1));
	} catch (RpcFailure ex2) {
	  if (ex2.getError() == Rpc.E_access_denied) {
	      out.println("misslyckades.</p><p class=\"statusError\">Fel: du får inte gå med i mötet.");
	      Conference conf = lyskom.getConfStat(ex2.getErrorStatus());
	      out.println("Administratör för mötet är " +
	          lookupName(lyskom, conf.getSuperConf(), true) + " - vänd dig dit för mer information.</p>");
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
		out.print("<p>Utträda ur möte " + lookupName(lyskom, confNo, true) + "...");
		out.flush();
		lyskom.subMember(confNo, lyskom.getMyPerson().getNo());
		out.println("OK!</p>");
		out.flush();
		lyskom.setAttribute("mbInited", Boolean.FALSE);
	    } else {
		out.println("<p class=\"statusError\">Fel: hittar inget sådant möte</p>");
	    }
	} catch (AmbiguousNameException ex1) {
	    out.println(ambiguousNameMsg(lyskom, ex1));
 	} catch (RpcFailure ex2) {
	    out.println("misslyckades.</p><p class=\"statusError\">Fel: du är inte medlem i mötet.</p>");
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
	out.print("<p>Läser in medlemskapsinformation...");
	out.flush();
	List unreadConferencesList = lyskom.getUnreadConfsList(me);
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
		"<a href=\"prefs.jsp\">inställningar</a>.</p>");
	    out.flush();
	    manyMemberships = true;
	} else {
	    if (!manyMemberships) {
		lyskom.updateUnreads(unreadConferencesList);
	    } else {
		out.println("(\"många möten\" aktiverad)...");
	    }
	    out.println("klart.</p>");
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
		%><p class="statusSuccess">Alarmmeddelande skickat.</p><%
	    } else {
	    	ConfInfo recipient = lookupName(lyskom, stn, true, true);
	    	if (recipient != null) {
		    lyskom.sendMessage(recipient.getNo(), _text);
		    lastReceivedOrSent = lookupName(lyskom, recipient.getNo());
		    %><p class="statusSuccess">Meddelande skickat till <%=lookupName(lyskom, recipient.getNo(), true)%>.</p><%
	    	} else {
		    %><p class="statusError">Hittade ingen mottagare som matchade "<%=htmlize(stn)%>".</p><%
	    	}
	    }
	} catch (RpcFailure ex2) {
	    if (ex2.getError() == Rpc.E_message_not_sent) {
		%><p class="statusError">Meddelandet gick inte att skicka.</p><%
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
		<p class="asynchMessage">
		<i>Meddelande från <%=lookupName(lyskom, sender, true)%> till
                      <%= recipient != 0 ? lookupName(lyskom, recipient, true) : "alla"%>:</i><br>
		<tt><%=htmlize(text).replaceAll("\n", "<br/>")%></tt><br/>
		<small>(mottaget <%= df.format(m.getArrivalTime()) %>)</small>
		</p>
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
	out.println("<p><pre>Listar sessioner...");
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
	out.println("</pre></p>");
	out.flush();
    }
    if (parameter(parameters, "conference") != null) {
	conferenceNumber = Integer.parseInt(parameter(parameters, "conference"));
    }
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
    if (parameter(parameters, "createText") != null) {

	boolean isMultipart = request.getParameter("multipart") != null ||
		request.getAttribute("multipart") != null;
	List parts = (List) request.getAttribute("multipart");
	List recipients = new LinkedList();
    	List ccRecipients = new LinkedList();

    	StringBuffer errors = new StringBuffer();
        boolean explicitRecipients = false;

   	for (int rcptType = 1; rcptType <= 2; rcptType++) {
            String[] recptFields;
	    Object _fields = parameters.get(rcptType == 1 ? "recipient" : "ccRecipient");
	    if (_fields == null) {
		recptFields = new String[0];
	    } else if (_fields instanceof String[]) {
		recptFields = (String[]) _fields;
	    } else {
		recptFields = new String[] { (String) _fields };
	    }
	    List list = rcptType == 1 ? recipients : ccRecipients;
	    if (recptFields == null) continue;
            for (int i=0; i < recptFields.length; i++) {
   	    	recptFields[i] = recptFields[i].trim();
	    	if ("".equals(recptFields[i])) continue;
	    	try {
            	    ConfInfo conf = lookupName(lyskom, recptFields[i], true, true);
		    if (conf == null) {
		    	errors.append("Namnet \"" + htmlize(recptFields[i]) + "\" hittas inte.<br>");
		    	continue;
		    }
		    if (list.contains(conf.getNameString())) continue;
	    	    list.add(conf);
	    	    explicitRecipients = true;
	    	} catch (AmbiguousNameException ex1) {
		    errors.append("<p class=\"statusError\">Fel: namnet är flertydigt. Följande namn matchar:");
	            errors.append("<ul>");
	            ConfInfo[] names = ex1.getPossibleNames();
	            for (int j=0; j < names.length; j++) 
		        errors.append("<li>" + lookupName(lyskom, names[j].getNo(), true));
	    	    errors.append("</ul>");
	    	}
	    }
    	}
	Text newText = null;
	if (errors.length() == 0) {
	    if (!isMultipart) {
		String charsetName = preferences.getString("create-text-charset");
		if (parameters.containsKey("charset"))
		    charsetName = parameter(parameters, "charset");
		String subject = parameter(parameters, "subject");
		String body = parameter(parameters, "body").replaceAll("\r", "");
		String textContents = subject + "\n" + body;   
		Charset charset = Charset.forName(charsetName);
		Debug.println("using default charset " + charset.displayName());
		CharsetEncoder encoder = charset.newEncoder();

		Charset utf8Charset = Charset.forName("utf-8");
		encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		if (!encoder.canEncode(textContents) && !charset.equals(utf8Charset)) {
		    Debug.println("default encoding failed, attempting utf-8 fallback");
		    CharsetEncoder utf8encoder = utf8Charset.newEncoder();
		    if (!utf8encoder.canEncode(textContents)) {
			Debug.println("utf-8 encoding failed also");
			errors.append("<p class=\"statusError\">Varning: texten " +
				      " kan inte kodas i vare sig utf-8 eller " + 
				      charset.displayName() + " (använder " + 
				      charset.displayName() + ").</p>");
		    } else {
			errors.append("<p class=\"statusError\">OBS: " +
				      "textens innehåll kunde inte kodas i vald " +
				      "teckenkodning (\"" + charset.displayName() +
				      "\"). " + 
				      "Texten har istället kodats till \"" + 
				      utf8Charset.displayName() + "\" för att " +
				      "hela innehållet skall representeras korrekt.</p>");

			encoder = utf8encoder;
			charsetName = "utf-8";
		    }
		}
		encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		encoder.reset();
		ByteBuffer textBuffer = encoder.encode(CharBuffer.wrap(textContents));
		if (!textBuffer.hasArray()) {
		    throw new IOException("returned ByteBuffer is not backed " +
					  "by an array");
		}
		byte[] _tmp = textBuffer.array();
		byte[] textContentBytes = new byte[textBuffer.limit()];
		System.arraycopy(_tmp, 0, textContentBytes, 0, textBuffer.limit());
		newText = new Text(textContentBytes, charsetName);
		wrapText(newText);

		if (parameters.containsKey("content-type") && !"".equals(parameters.get("content-type"))) {
		    newText.getStat().setAuxItem(new AuxItem(AuxItem.tagContentType, parameter(parameters, "content-type")));
		}

	    } else {
		ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
		ContentType ct;
		byte[] subject = lyskom.toByteArray(parameter(parameters, "subject"));
	  	boolean wrapInRfc822 = preferences.getBoolean("post-rich-texts-as-rfc822");

		if (parts.size() == 1) {
		    Map partMap = (Map) parts.get(0);
		    ContentType partContentType = new ContentType((String) partMap.get("content-type"));
		    String _uploaded = (String) partMap.get("uploaded");
		    String _contents = (String) partMap.get("contents");
		    if (_uploaded != null && !_uploaded.equals("")) {
			File file = new File(tempDir, (String) partMap.get("uploaded"));
	  		if (!file.getParentFile().equals(tempDir)) {
			    throw new IOException("uploaded file is not in temp dir!");
			}
		    	partContentType.getParameterList().set("name", (String) partMap.get("filename"));
			InputStream is = new FileInputStream(file);
			int read;
			byte[] buf = new byte[2048];
			while ((read = is.read(buf, 0, buf.length)) != -1) {
			    bodyStream.write(buf, 0, read);
			}
			is.close();
		    } else if (_contents != null && !_contents.equals("")) {
			String charset = partContentType.getParameterList().get("charset");
			if (charset == null) charset = preferences.getString("create-text-charset");

			if (partMap.containsKey("alternative-contents")) {
			    InternetHeaders headers = new InternetHeaders();
			    headers.addHeader("Content-Transfer-Encoding", "binary");
			    headers.addHeader("Content-Type", partContentType.toString());
			    byte[] contents = _contents.getBytes(charset);
			    MimeMultipart alternative = new MimeMultipart("alternative");
			    ContentType altContentType = new ContentType((String) partMap.get("alternative-content-type"));
			    altContentType.getParameterList().set("charset", charset);
			    byte[] altContents = ((String) partMap.get("alternative-contents")).getBytes(charset);
			    InternetHeaders altHeaders = new InternetHeaders();
			    altHeaders.addHeader("Content-Type", altContentType.toString());
			    altHeaders.addHeader("Content-Transfer-Encoding", "binary");

			    alternative.addBodyPart(new MimeBodyPart(altHeaders, altContents));
			    alternative.addBodyPart(new MimeBodyPart(headers, contents));
				
			    ByteArrayOutputStream os = new ByteArrayOutputStream();
			    alternative.writeTo(os);
			    byte[] _ac = os.toByteArray();
			    bodyStream.write(_ac);
			    partContentType = new ContentType(alternative.getContentType());
			} else {
			    bodyStream.write(_contents.getBytes(charset));
			}
		    }
		    ct = partContentType;
		} else {
		    MimeMultipart multipart = new MimeMultipart(wrapInRfc822 ? "related" : "mixed");
		    for (Iterator i = parts.iterator(); i.hasNext();) {
			Map partMap = (Map) i.next();
			ContentType partContentType = new ContentType((String) partMap.get("content-type"));
			byte[] contents = null;
			InternetHeaders headers = new InternetHeaders();

			String _contents = (String) partMap.get("contents");
			String _uploaded = (String) partMap.get("uploaded");
			if (_uploaded != null && !_uploaded.equals("")) {
	  		    headers.setHeader("Content-Transfer-Encoding", "base64");
			    File file = new File(tempDir, (String) partMap.get("uploaded"));
	  		    if (!file.getParentFile().equals(tempDir)) {
			    	throw new IOException("uploaded file is not in temp dir!");
			    }
			    partContentType.getParameterList().set("name", (String) partMap.get("filename"));
			    headers.setHeader("Content-Location", (String) partMap.get("filename"));
			    ByteArrayOutputStream bos = new ByteArrayOutputStream();
			    OutputStream os = new Base64.OutputStream(bos);
			    InputStream is = new FileInputStream(file);
			    int read;
			    byte[] buf = new byte[2048];
			    while ((read = is.read(buf, 0, buf.length)) != -1) {
				os.write(buf, 0, read);
			    }
			    os.close();
			    is.close();
			    contents = bos.toByteArray();
			} else if (_contents != null && !_contents.equals("")) {
			    String charset = partContentType.getParameterList().get("charset");
	  		    headers.setHeader("Content-Transfer-Encoding", "binary");
			    if (charset == null) charset = "iso-8859-1";

			    contents = _contents.getBytes(charset);
			}
			headers.addHeader("Content-Type", partContentType.toString());

			if (contents != null) {
			    if (partMap.containsKey("alternative-contents")) {
				MimeMultipart alternative = new MimeMultipart("alternative");
				ContentType altContentType = new ContentType((String) partMap.get("alternative-content-type"));
				String charset = altContentType.getParameterList().get("charset");
				if (charset == null) charset = preferences.getString("create-text-charset");
				altContentType.getParameterList().set("charset", charset);
				byte[] altContents = ((String) partMap.get("alternative-contents")).getBytes(charset);
				InternetHeaders altHeaders = new InternetHeaders();
				altHeaders.addHeader("Content-Type", altContentType.toString());
				altHeaders.addHeader("Content-Transfer-Encoding", "binary");

				alternative.addBodyPart(new MimeBodyPart(altHeaders, altContents));
				alternative.addBodyPart(new MimeBodyPart(headers, contents));
				
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				alternative.writeTo(os);
				byte[] _ac = os.toByteArray();
				InternetHeaders _hdrs = new InternetHeaders();
				_hdrs.addHeader("Content-Type", alternative.getContentType());
				_hdrs.addHeader("Content-Transfer-Encoding", "binary");
				multipart.addBodyPart(new MimeBodyPart(_hdrs, _ac));
			    } else {
			    	multipart.addBodyPart(new MimeBodyPart(headers, contents));
			    }
			}
		    }
		    ct = new ContentType(multipart.getContentType());
		    multipart.writeTo(bodyStream);		
		}
		newText = new Text();
		byte[] body = bodyStream.toByteArray();
		if (wrapInRfc822) {
		    ByteArrayOutputStream rfc822os =
	  		new ByteArrayOutputStream();
		    OutputStreamWriter rfc822writer =
			new OutputStreamWriter(rfc822os, "us-ascii");
		    ct.getParameterList().set("type", "html");
		    rfc822writer.write("Content-Type: " + ct.toString() + "\r\n");
		    rfc822writer.write("Content-Transfer-Encoding: binary\r\n");
		    rfc822writer.write("\r\n");
		    ct = new ContentType("message/rfc822; x-type=mhtml");
		    rfc822writer.flush();
		    rfc822os.write(body);
		    body = rfc822os.toByteArray();
		}

		byte[] contents = new byte[subject.length + body.length + 1];
		System.arraycopy(subject, 0, contents, 0, subject.length);
		System.arraycopy(body, 0, contents, subject.length+1, body.length);
		contents[subject.length] = (byte) '\n';

		newText.setContents(contents);
		newText.getStat().replaceOrAddAuxItem(new AuxItem(AuxItem.tagContentType,
						ct.toString()));
	    }
  	    

	    if (parameters.containsKey("inCommentTo")) {
		String[] cmtToFields;
		if (parameters.get("inCommentTo") instanceof String[]) {
		    cmtToFields = (String[]) parameters.get("inCommentTo");
		} else {
		    cmtToFields = new String[] { (String) parameters.get("inCommentTo") };
		}
		for (int i=0; i < cmtToFields.length; i++) {
		    int textNo = Integer.parseInt(cmtToFields[i]);
		    newText.addCommented(textNo);

		    TextStat commentedTextStat = lyskom.getTextStat(textNo);
		    int[] _recipients = commentedTextStat.getRecipients();
		    for (int j=0; !explicitRecipients && j < _recipients.length; j++) {
	    		Conference conf = lyskom.getConfStat(_recipients[j]);
	    	    	if (conf.getType().original()) {
			    int superconf = conf.getSuperConf();
			    if (superconf > 0) {
	  			Debug.println("added superconf as comment-to recipient: " + _recipients[j] +
					" -> " + superconf);
		    	    	newText.addRecipient(superconf);
			    } else {
		   	    	throw new RuntimeException("Du får inte skriva kommentarer i " +
				 	      conf.getNameString());
			    }
	    	    	} else {
			    if (!newText.getStat().hasRecipient(_recipients[j])) {
	  			Debug.println("added implicit comment-to recipient: " + _recipients[j]);
		            	newText.addRecipient(_recipients[j]);
			    } else {
	  			Debug.println("avoiding duplicate recipient: " + _recipients[j]);
			    }
		    	}
		    }

		    // so we won't keep the old text's comments status
		    lyskom.purgeTextCache(textNo); 

		}
	    }
	    if (request.getParameterValues("footnoteTo") != null) {
		String[] fntToFields = request.getParameterValues("footnoteTo");
		for (int i=0; i < fntToFields.length; i++) {
		    int textNo = Integer.parseInt(fntToFields[i]);
		    newText.addFootnoted(textNo);
		    // so we won't keep the old text's comments status
		    lyskom.purgeTextCache(textNo); 
		}
	    }
	    for (Iterator i=recipients.iterator(); i.hasNext();) {
	        int confNo = ((ConfInfo) i.next()).getNo();
		if (!newText.getStat().hasRecipient(confNo))
		    newText.addRecipient(confNo);
	    }
	    for (Iterator i=ccRecipients.iterator(); i.hasNext();) {
	        int confNo = ((ConfInfo) i.next()).getNo();
		if (!newText.getStat().hasRecipient(confNo))
		    newText.addCcRecipient(confNo);
	    }

	    newTextNo = lyskom.createText(newText);
	    TextStat newTextStat = lyskom.getTextStat(newTextNo);
	    if (newTextNo > 0) {
		if (commonPreferences.getBoolean("created-texts-are-read")) {
		    int[] recipientNos = newTextStat.getRecipients();
		    int[] ccRecps = newTextStat.getCcRecipients();
		    int[] allRecps = new int[recipientNos.length+ccRecps.length];
		    System.arraycopy(recipientNos, 0, allRecps, 0, recipientNos.length);
		    System.arraycopy(ccRecps, 0, allRecps, recipientNos.length, ccRecps.length);
		    recipientNos = allRecps;
		    for (int i=0; i < recipientNos.length; i++) {
			try {
			    lyskom.markAsRead(recipientNos[i], new int[] { newTextStat.getLocal(recipientNos[i]) });	
			} catch (RpcFailure ex1) {
			    if (ex1.getError() != Rpc.E_not_member)
				throw ex1;
			}
		    }
		}
%>
	        <p class="statusSuccess">Text nummer <%= textLink(request, lyskom, newTextNo, false) %> är skapad.</p>
<%
	        if (parameter(parameters, "changePresentation") != null) {
		    int confNo = Integer.parseInt(parameter(parameters, "changePresentation"));
		    try {
			lyskom.setPresentation(confNo, newTextNo);
%>
 		    	<p class="statusSuccess">Ny presentation för <%=lookupName(lyskom, confNo, true)%> är <%=textLink(request, lyskom, newTextNo)%>.</p>
<%
		    } catch (RpcFailure ex1) {
			if (ex1.getError() == Rpc.E_permission_denied) {
			    out.println("<p class=\"statusError\">Du får inte ändra presentation för möte " +
				lookupName(lyskom, ex1.getErrorStatus(), true) + ".</p>");
			    lyskom.deleteText(newTextNo);
			    out.println("<p class=\"statusError\">Text nummer " + newTextNo + " är borttagen.</p>");
			} else {
			    throw ex1;
			}
		    }
	    	}

	    }
	}
	if (errors.length() > 0)
	    out.println("<p class=\"statusError\">" + errors + "</p>");
	
    }


    /*
    if (parameter(parameters, "postCommentTo") != null &&
	parameter(parameters, "inCommentTo") == null) {
	Text commentedText = lyskom.getText(Integer.parseInt(parameter(parameters, "postCommentTo")));
	Text newText = new Text(parameter(parameters, "subject"),
			parameter(parameters, "body").replaceAll("\r", ""),
			preferences.getString("create-text-charset"));
	wrapText(newText);
	newText.addCommented(commentedText.getNo());
	int[] recipients = commentedText.getRecipients();
	for (int i=0; i < recipients.length; i++) {
	    Conference conf = lyskom.getConfStat(recipients[i]);
	    if (conf.getType().original()) {
		int superconf = conf.getSuperConf();
		if (superconf > 0) {
		    newText.addRecipient(superconf);
		} else {
		   throw new RuntimeException("Du får inte skriva kommentarer i " +
					      conf.getNameString());
		}
	    } else {
		newText.addRecipient(recipients[i]);
	    }
	}
	newTextNo = lyskom.createText(newText);
	TextStat newTextStat = lyskom.getTextStat(newTextNo);
	lyskom.purgeTextCache(commentedText.getNo());
	if (newTextNo > 0) {
	    recipients = newTextStat.getRecipients();
	    for (int i=0; i < recipients.length; i++) {
		try {
		    lyskom.markAsRead(recipients[i], new int[] { newTextStat.getLocal(recipients[i]) });	
		} catch (RpcFailure ex1) {
		    if (ex1.getError() != Rpc.E_not_member)
			throw ex1;
		}
	    }
%>
	    <p class="statusSuccess">Text nummer <%= textLink(request, lyskom, newTextNo, false) %> är skapad.</p>
<%
	}
	
    }
    */
    if (parameters.containsKey("addRecipient")) {
	int rtype = Integer.parseInt(parameter(parameters, "recipientType"));
	int _textNo = Integer.parseInt(parameter(parameters, "toText"));
	ConfInfo lbx = lookupName(lyskom, (String) parameters.get("addRecipient"), true, true);
	if (lbx == null) {
	    out.println("<p class=\"statusError\">Inget möte matchade det angivna namnet.</p>");
	} else {
	    int confNo = lbx.getNo();
	    lyskom.addRecipient(_textNo, confNo, rtype);
	    out.println("<p class=\"statusSuccess\">OK: ny mottagare för " + textLink(request, lyskom, _textNo) + ": " +
		lookupName(lyskom, confNo, true) + ".</p>");
	}
    }

    if (parameter(parameters, "setPassword") != null) {
	ConfInfo lbx = lookupName(lyskom, (String) parameters.get("setPasswordPerson"), true, false);
	if (lbx == null) {
	    out.println("<p class=\"statusError\">Ingen person har det angivna namnet.</p>");
	} else {
	    if (parameters.get("setPasswordNewPassword").
		equals(parameters.get("setPasswordNewPasswordVerify"))) {
	    	lyskom.setPassword(lbx.getNo(), (String) parameters.get("setPasswordUserPassword"),
			           (String) parameters.get("setPasswordNewPassword"));
	    	out.println("<p class=\"statusSuccess\">Person " +
			    lookupName(lyskom, lbx.getNo(), true) + " har bytt lösenord.</p>");
	    } else {
		out.println("<p class=\"statusError\">Lösenorden stämde inte överens.</p>");
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
	<script language="JavaScript1.2" src="stuff.jsp"></script>
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
	<p class="notice">Du har ingen presentation.
	Varför inte <a href="<%=basePath%>?changePresentation=<%=person.getNo()%>">skriva en</a>?
	</p>
<%
    }
    if (showPOM) {
%>
   	<p class="nav">
	    [ <a href="<%=basePath%>?logout">logga ut</a>
              (<a title="Logga ut mina andra sessioner" href="<%=myURI(request)%>?purgeOtherSessions">övriga</a>) |
	      <a href="<%=basePath%>?listnews">lista nyheter</a> |
	      <a href="composer.jsp">skriv inlägg</a> |
              <% if (lyskom.getServer().equals("sno.pp.se")) { %>
	      <a href="<%=basePath%>?uploadForm">ladda upp bild</a> | 
              <% } %>
              <a href="<%=basePath%>?reviewMarked">lista markerade</a> ]
	    <br/>
	    [ <a href="<%=basePath%>?setPasswordForm">ändra lösenord</a> ]
	    [ <a href="<%=basePath%>prefs.jsp">inställningar</a> ]
	    [ <a href="<%=basePath%>?suspend">pausa</a> ]
   	</p>
<%
    }
    if (parameter(parameters, "reviewMarked") != null) {
	out.println("<p><table><tr><td>Typ</td><td>text</td><td>författare</td><td>ärende</td></tr>");
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
		"</td><td>" + htmlize(new String(t.getSubject())) + "</td></tr>");
	    out.flush();
	}
	out.println("</table></p");
	out.flush();
    }
    if (conferenceNumber > 0 && !parameters.containsKey("listSubjects") &&
	!parameters.containsKey("comment")) {
	int nextUnreadText = 0;
	try {
	    lyskom.changeConference(conferenceNumber);
	    Membership ms = lyskom.queryReadTextsCached(conferenceNumber);
	    if (ms == null) ms = lyskom.queryReadTexts(lyskom.getMyPerson().getNo(), conferenceNumber);

	    UConference uconf = lyskom.getUConfStat(conferenceNumber);
	    int unreads = 0;
	    if (uconf.getHighestLocalNo() > ms.getLastTextRead()) {
	    	unreads = uconf.getHighestLocalNo() - ms.getLastTextRead();
		unreads -= ms.getReadTexts().length;
	    }
%>
	    <p>
	    Läser i <%= lookupName(lyskom, conferenceNumber, true) %> - <%= unreads %>
	    <%= unreads > 1 || unreads == 0 ? "olästa" : "oläst" %>.<br/>
<%
	    int maxTextsToShow = preferences.getInt("show-multiple-texts");
	    nextUnreadText = -1;	    

	    lyskom.doChangeWhatIAmDoing("Läser");
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
		int[] comments = stat.getComments();
		for (int i=comments.length-1; textNumbers.size() < maxTextsToShow && i >= 0; i--) {
		    TextStat commentStat = lyskom.getTextStat(comments[i]);
		    for (Iterator iter = commentStat.getAllRecipients().iterator();
			 iter.hasNext();) {
			if (lyskom.isMemberOf(((Integer) iter.next()).intValue())) {
			    if (!textNumbers.contains(new Integer(comments[i])))
				textNumbers.add(new Integer(comments[i]));
			}
		    }
		}
	    }

	} catch (RpcFailure ex1) {
	    if (ex1.getError() == Rpc.E_not_member) {
		out.println("<p class=\"statusError\">Fel: du är inte medlem i " +
			lookupName(lyskom, conferenceNumber, true) + "</p>");
	    } else if (ex1.getError() == Rpc.E_undefined_conference) {
		out.println("<p class=\"statusError\">Fel: möte " +
			ex1.getErrorStatus() + " finns inte.</p>");
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
	    if (textNumber == 0 && !parameters.containsKey("text") &&
		parameter(parameters, "comment") == null && newTextNo == 0) {
		listNews = true;
	    }
	    if (newTextNo > 0) {
		listNews = true;
	    }
	}
%>
	</p>
<%
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
		out.println("<p>Återser FAQ för " + lookupName(lyskom, conf.getNo(), true) + ".</p>");
	    } else {
		out.println("<p class=\"statusError\">Fel: mötet " + lookupName(lyskom, conf.getNo(), true) + 
			" har ingen FAQ.</p>");
	    }
	} catch (RpcFailure ex1) {
	    out.println("<p class=\"statusError\">Fel: felkod " + ex1.getError() +
		", status " + ex1.getErrorStatus() + "</p>");
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
		out.println("<p class=\"statusError\">Hittade inget möte eller person som matchade " 
		+ "\"" + htmlize((String) parameters.get("reviewPresentation")) + "\"</p>");
	    } else if (confs.length > 1) {
		out.println(ambiguousNameMsg(lyskom, new AmbiguousNameException(confs)));
	    } else {
		conf = lyskom.getConfStat(confs[0].getNo());
		pres = conf.getPresentation();
	    }
	}
	if (pres > 0) {
	    textNumber = conf.getPresentation();
	    out.println("<p class=\"statusSuccess\">Återser presentation för " +
	                lookupName(lyskom, conf.getNo(), true) + ".</p>");
	} else {
	    out.println("<p class=\"statusError\">" + lookupName(lyskom, conf.getNo(), true) + " har ingen presentation.</p>");
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
		    %><p class="statusError">Fel: text <%= textNumber %> existerar inte.</p><%
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
	if (viewedTexts.size() > 4)
	    linkText = new StringBuffer("Läsmarkera alla " + viewedTexts.size() + " texter");

	if (conferenceNumber > 0) {
	    linkText.append(" (och läs nästa)");
	    queryStr.append("&conference=").append(conferenceNumber);
	}
	linkText.append(".");
	out.println("<p><a accesskey=\"N\" href=\"?" + queryStr.toString() + "\">" +
		linkText.toString() + "</a></p>");
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
	Membership membership = lyskom.queryReadTextsCached(conferenceNumber);
	UConference uconf = lyskom.getUConfStat(conferenceNumber);
	TextMapping mapping = lyskom.localToGlobal(conferenceNumber,
						   membership.getLastTextRead()+1, 255);
	out.println("<p><table><tr><td>Nummer</td><td>författare</td><td>ärende</td><td>tecken</td></tr>");
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
	    out.print(htmlize(new String(text.getSubject(), charset)));
	    out.print("</td><td>");
	    out.print(""+ text.getStat().getSize());
	    out.println("</td></tr>");
	    out.flush();
	    pyjamas = !pyjamas;
	}
	out.println("</table></p>");
    }
    if (parameter(parameters, "comment") != null && textNumber > 0) {
	lyskom.doChangeWhatIAmDoing("Skriver en kommentar");
	Text commented = lyskom.getText(textNumber);
	String ccharset = commented.getCharset();
	if (ccharset.equals("us-ascii")) ccharset = "iso-8859-1";
%>
	<form class="boxed" method="post" action="<%=myURI(request)%><%=conferenceNumber>0?"?conference="+conferenceNumber:""%>">
	<input type="hidden" name="inCommentTo" value="<%=textNumber%>">
	Skriver en kommentar till text <%= textNumber %> av <%= lookupName(lyskom, lyskom.getTextStat(textNumber).getAuthor(), true) %><br/>
	<input size="50" type="text" name="subject" value="<%= dqescHtml(new String(commented.getSubject(), ccharset)) %>"><br/>
	<textarea name="body" cols="71" rows="10"></textarea><br/>
	<input type="submit" value="skicka!" name="createText">
	<input type="submit" name="dispatchToComposer" value="avancerat läge">
	</form>
<%
    }
%>
<%
	    listNews = listNews || (parameter(parameters, "listnews") != null ||
	        (justLoggedIn && preferences.getBoolean("list-news-on-login")));

	    if (listNews) {
		if (preferences.getBoolean("auto-refresh-news") &&
		    (request.getHeader("User-Agent").indexOf("MSIE") >= 0 ||
	    	     request.getHeader("User-Agent").indexOf("Gecko") >= 0)) {
%>
	    <script language="JavaScript1.2">
		var interval = <%= interval %>*1000;
		var timeLeft = interval;
	        var refreshInProgress = false;
		function countdown() {
		    timeLeft -= 1000;
		    var s = timeLeft / 1000;
		    var div = document.getElementById("countdown");
		    if (div != null && timeLeft > 0) {
			div.innerHTML = "<span class=\"countdown\">(uppdaterar om " + s + 
			    (s > 1 ? " sekunder" : " sekund") + ")</span>";
		    } else if (div != null) {
			if (!refreshInProgress) {
	                    div.innerHTML = "<span class=\"countdown\">(uppdaterar...)</span>";
	                    refresh();
	                }
		    }
		}
		function refresh() {
	            refreshInProgress = true;
		    document.location.href = "<%=basePath%>?listnews&saveMessages&autoRefresh";
		}
		var ivref = window.setInterval(countdown, 1000);
	    </script>

<%		} 
%>
	<p>
	<ul>
<%	
		List unreadConfsList = lyskom.getUnreadConfsListCached();
		Iterator confIter = unreadConfsList.iterator();
		int sum = 0, confsum = 0;
		int lastconf = 0;
		int skipTo = 0;
		int skipped = 0;
		if (parameters.containsKey("skipTo")) {
		    skipTo = Integer.parseInt(parameter(parameters, "skipTo"));
		}
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
			Membership membership = lyskom.queryReadTextsCached(conf);
			if (membership == null) {
			    try {
				membership = lyskom.queryReadTexts(me, conf);
			    } catch (RpcFailure ex1) {
				if (ex1.getError() == Rpc.E_not_member) {
				    confIter.remove();
				    continue;
				} else {
				    throw ex1;
				}
			    }
			}

			int[] readTexts = membership.getReadTexts();
			UConference uconf = lyskom.getUConfStat(conf);
			int unreads = 0, highestLocalNo = uconf.getHighestLocalNo();
			if (highestLocalNo > membership.getLastTextRead()) {
			    unreads = highestLocalNo - membership.getLastTextRead();
			}
			if (unreads == 0) {
			    lyskom.setLastRead(conf, highestLocalNo);
			    confIter.remove();
			    continue;
			}
			sum += unreads;
			sum -= readTexts.length;
			confsum++;
			out.print("<li> <a href=\"" + myURI(request) + "?conference=" +
				  conf + "\">" + 
				  lookupName(lyskom, conf, true) + "</a>: " +
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
		        out.println("<p>Hoppade över " + skipped + " möten.</p>");
		    }
		if (manyMemberships && confIter.hasNext()) {
%>
		<p>Många möten: det finns troligen fler olästa i de
	           <%= unreadConfsList.size()-confsum %> möten
		   som inte visas i denna lista (<a href="<%= basePath %>?listnews&skipTo=<%= lastconf %>">lista nästa 5</a>).</p>
<%
		}
%>
		<%= confsum == 0 ? "<b>inga olästa i något möte</b>" : sum + " oläst(a) i " + confsum + " möte(n)" %>
		<div id="countdown"></div>
<%		if (sum > 0) {
		    out.println(jsTitle(serverShort(lyskom) + ": " + 
			(sum == 1 ? "ett oläst" : sum + " olästa")));
		}
%>
	</p>
<%
	    }

	    MultipartParser multip = null;
	    if (parameter(parameters, "upload") != null) {
		multip = new MultipartParser(request, 1024*1024);
	    	Part nextPart = null;
		boolean imageOK = false;
		String imageFileName = null;
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
		    } else {
			ParamPart ppart = (ParamPart) nextPart;
			String url = ppart.getStringValue();
			if (url == null || url.trim().equals("")) continue;

			URLConnection con = new URL(url).openConnection();
			String fileExt = url.substring(url.lastIndexOf(".")).toLowerCase();
			imageFileName = lyskom.getMyPerson().getNo() + fileExt;
			File target = new File(dir, imageFileName);
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
    <form method="get" action="<%=myURI(request)%>" class="boxed">
    Läs ett inlägg: <input type="text" size="10" name="text">
    <input type="submit" value="ok!">
    </form>

    <form action="<%=myURI(request)%>" class="boxed" method="post">
    Endast: <input type="text" size="3" name="endast"> inlägg i möte
    <input type="text" size="40" name="endastConferenceName">
    <input type="submit" value="ok!">
<%  if (listNews) { %>
    <input type="hidden" name="listnews" value="<%=parameter(parameters, "listnews")%>">
<%  } %>
    </form>

    <form action="<%=myURI(request)%>" class="boxed" method="post">
    <a name="sendMessage"></a>
    Skicka ett meddelande till:<br/>
<%  if (listNews) { %>
    <input type="hidden" name="listnews" value="<%=parameter(parameters, "listnews")%>">
<%  } %>
    <input type="text" size="40" name="sendToName" value="<%=lastReceivedOrSent!=null?lastReceivedOrSent:""%>">
<br/>
    Text:<br/>
    <input type="text" name="sendText" size="60"><input type="submit" value="ok">
    </form>
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
<p class="intro">
Du är inte inloggad.
</p>

<form name="lyskomlogin" method="post" action="<%=myURI(request)%>">
<%
    String lyskomNamn = "";
    if (parameter(parameters, "lyskomNamn") != null) lyskomNamn = parameter(parameters, "lyskomNamn");
%>
<table class="boxed">
<tr><td>namn:</td><td><input type="text" name="lyskomNamn" value="<%= lyskomNamn %>" size="30"></td></tr>
<tr><td>lösenord:</td><td><input type="password" name="lyskomLosen" size="8"></td></tr>
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
<p class="nav">
<% if (showPOM) { %>
[ 
<%
        if (!authenticated.booleanValue() || lyskom.getServer().equals("sno.pp.se")) {
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
	<p><a href="<%= basePath%>?debug">debugdata</a>
	   <a href="prefs.jsp">inställningar</a></p>
<%  } %>
</p>
<p class="footer">
<%
    List suspendedSessions = null;
    try {
	suspendedSessions = (List) session.getAttribute("lyskom.suspended");
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
		(suspSessCount > 1 ? "pausade LysKOM-sessioner" :
		 "pausad LysKOM-Session") + "</b></a>");
	boolean unreads = false;
	synchronized (suspendedSessions) {
	    for (Iterator i=suspendedSessions.iterator();!unreads && i.hasNext();) {
		if (((SessionWrapper) i.next()).getSession().getUnreadConfsListCached().size() > 0)
		    unreads = true;
	    }
	}
	if (unreads) out.print(" (olästa)");
	out.print(" <a title=\"nästa session\" href=\"sessions.jsp?next\">>></a>");
	out.println("<br/>");
    }
%>
<a href="about.jsp">Hjälp och information om Weblatte</a><br/>
$Revision: 1.59 $
</p>
</body>
</html>

