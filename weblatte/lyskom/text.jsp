<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*,
				 java.net.*, java.io.*, java.text.*,java.util.regex.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ page import='javax.mail.BodyPart, javax.mail.MessagingException, javax.mail.internet.*' %>
<%@ include file='kom.jsp' %>
<%@ include file='prefs_inc.jsp' %>
<%
	boolean wantHtml = preferences.getBoolean("show-rich-texts") || request.getParameter("wantHtml") != null;
        boolean popupComment = request.getParameter("popupComment") != null ||
            request.getAttribute("popupComment") != null;
	boolean footnoteDisplay = request.getParameter("footnote") != null ||
	    request.getAttribute("footnote") != null;
        boolean inlineImages = preferences.getBoolean("inline-images");
	int conferenceNumber = ((Integer) request.getAttribute("conferenceNumber")).intValue();
	Debug.println("conferenceNumber: " + conferenceNumber);

        LinkedList reviewList = (LinkedList) lyskom.getAttribute("lyskom.review-list");
        LinkedList textNumbers = (LinkedList) request.getAttribute("text-numbers");

	if (reviewList == null) reviewList = new LinkedList();
	if (textNumbers == null) textNumbers = new LinkedList();

	int textNumber = ((Integer) request.getAttribute("text")).intValue();
        out.println("<a name=\"text" + textNumber + "\"></a>");

	Text text = null;
	try {
	    text = lyskom.getText(textNumber);
	} catch (RpcFailure ex1) {
	    if (ex1.getError() == Rpc.E_no_such_text) {
		out.println("<p class=\"statusError\">Fel: text " + 
		    textNumber + " finns inte.</div>");
		return;
	    }
	}
%>
 	<div class="text">
