package nu.dll.app.weblatte;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

import java.util.regex.*;
import java.util.*;
import java.io.*;
import java.net.URLEncoder;
import java.net.URLDecoder;

public class HtmlSanitizer extends HTMLEditorKit.ParserCallback {


    HTML.Attribute[] alwaysAllowedAttributesArray = {
	HTML.Attribute.BGCOLOR,
	HTML.Attribute.STYLE,
	HTML.Attribute.CLASS,
	HTML.Attribute.TITLE,
	HTML.Attribute.WIDTH,
	HTML.Attribute.HEIGHT,
	HTML.Attribute.ALIGN,
	HTML.Attribute.NOSHADE,
	HTML.Attribute.BORDER,
	HTML.Attribute.ID,
	HTML.Attribute.NAME
    };

    HTML.Tag[] strippedTagsArray = {
	HTML.Tag.SCRIPT
    };

    HTML.Tag[] allowedTagsArray = {
	HTML.Tag.A,
	HTML.Tag.ADDRESS,
	HTML.Tag.B,
	HTML.Tag.BASEFONT,
	HTML.Tag.BIG,
	HTML.Tag.BLOCKQUOTE,
	HTML.Tag.BODY,
	HTML.Tag.BR,
	HTML.Tag.CAPTION,
	HTML.Tag.CENTER,
	HTML.Tag.CODE,
	HTML.Tag.DIV,
	HTML.Tag.EM,
	HTML.Tag.FONT,
	HTML.Tag.H1,
	HTML.Tag.H2,
	HTML.Tag.H3,
	HTML.Tag.H4,
	HTML.Tag.H5,
	HTML.Tag.H6,
	HTML.Tag.HEAD,
	HTML.Tag.HR,
	HTML.Tag.HTML,
	HTML.Tag.I,
	HTML.Tag.IMG,
	HTML.Tag.LI,
	HTML.Tag.LINK,
	HTML.Tag.OL,
	HTML.Tag.P,
	HTML.Tag.PRE,
	HTML.Tag.SPAN,
	HTML.Tag.STRONG,
	HTML.Tag.STYLE,
	HTML.Tag.SUB,
	HTML.Tag.SUP,
	HTML.Tag.TABLE,
	HTML.Tag.TD,
	HTML.Tag.TH,
	HTML.Tag.TITLE,
	HTML.Tag.TR,
	HTML.Tag.U,
	HTML.Tag.UL
    };

    final static boolean debug = Boolean.getBoolean("htmlsanitizer.debug");

    static Pattern styleUrlPattern = Pattern.compile("url\\((.*?)\\)");
    static Pattern dqEscPattern = Pattern.compile("\"");

    Set allowedTags = new HashSet(Arrays.asList(allowedTagsArray));
    Set strippedTags = new HashSet(Arrays.asList(strippedTagsArray));
    Set alwaysAllowedAttributes = new HashSet(Arrays.asList(alwaysAllowedAttributesArray));

    Writer out;
    int textNo;

    HTML.Tag lastOpenedTag = null;

    public HtmlSanitizer(int textNo, OutputStream os, String charset) throws IOException {
	this.textNo = textNo;
	this.out = new OutputStreamWriter(os, charset);
    }

