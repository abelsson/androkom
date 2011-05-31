<%@ page language='java' import='nu.dll.lyskom.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ include file='kom.jsp' %>
<%@ include file='prefs_inc.jsp' %>
<%!
    static class TextTreeNode {
	public TextStat textStat;
	public List comments;
	public TextTreeNode(TextStat ts) {
	    this.textStat = ts;

	    comments = new LinkedList();
	}
    }

    void printNode(StringBuffer buf, Session lyskom, Map nodes, List texts, int textNumber, List textsToView, int depth) throws Exception {
	texts.remove(new Integer(textNumber));
	TextTreeNode node = (TextTreeNode) nodes.get(new Integer(textNumber));
	if (!textsToView.contains(new Integer(textNumber)))
	    textsToView.add(new Integer(textNumber));
	if (depth == 0) {
	    buf.append("<br/><i>");
            Text text = lyskom.getText(textNumber, false, true);
	    String charset = text.getCharset();
	    if ("us-ascii".equals(charset)) charset = "iso-8859-1";

	    buf.append(htmlize(new String(text.getSubject(), charset)));
	    buf.append("</i><br/>\n");
	}
	String authorName = lookupName(lyskom, node.textStat.getAuthor(), false);
	String authorNameStripped = komStrip(authorName);
	buf.append("<img src=\"bullet_unsel.gif\" id=\"bullet_" + textNumber + "\" />");
	buf.append("<tt>");
	for (int i=0; i < depth; i++) 
	    buf.append("&nbsp;&nbsp;");
	buf.append("</tt>");
	buf.append("<a onClick=\"selectText(" + textNumber + ");\" title=\"" + textNumber + " av " + dqescHtml(authorName) + "\" href=\"¤LINK¤#text" + textNumber + "\" target=\"textViewFrame\">");
	buf.append(htmlize(authorNameStripped));
	buf.append("</a>");
	buf.append("<br/>\n");
	//buf.append("<span title=\"" + node.textStat.getNo() + "\">*</span>\n");
	if (node.comments.size() > 0) {
            for (Iterator i = node.comments.iterator(); i.hasNext();) {
		TextTreeNode commentNode = (TextTreeNode) i.next();
		printNode(buf, lyskom, nodes, texts, commentNode.textStat.getNo(), textsToView, depth+1);
	    }
	}
    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Trädvy</title>
    <link rel="stylesheet" href="lattekom.css" />
  </head>
  <script language="JavaScript1.2">
    var selected = null;
    function selectText(textNo) {
	if (selected != null) {
	    var simg = document.getElementById("bullet_" + selected);
	    if (typeof(simg) != "undefined") {
		simg.src = "bullet_unsel.gif";
	    }
	}
	var nimg = document.getElementById("bullet_" + textNo);
	if (typeof(nimg) != "undefined") {
	    nimg.src = "bullet_sel.gif";
	    selected = textNo;
	}
    }
  </script>
  <body class="treeView">
<%
    if (lyskom == null || !lyskom.getConnected() || !lyskom.getLoggedIn()) {
	response.sendRedirect("/lyskom/");
	return;
    }

    boolean manyMemberships = lyskom.getAttribute("many-memberships") != null &&
		((Boolean) lyskom.getAttribute("many-memberships")).booleanValue();

    int me = lyskom.getMyPerson().getNo();
    int conferenceNumber = 0;
    int reviewTree = 0;
    try {
	conferenceNumber = Integer.parseInt(request.getParameter("conference"));
    } catch (NumberFormatException ex1) {}

    if (request.getParameter("reviewTree") != null) {
	reviewTree = Integer.parseInt(request.getParameter("reviewTree"));
    }

    if (conferenceNumber > 0) {
	out.println("<p>Läser inläggsträd (det kan ta ett tag)...</p>");
	out.flush();
	Membership membership = lyskom.queryReadTexts(me, conferenceNumber, true);
	UConference uconf = lyskom.getUConfStat(conferenceNumber, true);
	TextMapping mapping = uconf.getHighestLocalNo() > membership.getLastTextRead() ?
		lyskom.localToGlobal(conferenceNumber, membership.getLastTextRead()+1, 
				preferences.getInt("tree-texts-to-fetch")) : null;
	Map nodes = new HashMap();
	List texts = new LinkedList();
	Set seen = new HashSet();
	List textsToView = new LinkedList();
	while (mapping != null && mapping.hasMoreElements()) {
	    int textNo = ((Integer) mapping.nextElement()).intValue();
	    TextStat ts;
	    try {
		ts = lyskom.getTextStat(textNo);
	    } catch (RpcFailure ex1) {
		if (ex1.getError() == Rpc.E_no_such_text) {
		    continue;
		} else {
		    throw ex1;
		}
	    }

	    int[] footnoteTo = ts.getStatInts(TextStat.miscFootnTo);
	    for (int i=0; i < footnoteTo.length; i++) {
		Integer fto = new Integer(footnoteTo[i]);
		if (seen.contains(fto)) {
		    continue;
		}
	    }

	    TextTreeNode node = new TextTreeNode(ts);
	    nodes.put(new Integer(textNo), node);

	    if (!texts.contains(new Integer(textNo)) && !seen.contains(new Integer(textNo)))
		texts.add(new Integer(textNo));

	    seen.add(new Integer(textNo));
	    int[] comments = node.textStat.getStatInts(TextStat.miscCommIn);


	    for (int j=0; j < comments.length; j++) {
		int comment = comments[j];
		TextTreeNode commentNode = (TextTreeNode) nodes.get(new Integer(comment));
		if (commentNode == null) {
		    try {
		        commentNode = new TextTreeNode(lyskom.getTextStat(comment));
	    	    } catch (RpcFailure ex1) {
			if (ex1.getError() == Rpc.E_no_such_text) {
		    	    continue;
			} else {
		    	    throw ex1;
			}
	    	    }
		    nodes.put(new Integer(comment), commentNode);
		}
		if (commentNode != null) {
		    node.comments.add(commentNode);
		    seen.add(new Integer(comment));
		}
	    }
	}

	StringBuffer treeHtml = new StringBuffer(1024);
	while (texts.size() > 0) {
	    printNode(treeHtml, lyskom, nodes, texts, ((Integer) texts.remove(0)).intValue(), textsToView, 0);
	}

        String treeId = Integer.toHexString(rnd.nextInt());
	session.setAttribute("weblatte.tree." + treeId, textsToView);

	int firstText = textsToView.size() > 0 ? ((Integer) textsToView.get(0)).intValue() : 0;
	String link = basePath + "?popupComment&tv&hw&hs&treeId=" + treeId;
	String html = treeHtml.toString().replaceAll("¤LINK¤", link);
	out.println(html);
	out.println("<script language=\"JavaScript1.2\">parent.textViewFrame.document.location = \"" +
		link + "#text" + firstText + "\";selectText(" + firstText + ");</script>");
    } else if (reviewTree == 0) {
		Iterator confIter = new LinkedList(lyskom.getUnreadConfsList(me)).iterator();
		int sum = 0, confsum = 0;
		boolean abort = false;
		int skipTo = 0;
		int lastconf = 0;
	        int skipped = 0;
		if (request.getParameter("skipTo") != null)
		    skipTo = Integer.parseInt(request.getParameter("skipTo"));

		while (confIter.hasNext() && !abort) {
		    int conf = ((Integer) confIter.next()).intValue();

			if (skipTo > 0 && skipTo != conf) {
			    skipped++;
			    continue;
			} else if (skipTo == conf) {
			    skipped++;
			    skipTo = 0;
			    continue;
			}
		    lastconf = conf;
		    Membership membership = lyskom.queryReadTextsCached(conf);
		    if (membership == null)
			membership = lyskom.queryReadTexts(me, conf);

		    UConference uconf = lyskom.getUConfStat(conf);
		    int unreads = 0;
		    if (uconf.getHighestLocalNo() > membership.getLastTextRead()) {
			unreads = uconf.getHighestLocalNo() -
				membership.getLastTextRead();
		    }
		    if (unreads == 0) continue;
		    sum += unreads;
		    confsum++;
		    out.print("<a target=\"_top\" href=\"" + basePath + "frames.jsp?conference=" +
				conf + "\">" + 
				lookupName(lyskom, conf, true) + "</a>: " +
				unreads + "<br/>");
		    out.flush();
		    if (manyMemberships && confsum > 5) abort = true;
		}
	    if (manyMemberships && confIter.hasNext()) {
		out.println("<p><a href=\"tree.jsp?skipTo=" + lastconf + "\">Nästa 5 möten >></a></p>");
	    }
	out.println("<script language=\"JavaScript1.2\">parent.textViewFrame.document.location = \"" +
		basePath + "\";</script>");
    } else if (reviewTree > 0) {
	out.println("<p>Läser inläggsträd (det kan ta ett tag)...</p>");
	out.flush();
	Map nodes = new HashMap();
	List texts = new LinkedList();
	Set seen = new HashSet();
	List textsToView = new LinkedList();
	LinkedList textsToExamine = new LinkedList();
	textsToExamine.add(new Integer(reviewTree));
	while (textsToExamine.size() > 0) {
	    TextStat stat = null;
	    try {
		stat = lyskom.getTextStat(((Integer) textsToExamine.removeLast()).intValue());
	    } catch (RpcFailure ex1) {
		if (ex1.getError() == Rpc.E_no_such_text) continue;
		else throw ex1;
	    }
	    Integer textNoObj = new Integer(stat.getNo());
	    TextTreeNode node = new TextTreeNode(stat);
	    nodes.put(new Integer(stat.getNo()), node);
	    if (!texts.contains(textNoObj) && !seen.contains(textNoObj)) {
		texts.add(textNoObj);
	    }
	    int[] comments = stat.getComments();
	    for (int i=0; i < comments.length; i++) {
		int comment = comments[i];
		Integer commentNoObj = new Integer(comment);
		textsToExamine.add(commentNoObj);
		TextTreeNode commentNode = (TextTreeNode) nodes.get(commentNoObj);
		if (commentNode == null) {
		    try {
			commentNode = new TextTreeNode(lyskom.getTextStat(comment));
		    } catch (RpcFailure ex1) {
			if (ex1.getError() == Rpc.E_no_such_text) {
			    continue;
			} else {
			    throw ex1;
			}
		    }
		    nodes.put(commentNoObj, commentNode);
		}
		if (commentNode != null) {
		    node.comments.add(commentNode);
		    seen.add(commentNoObj);
		}
	    }
	}
	StringBuffer treeHtml = new StringBuffer(1024);
	while (texts.size() > 0) {
	    printNode(treeHtml, lyskom, nodes, texts, ((Integer) texts.remove(0)).intValue(), textsToView, 0);
	}
	int firstText = textsToView.size() > 0 ? ((Integer) textsToView.get(0)).intValue() : 0;
        String treeId = Integer.toHexString(rnd.nextInt());
	session.setAttribute("weblatte.tree." + treeId, textsToView);
	String link = basePath + "?popupComment&tv&hw&hs&treeId=" + treeId;
	String html = treeHtml.toString().replaceAll("¤LINK¤", link);
	out.println(html);
	out.println("<script language=\"JavaScript1.2\">parent.textViewFrame.document.location = \"" +
		link + "#text" + firstText + "\";selectText(" + firstText + ");</script>");

    }
%>
  <p>
<%
    if (reviewTree == 0) {
%>
  >> <a href="tree.jsp?conference=<%=conferenceNumber%>">uppdatera</a><br/>
<%
    }
%>
  >> <a target="_top" href="frames.jsp?conference=0">nyheter</a><br/>
  >> <a href="<%=basePath%>?listnews" target="_top">Till standardvy</a><br/>
  </p>
  </body>
</html>