<%
	List viewedTexts = (List) request.getAttribute("viewedTexts");
	if (viewedTexts == null) {
	    viewedTexts = new LinkedList();
	    request.setAttribute("viewedTexts", viewedTexts);
	}
	viewedTexts.add(new Integer(text.getNo()));

	boolean noComments = text.getAuxItems(AuxItem.tagNoComments).size() > 0;
	String contentType = text.getStat().getContentType();
	if (request.getParameter("forceContentType") != null)
	    contentType = request.getParameter("forceContentType");

	ContentType contentTypeObj = new ContentType(contentType);

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
	int[] commented = text.getCommented();
	int[] comments = text.getComments();
	int[] footnoted = text.getFootnoted();
	int[] footnotes = text.getFootnotes();
	List auxMxAuthor = Arrays.asList(text.getAuxData(AuxItem.tagMxAuthor));
	List auxMxFrom = Arrays.asList(text.getAuxData(AuxItem.tagMxFrom));
	List auxMxTo = Arrays.asList(text.getAuxData(AuxItem.tagMxTo));
	List auxMxCc = Arrays.asList(text.getAuxData(AuxItem.tagMxCc));
	List auxMxDate = Arrays.asList(text.getAuxData(AuxItem.tagMxDate));
	List auxMxMimePartIn = Arrays.asList(text.getAuxData(AuxItem.tagMxMimePartIn));


        out.println(jsTitle(serverShort(lyskom) + ": text " + text.getNo() + " av " + 
		lookupName(lyskom, text.getStat().getAuthor()) +
		": " + subject));
	boolean printAuthor = false;
	if (footnoteDisplay) {
	    for (int i=0; i < footnoted.length; i++)
	    	if (lyskom.getTextStat(footnoted[i]).getAuthor() != text.getStat().getAuthor())
		    printAuthor = true;
	}
	out.print("Text nummer " + textLink(request, lyskom, text.getNo(), printAuthor));
	if (footnoteDisplay) {
	    for (int i=0; i < footnoted.length; i++) {	    
		out.print(", fotnot till " + textLink(request, lyskom, footnoted[i], printAuthor));
	    }
	    out.println(", skapad " + df.format(text.getCreationTime()) + "<br/>");
	} else {
	    out.print(" av ");
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
	
%>	[ <a title="Markera text" href="<%=myURI(request)%>?mark=<%=text.getNo()%>">M</a>
        <a title="Avmarkera text" href="<%=myURI(request)%>?unmark=<%=text.getNo()%>">A</a>
        <a title="Personligt svar" href="<%=myURI(request)%>?privateReply=<%=text.getNo()%>">p</a>
	<% if (text.getAuthor() == lyskom.getMyPerson().getNo()) { %>
        <a title="Fotnotera" href="<%=myURI(request)%>?footnoteTo=<%=text.getNo()%>&dispatchToComposer">F</a>
        <% }
	   if (text.getCommented() != null && text.getCommented().length > 0) { %>
        <a title="Återse urinlägg" href="<%=myURI(request)%>?reviewOriginal=<%=text.getNo()%>">åu</a>
	<% } %>
	]<br/>
	Skapad <%= df.format(text.getCreationTime()) %><br/>
<%
	}

	if (debug) {
	    Hollerith[] creatingSoftware = text.getStat().getAuxData(AuxItem.tagCreatingSoftware);
	    for (int i=0; i < creatingSoftware.length; i++) {
	    	out.println("Klient: " + lyskom.toString(creatingSoftware[i].getContents()) + "<br/>");
            }
	    out.println("Datatyp: " + rawContentType + "<br/>");
	}

	for (int i=0; i < commented.length; i++) {
%>
	Kommentar till text <%= textLink(request, lyskom, commented[i]) %><br/>
<%
	}

	for (int i=0; !footnoteDisplay && i < footnoted.length; i++) {
%>
	Fotnot till text <%= textLink(request, lyskom, footnoted[i]) %><br/>

<%
	}

	List miscInfo = text.getStat().getMiscInfo();
	if (conferenceNumber > 0) {
	    boolean conferenceFoundAmongRecipients = false;
	    for (int i=0; !conferenceFoundAmongRecipients && i < miscInfo.size(); i++) {
	    	Selection misc = (Selection) miscInfo.get(i);
	        int key = misc.getKey();
	        if (key == TextStat.miscRecpt || key == TextStat.miscCcRecpt || key == TextStat.miscBccRecpt) {
		    if (misc.getIntValue() == conferenceNumber)
		    	conferenceFoundAmongRecipients = true;
	    	}
	    }

	    if (!conferenceFoundAmongRecipients) { // means text-stat is not up to date.
	    	Debug.println("text.jsp: refreshing text-stat for " + text.getNo());
	    	text.setStat(lyskom.getTextStat(text.getNo(), true));
	    }
	}
	miscInfo = text.getStat().getMiscInfo();
	
	for (int i=0; !footnoteDisplay && i < miscInfo.size(); i++) {
	    Selection misc = (Selection) miscInfo.get(i);
	    int key = misc.getKey();
	    if (key == TextStat.miscRecpt || key == TextStat.miscCcRecpt || key == TextStat.miscBccRecpt) {
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
	if (!contentTypeObj.match("x-kom/user-area")) {
%>
	Ärende: <%= htmlize(subject) %><br/>
<%
	} else {
	    out.println("<div class=\"statusSuccess\">Texten är en User-Area.</div>");
	}
        if (contentTypeObj.match("multipart/*")) {
	    out.println("Texten är flerdelad: ");
	    if (request.getParameter("forceContentType") == null) {
		out.print("<a href=\"" + basePath + "?text=" + text.getNo() +
			"&forceContentType=text/plain\">Visa som text</a> ");
	    } else {
		out.print("<a href=\"" + basePath + "?text=" + text.getNo() +
			"\">Visa normalt</a> ");
	    }
	    if (request.getParameter("showAll") == null) {
	    	out.println("<a href=\"" + basePath + "?text=" + text.getNo() +
			"&showAll\">Visa alla delar</a><br/>");
	    } else {
	    	out.println("<a href=\"" + basePath + "?text=" + text.getNo() +
			"\">Dölj vissa delar</a><br/>");
	    }
	}
	if (contentTypeObj.match("text/x-kom-basic") || contentTypeObj.match("text/plain")) {
            try {
		if (request.getParameter("forceCharset") != null) {
		    charset = request.getParameter("forceCharset");
		}
                String textBody = htmlize(new String(text.getBody(), charset));
		if (commonPreferences.getBoolean("dashed-lines")) {
        	    out.println("<hr noshade width=\"95%\" align=\"left\" />");
    		}
		out.print("<pre class=\"text-body\">");
		out.print(textBody);
		out.println("</pre>");
		if (commonPreferences.getBoolean("dashed-lines")) {
        	    out.println("<hr noshade width=\"95%\" align=\"left\" />");
    		}
            } catch (UnsupportedEncodingException ex1) {
%>
	<div class="statusError">Varning: textens teckenkodning ("<%=ex1.getMessage()%>") kan inte visas ordentligt.<br/>
	<a href="<%= basePath %>rawtext.jsp?text=<%=text.getNo()%>">Klicka här</a> för att visa rådata
        eller <a href="<%= basePath %>index.jsp?text=<%=text.getNo()%>&forceCharset=iso-8859-1">här</a> för att
	tolka innehållet enligt iso-8859-1.<br/>
	Textens fullständiga datatyp är "<b><%= htmlize(rawContentType) %></b>".
	</div>
<%
            }
	} else if (contentTypeObj.match("x-kom/user-area")) {
	    if (commonPreferences.getBoolean("dashed-lines")) {
	        out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    }
	    out.print("<pre>");
	    out.print(htmlize(new String(text.getContents(), charset)));
	    out.println("</pre>");
	    if (commonPreferences.getBoolean("dashed-lines")) {
	        out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    }
	} else if (contentTypeObj.match("multipart/*")) {
	    boolean showNonDisplayableParts = request.getParameter("showAll") != null;
	    try {
		MimeMultipart multipart = new MimeMultipart(text);
		int partCount = multipart.getCount();
		//out.println("Texten innehåller " + multipart.getCount() + " delar");
		boolean contentDisplayed = false;
		for (int i=0; i < partCount; i++) {
		    BodyPart part = multipart.getBodyPart(i);
		    int subpart = 0;
		    if (part.isMimeType("multipart/alternative")) {
			MimeMultipart alternative = new MimeMultipart(new MimePartDataSource((MimeBodyPart)part));
			for (int j=0; j < alternative.getCount(); j++) {
			    BodyPart _part = alternative.getBodyPart(j);
			    if (wantHtml && _part.isMimeType("text/html")) {
				subpart = j;
				part = _part;
			    }
			    if (!wantHtml && _part.isMimeType("text/plain")) {
				part = _part;
			    } else if (_part.isMimeType("text/html") && showNonDisplayableParts) {
				out.println("Visar ej: del " + (i+1) + "." + (j+1) + ": <a href=\"rawtext.jsp?text=" + text.getNo() + "&part=" + i + "&subpart=" + j + "\">data av typen " + new ContentType(_part.getContentType()).getBaseType() + "</a><br/>");
			    }
			}
		    }
		    ContentType partContentTypeObj = new ContentType(part.getContentType());
	            Debug.println("part " + i + " content-type: " + partContentTypeObj.toString());

		    if (partContentTypeObj.match("text/html") ||
	                partContentTypeObj.match("text/plain") ||
			partContentTypeObj.match("text/x-kom-basic") ||
			partContentTypeObj.match("text/enriched")) {
	    		if (commonPreferences.getBoolean("dashed-lines")) {
	        	    out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    		}

			if (wantHtml && part.isMimeType("text/html")) {
			    out.println("<input type=\"button\" onClick=\"var styleobj = document.getElementById('obj"+i+"."+subpart+"').style;var th = styleobj.height; th = th.substring(0, th.length-2); var ti = parseInt(th); ti -= 50; styleobj.height = ti + 'px';\" value=\"-\"/>");
			    out.println("<input type=\"button\" onClick=\"var styleobj = document.getElementById('obj"+i+"."+subpart+"').style;var th = styleobj.height; th = th.substring(0, th.length-2); var ti = parseInt(th); ti += 50; styleobj.height = ti + 'px';\" value=\"+\"/>");
			    out.println("<object style=\"height: 200px;\" id=\"obj"+i+"."+subpart+"\" width=\"95%\" type=\"" + partContentTypeObj.getBaseType() + "\" data=\"rawtext.jsp?text=" + text.getNo() + "&part=" + i + "&subpart=" + subpart + "&sanitize\"></object><br/>");
			    contentDisplayed = true;
			} else if (part.isMimeType("text/html")) {
			    out.println("<div class=\"statusError\">Texten är en HTML-text.<br/>");
			    out.println("<a href=\"" + basePath + "?text="+text.getNo()+"&wantHtml\">Klicka här</a> för att visa den.</div>");
			    contentDisplayed = true;
			} else {
			    out.println("<pre class=\"text\">" + htmlize((String) part.getContent()) + "</pre>");
			    contentDisplayed = true;
			}

	    		if (commonPreferences.getBoolean("dashed-lines")) {
	        	    out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    		}
		    } else if (part.isMimeType("image/*") &&
			       ((partCount == 1 && inlineImages) ||
				"inline".equals(((MimeBodyPart)part).getHeader("Content-Disposition", null)))) {
	    		if (commonPreferences.getBoolean("dashed-lines")) {
	        	    out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    		}
			contentDisplayed = true;
			out.println("<img src=\"rawtext.jsp?text=" + text.getNo() + "&part=" + i + "\" /><br/>");
	    		if (commonPreferences.getBoolean("dashed-lines")) {
	        	    out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    		}
		    } else if (showNonDisplayableParts) {
			out.println("Visar ej: del " + (i+1) + ": <a href=\"rawtext.jsp?text=" + text.getNo() + "&part=" + i + "\">data av typen " + partContentTypeObj.getBaseType() + "</a><br/>");
		    }
		}
		if (!contentDisplayed) {
		    out.println("<div class=\"statusError\">Texten innehåller enbart data som ej kunde visas.<br/> " +
			"<a href=\"" + basePath + "?text=" + text.getNo() + "&showAll\">Klicka här</a> " +
			"för att visa dolda delar.</div>");
		}
	    } catch (MessagingException ex1) {
		out.println("<div class=\"statusError\">Fel: det gick inte att tolka inläggets MIME-innehåll.<br/>");
		out.println("(" + ex1.getMessage() + ")<br/>");
		out.println("(<a href=\"/lyskom/?text=" + text.getNo() + "&forceContentType=text/plain\">återse omodifierad</a>)</div>");
	    }
        } else if (contentTypeObj.match("multipart/alternative")) {
  	    if (commonPreferences.getBoolean("dashed-lines")) {
		out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    }
	    MimeMultipart multipart = new MimeMultipart(text);
	    for (int i=0; i < multipart.getCount(); i++) {
		BodyPart part = multipart.getBodyPart(i);
		ContentType pct = new ContentType(part.getContentType());
		if (wantHtml && pct.match("text/html")) {
		    out.println("<input type=\"button\" onClick=\"var styleobj = document.getElementById('obj"+i+"').style;var th = styleobj.height; th = th.substring(0, th.length-2); var ti = parseInt(th); ti -= 50; styleobj.height = ti + 'px';\" value=\"-\"/>");
		    out.println("<input type=\"button\" onClick=\"var styleobj = document.getElementById('obj"+i+"').style;var th = styleobj.height; th = th.substring(0, th.length-2); var ti = parseInt(th); ti += 50; styleobj.height = ti + 'px';\" value=\"+\"/>");
		    out.println("<object style=\"height: 200px;\" id=\"obj"+i+"\" width=\"95%\" type=\"" + pct.getBaseType() + "\" data=\"rawtext.jsp?text=" + text.getNo() + "&part=" + i + "&sanitize\"></object><br/>");
		}
		if (!wantHtml && pct.match("text/plain") || pct.match("text/x-kom-basic")) {
		    out.print("<pre class=\"text\">" + htmlize((String) part.getContent()) + "</pre>");
		}
	    }
  	    if (commonPreferences.getBoolean("dashed-lines")) {
		out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    }
	} else if (wantHtml && contentTypeObj.match("text/html")) {
  	    if (commonPreferences.getBoolean("dashed-lines")) {
		out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    }
	    out.println("<input type=\"button\" onClick=\"var styleobj = document.getElementById('obj"+text.getNo()+"').style;var th = styleobj.height; th = th.substring(0, th.length-2); var ti = parseInt(th); ti -= 50; styleobj.height = ti + 'px';\" value=\"-\"/>");
	    out.println("<input type=\"button\" onClick=\"var styleobj = document.getElementById('obj"+text.getNo()+"').style;var th = styleobj.height; th = th.substring(0, th.length-2); var ti = parseInt(th); ti += 50; styleobj.height = ti + 'px';\" value=\"+\"/>");
	    out.println("<object style=\"height: 200px;\" id=\"obj"+text.getNo()+"\" width=\"95%\" type=\"" + contentTypeObj.getBaseType() + "\" data=\"rawtext.jsp?text=" + text.getNo() + "&sanitize\"></object><br/>");
  	    if (commonPreferences.getBoolean("dashed-lines")) {
		out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    }
	} else if (inlineImages && contentTypeObj.match("image/*")) {
	    if (commonPreferences.getBoolean("dashed-lines")) {
		out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    }
	    out.println("<img src=\"rawtext.jsp?text=" + text.getNo() + "\" /><br/>");
	    if (commonPreferences.getBoolean("dashed-lines")) {
		out.println("<hr noshade width=\"95%\" align=\"left\" />");
	    }
	} else {
%>
	<div class="statusError">Varning: textens datatyp ("<%=contentType%>") kan inte visas.<br/>
	<a href="rawtext.jsp?text=<%=text.getNo()%>">Klicka här</a> för att visa rådata.</div>
<%
	}

	if (!commonPreferences.getBoolean("dashed-lines")) out.println("<br/>");

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
	out.println("<div class=\"text-comment-list\">");
        for (int i=comments.length-1; i >= 0; i--) {
	    if (preferences.getBoolean("read-comments-first") &&
                request.getParameter("text") == null) {
		if (conferenceNumber > 0) {
		    TextStat ts = null;
		    try {
			ts = lyskom.getTextStat(comments[i]);
		    } catch (RpcFailure ex1) {
			if (ex1.getError() == Rpc.E_no_such_text) {
			    continue;
			}
			throw ex1;
		    }
		    if (ts.hasRecipient(conferenceNumber)) {
			if (!textNumbers.contains(new Integer(comments[i])) &&
			    !reviewList.contains(new Integer(comments[i])) &&
			    !lyskom.getReadTexts().contains(comments[i])) {
			    Debug.println("*** Adding " + comments[i] + " to review-list");
			    reviewList.add(new Integer(comments[i]));
			}
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
%>
		Kommentar i text <%= textLink(request, lyskom, comments[i]) %><br/>
<%
	    }
 	}
	for (int i=0; i < footnotes.length; i++) {
%>
	Fotnot i text <%= textLink(request, lyskom, footnotes[i]) %><br/>
<%
	}
%>
	</div>
	<div class="text-commands">
<%
    if (conferenceNumber > 0 && textNumber > 0 && request.getParameter("comment") == null) {
%>	
	<a accesskey="N" href="<%= myURI(request) %>?conference=<%=conferenceNumber%>&markAsRead=<%=textNumber%>">
	  Läsmarkera denna text (och läs nästa).</a><br/>
<%
    }
    if (textNumber > 0) {
%>
	<a <%= (popupComment ? "target=\"_blank\" " : "") %> <%= (noComments ? "onClick=\"return confirm('Textförfattaren vill helst inte ha några kommentarer till denna text. Vill du fortsätta ändå?');\"" : "") %> href="<%= myURI(request) %>?<%= conferenceNumber > 0 ? "conference="+conferenceNumber : ""%>&markAsRead=<%=textNumber%>&text=<%=textNumber%>&comment=<%=textNumber%>">
	  Kommentera (och läsmarkera) denna text.</a><br/>
<%
    }
%>
	</div>
	</div>
<%
    out.flush();
    RequestDispatcher d = getServletContext().getRequestDispatcher("/lyskom/text.jsp?footnote");
    for (int i=0; i < footnotes.length; i++) {
	request.setAttribute("text", new Integer(footnotes[i]));
	d.include(request, response);
    }
%>
