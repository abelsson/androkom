<%@ page language='java' import='nu.dll.lyskom.*, java.util.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ include file='kom.jsp' %>
<%@ include file='prefs_inc.jsp' %>
<%!
    String sessionInfo(SessionWrapper wrapper) throws IOException, RpcFailure {
	StringBuffer buf = new StringBuffer();
	Session lyskom = (Session) wrapper.getSession();
	String sessionId = getSessionId(wrapper);
	buf.append("<a href=\"" + basePath + "sessions.jsp?select=" + sessionId + "\">");
	buf.append(lookupNamePlain(lyskom, lyskom.getMyPerson().getNo()));
	buf.append(" @ ");
	buf.append(serverShort(lyskom));
	//buf.append(" (" + sessionId + ")</a>");
	buf.append("</a>");
        if (experimental) {
            buf.append(" (<a href=\"http://s-" + sessionId + "." + baseHost + basePath + "?listnews\">URL-session</a>)");
        }
	List unreadConfs = lyskom.getUnreadConfsListCached();
	boolean unreadLetters = unreadConfs.contains(new Integer(lyskom.getMyPerson().getNo()));
	if (unreadConfs.size() > 0) {
	    buf.append(" (olästa" + (unreadLetters ? " brev" : "") + ")");
	}
	return buf.toString();
    }
%>
<%
    response.setHeader("Refresh", "10; " + myURI(request));
    List activeSessions = (List) session.getAttribute("lyskom.active");
    if (activeSessions == null) activeSessions = Collections.EMPTY_LIST;
    List suspendedSessions = (List) session.getAttribute("lyskom.suspended");
    if (suspendedSessions == null) {
	suspendedSessions = new SuspendedSessionList();
	session.setAttribute("lyskom.suspended", suspendedSessions);
    }
    String selected = request.getParameter("select");
    if (request.getParameter("next") != null) {
	synchronized (suspendedSessions) {
	    selected = getSessionId((SessionWrapper) suspendedSessions.get(0));
	}
    }   
    if (request.getParameter("previous") != null) {
	synchronized (suspendedSessions) {
	    selected = getSessionId((SessionWrapper) suspendedSessions.get(suspendedSessions.size()-1));
	}
    }
    if (selected != null) {
	String newSessionId = selected;
	if (lyskomWrapper != null) {
	    synchronized (suspendedSessions) {
                if (request.getParameter("announce-pause") != null)
                    lyskom.changeWhatIAmDoing("Pausar och hoppar till ett annat LysKOM");

		suspendedSessions.add(lyskomWrapper);
		lyskomWrapper.setSuspended(true);
		session.removeAttribute("lyskom");
		session.removeAttribute("LysKOMauthenticated");
	    }
	}
	synchronized (suspendedSessions) {
	    Debug.println("wanted session id: " + newSessionId);
	    for (Iterator i = suspendedSessions.iterator(); i.hasNext();) {
		SessionWrapper w = (SessionWrapper) i.next();
		String id = getSessionId(w);
		if (id.equals(newSessionId)) {
		    w.setSuspended(false);
		    log("switching session to "  + w);
		    session.setAttribute("lyskom", w);
		    session.setAttribute("LysKOMauthenticated", new Boolean(w.getSession().getLoggedIn()));
		    session.removeAttribute("goto");
		    suspendedSessions.remove(w);
		    String redir = basePath + "?listnews";
		    Debug.println("Redirecting to " + redir);
		    response.sendRedirect(redir);
		    return;
		}
	    }
	}
    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <link rel="stylesheet" href="lattekom.css" />
    <title>Weblatte: Aktiva LysKOM-sessioner</title>
  </head>
  <body>
<%
    if (request.getParameter("loggedOut") != null) {
	out.println("<b>Följande LysKOM-sessioner är fortfarande inloggade:</b><br>");
    }
    if (lyskomWrapper != null) {
	out.println("<b>Aktiv LysKOM-session:</b> " + sessionInfo(lyskomWrapper) + "<br>");
    }
    out.println("<h3>Pausade LysKOM-sessioner:</h3>");
    synchronized (suspendedSessions) {
	out.println("<ol>");
	for (Iterator i = suspendedSessions.iterator(); i.hasNext();) {
	    SessionWrapper w = (SessionWrapper) i.next();
	    out.println("<li>" + sessionInfo(w) + "</li>");
	}
 	out.println("</ol>");
    }
    synchronized (activeSessions) {
	if (activeSessions.size() > 0) {
	    out.println("<h3>Aktiva LysKOM-sessioner:</h3>");
	}
	out.println("<ol>");
	for (Iterator i = activeSessions.iterator(); i.hasNext();) {
	    SessionWrapper w = (SessionWrapper) i.next();
	    out.println("<li>" + sessionInfo(w) + "</li>");
	}
 	out.println("</ol>");

    }
  %>
  <p>
    &lt;&lt; <a href="http://<%=baseHost + basePath%>?invalidate">Terminera alla sessioner</a><br/>
    &lt;&lt; <a href="<%=basePath%>">Till Weblattes startsida</a>
  </p>
  </body>
</html>
