<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*,
				 java.net.URLConnection, java.net.URL, java.io.*, java.text.*' %>
<%!
    int rightMargin = Integer.getInteger("lattekom.linewrap", new Integer(70)).intValue();
    void wrapText(Text newText) throws UnsupportedEncodingException {
	java.util.List rows = newText.getBodyList();
	java.util.List newRows = new LinkedList();

	Iterator i = rows.iterator();
	while (i.hasNext()) {
	    String row = (String) i.next();
	    while (row.length() > rightMargin) {
		int cutAt = row.lastIndexOf(' ', rightMargin);
		String wrappedRow = row.substring(0, cutAt);
		row = row.substring(cutAt+1);
		newRows.add(wrappedRow);
	    }
	    newRows.add(row);
	}

	i = newRows.iterator();
	StringBuffer newBody = new StringBuffer();
	while (i.hasNext()) {
	    String row = (String) i.next();
	    newBody.append(row + "\n");
	}
	newText.setContents((new String(newText.getSubject()) + "\n" +
			 newBody.toString()).getBytes(Session.serverEncoding));
    }

    public String lookupName(Session lyskom, int number)
    throws RpcFailure, IOException {
	String name = "[" + number + "]";
	try {
	    name = new String(lyskom.getUConfStat(number).getName());
	} catch (RpcFailure ex1) {
	    if (ex1.getError() != Rpc.E_undefined_conference)
		throw ex1;
	}
	return name;
    }
    SimpleDateFormat df = new SimpleDateFormat("EEEEE d MMMMM yyyy', klockan 'HH:mm", new Locale("sv", "se"));

    public String textLink(HttpServletRequest request, Session lyskom, int textNo)
    throws RpcFailure, IOException {
	return textLink(request, lyskom, textNo, true);
    }

    public String textLink(HttpServletRequest request, Session lyskom, int textNo, boolean includeName)
    throws RpcFailure, IOException {
	StringBuffer sb = new StringBuffer()
		.append("<a href=\"")
		.append(request.getRequestURI())
		.append("?text=")
		.append(textNo);
	if (request.getParameter("conference") != null) {
		sb.append("&conference=")
		.append(request.getParameter("conference"));
	}
	sb.append("\">")
		.append(textNo)
		.append("</a>");
	if (includeName) {
	    try {
		String a = lookupName(lyskom, lyskom.getTextStat(textNo).getAuthor());
		sb.append(" av ").append(a);
	    } catch (RpcFailure ex1) {
		if (ex1.getError() != Rpc.E_no_such_text)
		    throw ex1;
	    } 
	}
	return sb.toString();
    }

%>
<%
    /*
     * LysKOM session object is now put in session attribute "lyskom"
     * and re-used.
     */
    Cookie bajs = new Cookie("kilroy", "was here");
    bajs.setMaxAge(604800);
    response.addCookie(bajs);
    String dir = getServletContext().getRealPath("/lyskom/bilder/");
    Session lyskom = (Session) session.getAttribute("lyskom");
    Boolean authenticated = (Boolean) session.getAttribute("LysKOMauthenticated");
    if (authenticated == null) authenticated = new Boolean(false);
    boolean wap = true;
    String error = null;
    if (request.getParameter("lyskomNamn") != null ||
        (lyskom == null && session.getAttribute("lyskomPersonNo") != null)) {

	if (lyskom == null) lyskom = new Session();
	if (!lyskom.getConnected()) {
	    lyskom.setClientHost(request.getRemoteAddr());
	    lyskom.setClientUser("www");
	    lyskom.connect("sno.pp.se", 4894);
	}
	ConfInfo[] names = null;
	int person = 0;
	String password = (String) session.getAttribute("lyskomPassword");
	if (password == null) password = request.getParameter("lyskomLosen");

	if (request.getParameter("lyskomNamn") != null) {
	    names = lyskom.lookupName(request.getParameter("lyskomNamn"), true, false);
	    if (names.length == 1) person = names[0].getNo();
	} else {
	    person = ((Integer) session.getAttribute("lyskomPersonNo")).intValue();
	}
	if (person > 0) {
	    if (!lyskom.login(person, password,
			      request.getParameter("lyskomDold") != null, false)) {
		error = "Felaktigt lösenord!";
	    } else {
		if (names != null) {
		    session.setAttribute("lyskomPersonNo", new Integer(names[0].getNo()));
		    session.setAttribute("lyskomPassword", request.getParameter("lyskomLosen"));
		    session.setAttribute("lyskomPerson", lyskom.getMyPerson());
		}
		session.setAttribute("lyskomName",
			new String(lyskom.getConfName(lyskom.getMyPerson().getNo())));
		session.setAttribute("lyskom", lyskom);
		authenticated = new Boolean(true);
		lyskom.setClientVersion("dll.nu/lyskom", "$Revision: 1.1 $");
		lyskom.changeWhatIAmDoing("http://dll.nu/lyskom/");
	    }
	} else if (names != null && names.length == 0) {
	    error = "Hittade inget sån't namn!";
	} else if (names != null && names.length > 1) {
	    error = "Flertydigt namn!";
	} else {
	    error = "Nåt é knasigt.";
	}
    }
    
