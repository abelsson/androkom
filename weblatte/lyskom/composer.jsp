<%@ page language='java' import='nu.dll.lyskom.*, com.oreilly.servlet.multipart.*, java.util.*,
				 java.net.URLConnection, java.net.URL, java.io.*, java.text.*' %><%@ 
    page import='java.nio.charset.Charset, javax.mail.internet.ContentType'
%><%@ 
    page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8'
%><%@ include file='kom.jsp' %><%@ include file='prefs_inc.jsp' %><%
    if (request.getParameter("image") != null) {
	String name = request.getParameter("image");
	Map part = (Map) session.getAttribute("wl_composer_image_" + name);
	if (part == null) {
	    response.sendError(HttpServletResponse.SC_NOT_FOUND);
	    return;
	}
	FileInputStream is = new FileInputStream(new File(tempDir, (String) part.get("uploaded")));
	OutputStream os = response.getOutputStream();
	response.setContentType((String) part.get("content-type"));
	byte[] byf = new byte[2048];
	int readBytes;
	while ((readBytes = is.read(byf, 0, byf.length)) != -1)
	    os.write(byf, 0, readBytes);
	return;
    }
%>
<html><head><title>textskrivare</title>
<link rel="stylesheet" href="lattekom.css" />
<script type="text/javascript">
   _editor_url = "<%= basePath %>htmlarea/";
   _editor_lang = "en";
</script>
<script type="text/javascript" src="<%= basePath %>htmlarea/htmlarea.js"></script>
<script type="text/javascript">
   var haConfig = new HTMLArea.Config();
   function buttonHandler(editor, buttonId) {
      switch (buttonId) {
      case "weblatte-query-image":
	 var image = window.prompt("Ange bildens namn");
	 if (typeof(image) == undefined) return;
	 if (image == null) return;
	 if (image == "") return;
         editor.insertHTML("<img src=\"composer.jsp?image=" + escape(image) + "\" />");
         break;
      }
   }
   haConfig.registerButton("weblatte-query-image",  "Infoga bild", "htmlarea/images/ed_image.gif", false, buttonHandler);
   haConfig.toolbar = [
	[ "fontname", "space",
	  "fontsize", "space",
	  "formatblock", "space",
	  "bold", "italic", "underline", "separator",
	  "strikethrough", "subscript", "superscript", "separator",
	  "copy", "cut", "paste", "space", "undo", "redo" ],	
	[ "justifyleft", "justifycenter", "justifyright", "justifyfull", "separator",
	  "insertorderedlist", "insertunorderedlist", "outdent", "indent", "separator",
	  "forecolor", "hilitecolor", "textindicator", "separator",
	  "inserthorizontalrule", "createlink", "weblatte-query-image", "inserttable", "htmlmode", "separator",
	  "popupeditor", "separator", "showhelp", "about" ]
	];
