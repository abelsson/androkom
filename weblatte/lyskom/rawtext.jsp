<jsp:directive.page language='java' import='nu.dll.lyskom.*' /><jsp:directive.include file='kom.jsp' /><%
    Session lyskom = (Session) session.getAttribute("lyskom");
    if (lyskom == null) {
	response.sendRedirect("/lyskom/?error=nosession");
	return;
    }
    int textNumber = Integer.parseInt(request.getParameter("text"));
    Text text = lyskom.getText(textNumber);
    Hollerith[] _contentType = text.getAuxData(AuxItem.tagContentType);
    String contentType = "text/x-kom-basic";
    if (_contentType != null && _contentType.length > 0) {
	contentType = _contentType[0].getContentString();
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
