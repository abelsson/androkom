<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*,
				 java.net.*, java.io.*, java.text.*,java.util.regex.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ include file='kom.jsp' %>
<%@ include file='prefs_inc.jsp' %>
<%
	int conferenceNumber = ((Integer) request.getAttribute("conferenceNumber")).intValue();
        List reviewList = (List) request.getAttribute("reviewList");
	int textNumber = ((Integer) request.getAttribute("text")).intValue();
        out.println("<a name=\"text" + textNumber + "\"></a>");
%>
	<p class="boxed">
<%
	Text text = null;
	try {
	    text = lyskom.getText(textNumber);
	} catch (RpcFailure ex1) {
	    if (ex1.getError() == Rpc.E_no_such_text) {
		out.println("<p class=\"statusError\">Text " + 
		    textNumber + " finns inte.</p>");
		out.println("</p>");
		return;
	    }
	}
	List viewedTexts = (List) request.getAttribute("viewedTexts");
	if (viewedTexts == null) {
	    viewedTexts = new LinkedList();
	    request.setAttribute("viewedTexts", viewedTexts);
	}
	viewedTexts.add(new Integer(text.getNo()));

	boolean noComments = text.getAuxItems(AuxItem.tagNoComments).size() > 0;
	String contentType = text.getContentType();
	Hollerith[] ctdata = text.getStat().getAuxData(AuxItem.tagContentType);
	String rawContentType = (ctdata != null && ctdata.length > 0) ? new String(text.getStat().getAuxData(AuxItem.tagContentType)[0].getContents(), "us-ascii") : "text/x-kom-basic";
	String charset = text.getCharset();
	if (charset == null) charset = "iso-8859-1";
	if (charset.equals("us-ascii")) charset = "iso-8859-1"; // some broken elisp clients lie...
	if (contentType.equals("x-kom/text")) contentType = "text/x-kom-basic";
	byte[] subjectBytes = text.getSubject();
	// a bit clumsy but it allows us to always fallback on the default
	// system encoding, and if neither iso-8859-1 nor the text-specifies
	// charset is supported, it will be indicated by appending the
	// name in brackets
	String subject = new String(subjectBytes);
	try {
	    subject = new String(subjectBytes, "iso-8859-1");
	    subject = new String(subjectBytes, charset);
	} catch (UnsupportedEncodingException ex1) {
	    subject = subject + " [" + charset + "]";
	}
%>
        <%= jsTitle(serverShort(lyskom) + ": text " + text.getNo() + " av " + 
		lookupName(lyskom, lyskom.getTextStat(text.getNo()).getAuthor()) +
		": " + subject) %>
	<tt>Text nummer <%= textLink(request, lyskom, text.getNo(), false) %> av
<%
	List auxMxAuthor = Arrays.asList(text.getAuxData(AuxItem.tagMxAuthor));
	List auxMxFrom = Arrays.asList(text.getAuxData(AuxItem.tagMxFrom));
	List auxMxTo = Arrays.asList(text.getAuxData(AuxItem.tagMxTo));
	List auxMxCc = Arrays.asList(text.getAuxData(AuxItem.tagMxCc));
	List auxMxDate = Arrays.asList(text.getAuxData(AuxItem.tagMxDate));
	List auxMxMimePartIn = Arrays.asList(text.getAuxData(AuxItem.tagMxMimePartIn));
	if (auxMxAuthor.size() > 0) {
	     out.print(htmlize(((Hollerith) auxMxAuthor.get(0)).getContentString()));
	}
	if (auxMxFrom.size() > 0) {
	    String email = ((Hollerith) auxMxFrom.get(0)).getContentString();
	    out.print(" &lt;<a href=\"mailto:");
	    out.print(htmlize(email));
	    out.print("\">");
	    out.print(htmlize(email));
	    out.println("</a>>");
	}
	if (auxMxAuthor.size() == 0 && auxMxFrom.size() == 0) {
	    out.println(lookupName(lyskom, text.getAuthor(), true));
	}
	
