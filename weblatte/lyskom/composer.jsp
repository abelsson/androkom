<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*,
				 java.net.URLConnection, java.net.URL, java.io.*, java.text.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ include file='kom.jsp' %>
<html><head><title>textskrivare</title>
<link rel="stylesheet" href="lattekom.css" />
<body>
<%
    request.setCharacterEncoding("utf-8");
    StringBuffer metadata = new StringBuffer();
    Session lyskom = (Session) session.getAttribute("lyskom");
    if (lyskom == null || !lyskom.getConnected() || !lyskom.getLoggedIn()) {
	response.sendRedirect("/lyskom/");
	return;
    }

    if (request.getParameter("createText") != null) {
	request.setAttribute("set-uri", makeAbsoluteURL("/"));
	RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(appPath + "/");
	dispatcher.forward(request, response);
	return;	
    }


    String subject = request.getParameter("subject") != null ? request.getParameter("subject") : "";
    String body = request.getParameter("body") != null ? request.getParameter("body") : "";

    List recipients = new LinkedList();
    List ccRecipients = new LinkedList();

    if (request.getParameter("changePresentation") != null) {
	Text oldPresentation = null;
	int confNo = Integer.parseInt(request.getParameter("changePresentation"));
	Conference conf = lyskom.getConfStat(confNo);
	if (conf.getPresentation() > 0) {
	    oldPresentation = lyskom.getText(conf.getPresentation());

 	    int[] _rcpts = oldPresentation.getRecipients();
	    int[] _ccRcpts = oldPresentation.getCcRecipients();
	    for (int i=0; i < _rcpts.length; i++)
		recipients.add(lookupName(lyskom, _rcpts[i]));
	    for (int i=0; i < _ccRcpts.length; i++)
	    	ccRecipients.add(lookupName(lyskom, _ccRcpts[i]));

	    if ("".equals(subject)) subject = new String(oldPresentation.getSubject(), oldPresentation.getCharset());
	    if ("".equals(body)) body = new String(oldPresentation.getBody(), oldPresentation.getCharset());

	} else {
	    Map info = lyskom.getInfo();
	    if (conf.getType().getBitAt(ConfType.letterbox)) {
		recipients.add(lookupName(lyskom, ((KomToken) info.get("pers-pres-conf")).intValue()));
	    } else {
		recipients.add(lookupName(lyskom, ((KomToken) info.get("conf-pres-conf")).intValue()));
	    }
	    if ("".equals(subject)) subject = lyskom.toString(conf.getName());
	}

	metadata.append("Skriver presentation för " +
		lyskom.toString(conf.getName()) + "<br/>");

	
    }

    if (request.getParameter("privateReply") != null) {
	int privateReplyTo = Integer.parseInt(request.getParameter("privateReply"));
	Text txt = lyskom.getText(privateReplyTo);
	recipients.add(lookupName(lyskom, txt.getAuthor()));
	subject = new String(txt.getSubject());
	metadata.append("Personligt svar till text ").
		append(textLink(request, lyskom, txt.getNo())).
		append("<br/>");
    }


    if (request.getParameter("footnoteTo") != null) {
	int footnotedTextNo = Integer.parseInt(request.getParameter("footnoteTo"));
	Text footnotedText = lyskom.getText(footnotedTextNo);
	int[] _rcpts = footnotedText.getRecipients();
	int[] _ccRcpts = footnotedText.getCcRecipients();
	for (int i=0; i < _rcpts.length; i++)
	    recipients.add(lookupName(lyskom, _rcpts[i]));
	for (int i=0; i < _ccRcpts.length; i++)
	    ccRecipients.add(lookupName(lyskom, _ccRcpts[i]));
	subject = new String(footnotedText.getSubject());

	metadata.append("Fotnot till text ").
		append(textLink(request, lyskom, footnotedTextNo)).
		append("<br/>");
    }

    int commentedTextNo = 0;
    if (request.getParameter("postCommentTo") != null) {
	commentedTextNo = Integer.parseInt(request.getParameter("postCommentTo"));
	metadata.append("Kommentar till text ").
		append(textLink(request, lyskom, commentedTextNo)).
		append("<br/>");
    }
    if (request.getParameter("postCommentTo") != null &&
	request.getParameter("addNewRecipient") == null) {
	Text commentedText = lyskom.getText(commentedTextNo);
	int[] _recipients = commentedText.getRecipients();
	for (int i=0; i < _recipients.length; i++) {
	    Conference conf = lyskom.getConfStat(_recipients[i]);
	    if (conf.getType().original()) {
		int superconf = conf.getSuperConf();
		if (superconf > 0) {
		    recipients.add(lookupName(lyskom, superconf));
		} else {
		   throw new RuntimeException("Du får inte skriva kommentarer i " +
					      conf.getNameString());
		}
	    } else {
		recipients.add(lookupName(lyskom, _recipients[i]));
	    }
	}
    }

    StringBuffer errors = new StringBuffer();


    for (int rcptType = 1; rcptType <= 2; rcptType++) {
        String[] recptFields = request.getParameterValues(rcptType == 1 ? "recipient" : "ccRecipient");
	String[] recptNoFields = request.getParameterValues(rcptType == 1 ? "recipientNo" : "ccRecipientNo");
	List list = rcptType == 1 ? recipients : ccRecipients;
	if (recptNoFields != null) {
	    for (int i=0; i < recptNoFields.length; i++) {
		String conf = lookupName(lyskom, Integer.parseInt(recptNoFields[i]));
		if (conf == null) {
		    errors.append("Möte " + recptNoFields[i] + " finns ej.<br/>");
		    continue;
		}
		list.add(conf);
	    }
	}
	if (recptFields == null) continue;

        for (int i=0; i < recptFields.length; i++) {
   	    recptFields[i] = recptFields[i].trim();
	    if ("".equals(recptFields[i])) continue;
	    try {
            	ConfInfo conf = lookupName(lyskom, recptFields[i], true, true);
		if (conf == null) {
		    errors.append("Namnet \"" + htmlize(recptFields[i]) + "\" hittas inte.<br>");
  	    	    list.add(recptFields[i]);
		    continue;
		}
		if (list.contains(conf.getNameString())) continue;
	    	list.add(conf.getNameString());
	    } catch (AmbiguousNameException ex1) {
		errors.append("<p class=\"statusError\">Fel: namnet är flertydigt. Följande namn matchar:");
	        errors.append("<ul>");
	        ConfInfo[] names = ex1.getPossibleNames();
	        for (int j=0; j < names.length; j++) 
		    errors.append("<li>" + lookupName(lyskom, names[j].getNo(), true));
	    	errors.append("</ul>");
	    	list.add(recptFields[i]);
	    }
	}
    }


    recipients.add("");
    ccRecipients.add("");
