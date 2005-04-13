<%@ page language='java' import='nu.dll.lyskom.*, java.text.*, java.util.*' %>\
<%@ page import='java.util.regex.*, java.io.*, nu.dll.app.weblatte.*, java.net.URLDecoder' %>\
<%!
    static boolean experimental = Boolean.getBoolean("weblatte.experimental");
    static boolean vemArVem = Boolean.getBoolean("weblatte.vem-ar-vem");
    static File tempDir = new File(System.getProperty("weblatte.temp-dir", "/tmp"));
    static String baseHost = System.getProperty("weblatte.base-host", "kom.dll.nu");
    static String basePath = System.getProperty("weblatte.basepath", "/lyskom/"); // the absolute path on the webserver
    static String appPath = System.getProperty("weblatte.webapp-path", "/lyskom/"); // the weblatte root within the web application
    static {
        Debug.println("Weblatte configuration: basePath: " + basePath + ", appPath: " + appPath);
    }

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
	    LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), "iso-8859-1"));
	    this.name = reader.readLine();
	    this.image = reader.readLine();
	}
    }


    void wrapText(Text newText) throws UnsupportedEncodingException {
	newText.setContents((new String(newText.getSubject(), newText.getCharset()) + "\n" +
			 newText.getWrapped()).getBytes(newText.getCharset()));	
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


    Pattern weblinkPat = Pattern.compile("((?:https*://(?:(?:(?:(?:(?:[a-zA-Z\\d](?:(?:[a-zA-Z\\d]|-)*[a-zA-Z\\d])?)\\.)*(?:[a-zA-Z](?:(?:[a-zA-Z\\d]|-)*[a-zA-Z\\d])?))|(?:(?:\\d+)(?:\\.(?:\\d+)){3}))(?::(?:\\d+))?)(?:/(?:(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),#~\\?/]|(?:%[a-fA-F\\d]{2}))|[;:@&=])*)(?:/(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),#~\\?/]|(?:%[a-fA-F\\d]{2}))|[;:@&=])*))*)(?:\\?(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),#~\\?/]|(?:%[a-fA-F\\d]{2}))|[;:@&=])*))?)?)|(?:ftp://(?:(?:(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),~\\?/]|(?:%[a-fA-F\\d]{2}))|[;?&=])*)(?::(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),#~\\?/]|(?:%[a-fA-F\\d]{2}))|[;?&=])*))?@)?(?:(?:(?:(?:(?:[a-zA-Z\\d](?:(?:[a-zA-Z\\d]|-)*[a-zA-Z\\d])?)\\.)*(?:[a-zA-Z](?:(?:[a-zA-Z\\d]|-)*[a-zA-Z\\d])?))|(?:(?:\\d+)(?:\\.(?:\\d+)){3}))(?::(?:\\d+))?))(?:/(?:(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),~\\?/]|(?:%[a-fA-F\\d]{2}))|[?:@&=])*)(?:/(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),~\\?/]|(?:%[a-fA-F\\d]{2}))|[?:@&=])*))*)(?:;type=[AIDaid])?)?)|(?:mailto:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),;/?:@&=]|(?:%[a-fA-F\\d]{2}))+)))");

    Pattern confNamePattern = Pattern.compile("<(?:(?:möte|person)|(?:m|p)) +(\\d+)[^>]*>", Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    Pattern confNamePatternText = Pattern.compile("^(.*)<((?:möte|person)|(?:m|p)) +(\\d+)[^>]*>(.*)$", Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE|Pattern.MULTILINE);
    // match digits surrounded by whitespace, newlines and/or end of string
    Pattern textLinkPattern = Pattern.compile("(?:^(\\d+)\\s+.*$|^(\\d+)$|^\\s+(\\d+)$)", Pattern.MULTILINE);
    Pattern dqesc = Pattern.compile("\"");
    Pattern sqesc = Pattern.compile("'");
    //Pattern weblinkPat = Pattern.compile("(http://[^ \t\r\n\\\\\\[><,!]{3,}[^ ?.,)>])");

    String htmlize(String s, boolean makeHtml) {
	try {
	    return htmlize(null, s, makeHtml);
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.toString());
	} catch (RpcFailure ex2) {
	    throw new RuntimeException(ex2.toString());
	}
    }

    String htmlize(Session lyskom, String s, boolean makeHtml) throws IOException, RpcFailure {
	if (makeHtml) {
	    StringBuffer sb = new StringBuffer();
	    Matcher m = confNamePatternText.matcher(s);
	    //log("htmlize() lyskom=" + lyskom + ", s=\"" + s.substring(0, s.length() > 10 ? 10 : s.length()) + "\", makeHtml=" + makeHtml);
	    while (m.find()) {
		try {
		    int confNo = Integer.parseInt(m.group(3));
		    Matcher submatcher = confNamePattern.matcher(m.group());
		    String match = submatcher.find() ? submatcher.group() : "[Weblatte Regex Error!]";
		    String replacement;
		    if (lyskom != null) {
  		    	replacement = lookupNameHtml(lyskom, confNo, false, submatcher.group(0));
			if (Debug.ENABLED) log("htmlize(): replacement: " + replacement);
		    } else {
			replacement = confLink(true, m.group(2).toLowerCase().startsWith("p"), confNo, null, false, match);
		    }
		    String before = m.group(1);
		    String after = m.group(4);
		    // we should use 1.5.0's Matcher.quoteReplacement() on replacement instead.
		    replacement = entitize(before) + replacement.replaceAll("'", "\\\\\\'") + entitize(after);
		    m.appendReplacement(sb, replacement);
		} catch (NumberFormatException ex1) {
		    ex1.printStackTrace();
		    log("htmlize() warning: unexpected unparsable conf-number: " + m.group());
		    m.appendReplacement(sb, m.group(0));
		}
	    }
	    // we need to put the tail in a new stringbuffer to be able to escape "<" etc
	    // separately.
	    StringBuffer foo = new StringBuffer();
	    m.appendTail(foo);
	    sb.append(entitize(foo.toString()));
	    s = sb.toString();
	    s = weblinkPat.matcher(s).replaceAll("<a target=\"_blank\" href=\"$1\">$1</a>");
	} else {
	    s = entitize(s);
	}
	return s;
    }

    /** makes the given string suitable for HTML presentation */
    String htmlize(String s) {
	return htmlize(s, true);
    }

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

    String lookupNameComplete(Session lyskom, int number)
    throws RpcFailure, IOException {
	Conference conf = lyskom.getConfStat(number);
	return "<" + (conf.getType().letterbox() ? "Person" : "Möte") + " " + number + ": " + conf.getNameString() + ">";
    }

    String lookupNamePlain(Session lyskom, int number)
    throws RpcFailure, IOException {
	String name = "[" + number + "]";
	if (number == 0) return "[Anonym]";

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
	return lookupName(lyskom, number, useHtml, false);
    }


    String lookupName(Session lyskom, int number, boolean useHtml, boolean disablePopup)
    throws RpcFailure, IOException {
	if (useHtml) return lookupNameHtml(lyskom, number, disablePopup);
	else return lookupNamePlain(lyskom, number);
    }

    String lookupName(Session lyskom, int number)
    throws RpcFailure, IOException {
	return lookupName(lyskom, number, false);
    }

    String lookupNameHtml(Session lyskom, int number, boolean disablePopup)
    throws RpcFailure, IOException {
	return lookupNameHtml(lyskom, number, disablePopup, null);
    }

    String lookupNameHtml(Session lyskom, int number, boolean disablePopup, String linkText)
    throws RpcFailure, IOException {
	String name = "[" + number + "]";
	Conference conf = null;
	try {
	    conf = number != 0 ? lyskom.getConfStat(number) : null;
	} catch (RpcFailure ex1) {
	    if (ex1.getError() != Rpc.E_undefined_conference)
		throw ex1;
	}
	if (conf != null) {
	    name = lyskom.toString(conf.getName());
	    boolean isMe = lyskom.getLoggedIn() && lyskom.getMyPerson().getNo() == number;
	    KomPreferences prefs = preferences(lyskom, "weblatte");
	    boolean bold = isMe && prefs.getBoolean("my-name-in-bold");
            boolean letterbox = conf.getType().getBitAt(ConfType.letterbox);
	    return confLink(disablePopup, letterbox, conf.getNo(), conf.getNameString(), bold, linkText);
	} else {
	    if (number == 0) return "<span class=\"letterbox-name\">" + (linkText == null ? "[Anonym]" : linkText) + "</span>";
	    return entitize(linkText == null ? name : linkText);
	}
    }

    String confLink(boolean disablePopup, boolean letterbox, int number, String name, boolean bold, String linkText) {
	if (Debug.ENABLED) log("confLink(" + disablePopup + ", " + letterbox + ", " + number + ", " + (name != null ? "\"" + name + "\"" : "null") + ", " + bold + ", " + linkText + ")");
	StringBuffer buf = new StringBuffer();
	buf.append("<span ");
	buf.append(disablePopup ? "" : "onClick=\"showmenuie5(event, true);\"");
	buf.append(" class=\"");
	buf.append(letterbox ? "letterbox-name" : "conference-name");
	buf.append("\" title=\"");
	buf.append(letterbox ? "&lt;Person " : "&lt;Möte ");
	buf.append(number);
	buf.append(name != null ? (": " + entitize(name)) : "");
	buf.append("&gt;\" onMouseOut=\"context_out()\" onMouseOver=\"context_in(");
	buf.append(number);
	buf.append(", ");
	buf.append(letterbox);
	buf.append(", false, '");
	String ciarg = sqescJS(name != null ? name : linkText);
	buf.append(ciarg);
	buf.append("');\">");
	buf.append(bold ? "<b>" : "");
	buf.append(entitize(linkText != null ? linkText : name));
	buf.append(bold ? "</b>" : "");
	buf.append("</span>");
	return buf.toString();
    }


    Random rnd = new Random();
    SimpleDateFormat df = new SimpleDateFormat("EEEEE d MMMMM yyyy', klockan 'HH:mm", new Locale("sv", "se"));
    SimpleDateFormat shortdf = new SimpleDateFormat("d/M -yy' 'HH:mm", new Locale("sv", "se"));

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

	// match "<Möte 4711: conference name>" type strings
	Matcher m = confNamePattern.matcher(name);
	if (m.matches()) {
	    try {
		return lyskom.getConfStat(Integer.parseInt(m.group(1))).getConfInfo();
	    } catch (NumberFormatException ex1) {
		throw new RuntimeException(ex1.toString());
	    }
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
	return textLink(request, lyskom, textNo, includeName, null);
    }

    String textLink(HttpServletRequest request, Session lyskom, int textNo, boolean includeName, String linkText)
    throws RpcFailure, IOException {
	StringBuffer sb = new StringBuffer()
		.append("<a href=\"")
		/*.append(myURI(request))*/
		.append(basePath)
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
		.append(linkText == null ? ""+textNo : linkText)
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
	return request.getRequestURI().replaceAll("index\\.jsp", "");
    }

    String utf8ize(String s) throws UnsupportedEncodingException {
	return new String(s.getBytes("utf-8"), "iso-8859-1");
    }
    String entitize(String s) {
	return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll("\"", "&quot;").
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

    public String xmlConference(Session lyskom, int confNo)
    throws IOException, RpcFailure {
	StringBuffer buf = new StringBuffer();
	Conference conf = lyskom.getConfStat(confNo);
	buf.append("<conference id=\"" + confNo + "\" letterbox=\"" + 
		conf.getType().letterbox() + "\">");
	buf.append(lookupName(lyskom, confNo, false));
	buf.append("</conference>");
	return buf.toString();
    }

    public String xmlTextRef(Session lyskom, int textNo) 
    throws IOException, RpcFailure {
	StringBuffer buf = new StringBuffer();
	TextStat ts = lyskom.getTextStat(textNo);
	buf.append("<text-ref>");
	buf.append("<text-no>").append(textNo).append("</text-no>");
	buf.append("<author>" + xmlConference(lyskom, ts.getAuthor()) + "</author>");
	buf.append("</text-ref>");
	return buf.toString();
    }

    public String xmlTimeRef(Date date) {
	return "<formatted-time>" + df.format(date) + "</formatted-time>";
    }

    String getSessionId(SessionWrapper wrapper) {
	return Integer.toHexString(System.identityHashCode(wrapper));
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
String dir = getServletContext().getRealPath(appPath + "/bilder/");
UserArea userArea = null;
KomPreferences commonPreferences = null;
KomPreferences preferences = null;
boolean debug = (Debug.ENABLED && Boolean.getBoolean("weblatte.debug")) || request.getParameter("debug") != null;
String serverName = request.getServerName();
SessionWrapper lyskomWrapper = null;
Session lyskom = null;
if (serverName != null && serverName.startsWith("s-")) {
    List sessions = new LinkedList();
    List susp_sessions = (List) session.getAttribute("lyskom.suspended");
    if (susp_sessions != null) {
	sessions.addAll(susp_sessions);
    } else {
	susp_sessions = new LinkedList();
	session.setAttribute("lyskom.suspended", susp_sessions);
    }
    List act_sessions = (List) session.getAttribute("lyskom.active");
    if (act_sessions != null) {
	sessions.addAll(act_sessions);
    } else {
	act_sessions = new LinkedList();
	session.setAttribute("lyskom.active", act_sessions);
    }
    int dotOffset = serverName.indexOf(".");
    String requestedId = serverName.substring(2, dotOffset);
    for (Iterator i = sessions.iterator();i.hasNext();) {
        SessionWrapper w = (SessionWrapper) i.next();
        String id = getSessionId(w);
        if (requestedId.equals(id)) {
            lyskomWrapper = w;
            lyskom = lyskomWrapper.getSession();
	    susp_sessions.remove(w);
	    if (!act_sessions.contains(w)) act_sessions.add(w);
	    w.setSuspended(true);
	    SessionWrapper oldLyskom = (SessionWrapper) session.getAttribute("lyskom");
	    if (oldLyskom != null && w != oldLyskom) {
		oldLyskom.setSuspended(true);
	    }
	
	    session.setAttribute("lyskom", w);
	    session.setAttribute("LysKOMauthenticated", new Boolean(lyskom.getLoggedIn()));
        }
    }
    if (lyskom == null && requestedId != null) {
	if (Debug.ENABLED) log("Requested session ID " + requestedId + " not found.");
	response.sendRedirect("http://" + baseHost + basePath + "sessions.jsp");
	return;
    }
}

if (lyskomWrapper == null || lyskom == null) {
    lyskomWrapper = (SessionWrapper) session.getAttribute("lyskom");
    lyskom = lyskomWrapper != null ? lyskomWrapper.getSession() : null;
}
boolean minimalistic = lyskom != null && Boolean.TRUE.equals(lyskom.getAttribute("weblatte.minimalistic"));
if (Debug.ENABLED) {
    Debug.println("wrapper: " + Integer.toHexString(System.identityHashCode(lyskomWrapper)));
    Debug.println("lyskom: " + Integer.toHexString(System.identityHashCode(lyskom)));
}
%>\
