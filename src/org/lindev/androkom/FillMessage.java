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
    		any |= line.startsWith("+ ");
    		any |= line.startsWith("- ");
    		any |= line.startsWith("\t");
    		any |= line.contains("    ");
    		any |= line.contains(" \t");
    		any |= line.contains("\t ");
    	}
    	return any;
    }
    
    boolean isPointList()
    {
    	float characters = 0;
    	float charactersPerFullLine = 76;
    	  	
    	for(String line : lines) {
    		characters += line.length();
    	}
    	float maxCharacters = charactersPerFullLine * lines.size();
    	float fillRatio = characters/maxCharacters;
    	
    	System.out.println("Fillratio = " + fillRatio);
    	return fillRatio < 0.45;
    	
    }

    String formatQuote() {
    	StringBuilder ret = new StringBuilder();
    	
    	ret.append("<blockquote>");
    	
    	StringBuilder body = new StringBuilder();
    	  	
    	for(String line : lines) 
    		body.append(line.substring(1).trim() + "\n");
    	
    	FillMessage fm = new FillMessage(body.toString());
    	ret.append(fm.run());
        
    	ret.append("</blockquote>");
    	return ret.toString();
    }

    String formatBulletList() {
    	/*
    	TODO - enable when we can display html lists.
    	StringBuilder ret = new StringBuilder();
    	ret.append("<ul>");
    	for(String line : lines) 
    		ret.append("<li>" + escape(line.substring(1)) + "</li>\n");
    	ret.append("</ul>");
    	return ret.toString();
    	*/
    	
    	StringBuilder ret = new StringBuilder();
    	
    	ret.append("<p>");
    	for(String line : lines) 
    		ret.append(escape(line) + "<br />\n");
    	ret.append("</p>");
    	
    	return ret.toString();
    }

    String formatPreformatted() {
    	StringBuilder ret = new StringBuilder();
    	ret.append("<p>");
    	for(String line : lines) 
    		ret.append(escape(line).replaceAll(" ", "&nbsp;") + "<br />\n");
    	ret.append("</p>");
    	return ret.toString();
    }
    
    String format() {
    	if (lines.size() == 0 || (lines.size() == 1 && lines.get(0).trim().length() == 0))
    			return "";
    	
    	if (isQuote()) {
    		return formatQuote();
    	}
    	if (isBulletList()) {
    		return formatBulletList();
    	} 
    	if (isPreformatted()) {
    		return formatPreformatted();
    	}
    	if (isPointList()) {
    		return formatBulletList();
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
		StringBuilder ret = new StringBuilder();


		String lines[] = data.split("\\n");
		ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>();

		Paragraph p = new Paragraph();

		for(String line : lines) {
			if (line.trim().length() == 0) {
				// don't add empty paragraphs.
				if (p.lines.isEmpty())
					continue;
				paragraphs.add(p);
				p = new Paragraph();
			} else {    
				p.lines.add(line);
			}

		}
		paragraphs.add(p);

		for(Paragraph pp : paragraphs) {			
			ret.append(pp.format() + "\n");
		}

		return ret.toString();
	}
}
