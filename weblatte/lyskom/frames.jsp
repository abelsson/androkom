<%@ page language='java' import='nu.dll.lyskom.*, java.net.URLEncoder' %>
<%@ include file='kom.jsp' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%
    if (lyskom == null) response.sendRedirect(basePath);
    int conference = 0;
    try {
        conference = Integer.parseInt(request.getParameter("conference"));
    } catch (Exception e) {}
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <script language="JavaScript1.2">
      function textView(newUrl) {
          window.textViewFrame.document.location.url = newUrl;
      }
    </script>
    <title>
<%
    if (conference > 0) {
%>
	Läser <%= htmlize(lookupName(lyskom, Integer.parseInt(request.getParameter("conference")), false)) %>
<%
    } else {
%>
	WebLatte LysKOM-klient
<%
    }
%>
</title>
  </head>
  <frameset rows="*" cols="20%,*" border="1">
<%
    out.print("<frame src=\"tree.jsp?");
    if (conference > 0) {
	out.print("conference=" + conference + "&");
    }
    if (request.getParameter("reviewTree") != null) {
	out.print("reviewTree=" + URLEncoder.encode(request.getParameter("reviewTree")));
    }
    out.println("\" name=\"treeView\">");
%>
    <frame src="about:blank" name="textViewFrame">
  </frameset>
</html>