%>
<% if (errors.length() > 0) { %>
<p class="statusError"><%=errors.toString()%></p>
<% } %>

<form enctype="application/x-www-form-urlencoded; charset=utf-8" class="boxed" method="post" action="<%=request.getRequestURI()%>">
<%
    if (request.getParameter("contentType") != null) {
%>
    <input type="hidden" name="contentType" value="<%=request.getParameter("contentType")%>">
<%
    }
    out.println(metadata.toString());
    for (int rcptType = 1; rcptType <= 2; rcptType++) {
	List list = rcptType == 1 ? recipients : ccRecipients;
    	for (Iterator i = list.iterator(); i.hasNext();) {
	    String recipient = (String) i.next();
	    if (rcptType == 1) out.print("Mottagare: ");
	    else out.print("Kopiemottagare: ");
%>
<input name="<%=rcptType==1?"recipient":"ccRecipient"%>" type="text" size="40" value="<%=recipient%>"><br/>
<%
        }
    }
%>
<input type="submit" value="lägg till/uppdatera mottagare" name="addNewRecipient"><br/>
<br/>
Ämne: <input type="text" size="50" name="subject" value="<%=subject%>"><br/>
<textarea name="body" cols="71" rows="10"><%=body%></textarea><br/>
<% if (request.getParameter("postCommentTo") != null) { %>
<input type="hidden" name="postCommentTo" value="<%=request.getParameter("postCommentTo")%>">
<input type="hidden" name="inCommentTo" value="<%=request.getParameter("postCommentTo")%>">
<% } %>
<% if (request.getParameter("changePresentation") != null) { %>
<input type="hidden" name="changePresentation" value="<%=request.getParameter("changePresentation")%>">
<% } %>
<% if (request.getParameter("privateReply") != null) { %>
<input type="hidden" name="inCommentTo" value="<%=request.getParameter("privateReply")%>">
<% } %>
<% if (request.getParameter("footnoteTo") != null) { %>
<input type="hidden" name="footnoteTo" value="<%=request.getParameter("footnoteTo")%>">
<% } %>
<input type="submit" value="skicka!" name="createText">
</form>

<p class="footer">
$Id: composer.jsp,v 1.3 2004/04/22 22:16:07 pajp Exp $
</p>
</body>
</html>
