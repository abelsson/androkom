<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*,
				 java.net.*, java.io.*, java.text.*,java.util.regex.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' 
    isErrorPage='true' %>
<%@ include file='kom.jsp' %>
<% exception.printStackTrace() %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Weblatte: ett fel har uppst�tt</title>
    <link rel="stylesheet" href="lattekom.css" />
  </head>

  <body>
    <h1>Weblatte: ett fel har uppst�tt</h1>
    <p>
        Tyv�rr har ett fel uppst�tt som WebLatte inte kunde hantera p� egen hand.
        Du har blivit utloggad, och m�ste <a target="_top" href="<%=
        basePath %>">logga in p� nytt</a>
	om du vill fors�tta l�sa LysKOM. Jag beklagar ol�genheten. Om du vill f�r du g�rna
	rapportera felet till LatteKOM-utvecklarna. F�r att g�ra det, kopiera all text i 
	rutan nedanf�r, och g� sedan till
	<a href="http://sourceforge.net/tracker/?func=add&group_id=10071&atid=110071">
	bug-trackern p� SourceForge</a>, d�r du v�ljer "weblatte" vid rubriken "Category",
	och d�refter klistrar in informationen fr�n denna sida i rutan f�r "Detailed 
	Descrption", och klickar slutligen p� "SUBMIT". Skriv g�rna �ven
	vad du gjorde vid tillf�llet f�r felmeddelandet och annat som du tror kan vara 
	relevant f�r att vi ska lyckas �terskapa det.
    </p>
    <pre class="errorData">

Tid: <%= df.format(new Date()) %>
Request-URI: <%= request.getAttribute("javax.servlet.error.request_uri") + 
	(request.getQueryString() != null ? ("?"+request.getQueryString()) : "") %>

Request-parametrar: <%
    Enumeration enum = request.getParameterNames();
    while (enum.hasMoreElements()) {
	String name = (String) enum.nextElement();
	out.print(name+"=");
	if (name.equals("lyskomLosen"))
	    out.println("********");
	else
	    out.println(request.getParameter(name));
    }
%>

Felklass: <%= exception.getClass().getName() %>
Felmeddelande: <%= exception.getMessage() %>

Stacksp�rning:
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
