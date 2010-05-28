<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*,
				 java.net.*, java.io.*, java.text.*,java.util.regex.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' 
    isErrorPage='true' %>
<%@ include file='kom.jsp' %>
<% exception.printStackTrace(); %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Weblatte: ett fel har uppstått</title>
    <link rel="stylesheet" href="lattekom.css" />
  </head>

  <body>
    <h1>Weblatte: ett fel har uppstått</h1>
    <p>
        Tyvärr har ett fel uppstått som WebLatte inte kunde hantera på egen hand.
        Du har blivit utloggad, och måste <a target="_top" href="<%=
        basePath %>">logga in på nytt</a>
	om du vill forsätta läsa LysKOM. Jag beklagar olägenheten. Om du vill får du gärna
	rapportera felet till LatteKOM-utvecklarna. För att göra det, kopiera all text i 
	rutan nedanför, och gå sedan till
	<a href="http://sourceforge.net/tracker/?func=add&group_id=10071&atid=110071">
	bug-trackern på SourceForge</a>, där du väljer "weblatte" vid rubriken "Category",
	och därefter klistrar in informationen från denna sida i rutan för "Detailed 
	Descrption", och klickar slutligen på "SUBMIT". Skriv gärna även
	vad du gjorde vid tillfället för felmeddelandet och annat som du tror kan vara 
	relevant för att vi ska lyckas återskapa det.
    </p>
    <pre class="errorData">

Tid: <%= df.format(new Date()) %>
Request-URI: <%= request.getAttribute("javax.servlet.error.request_uri") + 
	(request.getQueryString() != null ? ("?"+request.getQueryString()) : "") %>

Request-parametrar: <%
    Enumeration en = request.getParameterNames();
    while (en.hasMoreElements()) {
	String name = (String) en.nextElement();
	out.print(name+"=");
	if (name.equals("lyskomLosen"))
	    out.println("********");
	else
	    out.println(request.getParameter(name));
    }
%>

Felklass: <%= exception.getClass().getName() %>
Felmeddelande: <%= htmlize(exception.getMessage()) %>

Stackspårning:
<%
    out.flush();
    exception.printStackTrace(response.getWriter());
    out.flush();
    Throwable cause = exception.getCause();
    while (cause != null) {
%>

	Ursprungsfel:
<%
	out.flush();
	cause.printStackTrace(response.getWriter());
	out.flush();
	cause = cause.getCause();
    }
%>

</pre>
<%
    out.flush();
    if (lyskom != null) {
        try {
	    lyskom.shutdown();
	} catch (Exception ex1) {}
    }
    session.invalidate();
%>
  </body>
</html>
