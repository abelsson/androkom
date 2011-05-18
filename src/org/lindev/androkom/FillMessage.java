package org.lindev.androkom;

import java.util.*;

class Paragraph {
    Paragraph() {
        lines = new ArrayList<String>();
    }
    
    String escape(String line)
    {
    	line = line.replaceAll("&", "&amp;");
        line = line.replaceAll("<", "&lt;");
        line = line.replaceAll(">", "&gt;");
        return line;        
    }

    boolean isQuote() {
    	boolean all = true;
    	for(String line : lines) {
    		line = line.trim();

    		all &= line.startsWith(">");
    	}

    	return all;
    }

    boolean isBulletList() {
    	boolean all = true;
    	for(String line : lines) {
    		line = line.trim();

    		all &= line.startsWith("*");
    	}

    	return all;
    }

    boolean isPreformatted() {
    	boolean any = false;
    	for(String line : lines) {
    		any |= line.startsWith(" ");
    		any |= line.startsWith("+");
    		any |= line.startsWith("-");
    		any |= line.startsWith("\t");
    		any |= line.contains("   ");
    		any |= line.contains(" \t");
    		any |= line.contains("\t ");
    	}
    	return any;
    }

    String formatQuote() {
    	String ret = "<blockquote>";
    	
    	StringBuilder body = new StringBuilder();
    	  	
    	for(String line : lines) 
    		body.append(line.substring(1).trim() + "\n");
    	
    	FillMessage fm = new FillMessage(body.toString());
        ret += fm.run();
        
    	ret += "</blockquote>";
    	return ret;
    }

    String formatBulletList() {
    	String ret = "<ul>";
    	for(String line : lines) 
    		ret += "<li>" + escape(line.substring(1)) + "</li>\n";
    	ret += "</ul>";
    	return ret;
    }

    String formatPreformatted() {
    	String ret = "<p>";
    	for(String line : lines) 
    		ret += escape(line).replaceAll(" ", "&nbsp;") + "<br />\n";
    	ret += "</p>";
    	return ret;
    }
    
    String format() {
    	if (isQuote()) {
    		return formatQuote();
    	}
    	if (isBulletList()) {
    		return formatBulletList();
    	} 
    	if (isPreformatted()) {
    		return formatPreformatted();
    	} else {
    		String ret = "<p>";
    		for(String line : lines) 
    			ret += escape(line) + "\n";
    		ret += "</p>";
    		return ret;
    	}
    }

    public ArrayList<String> lines;
}

class FillMessage {
	private String data;    

	FillMessage(String input)
	{
		data = input;
	}

	String run()
	{   
		String ret = new String();

		String lines[] = data.split("\\n");
		ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>();

		Paragraph p = new Paragraph();

		for(String line : lines) {          
			if (line.trim().length() == 0) {
				paragraphs.add(p);
				p = new Paragraph();
			} else {    
				p.lines.add(line);
			}

		}
		paragraphs.add(p);

		for(Paragraph pp : paragraphs) {
			ret += pp.format() + "\n";
		}

		return ret;
	}

}