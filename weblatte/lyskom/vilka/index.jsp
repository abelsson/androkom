<%@ page language='java' import='nu.dll.lyskom.*, java.io.*' %>
<%@ include file='../kom.jsp' %>
<%
    boolean disconnectLast = false;
    Session lyskom = (Session) session.getAttribute("lyskom");
    if (lyskom == null) {
	lyskom = new Session();
    	lyskom.connect(Servers.defaultServer.hostname, 4894);
	disconnectLast = true;
    }
    lyskom.changeWhatIAmDoing("Kollar vilkalistan");

    if (!disconnectLast) {
    	if (lyskom == null || !lyskom.getLoggedIn()) response.sendRedirect("http://dll.nu/lyskom/");
    }

    int activeLast = 600*6;
    if (request.getParameter("activeLast") != null) activeLast = Integer.parseInt(request.getParameter("activeLast"));
%>
<html><head><title>vilka är inloggade i <%= serverShort(lyskom) %>?</title></head>
<link rel="stylesheet" href="<%= basePath %>/lattekom.css" />
<body>
<h2>vilka är inloggade i <%= serverShort(lyskom) %>?</h2>
<%
    out.flush();
    DynamicSessionInfo[] who = lyskom.whoIsOnDynamic(request.getParameter("noVisible") == null,
						     request.getParameter("wantInvisible") != null,
						     activeLast);
%>
<table>
<tr bgcolor="#aaaaaa" width="80%"><td width="10%"><b>#</b></td><td width="30%"><b>namn</b></td><td width="15%"><b>gör</b></td><td width="35%"><b>i möte</b></td></tr>
<%
    boolean pyjamas = true;
    for (int i=0; i < who.length; i++) {
	pyjamas = !pyjamas;
	int conf = who[i].getWorkingConference();
	String confName = null;
	try {
	    if (conf > 0) confName = new String(lyskom.getConfName(conf));
        } catch (RpcFailure e1) {}
	Mugshot mug = null;
	if (lyskom.getServer().equals("sno.pp.se")) {
	    File mugFile = new File(dir, who[i].getPerson() + ".txt");
	    mug = mugFile.exists() ? new Mugshot(mugFile) : null;	
        }
%>
	<tr <%= pyjamas ? "bgcolor=\"#aaeeee\"" : "" %>><td align="right"><%= who[i].getSession() %></td>
	    <td><%= who[i].getPerson() == 0 ? "<i>ej inloggad</i>" :
		(mug != null ? "<a href=\"../bilder/" + mug.image + "\">" : "") +
		new String(lyskom.getConfName(who[i].getPerson())) +
		(mug != null ? "</a>" : "")
	    %></td>
            <td><%= who[i].getWhatAmIDoingString() %></td><td><%= confName != null ? confName : "&nbsp;" %></td></tr>
<%
	out.flush();
    }
    if (disconnectLast) lyskom.disconnect(true);
%>
</table>
<p>[ <a href="../">logga in här</a> ]</p>
<p class="footer">
$Id: index.jsp,v 1.1 2004/04/15 22:13:20 pajp Exp $<br>
(<a href="index.txt">visa källkod</a>)
</p>
</body>
</html>





