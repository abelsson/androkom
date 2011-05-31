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
  <table style="border: solid; border-width: 1px; width: 80%;">
<%
    for (Iterator i = allConferences.iterator();i.hasNext();) {
	Integer conf = (Integer) i.next();
	List texts = (List) conferences.get(conf);
	Conference c = lyskom.getConfStat(conf.intValue());
	if (c.getType().letterbox()) continue;
	
	out.print("<tr " + (pyjamas ? "class=\"pyjamas\"" : "") + "><td><a name=\"c" + c.getNo() + "\">" + lookupName(lyskom, conf.intValue(), true) + "</a>");
	out.print(" (" +
		  "<a href=\"" + request.getRequestURI() + "?base=" + base + "&" + 
		  "expand=" + c.getNo() + "#c" + c.getNo() + "\">" +
		  + texts.size() + (texts.size() == 1 ? " text" : " texter") + "</a>)<br>");
	Text text = lyskom.getText(((Integer) texts.get(0)).intValue(), false, true);
	out.println("Senaste inlägg: " + textLink(request, lyskom, text.getNo()));
	out.println(", " + df.format(lyskom.getTextStat(text.getNo()).getCreationTime().getTime()));
	out.println("<br>Ärende: " + htmlize(text.getSubjectString()) + "<br>");
	out.println("</td></tr>");
	if (Integer.toString(c.getNo()).equals(request.getParameter("expand"))) {
	    Collections.reverse(texts);
	    out.println("<tr><td><table width=100% " + (pyjamas ? "class=\"pyjamas\"" : "") + ">");
	    for (Iterator j = texts.iterator();j.hasNext();) {
		Text _text = lyskom.getText(((Integer) j.next()).intValue(), false, false);
		//if (_text.getNo() == text.getNo()) continue;
		String body = _text.getBodyString();
		if (body.length() > 512) {
		    body = htmlize(body.substring(0, 509)) + 
			textLink(request, lyskom, _text.getNo(), false, 
				 "[texten f&ouml;rkortad]");
		} else {
		    body = htmlize(body);
		}
		out.println("<tr><td title=\"Text " + _text.getNo() + "\">" + textLink(request, lyskom, _text.getNo(), false,
						  shortdf.format(_text.getCreationTime())) + 
			    "</td><td>" + lookupName(lyskom, _text.getAuthor(), true) + 
			    "</td><td width=40% >" + 
			    _text.getSubjectString() + 
			    "</td></tr>");
		out.println("<tr><td colspan=3><pre style=\"background: #ffa;\">" + 
			    body + "</pre></td></tr>");
	    }
	    out.println("</table></td></tr>");
	}

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
