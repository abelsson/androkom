package nu.dll.app.weblatte;

import javax.mail.internet.*;
import java.io.*;
import nu.dll.lyskom.*;
import java.util.StringTokenizer;
import java.util.Enumeration;

public class MimeText extends nu.dll.lyskom.Text implements javax.activation.DataSource {

    public MimeText(Text t) {
	super(t.getContents());
	setNo(t.getNo());
	setStat(t.getStat());
    }
    /**
     * This method is here to implement the DataSource interface, which allows
     * us to use a Text as a MimeMultipart backend.
     * 
     * If the text is an MHTML (RFC 2557) message, it will therefore effectively
     * change the texts content-type into that of the root MHTML body, as
     * specified in the RFC 882 headers in the beginning of the text. This
     * implementation ignores all RFC 822 headers contained in the root part of
     * the message, except for Content-Type.
     * 
     * An MHTML text is identified as having a content-type matching
     * "message/rfc822; x-lyskom-variant=rfc2557".
     * 
     */
    public String getContentType() {
        String contentTypeString = getStat().getFullContentType();
        ContentType contentType = null;
        try {
            contentType = new ContentType(contentTypeString);
        } catch (ParseException ex) {
            throw new RuntimeException(
                    "Error parsing contents while trying to parse content-type: "
                            + ex.toString());
        }

        boolean mhtml = contentType.match("message/rfc822")
                && ("mhtml"
                        .equals(contentType.getParameterList().get("x-type")) || "rfc2557"
                        .equals(contentType.getParameterList().get(
                                "x-lyskom-variant")));

        // hack for WinLMSG-created texts with incorrect content-type.
        // it checks the first row of the text to see if it contains
        // "mime:", and if so, it treats it as if it was an MHTML
        // text
        if (!mhtml && contentTypeString.equals("multipart/related")) {
            try {
                InputStream is = getInputStream();
                BufferedReader rdr = new BufferedReader(new InputStreamReader(
                        is, "us-ascii"));
                String row = rdr.readLine();
                if (row != null && row.equals("mime:")) {
                    mhtml = true;
                }
                is.close();
            } catch (IOException ex1) {
                throw new RuntimeException(
                        "Text.getContentType(): I/O error while examining text body.",
                        ex1);
            }
        }

        if (mhtml) {
            try {
                InputStream is = getInputStream();
                InternetHeaders rfc822headers = new InternetHeaders();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, "us-ascii"));
                String row;
                String lastRow = null;
                while ((row = reader.readLine()) != null && !row.equals("")) {
                    if (row.startsWith("\t")) {
                        row = lastRow + " " + row.substring(1);
                    }
                    StringTokenizer st = new StringTokenizer(row);
                    String name = st.nextToken();
                    name = name.substring(0, name.length() - 1);
                    String value = st.nextToken("");
                    rfc822headers.setHeader(name, value);

                    lastRow = row;
                }

                Debug.println("loaded RFC822 headers into " + rfc822headers);
                @SuppressWarnings("unchecked")
                Enumeration<Header> e = rfc822headers.getAllHeaders();
                while (e.hasMoreElements()) {
                    Header h = (Header) e.nextElement();
                    Debug.println("rfc822 header name: " + h.getName()
                            + ", value: " + h.getValue());
                }

                ContentType preambleContentType = new ContentType(
                        rfc822headers.getHeader("Content-Type", null));

                if (preambleContentType != null
                        && preambleContentType.match("multipart/*")) {
                    getStat().setAuxItem(new AuxItem(AuxItem.tagContentType,
						     preambleContentType.toString()));
                    contentTypeString = preambleContentType.toString();
                }
            } catch (ParseException ex) {
                throw new RuntimeException(ex.toString());
            } catch (IOException ex1) {
                ex1.printStackTrace();
                throw new RuntimeException(
                        "Error parsing contents while trying to parse MHTML message");
            }
        }
        return contentTypeString;
    }


}