package nu.dll.lyskom;

import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class UserArea {
    public static String contentType = "x-kom/user-area";
    Map blocks = new HashMap();
    String charset;
    int textNo;
    protected UserArea(Text text)
    throws IllegalArgumentException {
	charset = text.getCharset();
	textNo = text.getNo();
	parse(text);
    }

    protected UserArea(String charset) {
	this.charset = charset;
	textNo = 0;
    }

    public String getCharset() {
	return charset;
    }

    public int getTextNo() {
	return textNo;
    }

    public String[] getBlockNames() {
	Set nameSet = blocks.keySet();
	String[] names = new String[nameSet.size()];
	int i = 0;
	for (Iterator setIter = nameSet.iterator(); setIter.hasNext(); i++) {
	    names[i] = (String) setIter.next();
	}
	return names;
    }

    public Hollerith getBlock(String name) {
	return (Hollerith) blocks.get(name);
    }

    public void setBlock(String name, Hollerith data) {
	blocks.put(name, data);
    }

    private void parse(Text text)
    throws IllegalArgumentException {
	if (!text.getContentType().equals(contentType)) {
	    if (Debug.ENABLED) {
		Debug.println("Supplied Text has content type " +
			      "\"" + text.getContentType() + "\" " + 
			      "instead of expected \"" + contentType + 
			      "\"");
	    }
	}
	parse(text.getContents(), text.getCharset());
    }

    public byte[] toNetwork() {
	try {
	    ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
	    List tocData = new LinkedList();
	    List blockData = new LinkedList();
	    ByteArrayOutputStream tocStream = new ByteArrayOutputStream();
	    for (Iterator i = blocks.entrySet().iterator(); i.hasNext();) {
		Map.Entry entry = (Map.Entry) i.next();
		Hollerith tocEntry = new Hollerith(((String) entry.getKey()).getBytes(charset), charset);
		tocStream.write(tocEntry.toNetwork());
		if (i.hasNext()) tocStream.write(' ');
		blockData.add(entry.getValue());
	    }
	    Hollerith toc = new Hollerith(tocStream.toByteArray(), charset);
	    os.write(toc.toNetwork());
	    os.write(' ');
	    for (Iterator i = blockData.iterator(); i.hasNext();) {
		Hollerith h = (Hollerith) i.next();
		os.write(h.toNetwork());
		if (i.hasNext()) os.write(' ');
	    }

	    return os.toByteArray();

	} catch (IOException ex1) {
	    throw new RuntimeException("Internal Error: " + ex1.toString(), ex1);
	}
    }

    private void parse(byte[] data, String charset) 
    throws IllegalArgumentException {
	try {
	    ByteArrayInputStream userAreaStream = new ByteArrayInputStream(data);
	    Hollerith toc = (Hollerith) KomTokenReader.readToken(userAreaStream, charset);
	    ByteArrayInputStream tocStream = new ByteArrayInputStream(toc.getContents());
	    List blockNames = new LinkedList();
	    Hollerith nextBlock = (Hollerith) KomTokenReader.readToken(tocStream, charset);
	    while (nextBlock != null) {
		String name = new String(nextBlock.getContents(), charset);
		blockNames.add(name);
		if (Debug.ENABLED) {
		    Debug.println("User-Area block name: " + name);
		}
		nextBlock = (Hollerith) KomTokenReader.readToken(tocStream, charset);
	    }

	    for (Iterator names = blockNames.iterator(); names.hasNext();) {
		String name = (String) names.next();
		Hollerith blockData = (Hollerith) KomTokenReader.readToken(userAreaStream, charset);
		blocks.put(name, blockData);
		if (Debug.ENABLED) {
		    Debug.println("User-Area block \"" + name + "\" parsed with " +
				  blockData.getContents().length + " bytes");
		}
	    }
	} catch (IOException ex1) {
	    throw new RuntimeException("Internal error: " + ex1.toString());
	}
    }
}
