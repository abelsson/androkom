package nu.dll.lyskom;

import java.util.Collection;
import java.util.Iterator;

/**
 * Represents a marked text and it's mark type.
 *
 * @see nu.dll.lyskom.Session#getMarks()
 */
public class Mark {
    int text;
    int type;

    Mark(int text, int type) {
	this.text = text;
	this.type = type;
    }
    
    /**
     * Returns the text number.
     */
    public int getText() {
	return text;
    }

    /**
     * Returns the mark type.
     */
    public int getType() {
	return type;
    }

    public static boolean isIn(Collection c, int textNo) {
	for (Iterator i = c.iterator();i.hasNext();) {
	    if (((Mark) i.next()).getText() == textNo) return true;
	}
	return false;
    }

    public boolean isIn(Collection c) {
	return isIn(c, text);
    }
    
    public String toString() {
	return "<Mark: " + text + "/" + type + ">";
    }
}
