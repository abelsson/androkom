<%@ page language='java' import='nu.dll.lyskom.*, java.text.*, java.util.*' %>\
<%@ page import='java.util.regex.*, java.io.*, nu.dll.app.weblatte.*, java.net.URLDecoder' %>\
<%!
    String basePath = "/lyskom/"; // the absolute path on the webserver
    String appPath = "/lyskom/"; // the weblatte root within the web application

    public KomPreferences preferences(Session lyskom, String blockName) throws IOException, RpcFailure {
        if (lyskom == null) return null;
	KomPreferences prefs = (KomPreferences) lyskom.getAttribute("weblatte.preferences." + blockName);
	if (prefs == null) {
	    Hollerith data = null;
	    if (lyskom.getLoggedIn()) {
		UserArea userArea = lyskom.getUserArea();
		data = userArea.getBlock(blockName);
	    }
	    if (data != null) {	
		prefs = new KomPreferences(new HollerithMap(data), blockName);
	    } else {
	    	prefs = new KomPreferences(new HollerithMap(lyskom.getServerEncoding()), blockName);
	    }
	    lyskom.setAttribute("weblatte.preferences." + blockName, prefs);
	}
	return prefs;
    }

    public void clearPreferenceCache(Session lyskom) {
	lyskom.removeAttributes("weblatte\\.preferences\\..*");
    }

    public Map parseQueryString(String query, String charset)
    throws UnsupportedEncodingException {
	if (query == null) return new HashMap();
	StringTokenizer st = new StringTokenizer(query, "&");
	Map result = new HashMap();
	while (st.hasMoreTokens()) {
	    String keyvalue = st.nextToken();
	    StringTokenizer kvt = new StringTokenizer(keyvalue, "=");
	    String key = URLDecoder.decode(kvt.nextToken(), charset);
	    String value = "";
	    if (kvt.hasMoreTokens()) 
		value = URLDecoder.decode(kvt.nextToken(), charset);
	    result.put(key, value);
	}
	return result;
    }

    public String parameter(Map map, String key) {
	return (String) map.get(key);
    }

    final static int SORT_FILEID = 1, SORT_MODIFIED = 2, SORT_MUGNAME = 3;
    Collator collator = Collator.getInstance(new Locale("sv", "SE"));
    Pattern komNamePat = Pattern.compile("\\(.*?\\)");

    public int compareFiles(File f1, File f2, int sortBy) throws NumberFormatException {
	if (sortBy == SORT_MUGNAME) {
	    try {
		LineNumberReader reader = 
			new LineNumberReader(new InputStreamReader(new FileInputStream(f1)));
		String name1 = komNamePat.matcher(reader.readLine()).replaceAll("").trim();
		reader.close();
		reader = 
			new LineNumberReader(new InputStreamReader(new FileInputStream(f2)));
		String name2 = komNamePat.matcher(reader.readLine()).replaceAll("").trim();
		reader.close();
		return collator.compare(name1, name2);
	    } catch (IOException ex1) {
		throw new RuntimeException(ex1);
	    }

	}
	int n1 = sortBy == SORT_FILEID ? Integer.parseInt(f1.getName().substring(0, f1.getName().indexOf("."))) : (int) (f1.lastModified()/1000l);
	int n2 = sortBy == SORT_FILEID ? Integer.parseInt(f2.getName().substring(0, f2.getName().indexOf("."))) : (int) (f2.lastModified()/1000l);
	int r = 0;
	if (n1 < n2) r = -1;
	if (n1 > n2) r = 1;
	if (sortBy == SORT_MODIFIED) r = -r; // reverse
	return r;
    }


    static class Mugshot {
	public String id = null, name = null, image = null;
	public File file;
	public Mugshot(File _file) throws IOException {
	    this.file = _file;
	    this.id = file.getName().substring(0,file.getName().indexOf("."));
	    LineNumberReader reader = new LineNumberReader(new FileReader(file));
	    this.name = reader.readLine();
	    this.image = reader.readLine();
	}
    }

    int rightMargin = Integer.getInteger("lattekom.linewrap", new Integer(70)).intValue();
    void wrapText(Text newText) throws UnsupportedEncodingException {
	java.util.List rows = newText.getBodyList();
	java.util.List newRows = new LinkedList();

	Iterator i = rows.iterator();
	while (i.hasNext()) {
	    String row = (String) i.next();
	    boolean skip = false;
	    while (!skip && row.length() > rightMargin) {
		int cutAt = row.lastIndexOf(' ', rightMargin);
		if (cutAt == -1) { // can't break row
		    skip = true;
		    continue;
		}
		String wrappedRow = row.substring(0, cutAt);
		row = row.substring(cutAt+1);
		newRows.add(wrappedRow);
	    }
	    newRows.add(row);
	}

	i = newRows.iterator();
	StringBuffer newBody = new StringBuffer();
	while (i.hasNext()) {
	    String row = (String) i.next();
	    newBody.append(row + "\n");
	}
	newText.setContents((new String(newText.getSubject(), newText.getCharset()) + "\n" +
			 newBody.toString()).getBytes(newText.getCharset()));	
    }

    String makeAbsoluteURL(String path) {
        if ("".equals(path)) return basePath;
        if (path.startsWith("/")) path = path.substring(1);
        return basePath + path;
    }

    String serverShort(Session lyskom) {
	String host = lyskom.getServer().toLowerCase();
        for (Iterator i = Servers.list.iterator(); i.hasNext();) {
            KomServer s = (KomServer) i.next();
            if (host.equals(s.hostname)) return s.name;
        }
	return lyskom.getServer();
    }

    // makes the given string suitable for HTML presentation
    Pattern weblinkPat = Pattern.compile("(http://[^ \t\r\n\\\\\\[><,!]{3,}[^ ?.,)>])");
    String htmlize(String s) {
	s = s.replaceAll("&", "&amp;").replaceAll("<", "&lt;");
	return weblinkPat.matcher(s).replaceAll("<a target=\"_blank\" href=\"$1\">$1</a>");
    }

    Pattern dqesc = Pattern.compile("\"");
    Pattern sqesc = Pattern.compile("'");
    public String dqescHtml(String s) {
	return dqesc.matcher(s).replaceAll("&quot;");
    }
    public String dqescJS(String s) {
	return dqesc.matcher(s).replaceAll("\\\\\"");
    }
    public String sqescJS(String s) {
	return sqesc.matcher(s).replaceAll("\\\\'");
    }

    public String jsTitle(String title) {
	return new StringBuffer("<script language=\"JavaScript1.2\">")
	    .append("document.title = \"")
	    .append(dqescJS(title))
	    .append("\";</script>").toString();
    }


    String lookupNamePlain(Session lyskom, int number)
    throws RpcFailure, IOException {
	String name = "[" + number + "]";
	UConference uconf = null;
	try {
	    uconf = lyskom.getUConfStat(number);
	    name = lyskom.toString(uconf.getName());
	}  catch (RpcFailure ex1) {
	    if (ex1.getError() != Rpc.E_undefined_conference)
		throw ex1;
	}
	return name;
    }

    String lookupName(Session lyskom, int number, boolean useHtml)
    throws RpcFailure, IOException {
	if (useHtml) return lookupNameHtml(lyskom, number);
	else return lookupNamePlain(lyskom, number);
    }

    String lookupName(Session lyskom, int number)
    throws RpcFailure, IOException {
	return lookupName(lyskom, number, false);
    }


    String lookupNameHtml(Session lyskom, int number)
    throws RpcFailure, IOException {
	String name = "[" + number + "]";
	Conference conf = null;
	try {
	    conf = lyskom.getConfStat(number);
	    name = lyskom.toString(conf.getName());
	} catch (RpcFailure ex1) {
	    if (ex1.getError() != Rpc.E_undefined_conference)
		throw ex1;
	}
	if (conf != null) {
	    boolean isMe = lyskom.getLoggedIn() && lyskom.getMyPerson().getNo() == number;
	    KomPreferences prefs = preferences(lyskom, "weblatte");
	    boolean bold = isMe && prefs.getBoolean("my-name-in-bold");
	    return "<span title=\"" + (conf.getType().getBitAt(ConfType.letterbox) ? "Person " : "Möte ") + conf.getNo() + "\" onMouseOut=\"context_out()\" onMouseOver=\"context_in(" + number + ", " + conf.getType().getBitAt(ConfType.letterbox) + ", false, '" + sqescJS(lyskom.toString(conf.getName())) + "');\">" + (bold ? "<b>" : "") + htmlize(name) + (bold ? "</b>" : "") + "</span>";
	} else {
	    return htmlize(name);
	}
    }


    Random rnd = new Random();
    SimpleDateFormat df = new SimpleDateFormat("EEEEE d MMMMM yyyy', klockan 'HH:mm", new Locale("sv", "se"));

    String textLink(HttpServletRequest request, Session lyskom, int textNo)
    throws RpcFailure, IOException {
	return textLink(request, lyskom, textNo, true);
    }

    String ambiguousNameMsg(Session lyskom, AmbiguousNameException ex) 
    throws RpcFailure, IOException {
	return ambiguousNameMsg(lyskom, null, ex);
    }

    String ambiguousNameMsg(Session lyskom, String name, AmbiguousNameException ex)
    throws RpcFailure, IOException {
	StringBuffer buf = new StringBuffer();
	buf.append("<p class=\"statusError\">Fel: namnet är flertydigt. Följande namn matchar:\n");
	buf.append("<ul>\n");
	ConfInfo[] names = ex.getPossibleNames();
	for (int i=0; i < names.length; i++) 
	    buf.append("\t<li>" + lookupName(lyskom, names[i].getNo(), true) + "\n");
	buf.append("</ul>");
	return buf.toString();
	
    }

    ConfInfo lookupName(Session lyskom, String name, boolean wantPersons, boolean wantConferences)
    throws IOException, RpcFailure, AmbiguousNameException {
	if (name.startsWith("#")) {
	    // this is quite ugly and only works with readable names
	    String nameStr = name.substring(1);
	    int confNo = Integer.parseInt(nameStr);
	    return lyskom.lookupName(lookupName(lyskom, confNo), wantPersons, wantConferences)[0];
	}
	ConfInfo[] names = lyskom.lookupName(name, wantPersons, wantConferences);
	if (names.length == 0) return null;
	if (names.length > 1) throw new AmbiguousNameException(names);
	return names[0];
    }

    /* hack to fallback to js redirect when buffer is small
     * and the response has already been committed to the client.
     * returns true if a HTTP redirect has been done, signalling to
     * the caller that it may stop sending further data to the client
     */
    boolean redirectHack(HttpServletResponse response, JspWriter out, String target) throws IOException {
        if (!response.isCommitted()) {
	    response.sendRedirect(target);
	    return true;
        } else {
	    out.println("<script language=\"JavaScript1.2\">");
	    out.println("document.location.href=\"" + basePath + "sessions.jsp?loggedOut");
	    out.println("</script>");
	    return false;
        }
    }

    String textLink(HttpServletRequest request, Session lyskom, int textNo, boolean includeName)
    throws RpcFailure, IOException {
	StringBuffer sb = new StringBuffer()
		.append("<a href=\"")
		/*.append(myURI(request))*/
		.append("/lyskom/")
		.append("?text=")
		.append(textNo);
	if (request.getParameter("conference") != null) {
		sb.append("&conference=")
		.append(request.getParameter("conference"));
	}
	sb.append("\" ")
		.append("onMouseOver=\"context_in(").append(textNo).append(", false, true);\" ")
		.append("onMouseOut=\"context_out()\" ")
		.append(">")
		.append(textNo)
		.append("</a>");
	if (includeName) {
	    try {
		String a = lookupName(lyskom, lyskom.getTextStat(textNo).getAuthor(), true);
		sb.append(" av ").append(a);
	    } catch (RpcFailure ex1) {
		if (ex1.getError() != Rpc.E_no_such_text)
		    throw ex1;
	    } 
	}
	return sb.toString();
    }

    String myURI(HttpServletRequest request) {
	String setURI = (String) request.getAttribute("set-uri");
	if (setURI != null) return setURI;
	return request.getRequestURI();
    }

    String utf8ize(String s) throws UnsupportedEncodingException {
	return new String(s.getBytes("utf-8"), "iso-8859-1");
    }
    String entitize(String s) {
	return s.
	replaceAll("å", "&#229;").
	replaceAll("ä", "&#228;").
	replaceAll("ö", "&#246;").
	replaceAll("Å", "&#197;").
	replaceAll("Ä", "&#196;").
	replaceAll("Ö", "&#214;");
	
    }

    String komStrip(String s) {
	return s.replaceAll(" *\\(.*?\\) *", "").trim();
    }

    class AmbiguousNameException extends RuntimeException {
	ConfInfo[] possibleNames;
	public AmbiguousNameException(ConfInfo[] names) {
	    this.possibleNames = names;
	}
	public ConfInfo[] getPossibleNames() {
	    return possibleNames;
	}
    }

    static class MessageReceiver implements AsynchMessageReceiver {
	LinkedList list;
	public MessageReceiver(LinkedList list) {
	    this.list = list;
	}

	public void asynchMessage(AsynchMessage m) {
	    synchronized (list) {
		list.addLast(m);
	    }
	}
    }
%>\
<%

String dir = getServletContext().getRealPath("/lyskom/bilder/");
UserArea userArea = null;
KomPreferences commonPreferences = null;
KomPreferences preferences = null;
boolean debug = Debug.ENABLED || request.getParameter("debug") != null;
SessionWrapper lyskomWrapper = (SessionWrapper) session.getAttribute("lyskom");
Session lyskom = lyskomWrapper != null ? lyskomWrapper.getSession() : null;
if (Debug.ENABLED) {
    Debug.println("wrapper: " + Integer.toHexString(System.identityHashCode(lyskomWrapper)));
    Debug.println("lyskom: " + Integer.toHexString(System.identityHashCode(lyskom)));
}
%>\