%><br>
	textkommandon: 
        <a title="Markera text" href="<%=myURI(request)%>?mark=<%=text.getNo()%>">M</a>
        <a title="Avmarkera text" href="<%=myURI(request)%>?unmark=<%=text.getNo()%>">A</a>
        <a title="Personligt svar" href="<%=myURI(request)%>?privateReply=<%=text.getNo()%>">p</a>
	<% if (text.getAuthor() == lyskom.getMyPerson().getNo()) { %>
        <a title="Fotnotera" href="<%=myURI(request)%>?footnoteTo=<%=text.getNo()%>&dispatchToComposer">F</a>
        <% }
	   if (text.getCommented() != null && text.getCommented().length > 0) { %>
        <a title="Återse urinlägg" href="<%=myURI(request)%>?reviewOriginal=<%=text.getNo()%>">åu</a>
	<% } %>
	<br/>
	Skapad <%= df.format(text.getCreationTime()) %><br/>
<%	if (debug) {
	Hollerith[] creatingSoftware = text.getStat().getAuxData(AuxItem.tagCreatingSoftware);
	for (int i=0; i < creatingSoftware.length; i++) {
	    out.println("Klient: " + lyskom.toString(creatingSoftware[i].getContents()) + "<br/>");
        }
%>
	Datatyp: <%= rawContentType %><br/>
<%	}
	int[] commented = text.getCommented();
	int[] comments = text.getComments();
	int[] footnoted = text.getFootnoted();
	int[] footnotes = text.getFootnotes();
	for (int i=0; i < commented.length; i++) {
%>
	Kommentar till text <%= textLink(request, lyskom, commented[i]) %><br/>
<%
	}
	for (int i=0; i < footnoted.length; i++) {
%>
	Fotnot till text <%= textLink(request, lyskom, footnoted[i]) %><br/>
<%
	}

	List miscInfo = text.getStat().getMiscInfo();
	for (int i=0; i < miscInfo.size(); i++) {
	    Selection misc = (Selection) miscInfo.get(i);
	    Debug.println("misc-info key: " + misc.getKey() + ", value: " + misc.getValue());
	    int key = misc.getKey();
	    if (key == TextStat.miscRecpt || key == TextStat.miscCcRecpt) {
		    String title = "";
		    int value = misc.getIntValue();
		    String type = "Mottagare";
		    if (key == TextStat.miscCcRecpt)
			type = "Extra kopiemottagare";
		    if (key == TextStat.miscBccRecpt)
			type = "Dold kopiemottagare";

		    if (misc.contains(TextStat.miscSentBy)) {
			title += "Skickat av " + lookupNamePlain(lyskom, misc.getIntValue(TextStat.miscSentBy));
		    }
		    if (misc.contains(TextStat.miscSentAt)) {
			title += (title != "" ? ", " : "Skickad ") + df.format(((KomTime)misc.getValue(TextStat.miscSentAt)).getTime());
		    }
		    out.println("<span title=\"" + htmlize(title) + "\">" + type + (title != "" ? "*" : "") + ":</span> " + lookupName(lyskom, value, true) + " " + (preferences.getBoolean("show-local-text-numbers") ? ("&lt;" + text.getLocal(value) + ">") : "") + "<br/>");

	    }
	}
	if (!contentType.equals("x-kom/user-area")) {
%>
	Ärende: <%= htmlize(subject) %><br/>
<%
	} else {
	    out.println("<p class=\"statusSuccess\">Texten är en User-Area.</p>");
	}
	if (commonPreferences.getBoolean("dashed-lines")) {
%>
	<hr noshade width="95%" align="left" />
<%
	}
	if (contentType.equals("text/x-kom-basic") || contentType.equals("text/plain")) {
            try {
		if (request.getParameter("forceCharset") != null) {
		    charset = request.getParameter("forceCharset");
		}
                String textBody = htmlize(new String(text.getBody(), charset));
%>
		<pre class="text"><%= textBody %></pre>
<%
            } catch (UnsupportedEncodingException ex1) {
%>
	<p class="statusError">Varning: textens teckenkodning ("<%=ex1.getMessage()%>") kan inte visas ordentligt.<br/>
	<a href="<%= basePath %>rawtext.jsp?text=<%=text.getNo()%>">Klicka här</a> för att visa rådata
        eller <a href="<%= basePath %>index.jsp?text=<%=text.getNo()%>&forceCharset=iso-8859-1">här</a> för att
	tolka innehållet enligt iso-8859-1.<br/>
	Textens fullständiga datatyp är "<b><%= htmlize(rawContentType) %></b>".
	</p>
<%
            }
	} else if (contentType.equals("x-kom/user-area")) {
	    out.print("<pre>");
	    out.print(htmlize(new String(text.getContents(), charset)));
	    out.println("</pre>");
	} else {
%>
	<p class="statusError">Varning: textens datatyp ("<%=contentType%>") kan inte visas.<br/>
	<a href="/lyskom/rawtext.jsp?text=<%=text.getNo()%>">Klicka här</a> för att visa rådata.</p>
<%
	}
	if (commonPreferences.getBoolean("dashed-lines")) {
%>
	<hr noshade width="95%" align="left" />
<%
	} else {
%>
	<br/>
<%
	}
	List fastReplies = text.getAuxItems(AuxItem.tagFastReply);
	for (Iterator i = fastReplies.iterator(); i.hasNext();) {
	    AuxItem item = (AuxItem) i.next();
	    out.print("<span title=\"Anmärkning skapad ");
	    out.print(df.format(item.getCreatedAt().getTime()));
	    out.print("\">");
	    if (item.getCreator() == text.getAuthor()) {
		out.print("<b>Anmärkning av författaren:</b> ");
	    } else {
		out.print("<b>Anmärkning av ");
		out.print(lookupName(lyskom, item.getCreator(), true));
		out.print(":</b> ");
	    }
	    out.print(htmlize(item.getDataString()));
	    out.println("</span></br>");
	}
	Set attachmentTexts = new HashSet();
	for (Iterator i = auxMxMimePartIn.iterator(); i.hasNext();) {
	    attachmentTexts.add(new Integer(((KomToken) i.next()).intValue()));
	}
	for (int i=0; i < comments.length; i++) {
	    if (attachmentTexts.contains(new Integer(comments[i]))) {
		TextStat ts = lyskom.getTextStat(comments[i]);
		lyskom.markAsRead(comments[i]);
%>
		Bilaga av typen <%= ts.getContentType() %> i <%= textLink(request, lyskom, comments[i], false) %>
		 (<a href="/lyskom/rawtext.jsp?text=<%=comments[i]%>">visa</a>)<br/>
<%
	    } else {
	        if (preferences.getBoolean("read-comments-first")) {
		    if (conferenceNumber > 0) {
			TextStat ts = lyskom.getTextStat(comments[i]);
			if (ts.hasRecipient(conferenceNumber)) {
			    if (reviewList.contains(new Integer(comments[i]))) {
				reviewList.add(0, new Integer(comments[i]));
			    }
			    Debug.println("Adding " + comments[i] + " to review-list");
			} else {
			    Debug.println("NOT adding " + comments[i] + " to review-list " +
				" (not in conference " + conferenceNumber + ")");
			}
		    } else {
		        Debug.println("read-comments-first is true, but not in a conference");
		    }
	        } else {
		    Debug.println("read-comments-first is false");
		}
%>
		Kommentar i text <%= textLink(request, lyskom, comments[i]) %><br/>
<%
	    }
 	}
