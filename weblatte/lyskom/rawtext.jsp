<jsp:directive.page language='java' import='nu.dll.lyskom.*, javax.mail.BodyPart, javax.mail.internet.*' /><jsp:directive.include file='kom.jsp' /><%
    if (lyskom == null) {
	response.sendRedirect("/lyskom/?error=nosession");
	return;
    }
    Debug.println("rawtext.jsp START. Query: " + request.getQueryString());
    int textNumber = Integer.parseInt(request.getParameter("text"));
    Text text = lyskom.getText(textNumber);
    if (new ContentType(text.getStat().getContentType()).match("multipart/*") &&
        (request.getParameter("part") != null ||
	 request.getParameter("name") != null)) {
        int partNo = request.getParameter("part") != null ?
		Integer.parseInt(request.getParameter("part")) : -1;
        MimeMultipart multipart = new MimeMultipart(text);
	BodyPart part = null;
	if (partNo == -1) {
	    String name = request.getParameter("name");
	    for (int i=0; part == null && i < multipart.getCount(); i++) {
		BodyPart _part = multipart.getBodyPart(i);
                String partName = _part.getFileName();
           	if (partName == null) {
		    String[] locHeaders = _part.getHeader("Content-Location");
		    if (locHeaders != null && locHeaders.length > 0) {
			partName = locHeaders[0];
		    } else {
			Debug.println("part " + i + " has no name nor " +
				      "content-location");
		    }
		}
		Debug.println("examining " + i + " with name " + partName + 
			" ?= " + name);
		if (name.equals(partName)) {
		    part = _part;
		}
	    }
	    if (part == null) {
		response.setContentType("text/plain");
		response.setStatus(404);
		out.println("no such file");
		return;
	    }
	}
        if (partNo > multipart.getCount()) {
	    response.setStatus(404);
	    response.setContentType("text/plain");
	    out.println("part not found");
	    return;
	} else if (partNo > -1) {
	    part = multipart.getBodyPart(partNo);
	} else if (part == null) {
	    throw new ServletException("part not found.");
	}
	if (part.isMimeType("multipart/*") && request.getParameter("subpart") != null) {
	    MimeMultipart alternative = new MimeMultipart(new MimePartDataSource((MimeBodyPart) part));
	    part = alternative.getBodyPart(Integer.parseInt(request.getParameter("subpart")));
	}
	Debug.println("sending part " + part + " to client.");
	InputStream is = part.getInputStream();
	ContentType partContentType = new ContentType(part.getContentType());
	response.setContentType(partContentType.toString());

	if (partContentType.match("text/html") && request.getParameter("sanitize") != null) {
	    String charset = partContentType.getParameterList().get("charset");
	    if (charset == null) charset = "iso-8859-1";
	    HtmlSanitizer.parse(text.getNo(), is, response.getOutputStream(), charset);
	} else {
	    if (request.getParameter("download") != null) {
		response.setHeader("Content-Disposition", "attachment; filename=\"" + 
			part.getFileName() + "\"");
	    }	
	    OutputStream os = response.getOutputStream();
	    byte[] buf = new byte[1024];

	    int readBytes;
	    while ((readBytes = is.read(buf)) != -1)
		os.write(buf, 0, readBytes);

	    is.close();
	}
	return;
	
    }
    Hollerith[] _contentType = text.getAuxData(AuxItem.tagContentType);
    String contentType = "text/x-kom-basic";
    if (_contentType != null && _contentType.length > 0) {
	contentType = _contentType[0].getContentString();
    }
    if (request.getParameter("forceContentType") != null) {
        contentType = request.getParameter("forceContentType");
    }
    response.setContentType(contentType);
    OutputStream os = response.getOutputStream();
    if (text instanceof BigText) {
	BigText bt = (BigText) text;
	HollerithStream hs = bt.getBodyStream();
	InputStream is = hs.getStream();
	int bytesRead = 0;
	byte[] buf = new byte[1024];
	while (bytesRead < hs.getSize()) {
	    int bytesToRead = buf.length;
	    if ((hs.getSize() - bytesRead) < buf.length) {
		bytesToRead = hs.getSize()-bytesRead;
	    }
	    int read = is.read(buf, 0, bytesToRead);
	    bytesRead += read;
	    os.write(buf, 0, read);
	}
	hs.setExhausted();
    } else {
	os.write(text.getBody());
    }
    os.close();
%>
