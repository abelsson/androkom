<%@ page language='java' %>
<%@ include file='kom.jsp' %>
<%
    if (session.getAttribute("lyskom") == null) {
        response.sendRedirect(basePath);
        return;
    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>LysKOM-chat</title>
  </head>
  <frameset rows="*,100">
    <frame src="chat_reader.jsp" />
    <frame scrolling="no" src="chat_sender.jsp<%= request.getParameter("default") != null ? "?default=" + request.getParameter("default") : "" %>" />
  </framset>
</html>
