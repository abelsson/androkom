<jsp:directive.page pageEncoding='iso-8859-1' contentType='text/html;charset=utf-8' language='java' import='java.nio.*, java.nio.charset.*, nu.dll.lyskom.*, javax.mail.BodyPart, javax.mail.internet.*' /><jsp:directive.include file='kom.jsp' /><jsp:directive.include file='prefs_inc.jsp' /><%
    if (lyskom == null) {
	response.sendRedirect("/lyskom/?error=nosession");
	return;
    }
    int newTextNo = 0;
    Map parameters = (Map) request.getAttribute("parsed-parameters");
    if (Debug.ENABLED) Debug.println("savetext.jsp: parameters: " + parameters);
    if (parameters.containsKey("createText")) {
	boolean isMultipart = request.getParameter("multipart") != null ||
		request.getAttribute("multipart") != null;
	Debug.println("savetext.jsp: multipart: " + isMultipart);
	List parts = (List) request.getAttribute("multipart");
	List recipients = new LinkedList();
    	List ccRecipients = new LinkedList();

    	StringBuffer errors = new StringBuffer();
        boolean explicitRecipients = false;

   	for (int rcptType = 1; rcptType <= 2; rcptType++) {
            String[] recptFields;
	    Object _fields = parameters.get(rcptType == 1 ? "recipient" : "ccRecipient");
	    if (_fields == null) {
		recptFields = new String[0];
	    } else if (_fields instanceof String[]) {
		recptFields = (String[]) _fields;
	    } else {
		recptFields = new String[] { (String) _fields };
	    }
	    List list = rcptType == 1 ? recipients : ccRecipients;
	    if (recptFields == null) continue;
            for (int i=0; i < recptFields.length; i++) {
   	    	recptFields[i] = recptFields[i].trim();
	    	if ("".equals(recptFields[i])) continue;
	    	try {
            	    ConfInfo conf = lookupName(lyskom, recptFields[i], true, true);
		    if (conf == null) {
		    	errors.append("Namnet \"" + htmlize(recptFields[i]) + "\" hittas inte.<br>");
		    	continue;
		    }
		    if (list.contains(conf.getNameString())) continue;
	    	    list.add(conf);
	    	    explicitRecipients = true;
	    	} catch (AmbiguousNameException ex1) {
		    errors.append("<div class=\"statusError\">Fel: namnet är flertydigt. Följande namn matchar:");
	            errors.append("<ul>");
	            ConfInfo[] names = ex1.getPossibleNames();
	            for (int j=0; j < names.length; j++) 
		        errors.append("<li>" + lookupName(lyskom, names[j].getNo(), true));
	    	    errors.append("</ul>");
		    errors.append("</div>");
	    	}
	    }
    	}
	Text newText = null;
	if (errors.length() == 0) {
	    if (!isMultipart) {
		String charsetName = preferences.getString("create-text-charset");
		if (parameters.containsKey("charset"))
		    charsetName = parameter(parameters, "charset");
		String subject = parameter(parameters, "subject");
		String body = parameter(parameters, "body").replaceAll("\r", "");
		String textContents = subject + "\n" + body;   
		Charset charset = Charset.forName(charsetName);
		Debug.println("using default charset " + charset.displayName());
		CharsetEncoder encoder = charset.newEncoder();

		Charset utf8Charset = Charset.forName("utf-8");
		encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
		if (!encoder.canEncode(textContents) && !charset.equals(utf8Charset)) {
		    Debug.println("default encoding failed, attempting utf-8 fallback");
		    CharsetEncoder utf8encoder = utf8Charset.newEncoder();
		    if (!utf8encoder.canEncode(textContents)) {
			Debug.println("utf-8 encoding failed also");
			errors.append("<div class=\"statusError\">Varning: texten " +
				      " kan inte kodas i vare sig utf-8 eller " + 
				      charset.displayName() + " (använder " + 
				      charset.displayName() + ").</div>");
		    } else {
			errors.append("<div class=\"statusError\">OBS: " +
				      "textens innehåll kunde inte kodas i vald " +
				      "teckenkodning (\"" + charset.displayName() +
				      "\"). " + 
				      "Texten har istället kodats till \"" + 
				      utf8Charset.displayName() + "\" för att " +
				      "hela innehållet skall representeras korrekt.</div>");

			encoder = utf8encoder;
			charsetName = "utf-8";
		    }
		}
		encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		encoder.reset();
		ByteBuffer textBuffer = encoder.encode(CharBuffer.wrap(textContents));
		if (!textBuffer.hasArray()) {
		    throw new IOException("returned ByteBuffer is not backed " +
					  "by an array");
		}
		byte[] _tmp = textBuffer.array();
		byte[] textContentBytes = new byte[textBuffer.limit()];
		System.arraycopy(_tmp, 0, textContentBytes, 0, textBuffer.limit());
		newText = new Text(textContentBytes, charsetName);
		wrapText(newText);

		if (parameters.containsKey("content-type") && !"".equals(parameters.get("content-type"))) {
		    newText.getStat().setAuxItem(new AuxItem(AuxItem.tagContentType, parameter(parameters, "content-type")));
		}

	    } else {
		ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
		ContentType ct;
		byte[] subject = lyskom.toByteArray(parameter(parameters, "subject"));
	  	boolean wrapInRfc822 = preferences.getBoolean("post-rich-texts-as-rfc822");
	 	boolean plainTextWithImages = false;
		if (parts.size() == 1) {
		    Map partMap = (Map) parts.get(0);
		    Debug.println("savetext.jsp: only one part: " + partMap);
		    ContentType partContentType = new ContentType((String) partMap.get("content-type"));
		    String _uploaded = (String) partMap.get("uploaded");
		    String _contents = (String) partMap.get("contents");
		    if (_uploaded != null && !_uploaded.equals("")) {
			File file = new File(tempDir, (String) partMap.get("uploaded"));
	  		if (!file.getParentFile().equals(tempDir)) {
			    throw new IOException("uploaded file is not in temp dir!");
			}
		    	partContentType.getParameterList().set("name", (String) partMap.get("filename"));
			InputStream is = new FileInputStream(file);
			int read;
			byte[] buf = new byte[2048];
			while ((read = is.read(buf, 0, buf.length)) != -1) {
			    bodyStream.write(buf, 0, read);
			}
			is.close();
			Debug.println("read file " + file.getAbsolutePath() + 
			   " into text with content-type " + partContentType);
		    } else if (_contents != null && !_contents.equals("")) {
			String charset = partContentType.getParameterList().get("charset");
			if (charset == null) charset = preferences.getString("create-text-charset");

			if (partMap.containsKey("alternative-contents")) {
			    InternetHeaders headers = new InternetHeaders();
			    headers.addHeader("Content-Transfer-Encoding", "binary");
			    headers.addHeader("Content-Type", partContentType.toString());
			    byte[] contents = _contents.getBytes(charset);
			    MimeMultipart alternative = new MimeMultipart("alternative");
			    ContentType altContentType = new ContentType((String) partMap.get("alternative-content-type"));
			    altContentType.getParameterList().set("charset", charset);
			    byte[] altContents = ((String) partMap.get("alternative-contents")).getBytes(charset);
			    InternetHeaders altHeaders = new InternetHeaders();
			    altHeaders.addHeader("Content-Type", altContentType.toString());
			    altHeaders.addHeader("Content-Transfer-Encoding", "binary");

			    alternative.addBodyPart(new MimeBodyPart(altHeaders, altContents));
			    alternative.addBodyPart(new MimeBodyPart(headers, contents));
				
			    ByteArrayOutputStream os = new ByteArrayOutputStream();
			    alternative.writeTo(os);
			    byte[] _ac = os.toByteArray();
			    bodyStream.write(_ac);
			    partContentType = new ContentType(alternative.getContentType());
			} else {
			    bodyStream.write(_contents.getBytes(charset));
			}
		    }
		    ct = partContentType;
		} else {
		    MimeMultipart multipart = new MimeMultipart(wrapInRfc822 ? "related" : "mixed");
		    boolean hasXkomBasic = false;
		    boolean hasImage = false;
		    boolean hasOther = false;
      		    for (Iterator i = parts.iterator(); i.hasNext();) {
			ContentType _ct = new ContentType((String) ((Map) i.next()).get("content-type"));
			if (_ct.match("text/x-kom-basic"))
			    hasXkomBasic = true;
			else if (_ct.match("image/*")) {
			    hasImage = true;
			} else {
			    hasOther = true;
			}
		    }

		    plainTextWithImages = hasXkomBasic && hasImage && !hasOther;

		    
		    for (Iterator i = parts.iterator(); i.hasNext();) {
			Map partMap = (Map) i.next();
			ContentType partContentType = new ContentType((String) partMap.get("content-type"));
			byte[] contents = null;
			InternetHeaders headers = new InternetHeaders();

			String _contents = (String) partMap.get("contents");
			String _uploaded = (String) partMap.get("uploaded");
			if (_uploaded != null && !_uploaded.equals("")) {
	  		    headers.setHeader("Content-Transfer-Encoding", "base64");
			    File file = new File(tempDir, (String) partMap.get("uploaded"));
	  		    if (!file.getParentFile().equals(tempDir)) {
			    	throw new IOException("uploaded file is not in temp dir!");
			    }
			    partContentType.getParameterList().set("name", (String) partMap.get("filename"));
			    headers.setHeader("Content-Location", (String) partMap.get("filename"));
			    ByteArrayOutputStream bos = new ByteArrayOutputStream();
			    OutputStream os = new Base64.OutputStream(bos);
			    InputStream is = new FileInputStream(file);
			    int read;
			    byte[] buf = new byte[2048];
			    while ((read = is.read(buf, 0, buf.length)) != -1) {
				os.write(buf, 0, read);
			    }
			    os.close();
			    is.close();
			    contents = bos.toByteArray();
			    if (plainTextWithImages) {
				headers.setHeader("Content-Disposition", "inline");
			    }

			} else if (_contents != null && !_contents.equals("")) {
			    String charset = partContentType.getParameterList().get("charset");
	  		    headers.setHeader("Content-Transfer-Encoding", "binary");
			    if (charset == null) charset = "iso-8859-1";

			    contents = _contents.getBytes(charset);
			}
			headers.addHeader("Content-Type", partContentType.toString());

			if (contents != null) {
			    if (partMap.containsKey("alternative-contents")) {
				MimeMultipart alternative = new MimeMultipart("alternative");
				ContentType altContentType = new ContentType((String) partMap.get("alternative-content-type"));
				String charset = altContentType.getParameterList().get("charset");
				if (charset == null) charset = preferences.getString("create-text-charset");
				altContentType.getParameterList().set("charset", charset);
				byte[] altContents = ((String) partMap.get("alternative-contents")).getBytes(charset);
				InternetHeaders altHeaders = new InternetHeaders();
				altHeaders.addHeader("Content-Type", altContentType.toString());
				altHeaders.addHeader("Content-Transfer-Encoding", "binary");

				alternative.addBodyPart(new MimeBodyPart(altHeaders, altContents));
				alternative.addBodyPart(new MimeBodyPart(headers, contents));
				
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				alternative.writeTo(os);
				byte[] _ac = os.toByteArray();
				InternetHeaders _hdrs = new InternetHeaders();
				_hdrs.addHeader("Content-Type", alternative.getContentType());
				_hdrs.addHeader("Content-Transfer-Encoding", "binary");
				multipart.addBodyPart(new MimeBodyPart(_hdrs, _ac));
			    } else {
			    	multipart.addBodyPart(new MimeBodyPart(headers, contents));
			    }
			}
		    }
		    ct = new ContentType(multipart.getContentType());
		    multipart.writeTo(bodyStream);		
		}
		newText = new Text();
		byte[] body = bodyStream.toByteArray();
		if (wrapInRfc822 && parts.size() > 1 && !plainTextWithImages) {
		    ByteArrayOutputStream rfc822os =
	  		new ByteArrayOutputStream();
		    OutputStreamWriter rfc822writer =
			new OutputStreamWriter(rfc822os, "us-ascii");
	            if (ct.match("multipart/*")) {
		    	ct.getParameterList().set("type", "html");
		    }
		    rfc822writer.write("Content-Type: " + ct.toString() + "\r\n");
		    rfc822writer.write("Content-Transfer-Encoding: binary\r\n");
		    rfc822writer.write("\r\n");
		    ContentType oldCt = ct;
		    ct = new ContentType("message/rfc822");
	            if (oldCt.match("multipart/*")) {
		    	ct.getParameterList().set("x-type", "mhtml");
		    }	
		    rfc822writer.flush();
		    rfc822os.write(body);
		    body = rfc822os.toByteArray();
		}

		byte[] contents = new byte[subject.length + body.length + 1];
		System.arraycopy(subject, 0, contents, 0, subject.length);
		System.arraycopy(body, 0, contents, subject.length+1, body.length);
		contents[subject.length] = (byte) '\n';

		newText.setContents(contents);
		newText.getStat().replaceOrAddAuxItem(new AuxItem(AuxItem.tagContentType,
						ct.toString()));
	    }
  	    

	    if (parameters.containsKey("inCommentTo")) {
		String[] cmtToFields;
		if (parameters.get("inCommentTo") instanceof String[]) {
		    cmtToFields = (String[]) parameters.get("inCommentTo");
		} else {
		    cmtToFields = new String[] { (String) parameters.get("inCommentTo") };
		}
		for (int i=0; i < cmtToFields.length; i++) {
		    int textNo = Integer.parseInt(cmtToFields[i]);
		    newText.addCommented(textNo);

		    TextStat commentedTextStat = lyskom.getTextStat(textNo);
		    int[] _recipients = commentedTextStat.getRecipients();
		    for (int j=0; !explicitRecipients && j < _recipients.length; j++) {
	    		Conference conf = lyskom.getConfStat(_recipients[j]);
	    	    	if (conf.getType().original()) {
			    int superconf = conf.getSuperConf();
			    if (superconf > 0) {
	  			Debug.println("added superconf as comment-to recipient: " + _recipients[j] +
					" -> " + superconf);
		    	    	newText.addRecipient(superconf);
			    } else {
		   	    	throw new RuntimeException("Du får inte skriva kommentarer i " +
				 	      conf.getNameString());
			    }
	    	    	} else {
			    if (!newText.getStat().hasRecipient(_recipients[j])) {
	  			Debug.println("added implicit comment-to recipient: " + _recipients[j]);
		            	newText.addRecipient(_recipients[j]);
			    } else {
	  			Debug.println("avoiding duplicate recipient: " + _recipients[j]);
			    }
		    	}
		    }

		    // so we won't keep the old text's comments status
		    lyskom.purgeTextCache(textNo); 

		}
	    }
	    if (parameters.containsKey("footnoteTo")) {
                Object obj = parameters.get("footnoteTo");
       		String[] fntToFields;
		if (obj instanceof String)
		    fntToFields = new String[] { (String) obj };
		else 
		    fntToFields = (String[]) obj;

		for (int i=0; i < fntToFields.length; i++) {
		    int textNo = Integer.parseInt(fntToFields[i]);
		    newText.addFootnoted(textNo);
                    Debug.println("new text is a footnote-to: " + textNo);
		    // so we won't keep the old text's comments status
		    lyskom.purgeTextCache(textNo); 
		}
	    }
	    for (Iterator i=recipients.iterator(); i.hasNext();) {
	        int confNo = ((ConfInfo) i.next()).getNo();
		if (!newText.getStat().hasRecipient(confNo))
		    newText.addRecipient(confNo);
	    }
	    for (Iterator i=ccRecipients.iterator(); i.hasNext();) {
	        int confNo = ((ConfInfo) i.next()).getNo();
		if (!newText.getStat().hasRecipient(confNo))
		    newText.addCcRecipient(confNo);
	    }

	    newTextNo = lyskom.createText(newText);
	    TextStat newTextStat = lyskom.getTextStat(newTextNo);
	    if (newTextNo > 0) {
		if (commonPreferences.getBoolean("created-texts-are-read")) {
		    int[] recipientNos = newTextStat.getRecipients();
		    int[] ccRecps = newTextStat.getCcRecipients();
		    int[] allRecps = new int[recipientNos.length+ccRecps.length];
		    System.arraycopy(recipientNos, 0, allRecps, 0, recipientNos.length);
		    System.arraycopy(ccRecps, 0, allRecps, recipientNos.length, ccRecps.length);
		    recipientNos = allRecps;
		    for (int i=0; i < recipientNos.length; i++) {
			try {
			    lyskom.markAsRead(recipientNos[i], new int[] { newTextStat.getLocal(recipientNos[i]) });	
			} catch (RpcFailure ex1) {
			    if (ex1.getError() != Rpc.E_not_member)
				throw ex1;
			}
		    }
		}
%>
	        <div class="statusSuccess">Text nummer <%= textLink(request, lyskom, newTextNo, false) %> är skapad.</div>
<%
	        if (parameter(parameters, "changePresentation") != null) {
		    int confNo = Integer.parseInt(parameter(parameters, "changePresentation"));
		    try {
			lyskom.setPresentation(confNo, newTextNo);
%>
 		    	<div class="statusSuccess">Ny presentation för <%=lookupName(lyskom, confNo, true)%> är <%=textLink(request, lyskom, newTextNo)%>.</div>
<%
		    } catch (RpcFailure ex1) {
			if (ex1.getError() == Rpc.E_permission_denied) {
			    out.println("<div class=\"statusError\">Du får inte ändra presentation för möte " +
				lookupName(lyskom, ex1.getErrorStatus(), true) + ".</div>");
			    lyskom.deleteText(newTextNo);
			    out.println("<div class=\"statusError\">Text nummer " + newTextNo + " är borttagen.</div>");
			} else {
			    throw ex1;
			}
		    }
	    	}

	    }
	}
	if (errors.length() > 0)
	    out.println("<div class=\"statusError\">" + errors + "</div>");
        request.setAttribute("new-text-no", new Integer(newTextNo));
    }
