package nu.dll.app.test;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;


/**
 * Outputstream that writes all output to a Console component.
 * Useful for redirection of stderr or stdout to a GUI window.
 * $Revision: 1.4 $
 */
class LogOutputStream extends OutputStream {
    Console target = null;
    String encoding = "iso-8859-1";
    public LogOutputStream(Console console) {
	this.target = console;
    }

    public LogOutputStream(Console console, String encoding) {
	this.target = console;
	this.encoding = encoding;
    }

    public void setEncoding(String s) {
	this.encoding = s;
    }

    public void write(String s) {
	target.append(s, false);
    }
    public void write(byte[] b) {
	try {
	    write(new String(b, encoding));
	} catch (UnsupportedEncodingException ex) {
	    write("Environmental error: " + encoding + " encoding not supported!\n");
	    target.setVisible(true);
	}
    }
    public void write(int b) {
	write(new byte[] { (byte) b });
    }
}