%>
<html><head><title>snoppkom.dll.nu</title>
<% if (request.getParameter("listnews") != null) { %>
<meta http-equiv="Refresh" content="120; URL=<%=request.getRequestURI()%>?listnews=1" />
<% } %>
</head>
<link rel="stylesheet" href="/xss.css" />
<body>
<%
    if (error != null) {
%>
<h3 style="color: red;"><%= error %></h3>
<%
    }
%>
<%
    if (authenticated.booleanValue()) {
	session.setAttribute("LysKOMauthenticated", authenticated);
	if (request.getParameter("logout") != null) {
	    if (lyskom != null) {
	    	lyskom.disconnect(true);
	    }
	    session.invalidate();
	    %><h2>utloggad.</h2>
	    <p>
		[ <a href="/lyskom/">logga in</a> ]
	    </p>
	    <%
	} else {
%>
    	<h2>välkommen till LysKOM, <%= (String) session.getAttribute("lyskomName") %>!</h2>
	<!-- Ditt sessions-ID är "<%= Integer.toHexString(System.identityHashCode(lyskom)) %>". -->
<%
    int textNumber = 0;
    int conferenceNumber = 0;
    int newTextNo = 0;
    if (request.getParameter("conference") != null) {
	conferenceNumber = Integer.parseInt(request.getParameter("conference"));
    }
    if (request.getParameter("markAsRead") != null) {
	Text readText = lyskom.getText(Integer.parseInt(request.getParameter("markAsRead")));
	int[] rcpts = readText.getRecipients();
	int[] ccs = readText.getCcRecipients();
	int[] tmp = new int[rcpts.length+ccs.length];
	System.arraycopy(rcpts, 0, tmp, 0, rcpts.length);
	System.arraycopy(ccs, 0, tmp, rcpts.length, ccs.length);
	for (int i=0; i < tmp.length; i++) {
	    try {
		lyskom.markAsRead(tmp[i], new int[] {readText.getLocal(tmp[i])});
	    } catch (RpcFailure ex1) {
	        if (ex1.getError() != Rpc.E_not_member)
		    throw ex1;
	    }
	}
    }
    if (request.getParameter("postCommentTo") != null) {
	Text commentedText = lyskom.getText(Integer.parseInt(request.getParameter("postCommentTo")));
	Text newText = new Text(request.getParameter("subject"),
			request.getParameter("body").replaceAll("\r", ""));
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
	    <font color="green">Text nummer <%= textLink(request, lyskom, newTextNo, false) %> är skapad.</font><br><br>
<%
	}
	
    }

    try {
    if (conferenceNumber > 0) {
	lyskom.changeWhatIAmDoing("Läser");
	lyskom.changeConference(conferenceNumber);
	lyskom.updateUnreads();
	int nextUnreadText = lyskom.nextUnreadText(conferenceNumber, false);
	if (nextUnreadText > 0) {
%>
	    <br>
	    Nästa olästa text i möte <%= lookupName(lyskom, conferenceNumber) %>: <%= textLink(request, lyskom, nextUnreadText) %>
<%
	    textNumber = nextUnreadText;
	} else {
%>
	<br>Det finns inte fler olästa i <%= lookupName(lyskom, conferenceNumber) %>.<br/>
<%
	    if (textNumber == 0 && request.getParameter("text") == null &&
		request.getParameter("comment") == null && newTextNo == 0) {
		response.sendRedirect(request.getRequestURL().append("?listnews=1").toString());
	    }
	}
    }
    if (textNumber != 0 || request.getParameter("text") != null) {
	// xxx: catch NFE for more graceful error handling
	if (request.getParameter("text") != null)
	    textNumber = Integer.parseInt(request.getParameter("text"));

	Text text = lyskom.getText(textNumber);
%>
	<br><br>
	<tt>Text nummer <%= textLink(request, lyskom, text.getNo()) %><br/>
	Skapad <%= df.format(text.getCreationTime()) %><br/>
<%
	int[] commented = text.getCommented();
	int[] comments = text.getComments();
	for (int i=0; i < commented.length; i++) {
%>
	Kommentar till text <%= textLink(request, lyskom, commented[i]) %><br/>
<%
	}
	int[] recipients = text.getRecipients();
	int[] ccRecipients = text.getCcRecipients();
	for (int i=0; i < recipients.length; i++) {
%>
	Mottagare: <%= lookupName(lyskom, recipients[i]) %><br/>
<%
	}
	for (int i=0; i < ccRecipients.length; i++) {
%>
	Extra kopia: <%= lookupName(lyskom, ccRecipients[i]) %><br/>
<%
	}
%>
	Ärende: <%= new String(text.getSubject()) %><br/>
        <hr noshade width="80%" align="left"/>
        <pre><%= new String(text.getBody()).replaceAll("&", "&amp;").replaceAll("<", "&lt;") %></pre>
        <hr noshade width="80%" align="left"/>
<%
	for (int i=0; i < comments.length; i++) {
%>
	Kommentar i text <%= textLink(request, lyskom, comments[i]) %><br/>
<%
	}
%>	
	</tt>
	<br>
<%
    }
    } catch (RpcFailure ex1) {
	%><font color="red">Felkod <%= ex1.getError() %>, statuskod <%= ex1.getErrorStatus() %></font><br><%
    }
    if (request.getParameter("comment") != null && textNumber > 0) {
	lyskom.changeWhatIAmDoing("Skriver en kommentar");
	Text commented = lyskom.getText(textNumber);
%>
	<form method="post" action="<%=request.getRequestURI()%>?conference=<%=conferenceNumber%>">
	<input type="hidden" name="postCommentTo" value="<%=textNumber%>">
	Skriver en kommentar till text <%= textNumber %> av <%= lookupName(lyskom, lyskom.getTextStat(textNumber).getAuthor()) %><br/>
	<input size="50" type="text" name="subject" value="<%= new String(commented.getSubject()) %>"><br/>
	<textarea name="body" cols="78" rows="10"></textarea><br/>
	<input type="submit" value="skicka!">
	</form>
<%
    }

    if (conferenceNumber > 0 && textNumber > 0 && request.getParameter("comment") == null) {
%>
	<a href="<%= request.getRequestURI() %>?conference=<%=conferenceNumber%>&markAsRead=<%=textNumber%>">
	  Markera denna text som läst (och läs nästa).</a><br/>
	<a href="<%= request.getRequestURI() %>?conference=<%=conferenceNumber%>&markAsRead=<%=textNumber%>&text=<%=textNumber%>&comment=<%=textNumber%>">
	  Kommentera (och läsmarkera) denna text.</a><br/>
	<br/>
<%
    }
%>
   	<p>
	    [ <a href="?logout=1">logga ut</a> | <a href="?listnews=1">lista nyheter</a> ]
   	</p>
<%
	    if (request.getParameter("listnews") != null) {
		lyskom.changeWhatIAmDoing("Listar nyheter");
		lyskom.changeConference(0);
%>
	<p>
	<ul>
<%
		lyskom.updateUnreads();
		int[] conferences = lyskom.getUnreadConfs(lyskom.getMyPerson().getNo());
		int sum = 0, confsum = 0;
		for (int i=0; i < conferences.length; i++) {
		    int unreads = lyskom.getUnreadCount(conferences[i]);
		    if (unreads == 0) continue;
		    sum += unreads;
		    confsum++;
		    out.println("<li> " + unreads + " " + (unreads > 1 ? "olästa" : "oläst") +
				" i <a href=\"" + request.getRequestURL().append("?conference=").
				append(conferences[i]) + "\">" + 
				new String(lyskom.getConfName(conferences[i])) + "</a>");
		}
%>
	</ul>
		<%= confsum == 0 ? "<b>inga olästa i något möte</b>" : sum + " oläst(a) i " + confsum + " möte(n)" %>
<%		if (sum > 0) { %>
		    <script language="JavaScript1.2">
			document.title = "snoppkom - <%=sum%> olästa";
		    </script>
<%		} else { %>
		    <script language="JavaScript1.2">
			document.title = "snoppkom";
		    </script>
<%		} %>

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
	<form enctype="multipart/form-data" method="post" action="<%=request.getRequestURI()%>?upload=1">
	    skriv en bild-URL här: <input type="text" size="50" name="urlsubmitter"> <br>
	    eller ladda upp en bild: <input type="file" name="uploader"> <br>
	    <input type="submit" value="skicka"><br>
	</form>
<%
	    }
%>

    <form method="post" action="<%=request.getRequestURI()%>">
    Läs ett inlägg: <input type="text" size="10" name="text">
    <input type="submit" value="ok">
    </form>
<%
	}
    }
%>
<%
    if (!authenticated.booleanValue()) {
%>
Du måste logga in för att kunna läsa texter eller ladda upp din bild.

<form name="lyskomlogin" method="post" action="<%=request.getRequestURI()%>">
<%  if (request.getParameter("text") != null) { %>
<input type="hidden" name="text" value="<%=request.getParameter("text")%>">
<%  }
    if (request.getParameter("listnews") != null) { %>
<input type="hidden" name="listnews" value="<%=request.getParameter("listnews")%>">
<%  }  %>
<table>
<tr><td>namn:</td><td><input type="text" name="lyskomNamn" size="30"></td></tr>
<tr><td>lösenord:</td><td><input type="password" name="lyskomLosen" size="8"></td></tr>
<tr><td>dold session:</td><td><input type="checkbox" name="lyskomDold"></td></tr>
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
<p>
[ <a href="?uploadForm=1">ladda upp bild</a> | <a href="bilder/">visa bilder</a> | <a href="vilka/">vilka är inloggade?</a> ]
</p>
<p class="footer">
$Id: wap.jsp,v 1.1 2004/04/15 22:13:20 pajp Exp $<br>
(<a href="index.txt">visa källkod</a>)
</p>
</body>
</html>
