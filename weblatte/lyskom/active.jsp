<%@ page language='java' import='nu.dll.lyskom.*, java.util.*, nu.dll.app.weblatte.*, java.net.*, java.io.*, java.text.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ page errorPage='fubar.jsp' %>
<%@ include file='kom.jsp' %>
<%
    if (lyskom == null) {
        response.sendRedirect(basePath);
        return;
    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Aktivitet i LysKOM</title>
    <link rel="stylesheet" href="lattekom.css">
    <script language="JavaScript1.2" src="stuff.jsp?pleasecache"></script>
  </head>
  <style>
    .pyjamas {
	background: #ddffff;
    }
  </style>
  <body>
<%
    if (request.getHeader("User-Agent").indexOf("MSIE") >= 0) {
	out.println("<script language=\"JavaScript1.2\">");
	%><%@ include file='stuff.jsp' %><%
	out.println("</script>");
    } else {
	out.println("<script language=\"JavaScript1.2\" src=\"stuff.jsp?pleasecache\"></script>");
    }
%>
<%@ include file='dhtmlMenu.jsp' %>
    <div id="waitmsg">Hämtar data från servern, det kan ta någon minut...</p>
<%
    out.flush();
    int base = 100;
    if (request.getParameter("base") != null) {
	base = Integer.parseInt(request.getParameter("base"));
    }
    int lastText = 1073741823;
    List allConferences = new LinkedList();
    List allTexts = new LinkedList();
    final Map conferences = new HashMap();
    for (int i=0; i < base; i++) {
        lastText = lyskom.findPreviousTextNo(lastText);
	Integer textNo = new Integer(lastText);
	TextStat ts = lyskom.getTextStat(lastText);
	List recipients = ts.getAllRecipients();
	for (Iterator it = recipients.iterator();it.hasNext();) {
	    Integer conf = (Integer) it.next();
	    if (!conferences.containsKey(conf)) {
		conferences.put(conf, new LinkedList());
	    }
	    ((List) conferences.get(conf)).add(textNo);
	    if (!allConferences.contains(conf)) allConferences.add(conf);
	}
	allTexts.add(textNo);
    }
    final Session _lyskom = lyskom;
    Collections.sort(allConferences, new Comparator() {
	    public int compare(Object o1, Object o2) {
		try {
		    List l1 = (List) conferences.get(o1);
		    List l2 = (List) conferences.get(o2);
		    TextStat ts1 = _lyskom.getTextStat(((Integer)l1.get(0)).intValue());
		    TextStat ts2 = _lyskom.getTextStat(((Integer)l2.get(0)).intValue());
		    return (int) (ts2.getCreationTime().getTime().getTime() -
				  ts1.getCreationTime().getTime().getTime());
		} catch (IOException ex1) {
		    ex1.printStackTrace();
		    throw new RuntimeException(ex1.toString());
		}
	    }
	});
%>
  <script language="JavaScript">document.getElementById('waitmsg').visibility = 'hidden';</script>
  <p>
  Sammanställer de <%= base %> senaste inläggen, varav det första är text
  <%= textLink(request, lyskom, lastText) %>, skrivet <%= df.format(lyskom.getTextStat(lastText).getCreationTime().getTime()) %>.<br>
  </p>
<%
    boolean pyjamas = true;
%>
  <table style="border: solid; border-width: 1px;">
<%
    for (Iterator i = allConferences.iterator();i.hasNext();) {
	Integer conf = (Integer) i.next();
	List texts = (List) conferences.get(conf);
	UConference uc = lyskom.getUConfStat(conf.intValue());
	out.print("<tr " + (pyjamas ? "class=\"pyjamas\"" : "") + "><td><b>" + lookupName(lyskom, conf.intValue(), true));
	out.print("</b> (" + texts.size() + (texts.size() == 1 ? " text" : " texter") + ")<br>");
	Text text = lyskom.getText(((Integer) texts.get(0)).intValue(), false, true);
	out.println("Senaste inlägg: " + textLink(request, lyskom, text.getNo()) +
		    "<br>Ärende: " + htmlize(text.getSubjectString()) + "<br>");
	out.println(df.format(lyskom.getTextStat(text.getNo()).getCreationTime().getTime()) + 
		    "</td></tr>");
	pyjamas = !pyjamas;
    }
  %>
   </table>
   <br><br>
    <form method="get" class="boxed">
       Sammanställ de senaste <input type="text" size="8" name="base" value="<%= base %>"> inläggen.
       <input type="submit" value="OK">
    </form>
  </body>
</html>
