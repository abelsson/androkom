<%@ page language='java' import='nu.dll.lyskom.*, java.util.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ include file='kom.jsp' %>
<%@ include file='prefs_inc.jsp' %>
<%!
    String getSessionId(SessionWrapper wrapper) {
	return Integer.toHexString(System.identityHashCode(wrapper));
    }
    String sessionInfo(SessionWrapper wrapper) throws IOException, RpcFailure {
	StringBuffer buf = new StringBuffer();
	Session lyskom = (Session) wrapper.getSession();
	String sessionId = getSessionId(wrapper);
	buf.append("<a href=\"" + basePath + "sessions.jsp?select=" + sessionId + "\">");
	buf.append(lookupNamePlain(lyskom, lyskom.getMyPerson().getNo()));
	buf.append(" @ ");
	buf.append(serverShort(lyskom));
	buf.append("</a>");
	List unreadConfs = lyskom.getUnreadConfsListCached();
	if (unreadConfs.size() > 0) {
	    buf.append(" (olästa)");
	}
	return buf.toString();
    }
%>
<%
    List suspendedSessions = (List) session.getAttribute("lyskom.suspended");
    if (suspendedSessions == null) {
	suspendedSessions = new SuspendedSessionList();
	session.setAttribute("lyskom.suspended", suspendedSessions);
    }
    if (request.getParameter("select") != null) {
	String newSessionId = request.getParameter("select");
	if (lyskomWrapper != null) {
	    synchronized (suspendedSessions) {
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
    synchronized (suspendedSessions) {
	out.println("<ul>");
	for (Iterator i = suspendedSessions.iterator(); i.hasNext();) {
	    SessionWrapper w = (SessionWrapper) i.next();
	    out.println("<li>Pausad LysKOM-session: " + sessionInfo(w) + "</li>");
	}
 	out.println("</ul>");
    }
  %>
  <p>
    &lt;&lt; <a href="<%=basePath%>?invalidate">Terminera alla sessioner</a><br/>
    &lt;&lt; <a href="<%=basePath%>">Till Weblattes startsida</a>
  </p>
  </body>
</html>