</script>
<body>
<%
    boolean expert = debug || request.getParameter("expert") != null;
    Debug.println("____ **** composer.jsp START.");
    request.setCharacterEncoding("utf-8");
    StringBuffer metadata = new StringBuffer();
    if (lyskom == null || !lyskom.getConnected() || !lyskom.getLoggedIn()) {
	response.sendRedirect("/lyskom/");
	return;
    }
    Map parameters = new HashMap();
    Map fileMap = new HashMap();
    Map typeMap = new HashMap();
    List parameterKeys = new LinkedList();
    String defaultCharset = preferences.getString("create-text-charset");

    Enumeration parameterEnum = request.getParameterNames();
    while (parameterEnum.hasMoreElements()) {
	String key = (String) parameterEnum.nextElement();
	String value = request.getParameter(key);
	Debug.println("___ req param: " + key + "=" + value);
	parameters.put(key, value);
	parameterKeys.add(key);
    }

    Debug.println("____ composer.jsp input ct: " + request.getContentType());
    String _ct = request.getContentType();
    if (_ct != null && _ct.startsWith("multipart/form-data")) {
	MultipartParser multip = new MultipartParser(request, 1024*1024);
    	Debug.println("____ composer.jsp using multipartparser " + multip);
	Part nextPart = null;
	while ((nextPart = multip.readNextPart()) != null) {
	    if (nextPart.isFile()) {
		FilePart fpart = (FilePart) nextPart;
		File tempFile = File.createTempFile("weblatte", ".tmp", tempDir);
		tempFile.deleteOnExit();
		fpart.writeTo(tempFile);
		Debug.println("____ Saved to " + tempFile.getAbsolutePath());
		fileMap.put(fpart.getName(), tempFile.getName());
		parameters.put(fpart.getName(), fpart.getFileName());
		ContentType contentType = new ContentType(fpart.getContentType());
		if (contentType.match("application/octet-stream")) {
		    if (fpart.getFileName() == null) continue;
		    if (fpart.getFileName().toLowerCase().endsWith(".jpg") ||
			fpart.getFileName().toLowerCase().endsWith(".jpeg")) {
			contentType = new ContentType("image/jpeg");
		    } else if (fpart.getFileName().toLowerCase().endsWith(".png")) {
			contentType = new ContentType("image/png");
		    } else if (fpart.getFileName().toLowerCase().endsWith(".gif")) {
			contentType = new ContentType("image/gif");
		    }
		}
		typeMap.put(fpart.getName(), contentType.toString());
		parameterKeys.add(fpart.getName());
		String upkey = fpart.getName().substring(0, fpart.getName().lastIndexOf("_")) +
			"_uploaded";
		Debug.println("____ uploaded-variable: " + upkey + ", name: " + tempFile.getName());
		parameters.put(upkey, tempFile.getName());
	    } else {
		String value = ((ParamPart) nextPart).getStringValue();
		value = new String(value.getBytes("iso-8859-1"), "utf-8");
		Debug.println("multipart-parameter " + nextPart.getName() + "=" +
			value);
		if (parameters.containsKey(nextPart.getName())) {
		    Object _value = parameters.get(nextPart.getName());
		    String[] values;
		    if (_value instanceof String[]) {
			String[] oldValues = (String[]) _value;
			values = new String[oldValues.length+1];
			System.arraycopy(oldValues, 0, values, 0, oldValues.length);
			values[values.length-1] = value;
		    } else {
			values = new String[2];
			values[0] = (String) _value;
			values[1] = value;
		    }
		    parameters.put(nextPart.getName(), values);
		} else {
		    parameters.put(nextPart.getName(), value);
		}
		parameterKeys.add(nextPart.getName());
	    }
	}
    }
    
    Debug.println("____ composer.jsp args: " + parameters);

    boolean multipart = (parameters.containsKey("multipart") || parameters.contaiksKey("multipart-file")) && !parameters.contansKey("noMultipart");

    List parts = new LinkedList();
    Set handledParts = new HashSet();
    Iterator parameterKeysIter = parameterKeys.iterator();
    while (multipart && parameterKeysIter.hasNext()) {
	String name = (String) parameterKeysIter.next();
	if (name.startsWith("part_")) {
	    StringTokenizer kt = new StringTokenizer(name, "_");
	    kt.nextToken();
	    int partNo = Integer.parseInt(kt.nextToken());
	    if (handledParts.contains(new Integer(partNo))) continue;
	    Debug.println("___ parsing part: " + partNo);
	    handledParts.add(new Integer(partNo));
	    if (parameters.get("part_" + partNo + "_delete") != null) continue;

	    String contentType = (String) parameters.get("part_" + partNo + "_type");
	    String uploaded = (String) parameters.get("part_" + partNo + "_uploaded");
	    Debug.println("____ uploaded: " + uploaded);
	    Map part = new HashMap();
	    part.put("content-type", contentType);
	    Debug.println("____ content-type: " + contentType);
	    String urlResource = (String) parameters.get("part_" + partNo + "_url");
	    Debug.println("____ url: " + urlResource);
	    if (urlResource != null && !"".equals(urlResource)) {
		URL url = new URL(urlResource);
		URLConnection con = new URL(urlResource).openConnection();
		File storeFile = File.createTempFile("weblatte", ".tmp", tempDir);
		storeFile.deleteOnExit();
		OutputStream os = new FileOutputStream(storeFile);
		part.put("content-type", con.getContentType());
	    	Debug.println("____ new content-type: " + con.getContentType());
		InputStream is = con.getInputStream();
		byte[] buf = new byte[2048];
		int read = 0;
		while ((read = is.read(buf)) > 0) {
		    os.write(buf, 0, read);
		}
		os.close();
		part.put("uploaded", storeFile.getName());
		String fpath = url.getPath();
		int lastSlash = fpath.lastIndexOf("/");
		if (lastSlash > -1) {
		    fpath = fpath.substring(lastSlash+1);
		}
		part.put("filename", fpath);
	    } else if (contentType.toLowerCase().startsWith("text/") &&
		       !"".equals(parameters.get("part_" + partNo + "_contents"))) {
		part.put("contents", parameters.get("part_" + partNo + "_contents"));
	    } else if (uploaded != null && !"".equals(uploaded)) {
		if (fileMap.containsKey("part_" + partNo + "_file"))
		    part.put("uploaded", fileMap.get("part_" + partNo + "_file"));
		else
		    part.put("uploaded", uploaded);

		part.put("filename", parameters.get("part_" + partNo + "_file"));
		if (typeMap.containsKey("part_" + partNo + "_file"))
		    part.put("content-type", typeMap.get("part_" + partNo + "_file"));
		
	    	Debug.println("____ new content-type: " + part.get("content-type"));
	    }
	    Debug.println("Adding part " + part);
	    parts.add(part);
	}
    }

    if (parameters.get("createText") != null) {
	request.setAttribute("set-uri", makeAbsoluteURL("/"));
	if (multipart && parts.size() > 0) {
	    for (Iterator i = parts.iterator();i.hasNext();) {
		Map part = (Map) i.next();
		String ctstr = (String) part.get("content-type");
		if (ctstr == null) continue;
		ContentType ct = new ContentType(ctstr);
		if (!ct.match("text/html")) continue;
		String contents = (String) part.get("contents");
      		if (contents == null || contents.trim().equals("")) {
		    i.remove();
		    continue;
		}
		Debug.println("will search and replace composer.jsp image references.");
		Matcher m = Pattern.compile("composer\\.jsp\\?image=([^\">]*)").matcher(contents);
		StringBuffer buf = new StringBuffer();
		while (m.find()) {
		    Debug.println("found composer.jsp match: " + m.group(1));
		    m.appendReplacement(buf, URLDecoder.decode(m.group(1), "iso-8859-1"));
		}
		m.appendTail(buf);
		part.put("contents", buf.toString());
		if (parameters.containsKey("createAlternative")) {
		    String plainTextVersion = buf.toString().replaceAll("<[bB][rR].*?>", "\n").replaceAll("<.*?>", "");
		    part.put("alternative-contents", plainTextVersion);
		    part.put("alternative-content-type", "text/plain; charset=" +
			preferences.getString("create-text-charset"));
		}
	    }
	    request.setAttribute("multipart", parts);
	}
	request.setAttribute("parsed-parameters", parameters);
	RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(appPath + "/");
	dispatcher.forward(request, response);
	return;	
    }

    if (multipart && (parts.size() == 0 ||
	parameters.get("newPart") != null)) {
	Map part = new HashMap();
	part.put("content-type", "text/html;charset="+defaultCharset);
	parts.add(part);
    }

    if (multipart && (parts.size() == 0 ||
	parameters.get("newFile") != null ||
	parameters.contaisKey("multipart-file"))) {
	Map part = new HashMap();
	part.put("content-type", "application/octet-stream");
	parts.add(part);
    }

    String subject = parameters.get("subject") != null ? (String) parameters.get("subject") : "";
    String body = parameters.get("body") != null ? (String) parameters.get("body") : "";

    List recipients = new LinkedList();
    List ccRecipients = new LinkedList();

    if (parameters.get("changePresentation") != null) {
	Text oldPresentation = null;
	int confNo = Integer.parseInt((String) parameters.get("changePresentation"));
	Conference conf = lyskom.getConfStat(confNo);
	if (conf.getPresentation() > 0) {
	    oldPresentation = lyskom.getText(conf.getPresentation());
	    String charset = oldPresentation.getCharset();

	    // work around texts created by buggy elisp clients...
	    if ("us-ascii".equals(charset)) charset = "iso-8859-1";

 	    int[] _rcpts = oldPresentation.getRecipients();
	    int[] _ccRcpts = oldPresentation.getCcRecipients();
	    for (int i=0; i < _rcpts.length; i++)
		recipients.add(lookupName(lyskom, _rcpts[i]));
	    for (int i=0; i < _ccRcpts.length; i++)
	    	ccRecipients.add(lookupName(lyskom, _ccRcpts[i]));

	    if ("".equals(subject)) subject = new String(oldPresentation.getSubject(), charset);
	    if ("".equals(body)) body = new String(oldPresentation.getBody(), charset);

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

    if (parameters.get("privateReply") != null) {
	int privateReplyTo = Integer.parseInt((String) parameters.get("privateReply"));
	Text txt = lyskom.getText(privateReplyTo);
	recipients.add(lookupName(lyskom, txt.getAuthor()));
	subject = new String(txt.getSubject());
	metadata.append("Personligt svar till text ").
		append(textLink(request, lyskom, txt.getNo())).
		append("<br/>");
    }


    if (parameters.get("footnoteTo") != null) {
        lyskom.changeWhatIAmDoing("Skriver en fotnot");
	int footnotedTextNo = Integer.parseInt((String) parameters.get("footnoteTo"));
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
    if (parameters.get("inCommentTo") != null) {
        lyskom.changeWhatIAmDoing("Skriver en kommentar");
	commentedTextNo = Integer.parseInt((String) parameters.get("inCommentTo"));
	metadata.append("Kommentar till text ").
		append(textLink(request, lyskom, commentedTextNo)).
		append("<br/>");
    }
    if (parameters.get("inCommentTo") != null &&
	parameters.get("addNewRecipient") == null) {
	Text commentedText = lyskom.getText(commentedTextNo);
	int[] _recipients = commentedText.getRecipients();
	Debug.println("____ composer.jsp, comment to text: " + commentedTextNo);
	for (int i=0; i < _recipients.length; i++) {
	    Debug.println("____ composer.jsp, comment recipient: " + _recipients[i]);
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

    if (parameters.get("inCommentTo") == null &&
        parameters.get("footnoteTo") == null) {
        lyskom.changeWhatIAmDoing("Skriver ett inlägg");	
    }

    StringBuffer errors = new StringBuffer();


    for (int rcptType = 1; rcptType <= 2; rcptType++) {
	Object recptFieldsObj = parameters.get(rcptType == 1 ? "recipient" : "ccRecipient");
	Object recptNoFieldsObj = parameters.get(rcptType == 1 ? "recipientNo" : "ccRecipientNo");
        String[] recptFields = recptFieldsObj instanceof String ?
		new String[] { (String) recptFieldsObj } :
		(String[]) recptFieldsObj;
	String[] recptNoFields = recptNoFieldsObj instanceof String ?
		new String[] { (String) recptNoFieldsObj } :
		(String[]) recptNoFieldsObj;
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
		Conference conference = lyskom.getConfStat(conf.getNo());
		String contentType = multipart ? "multipart/mixed" : "text/plain";
		if (multipart && parts.size() == 1) {
		    contentType = (String) ((Map) parts.get(0)).get("content-type");
		} else {
		    if (parameters.containsKey("content-type")) {
			contentType = (String) parameters.get("content-type");
		    }
		}
		if (!conference.allowsMimeType(lyskom.getAllowedContentTypes(), contentType)) {
		    errors.append("Varning: mötet \"" + lookupName(lyskom, conf.getNo(), true) +
			"\" tillåter inte inlägg utav typen \"" +
			new ContentType(contentType).getBaseType() + "\".<br/>");
		}

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
<% } %> <!-- was: application/x-www-form-urlencoded -->
<form enctype="multipart/form-data" class="boxed" method="post" action="<%=request.getRequestURI()%>">
<%
    out.println(metadata.toString());
    out.println("<table border=\"0\">");
    for (int rcptType = 1; rcptType <= 2; rcptType++) {
	List list = rcptType == 1 ? recipients : ccRecipients;
    	for (Iterator i = list.iterator(); i.hasNext();) {
	    out.print("<tr><td>");
	    String recipient = (String) i.next();
	    if (rcptType == 1) out.print("Mottagare: ");
	    else out.print("Kopiemottagare: ");
	    out.print("</td><td>");
	    out.print("<input name=\"" + (rcptType==1?"recipient":"ccRecipient") + "\" type=\"text\" size=\"40\" value=\"" + recipient + "\">");
	    out.println("</td></tr>");
        }
    }
    out.println("</table>");
%>
<input type="submit" value="lägg till/uppdatera mottagare" name="addNewRecipient"><br/>
<br/>
Ämne: <input type="text" size="50" name="subject" value="<%=subject%>"><br/>
<%
    if (!multipart) {
%><textarea name="body" cols="71" rows="10"><%=body%></textarea><br/><%
    } else {
	int count = 0;
	List imageParts = new LinkedList();
	for (Iterator i = parts.iterator(); i.hasNext();) {
	    Map part = (Map) i.next();
	    out.println("<br/>Del " + (count+1) + ":<br/>");
	    if (part.containsKey("uploaded")) {
		if (new ContentType((String)part.get("content-type")).match("image/*")) {
		    imageParts.add(part);
		}
	    }
	    out.println("<input type=\"submit\" name=\"part_" + count + "_delete\" value=\"ta bort del " + (count+1) + "\"/><br/>");
	    if (part.containsKey("uploaded")) {
		out.println("Datatyp: " + part.get("content-type") + "<br/>");
		out.println("<input type=\"hidden\" name=\"part_" + count + "_type\" value=\"" +
			part.get("content-type") + "\" />");
		File f = new File(tempDir, (String) part.get("uploaded"));
		out.println("Binärdata: <b>uppladdad, " + f.length() + " bytes</b><br/>");
		session.setAttribute("wl_composer_image_" + part.get("filename"), part);
		if (part.containsKey("filename")) {
		    out.println("<input type=\"hidden\" name=\"part_" + count + "_file\" value=\"" +
			part.get("filename") + "\"/>");
		    out.println("Filnamn: " + part.get("filename") + "<br/>");
		}
		out.println("<input type=\"hidden\" name=\"part_" + count + "_uploaded\" value=\"" + part.get("uploaded") + "\"/>");
	    } else {
		ContentType ctype = new ContentType((String) part.get("content-type"));
		String url = (String) part.get("url");
		String contents = (String) part.get("contents");
		if (contents == null) contents = "";
		if (url == null) url = "";
		if (!ctype.match("text/*")) {
		    out.println("Ladda upp fil: <input type=\"file\" name=\"part_" + count + "_file\"/>");
		    out.println("<input type=\"hidden\" name=\"part_" + count + "_type\" value=\"" + ctype.toString() + "\"/>");
		    out.println("<input type=\"submit\" name=\"doUpload\" value=\"ladda upp\"/><br/>");
		    out.println("<i>Eller</i> ange URL: <input type=\"text\" name=\"part_" + count + "_url" + "\" size=\"40\" value=\"" + url + "\">");
		    out.println("<input type=\"submit\" name=\"doUpload\" value=\"hämta\"/><br/>");
		} else {
		    out.println("Skriv textinnehåll:<br/>");
		    String taid = "part_" + count + "_contents";
		    out.println("<textarea id=\"" + taid + "\" name=\"" + taid + "\" style=\"width: 100%; height: 25em;\">" + contents + "</textarea><br/>");
		    out.println("Typ av text: <select name=\"part_" + count + "_type\">");
		    out.println("<option value=\"text/plain;charset=" + defaultCharset + "\" " + 
			(ctype.match("text/plain") ? "selected" : "") + ">Ren text</option>");
		    out.println("<option value=\"text/html;charset=" + defaultCharset + "\" " + 
			(ctype.match("text/html") ? "selected" : "") + ">HTML</option>");
		    out.println("</select>");
		    out.println("<input id=\"" + taid + "_wbtn\" type=\"button\" value=\"aktivera WYSIWYG\" onClick=\"HTMLArea.replace('"+taid+"', haConfig); document.getElementById('"+taid+"_wbtn').disabled = true;\"/><br>");
		}
	    }
	    count++;
	}
	out.print("<input type=\"hidden\" id=\"imagePartList\" value=\"");
	for (Iterator i = imageParts.iterator(); i.hasNext();) {
	    out.print((String) ((Map) i.next()).get("filename"));
	    if (i.hasNext()) out.print(";");
	}
        out.println("\"/>");
	out.println("<br/>");
	out.println("Infoga: <input type=\"submit\" name=\"newPart\" value=\"ny text\"/>");
	out.println(" <input type=\"submit\" name=\"newFile\" value=\"ny fil\"/>");
	out.println("<br/>");
	out.println("Skapa textalernativ till HTML-delar: <input type=\"checkbox\" name=\"createAlternative\"/><br/>");
	out.println("<br/>");
    }
    if (parameters.get("inCommentTo") != null) { 
%><input type="hidden" name="inCommentTo" value="<%=parameters.get("inCommentTo")%>"><%
    }
    if (parameters.get("changePresentation") != null) { 
%><input type="hidden" name="changePresentation" value="<%=parameters.get("changePresentation")%>"><%
    }
    if (parameters.get("privateReply") != null) { 
%><input type="hidden" name="inCommentTo" value="<%=parameters.get("privateReply")%>"><%
    } 
    if (parameters.get("footnoteTo") != null) { 
%><input type="hidden" name="footnoteTo" value="<%=parameters.get("footnoteTo")%>"><%
    } 
    if (multipart) {
%><input type="hidden" name="multipart" value="<%=parameters.get("multipart")%>"><%
    }

    /*
    if (expert) {
	Map charsets = Charset.availableCharsets();
	Iterator charsetIterator = charsets.keySet().iterator();
	out.println("Teckenkodning: <select name=\"charset\">");
	for (int i=0; i < charsets.size(); i++) {
	    String cs = (String) charsetIterator.next();
	    out.print("<option value=\"" + cs.toLowerCase() + "\"");
	    if (cs.toLowerCase().equals(preferences.getString("create-text-charset").toLowerCase())) {
	        out.print(" selected");
	    }
	    out.println(">" + cs + "</option>");
	}	
	out.println("</select><br/>");
	String contentType = "text/x-kom-basic";
	if (multipart) contentType = "multipart/related";
	out.println("Datatyp: <input type=\"text\" size=\"50\" name=\"content-type\" value=\"" + contentType + "\" /><br/>");
    } 
    */
    if (parameters.containsKey("content-type")) {
	out.println("<input type=\"hidden\" name=\"content-type\" value=\"" + 
		parameters.get("content-type") + "\"/>");
    }
%>
<input type="submit" value="skicka!" name="createText">
<%  
    if (!multipart) {
%><input type="submit" value="multimedia (fil)" name="multipart-file"><%
<input type="submit" value="multimedia (text/HTML)" name="multipart">
    } else {
%><input type="submit" value="vanlig text" name="noMultipart"><%
    }
   if (!expert && debug) { %>
<input type="submit" value="avancerat läge" name="createText">
<%  } %>
</form>

<p class="footer">
$Id: composer.jsp,v 1.14 2004/06/07 01:28:32 pajp Exp $
</p>
</body>
</html>
