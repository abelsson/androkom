<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*,
				 java.net.*, java.io.*, java.text.*,java.util.regex.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ page errorPage='fubar.jsp' %>
<%@ include file='kom.jsp' %>
<%
    String server = request.getParameter("server") != null ? 
	request.getParameter("server") : Servers.defaultServer.hostname;
    Boolean authenticated = (Boolean) session.getAttribute("LysKOMauthenticated");
    if (authenticated == null) authenticated = Boolean.FALSE;
    String error = null;
    boolean justLoggedIn = false;
    try {
	if (request.getParameter("lyskomNamn") != null ||
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
		session.setAttribute("lyskom.messages", messages);
		lyskom.addAsynchMessageReceiver(new MessageReceiver(messages));
	    }
	    ConfInfo[] names = null;
	    int person = 0;
	    String password = (String) session.getAttribute("lyskomPassword");
	    if (password == null) password = request.getParameter("lyskomLosen");

	    if (request.getParameter("lyskomNamn") != null) {
		String namnParam = request.getParameter("lyskomNamn");
		if (request.getParameter("createPerson") != null) {
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
		    names = lyskom.lookupName(request.getParameter("lyskomNamn"), true, false);
		    if (names.length == 1) person = names[0].getNo();
		}
	    } else {
		person = ((Integer) session.getAttribute("lyskomPersonNo")).intValue();
	    }
	    if (person > 0) {
		if (!lyskom.login(person, password,
				  request.getParameter("lyskomDold") != null, false)) {
		    error = "Felaktigt lösenord!";
		} else {
		    session.setAttribute("lyskom", lyskom);
		    authenticated = Boolean.TRUE;
                    justLoggedIn = true;
		    lyskom.setLatteName("WebLatte");
		    lyskom.setClientVersion("dll.nu/lyskom", "$Revision: 1.14 $" + 
					    (debug ? " (devel)" : ""));
		    lyskom.doChangeWhatIAmDoing("kör web-latte");
		}
	    } else if (names != null && names.length == 0) {
		error = "Namnet du angav (\"" + htmlize(request.getParameter("lyskomNamn")) + "\") " +
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
	lyskom = null;
	error = "Det gick inte att ansluta till servern: " + ex1.getMessage();
    } catch (UnknownHostException ex2) {
	lyskom = null;
	error = "Det gick inte att slå upp serverns DNS-namn: " + ex2.getMessage();
    }

    try {
 	session.setAttribute("LysKOMauthenticated", authenticated);
        if (authenticated.booleanValue()) {
	    String gotoURL = (String) session.getAttribute("goto");
    	    if (gotoURL != null) {
		if (justLoggedIn) {
		    if (gotoURL.indexOf("?") == -1) {
			gotoURL = gotoURL + "?jul";
		    } else {
			gotoURL = gotoURL + "&jul";
		    }
		}
	        session.removeAttribute("goto");
	        response.sendRedirect(gotoURL);
	        return;
            }
	    justLoggedIn = justLoggedIn || request.getParameter("jul") != null;
        }
    } catch (IllegalStateException ex1) {}
    List messages = null;
    int interval = 120; // seconds
%>
<html><head>
<script language="JavaScript1.2" src="stuff.jsp"></script>
<% if (authenticated.booleanValue()) { %>
<title><%= serverShort(lyskom) %></title>
<% } else { %>
<title>snoppkom (web-latte)</title>
<% } %>
</head>
<link rel="stylesheet" href="lattekom.css" />
<body>
<%@ include file='dhtmlMenu.jsp' %>
<%
    if (error != null) {
%>
<p class="statusError"><%= error %></p>
<%
    }
    if (request.getParameter("debug") != null) {
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

		if (request.getParameter("uablock") != null) {
		    String blockName = request.getParameter("uablock");
		    Hollerith block = userArea.getBlock(blockName);
		    out.println("------ Block \"" + blockName + "\" contents:");
		    out.println(block.getContentString());
		    out.println("------");
		}
	    }
	}
	
	out.println("</pre>");
    }
    if (request.getParameter("logout") != null) {
	if (lyskom != null) {
	    lyskom.shutdown();
	}
	if (session != null) session.invalidate();
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
	    response.sendRedirect(basePath + "frames.jsp?conference=0");
	    return;
	}
    }
    if (request.getParameter("pom") != null) {
	session.setAttribute("pom", new Boolean(request.getParameter("pom").equals("true")));
    }

    boolean showPOM = preferences != null && preferences.getBoolean("show-plain-old-menu");
    boolean showWelcome = request.getParameter("hw") == null &&
		(preferences != null && preferences.getBoolean("always-show-welcome"));

    boolean showStandardBoxes = request.getParameter("hs") == null
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
		out.println("</pre>");
	    }
	    if (justLoggedIn || showWelcome) {
%>
    	<h2>välkommen till LysKOM, <%= lookupName(lyskom, lyskom.getMyPerson().getNo(), true) %>!</h2>
	<!-- Ditt sessions-ID är "<%= Integer.toHexString(System.identityHashCode(lyskom)) %>". -->
<%
	    }
    if (request.getParameter("dispatchToComposer") != null) {
	request.setAttribute("set-uri", makeAbsoluteURL("composer.jsp"));
	RequestDispatcher d = getServletContext().getRequestDispatcher(appPath + "/composer.jsp");
	d.forward(request, response);
	return;
    }
    if (request.getParameter("changeName")!= null) {
	String oldName = request.getParameter("changeName");
	String newName = request.getParameter("newName");
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
    if (request.getParameter("mark") != null) {
	lyskom.markText(Integer.parseInt(request.getParameter("mark")), 100);
	out.println("<p class=\"statusSuccess\">Text " +
		request.getParameter("mark") + " har markerats.</p>");
    }
    if (request.getParameter("unmark") != null) {
	lyskom.unmarkText(Integer.parseInt(request.getParameter("unmark")));
	out.println("<p class=\"statusSuccess\">Text " +
		request.getParameter("unmark") + " har avmarkerats.</p>");
    }
    if (request.getParameter("privateReply") != null) {
	request.setAttribute("set-uri", makeAbsoluteURL("composer.jsp"));
	RequestDispatcher d = getServletContext().getRequestDispatcher(appPath + "/composer.jsp");
	d.forward(request, response);
	return;	

    }
    if (request.getParameter("changePresentation") != null &&
	request.getParameter("createText") == null) {
	request.setAttribute("set-uri", makeAbsoluteURL("composer.jsp"));
	RequestDispatcher d = getServletContext().getRequestDispatcher(appPath + "/composer.jsp");
	d.forward(request, response);
	return;		
    }

    if (request.getParameter("endast") != null) {
	int textcount = Integer.parseInt(request.getParameter("endast"));
	ConfInfo conf = null;
	try {
	    conf = lookupName(lyskom, request.getParameter("endastConferenceName"), true, true);
	    if (conf != null) {
	    	out.print("<p>Endast " + textcount + " inlägg i möte " +
			lookupName(lyskom, conf.getNo(), true) + "...");
	    	out.flush();
	    	lyskom.endast(conf.getNo(), textcount);
	    	out.println(" ok.</p>");
	    	session.removeAttribute("mbInited");
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

    if (request.getParameter("join") != null || request.getParameter("joinNo") != null) {
	int confNo = request.getParameter("joinNo") != null ? 
		Integer.parseInt(request.getParameter("joinNo")) : 0;

	try {
	    ConfInfo conf = null;
	    if (request.getParameter("join") != null) {
		conf = lookupName(lyskom, request.getParameter("join"), false, true);
		if (conf != null) confNo = conf.getNo();
	    }
	    if (confNo > 0) {
		out.print("<p>Bli medlem i " + lookupName(lyskom, confNo, true) + "...");
		out.flush();
		lyskom.joinConference(confNo);
		out.print("OK!</p>");
		out.flush();
		session.setAttribute("mbInited", Boolean.FALSE);
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
 
    if (request.getParameter("leave") != null || request.getParameter("leaveNo") != null) {
	try {
	    int confNo = request.getParameter("leaveNo") != null ?
		Integer.parseInt(request.getParameter("leaveNo")) : 0;

	    if (request.getParameter("leave") != null) {
	        ConfInfo conf = lookupName(lyskom, request.getParameter("leave"), false, true);
		if (conf != null) confNo = conf.getNo();
	    }
	    if (confNo > 0) {
		out.print("<p>Utträda ur möte " + lookupName(lyskom, confNo, true) + "...");
		out.flush();
		lyskom.subMember(confNo, lyskom.getMyPerson().getNo());
		out.println("OK!</p>");
		out.flush();
		session.setAttribute("mbInited", Boolean.FALSE);
	    } else {
		out.println("<p class=\"statusError\">Fel: hittar inget sådant möte</p>");
	    }
	} catch (AmbiguousNameException ex1) {
	    out.println(ambiguousNameMsg(lyskom, ex1));
 	} catch (RpcFailure ex2) {
	    out.println("misslyckades.</p><p class=\"statusError\">Fel: du är inte medlem i mötet.</p>");
	}
    }

    Boolean mbInitedObj = (Boolean) session.getAttribute("mbInited");
    if (mbInitedObj == null) mbInitedObj = Boolean.FALSE;
    if (!mbInitedObj.booleanValue()) {
	out.print("<p>Läser in medlemskapsinformation...");
	out.flush();
	lyskom.updateUnreads();
	out.println("klart.</p>");
	mbInitedObj = Boolean.TRUE;
    }    
    session.setAttribute("mbInited", mbInitedObj);

    if (request.getParameter("autoRefresh") == null) {
	lyskom.doUserActive();
    }
    String lastReceivedOrSent = null;


    if (request.getParameter("sendToName") != null) {
	String stn = request.getParameter("sendToName");
	try {
	    if (request.getParameter("chat") != null) {
		if (!stn.trim().equals("")) {
	            ConfInfo conf = lookupName(lyskom, stn, true, true);
	            response.sendRedirect(basePath + "chat.jsp?default=" + conf.getNo());
	            return;
		} else {
		    response.sendRedirect(basePath + "chat.jsp");
		    return;
		}
	    } 
	    String _text = request.getParameter("sendText");
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


    messages = (List) session.getAttribute("lyskom.messages");
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
    		    if (request.getParameter("saveMessages") == null) {
			i.remove();
		     }
		} else if (m.getNumber() == Asynch.new_text ||
		    m.getNumber() == Asynch.new_text_old ||
		    m.getNumber() == Asynch.new_recipient) {
		    lyskom.updateUnreads();
		    i.remove();
		} else {
		    i.remove();
		}
	    }
	}
    }

    if (request.getParameter("stopChat") != null) {
        session.setAttribute("lyskom.chat-running", Boolean.FALSE);
    }

    if (request.getParameter("sendTo") != null) {
	lastReceivedOrSent = lookupName(lyskom, Integer.parseInt(request.getParameter("sendTo")));
    }

    int textNumber = 0;
    int conferenceNumber = 0;
    int newTextNo = 0;
    if (request.getParameter("purgeOtherSessions") != null) {
	out.println("<p><pre>Listar sessioner...");
	out.flush();
	int mySession = lyskom.whoAmI();
	DynamicSessionInfo[] sessions = lyskom.whoIsOnDynamic(true, true, 0);
	for (int i=0; i < sessions.length; i++) {
	    if (sessions[i].getPerson() == lyskom.getMyPerson().getNo() &&
		sessions[i].getSession() != mySession) {
		out.print("Avslutar session nummer " + sessions[i].getSession() + "...");
		out.flush();
		lyskom.disconnect(sessions[i].getSession());
		out.println(" OK.");
	    }
	}
	out.println("Klar.");
	out.println("</pre></p>");
	out.flush();
    }
    if (request.getParameter("conference") != null) {
	conferenceNumber = Integer.parseInt(request.getParameter("conference"));
    }
    if (request.getParameter("markAsRead") != null) {
	String[] values = request.getParameterValues("markAsRead");
	Map conferences = new HashMap();
	for (int i=0; i < values.length; i++) {
	    TextStat readText = lyskom.getTextStat(Integer.parseInt(values[i]));
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
    if (request.getParameter("createText") != null) {

	List recipients = new LinkedList();
    	List ccRecipients = new LinkedList();

    	StringBuffer errors = new StringBuffer();

    	for (int rcptType = 1; rcptType <= 2; rcptType++) {
            String[] recptFields = request.getParameterValues(rcptType == 1 ? "recipient" : "ccRecipient");
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
	if (errors.length() == 0) {
  	    Text newText = new Text(request.getParameter("subject"),
			request.getParameter("body").replaceAll("\r", ""),
	                preferences.getString("create-text-charset"));
	    if (request.getParameter("contentType") != null) {
		newText.getStat().addAuxItem(new AuxItem(AuxItem.tagContentType,
					     new Bitstring("00000000"), 0,
					     new Hollerith(request.getParameter("contentType"))));
	    }
	    wrapText(newText);
	    if (request.getParameterValues("inCommentTo") != null) {
		String[] cmtToFields = request.getParameterValues("inCommentTo");
		for (int i=0; i < cmtToFields.length; i++) {
		    int textNo = Integer.parseInt(cmtToFields[i]);
		    newText.addCommented(textNo);
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
		newText.addRecipient(((ConfInfo) i.next()).getNo());
	    }
	    for (Iterator i=ccRecipients.iterator(); i.hasNext();) {
		newText.addCcRecipient(((ConfInfo) i.next()).getNo());
	    }

	    newTextNo = lyskom.createText(newText);
	    newText = lyskom.getText(newTextNo);
	    if (newTextNo > 0) {
		if (commonPreferences.getBoolean("created-texts-are-read")) {
		    int[] recipientNos = newText.getRecipients();
		    int[] ccRecps = newText.getCcRecipients();
		    int[] allRecps = new int[recipientNos.length+ccRecps.length];
		    System.arraycopy(recipientNos, 0, allRecps, 0, recipientNos.length);
		    System.arraycopy(ccRecps, 0, allRecps, recipientNos.length, ccRecps.length);
		    recipientNos = allRecps;
		    for (int i=0; i < recipientNos.length; i++) {
			try {
			    lyskom.markAsRead(recipientNos[i], new int[] { newText.getLocal(recipientNos[i]) });	
			} catch (RpcFailure ex1) {
			    if (ex1.getError() != Rpc.E_not_member)
				throw ex1;
			}
		    }
		}
%>
	        <p class="statusSuccess">Text nummer <%= textLink(request, lyskom, newTextNo, false) %> är skapad.</p>
<%
	        if (request.getParameter("changePresentation") != null) {
		    int confNo = Integer.parseInt(request.getParameter("changePresentation"));
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
	} else {
	    out.println("<p class=\"statusError\">" + errors + "</p>");
	}
    }

    if (request.getParameter("postCommentTo") != null &&
	request.getParameter("inCommentTo") == null) {
	Text commentedText = lyskom.getText(Integer.parseInt(request.getParameter("postCommentTo")));
	Text newText = new Text(request.getParameter("subject"),
			request.getParameter("body").replaceAll("\r", ""),
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
	newText = lyskom.getText(newTextNo);
	lyskom.purgeTextCache(commentedText.getNo());
	if (newTextNo > 0) {
	    recipients = newText.getRecipients();
	    for (int i=0; i < recipients.length; i++) {
		try {
		    lyskom.markAsRead(recipients[i], new int[] { newText.getLocal(recipients[i]) });	
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

    if (request.getParameter("setPassword") != null) {
	ConfInfo lbx = lookupName(lyskom, request.getParameter("setPasswordPerson"), true, false);
	if (lbx == null) {
	    out.println("<p class=\"statusError\">Ingen person har det angivna namnet.</p>");
	} else {
	    if (request.getParameter("setPasswordNewPassword").
		equals(request.getParameter("setPasswordNewPasswordVerify"))) {
	    	lyskom.setPassword(lbx.getNo(), request.getParameter("setPasswordUserPassword"),
			           request.getParameter("setPasswordNewPassword"));
	    	out.println("<p class=\"statusSuccess\">Person " +
			    lookupName(lyskom, lbx.getNo(), true) + " har bytt lösenord.</p>");
	    } else {
		out.println("<p class=\"statusError\">Lösenorden stämde inte överens.</p>");
	    }
	}
	
    }

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
   	</p>
<%
    }
    if (request.getParameter("reviewMarked") != null) {
	out.println("<p><table><tr><td>Typ</td><td>text</td><td>författare</td><td>ärende</td></tr>");
	Mark[] marks = lyskom.getMarks();
	boolean pyjamas = false;
	for (int i=0; i < marks.length; i++) {
	    pyjamas = !pyjamas;
	    Text t = null;
	    try {
		t = lyskom.getText(marks[i].getText());
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
    if (conferenceNumber > 0 && request.getParameter("listSubjects") == null) {
%>
	<p>
<%
	int nextUnreadText = 0;

	try {
	    lyskom.doChangeWhatIAmDoing("Läser");
	    lyskom.changeConference(conferenceNumber);
	    nextUnreadText = lyskom.nextUnreadText(conferenceNumber, false);
	} catch (RpcFailure ex1) {
	    if (ex1.getError() == Rpc.E_not_member) {
		out.println("<p class=\"statusError\">Fel: du är inte medlem i " +
			lookupName(lyskom, conferenceNumber, true) + "</p>");
	    } else {
		throw ex1;
	    }
	}
	if (nextUnreadText > 0) {
	    if (request.getParameter("text") == null) textNumber = nextUnreadText;
	} else if (nextUnreadText == -1) {
%>
	Det finns inte fler olästa i <%= lookupName(lyskom, conferenceNumber, true) %>.
<%
	    if (textNumber == 0 && request.getParameter("text") == null &&
		request.getParameter("comment") == null && newTextNo == 0 &&
		!response.isCommitted()) {
		response.sendRedirect(myURI(request)+"?listnews");
		return;
	    }
	}
%>
	</p>
<%
    }
    if (request.getParameter("reviewPresentation") != null) {
	Conference conf = null;
	int pres = 0;
	try {
	    conf = lyskom.getConfStat(Integer.parseInt(request.getParameter("reviewPresentation")));
	    pres = conf.getPresentation();

	} catch (NumberFormatException ex1) {
	    ConfInfo[] confs = lyskom.lookupName(request.getParameter("reviewPresentation"), true, true);
	    if (confs.length == 0) {
		out.println("<p class=\"statusError\">Hittade inget möte eller person som matchade " 
		+ "\"" + htmlize(request.getParameter("reviewPresentation")) + "\"</p>");
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
    if (request.getParameter("reviewOriginal") != null) {
	int startTextNo = Integer.parseInt(request.getParameter("reviewOriginal"));
	Text t = lyskom.getText(startTextNo);
	while (t.getCommented() != null &&
	       t.getCommented().length > 0) {
	    t = lyskom.getText(t.getCommented()[0]);
	}
	textNumber = t.getNo();
	
    }

    if (textNumber != 0 || request.getParameter("text") != null) {
	// xxx: catch NFE for more graceful error handling
        List textNumbers = new LinkedList();
	if (textNumber > 0) textNumbers.add(new Integer(textNumber));
	if (request.getParameter("text") != null) {
	    String[] textNumberParams = request.getParameterValues("text");
	    for (int i=0; i < textNumberParams.length; i++) {
	        textNumbers.add(new Integer(textNumberParams[i]));
	    }
        }
        for (Iterator i = textNumbers.iterator(); i.hasNext();) {
	    textNumber = ((Integer) i.next()).intValue();

	    try {
		Text text = lyskom.getText(textNumber);
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
    List viewedTexts = (List) request.getAttribute("viewedTexts");
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
		linkText.append(", ");
		queryStr.append("&");
	    } else {
		linkText.append(" ");
	    }
	}
	if (viewedTexts.size() > 4)
	    linkText = new StringBuffer("Läsmarkera alla " + viewedTexts.size() + " texter");

	if (conferenceNumber > 0) {
	    linkText.append(" (och läs nästa)");
	    queryStr.append("&conference=").append(conferenceNumber);
	}
	linkText.append(".");
	out.println("<p><a href=\"?" + queryStr.toString() + "\">" +
		linkText.toString() + "</a></p>");
    }

    if (request.getParameter("lookup") != null) {
	String str = request.getParameter("lookup");
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

    if (request.getParameter("listSubjects") != null && conferenceNumber > 0) {
	Membership membership = lyskom.queryReadTextsCached(conferenceNumber);
	UConference uconf = lyskom.getUConfStat(conferenceNumber);
	TextMapping mapping = lyskom.localToGlobal(conferenceNumber,
						   membership.getLastTextRead()+1, 255);
	out.println("<p><table><tr><td>Nummer</td><td>författare</td><td>ärende</td></tr>");
	boolean pyjamas = true;
	while (mapping.hasMoreElements()) {
	    int textNo = ((Integer) mapping.nextElement()).intValue();
	    Text text = lyskom.getText(textNo);
	    String charset = text.getCharset();
	    if ("us-ascii".equals(charset)) charset = "iso-8859-1";
	    if (pyjamas) out.print("<tr bgcolor=\"#ccffff\">");
	    else out.print("<tr>");
	    out.print("<td>");
	    out.print(textLink(request, lyskom, textNo, false));
	    out.print("</td><td>");
	    out.print(lookupName(lyskom, text.getAuthor(), true));
	    out.print("</td><td>");
	    out.print(new String(text.getSubject(), charset));
	    out.println("</td></tr>");
	    out.flush();
	    pyjamas = !pyjamas;
	}
	out.println("</table></p>");
    }
    if (request.getParameter("comment") != null && textNumber > 0) {
	lyskom.doChangeWhatIAmDoing("Skriver en kommentar");
	Text commented = lyskom.getText(textNumber);
%>
	<form class="boxed" method="post" action="<%=myURI(request)%><%=conferenceNumber>0?"?conference="+conferenceNumber:""%>">
	<input type="hidden" name="postCommentTo" value="<%=textNumber%>">
	Skriver en kommentar till text <%= textNumber %> av <%= lookupName(lyskom, lyskom.getTextStat(textNumber).getAuthor(), true) %><br/>
	<input size="50" type="text" name="subject" value="<%= dqescHtml(new String(commented.getSubject(), commented.getCharset())) %>"><br/>
	<textarea name="body" cols="71" rows="10"></textarea><br/>
	<input type="submit" value="skicka!">
	<input type="submit" name="dispatchToComposer" value="ändra mottagarlista">
	</form>
<%
    }
%>
<%

	    boolean listNews = request.getParameter("listnews") != null ||
	        (justLoggedIn && preferences.getBoolean("list-news-on-login"));

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
			div.innerHTML = "(uppdaterar om " + s + 
			    (s > 1 ? " sekunder" : " sekund") + ")";
		    } else if (div != null) {
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
		var ivref = window.setInterval(countdown, 1000);
	    </script>

<%		} 
%>
	<p>
	<ul>
<%
		Iterator confIter = new LinkedList(lyskom.getUnreadConfsListCached()).iterator();
		int sum = 0, confsum = 0;
		while (confIter.hasNext()) {
		    int conf = ((Integer) confIter.next()).intValue();
		    Membership membership = lyskom.queryReadTextsCached(conf);
		    UConference uconf = lyskom.getUConfStat(conf);
		    int unreads = 0;
		    if (uconf.getHighestLocalNo() > membership.getLastTextRead()) {
			unreads = uconf.getHighestLocalNo() -
				membership.getLastTextRead();
		    }
		    if (unreads == 0) continue;
		    sum += unreads;
		    confsum++;
		    out.print("<li> <a href=\"" + myURI(request) + "?conference=" +
				conf + "\">" + 
				lookupName(lyskom, conf, true) + "</a>: " +
				unreads + " " + (unreads > 1 ? "olästa" : "oläst"));
		    out.println(" [ <a href=\"" + myURI(request) + "?conference=" +
			conf + "&listSubjects\">lista ärenden</a> ]");
		}
		lyskom.changeWhatIAmDoing("Väntar på inlägg");
%>

	</ul>
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
	    if (request.getParameter("upload") != null) {
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
	    if (request.getParameter("uploadForm") != null) {
%>
	<form enctype="multipart/form-data" method="post" action="<%=myURI(request)%>?upload" class="boxed">
	    skriv en bild-URL här: <input type="text" size="50" name="urlsubmitter"> <br/>
	    eller ladda upp en bild: <input type="file" name="uploader"> <br/>
	    <input type="submit" value="skicka"><br/>
	</form>
<%
	    }
	    if (authenticated.booleanValue() && request.getParameter("setPasswordForm") != null) {
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
    <input type="hidden" name="listnews" value="<%=request.getParameter("listnews")%>">
<%  } %>
    </form>

    <form action="<%=myURI(request)%>" class="boxed" method="post">
    <a name="sendMessage"></a>
    Skicka ett meddelande till:<br/>
<%  if (listNews) { %>
    <input type="hidden" name="listnews" value="<%=request.getParameter("listnews")%>">
<%  } %>
    <input type="text" size="40" name="sendToName" value="<%=lastReceivedOrSent!=null?lastReceivedOrSent:""%>">
    <input type="submit" name="chat" value="starta chat" />
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
    if (request.getParameter("lyskomNamn") != null) lyskomNamn = request.getParameter("lyskomNamn");
%>
<table class="boxed">
<tr><td>namn:</td><td><input type="text" name="lyskomNamn" value="<%= lyskomNamn %>" size="30"></td></tr>
<tr><td>lösenord:</td><td><input type="password" name="lyskomLosen" size="8"></td></tr>
<tr><td>dold session:</td><td><input type="checkbox" name="lyskomDold"></td></tr>
<tr><td>registrera ny användare:</td><td><input type="checkbox" name="createPerson"></td></tr>
<tr><td>server:</td><td>
<select name="server">
<%
    String selectedServer = request.getParameter("server");
    if (selectedServer == null) selectedServer = Servers.defaultServer.hostname;
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
$Id: index.jsp,v 1.14 2004/04/26 00:52:40 pajp Exp $
</p>
</body>
</html>

