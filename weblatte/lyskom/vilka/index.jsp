<%@ page language='java' import='nu.dll.lyskom.*, java.io.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ include file='../kom.jsp' %>
<%
    boolean disconnectLast = false;
    if (lyskom == null) {
	lyskom = new Session();
    	lyskom.connect(Servers.defaultServer.hostname, 4894);
	disconnectLast = true;
    }
    //lyskom.changeWhatIAmDoing("Kollar vilkalistan");

    if (!disconnectLast) {
    	if (lyskom == null || !lyskom.getLoggedIn()) response.sendRedirect("http://dll.nu/lyskom/");
    }
    String sort = request.getParameter("sort");
    if (sort == null) sort = "";

    int activeLast = 3600;
    if (request.getParameter("activeLast") != null &&
        !"".equals(request.getParameter("activeLast"))) activeLast = Integer.parseInt(request.getParameter("activeLast"));
%>
<html><head><title>vilka är inloggade i <%= serverShort(lyskom) %>?</title></head>
<link rel="stylesheet" href="<%= basePath %>lattekom.css" />
<body>
<h2>vilka är inloggade i <%= serverShort(lyskom) %>?</h2>
<%
    if (request.getHeader("User-Agent").indexOf("MSIE") >= 0) {
%>
<script language="JavaScript1.2">
<%@ include file='../stuff.jsp' %>
</script>
<%
    } else {
%>
	<script language="JavaScript1.2" src="../stuff.jsp"></script>
<%
    }
%>
<%@ include file='../dhtmlMenu.jsp' %>
<%
    out.flush();
    DynamicSessionInfo[] who = lyskom.whoIsOnDynamic(request.getParameter("noVisible") == null,
						     request.getParameter("wantInvisible") != null,
						     activeLast);
    if (sort.equals("")) {
	Arrays.sort(who, new Comparator() {
	    public int compare(Object o1, Object o2) {
		DynamicSessionInfo s1 = (DynamicSessionInfo) o1;
		DynamicSessionInfo s2 = (DynamicSessionInfo) o2;
		return s1.getIdleTime() - s2.getIdleTime();
	    }
	});
    }

%>
<div class="StatusSuccess">Listar <%= who.length %> sessioner:</div>
<table>
<tr bgcolor="#aaaaaa" width="80%"><td width="10%"><b>#</b></td><td width="30%"><b>namn</b></td><td width="15%"><b>gör</b></td><td width="35%"><b>i möte</b></td></tr>
<%
    boolean pyjamas = true;
    for (int i=0; i < who.length; i++) {
	pyjamas = !pyjamas;
	int conf = who[i].getWorkingConference();
	String confName = null;
	int idle = who[i].getIdleTime()/60;
	int idleHours = idle/60;
	int idleMinutes = idle - idleHours*60;
	SessionInfo _session = null;
	try {
	    _session = lyskom.getLoggedIn() ? lyskom.getStaticSessionInfo(who[i].getSession()) : null;
	} catch (RpcFailure ex1) {
	    if (ex1.getError() != Rpc.E_undefined_session) throw ex1;
	}
	Mugshot mug = null;
	if (lyskom.getServer().equals("sno.pp.se")) {
	    File mugFile = new File(dir, who[i].getPerson() + ".txt");
	    mug = mugFile.exists() ? new Mugshot(mugFile) : null;	
        }
%>
	<tr <%= pyjamas ? "bgcolor=\"#aaeeee\"" : "" %>><td align="right"
<%
	    if (_session != null) {
	        String identUser = lyskom.toString(_session.getIdentUser());
	        String username = lyskom.toString(_session.getUsername());
	        String hostname = lyskom.toString(_session.getHostname());
	        out.print("title=\"Uppkopplad " + df.format(_session.getConnectionTime().getTime()) + ", ");
	        out.print("från " + hostname);
	        out.print("\"");
	    }
%>
	    ><%= who[i].getSession() %></td>
	    <td><%= who[i].getPerson() == 0 ? "<i>ej inloggad</i>" :
		(mug != null ? "<a href=\"../bilder/" + mug.image + "\">" : "") +
		lookupName(lyskom, who[i].getPerson(), true) +
		(mug != null ? "</a>" : "")
	    %></td>
            <td title="<%= (idle > 0 ? "inaktiv " : "aktiv") + (idle > 0 ? (idleHours > 0 ? idleHours + (idleHours > 1 ? " timmar" : " timme") + ", " : "") + idleMinutes + (idleMinutes > 1 ? " minuter" : " minut") : "") %>">
<%= lyskom.toString(who[i].getWhatAmIDoing()) %></td>
	    <td><%= conf > 0 ? lookupName(lyskom, conf, true) : "&nbsp;" %></td></tr>
<%
	        if (request.getParameter("showClientInfo") != null) {
		    try {
	            	String clientName = lyskom.toString(lyskom.getClientName(who[i].getSession()));
	            	String clientVersion = lyskom.toString(lyskom.getClientVersion(who[i].getSession()));
	            	if (!clientName.equals("")) {
	                    out.print("<tr " + (pyjamas ? "bgcolor=\"#aaeeee\"" : "") + "><td>&nbsp;</td><td colspan=\"3\">Kör ");
	                    out.print(htmlize(clientName));
	                    if (!clientVersion.equals("")) {
	                    	out.print(" version " + htmlize(clientVersion));
	                    }
	             	    out.println("</td></tr>");
	            	}
		    } catch (RpcFailure ex1) {
			if (ex1.getError() != Rpc.E_undefined_session) 
			    throw ex1;
		    }
	        }
	out.flush();
    }

    if (disconnectLast || !lyskom.getLoggedIn()) lyskom.disconnect(true);
%>
</table>
<div>
<form method="get" action="<%= myURI(request) %>" class="boxed">
Sorteringsordning:
<select name="sort">
<option value="" <%= sort.equals("") ? "selected" : "" %>>senast aktiv först
<option value="none" <%= sort.equals("none") ? "selected" : "" %>>ingen
</select><br/>
Visa dolda sessioner <input type="checkbox" <%= request.getParameter("wantInvisible") != null ? "checked" : "" %> name="wantInvisible" /><br/>
<span title="Visar varje sessions klientprogramvarunamn och version, om tillgänglig">Visa klientinformation <input type="checkbox" <%= request.getParameter("showClientInfo") != null ? "checked" : "" %> name="showClientInfo" /></span><br/>
Visa sessioner som varit aktiva inom:
<select name="activeLast">
<option <%= activeLast == 3600 ? "selected" : "" %> value="" />1 timme
<option <%= activeLast == 14400 ? "selected" : "" %> value="14400" />4 timmar
<option <%= activeLast == 0 ? "selected" : "" %> value="0" />Visa alla
</select><br/>
<input type="submit" value="ok"/>
</form>
</div>
<div class="nav">[ <a href="../">till huvudsidan</a> ]</div>
<div class="footer">
$Id: index.jsp,v 1.16 2004/11/15 03:56:35 pajp Exp $
</div>
</body>
</html>