%>	<%= comments.length > 0 ? "<br/>" : "" %>
<%
	for (int i=0; i < footnotes.length; i++) {
%>
	Fotnot i text <%= textLink(request, lyskom, footnotes[i]) %><br/>
<%
	}
%>	<%= footnotes.length > 0 ? "<br/>" : "" %>
	</tt>
<%
    if (conferenceNumber > 0 && textNumber > 0 && request.getParameter("comment") == null) {
%>
	<a href="<%= myURI(request) %>?conference=<%=conferenceNumber%>&markAsRead=<%=textNumber%>">
	  Läsmarkera denna text (och läs nästa).</a><br/>
<%
    }
    if (textNumber > 0) {
%>
	<a <%= (noComments ? "onClick=\"return confirm('Textförfattaren vill helst inte ha några kommentarer till denna text. Vill du fortsätta ändå?');\"" : "") %> href="<%= myURI(request) %>?<%= conferenceNumber > 0 ? "conference="+conferenceNumber : ""%>&markAsRead=<%=textNumber%>&text=<%=textNumber%>&comment=<%=textNumber%>">
	  Kommentera (och läsmarkera) denna text.</a><br/>
<%
    }
%>
	</p>
<%
    out.flush();
    RequestDispatcher d = getServletContext().getRequestDispatcher("/lyskom/text.jsp");
    for (int i=0; i < footnotes.length; i++) {
	request.setAttribute("text", new Integer(footnotes[i]));
	d.include(request, response);
    }
%>