    public void flush() {
	try {
	    out.flush();
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.toString(), ex1);
	}
    }

    public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
	handleTag(tag, attrs, pos);
    }


    public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
	handleTag(tag, attrs, pos);
    }

    public void handleTag(HTML.Tag tag, MutableAttributeSet attrs, int pos) {
	lastOpenedTag = tag;
	try {
	    dprintln("handleTag(): tag " + tag.toString());
	    if (!allowedTags.contains(tag)) {
		dprintln("\t(ignoring)");
		return;
	    }
	    
	    java.util.Map attributes = new HashMap();
	    Enumeration e = attrs.getAttributeNames();
	    while (e.hasMoreElements()) {
		Object o = e.nextElement();
		if (o == IMPLIED) {
		    continue;
		}
		if (o == HTML.Attribute.ENDTAG) {
		    out.write("</");
		    out.write(tag.toString());
		    out.write(">");
		    continue;
		}
		String attributeString = null;
		HTML.Attribute attribute = null;
		if (o instanceof String) {
		    attributeString = (String) o;
		} else if (o instanceof HTML.Attribute) {
		    attribute = (HTML.Attribute) o;
		} else {
		    dprintln("***unknown attribute class: " + o.getClass().getName());
		    continue;
		}
	        Object value = attrs.getAttribute(o);
		dprintln("\t" + attribute + "=" + value);
		if (alwaysAllowedAttributes.contains(o)) {
		    attributes.put(attribute, value);
		    continue;
		}

		if (tag == HTML.Tag.A) {
		    if (attribute == HTML.Attribute.HREF) {
			if (!((String) value).startsWith("javascript:")) {
			    attributes.put(attribute, value);
			}
		    }
		} else if (tag == HTML.Tag.BODY) {
		    if (attribute == HTML.Attribute.BACKGROUND ||
			attribute == HTML.Attribute.BGCOLOR) {
			attributes.put(attribute, value);
		    }
		} else if (tag == HTML.Tag.FONT) {
		    if (attribute == HTML.Attribute.SIZE ||
			attribute == HTML.Attribute.COLOR ||
			attribute == HTML.Attribute.FACE) {
			attributes.put(attribute, value);
		    }
		} else if (tag == HTML.Tag.IMG) {
		    if (attribute == HTML.Attribute.SRC) {
			String lcaseValue = ((String)value).toLowerCase();
			if (lcaseValue.startsWith("http://") &&
			    lcaseValue.startsWith("https://")) {
			    attributes.put(attribute, value);
			} else {
			    rewriteRef((String) value, textNo, attributes, attribute, null);
			}
		    }
		} else if (tag == HTML.Tag.TD ||
			   tag == HTML.Tag.TR ||
			   tag == HTML.Tag.TH) {
		    if (attribute == HTML.Attribute.ROWSPAN ||
			attribute == HTML.Attribute.COLSPAN) {
			attributes.put(attribute, value);
		    }
		} else if (tag == HTML.Tag.LINK) {
		    if (attribute == HTML.Attribute.REL) {
			String lcvalue = ((String) value).toLowerCase();
			if (lcvalue.equals("stylesheet"))
			    attributes.put(attribute, value);
		    }		
		    if (attribute == HTML.Attribute.HREF) {
			rewriteRef((String) value, textNo, attributes, attribute, null);
		    }
		}
		if (attribute == HTML.Attribute.BACKGROUND) {
		    rewriteRef((String) value, textNo, attributes, attribute, null);
		}
	    }
	    if (tag == HTML.Tag.A) {
		attributes.put("target", "_top");
	    }
	    out.write("<" + tag.toString());
	    for (Iterator i = attributes.entrySet().iterator();i.hasNext();) {
		java.util.Map.Entry entry = (java.util.Map.Entry) i.next();
		out.write(" ");
		out.write(entry.getKey().toString());
		out.write("=\"");
		if (entry.getValue() != HTML.NULL_ATTRIBUTE_VALUE) 
		    out.write(dquote(entry.getValue().toString()));
		out.write("\"");
	    }
	    out.write(">");
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.toString(), ex1);
	}
    }

    private void rewriteRef(String value, int textNo, java.util.Map attributes,
			    HTML.Attribute attribute, String append)
    throws UnsupportedEncodingException {
	attributes.put(attribute, 
		       "rawtext.jsp?text=" + textNo + 
		       "&name=" + 
		       URLEncoder.encode(value, "iso-8859-1") + 
		       (append != null ? "&" + append : ""));
    }

    public void handleText(char[] data, int pos) {
	try {
	    /* For some reason "<br />" translates to:
	     * handleSimpleTag(<BR>) -> handleText(">" + trailing text)
	     * This is true for all tags, so we simple remove any
	     * introducing ">"
	     */
	    if (data.length == 1 && data[0] == '>') {
		return;
	    }
	    String dataString;
	    if (data.length > 1 && data[0] == '>') {
		dataString = new String(data, 1, data.length-1);
	    } else {
		dataString = new String(data);
	    }
	    dprintln("handleText(): " + dataString);
	    if (strippedTags.contains(lastOpenedTag)) {
		dprintln("\t(stripping)");
		return;
	    }
	    dataString = dataString.replaceAll("<", "&lt;");
	    if (lastOpenedTag == HTML.Tag.STYLE) {
		Matcher m = styleUrlPattern.matcher(dataString);
		StringBuffer buf = new StringBuffer();
		while (m.find()) {
		    m.appendReplacement(buf, "url(rawtext.jsp?text=" +
					textNo + "&name=" + 
					URLEncoder.encode(m.group(1),
							  "iso-8859-1") + 
					")");
		}
		m.appendTail(buf);
		dataString = buf.toString();
		dataString = dataString.replaceAll("url(javascript:.*)", "url()");
	    }
	    data = dataString.toCharArray();
	    out.write(data);
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.toString(), ex1);
	}
    }

    public void handleEndTag(HTML.Tag t, int pos) {
	try {
	    dprintln("handleEndTag(): " + t);
	    if (allowedTags.contains(t)) {
		out.write("</");
		out.write(t.toString());
		out.write(">");
	    }
	} catch (IOException ex1) {
	    throw new RuntimeException(ex1.toString(), ex1);
	}
    }

    static void dprintln(String s) {
	if (debug) nu.dll.lyskom.Debug.println(s);
    }

    private static String dquote(String s) {
	return dqEscPattern.matcher(s).replaceAll("'");
    }

    public static void parse(int textNo, InputStream is,
			     OutputStream os, String charset)
    throws IOException {
	HtmlSanitizer hs = new HtmlSanitizer(textNo, os, charset);
	ParserDelegator pd = new ParserDelegator();
	pd.parse(new InputStreamReader(is, charset), hs, true);
	hs.flush();
    }

    public static void main(String[] argv) throws Exception {
	OutputStream os = new FileOutputStream("out.html");
	os.write("<!-- begin -->\n".getBytes("iso-8859-1"));
	HtmlSanitizer hs = new HtmlSanitizer(1337, os, "iso-8859-1");
	ParserDelegator pd = new ParserDelegator();
	pd.parse(new FileReader(new File(argv[0])), hs, false);
	hs.flush();
	System.out.println("Parse complete.");
	os.write("\n<!-- end -->\n".getBytes("iso-8859-1"));
	os.close();
    }

}
