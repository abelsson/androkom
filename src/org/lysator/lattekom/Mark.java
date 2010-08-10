package org.lysator.lattekom;

import java.util.Collection;
import java.util.Iterator;

/**
 * Represents a marked text and it's mark type.
 * 
 * @see nu.dll.lyskom.Session#getMarks()
 */
public class Mark implements java.io.Serializable {
	private static final long serialVersionUID = 4104867630232961582L;

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

    public static boolean isIn(Collection<Mark> c, int textNo) {
        for (Iterator<Mark> i = c.iterator(); i.hasNext();) {
            if (i.next().getText() == textNo)
                return true;
        }
        return false;
    }

    public boolean isIn(Collection<Mark> c) {
        return isIn(c, text);
    }

    public String toString() {
        return "<Mark: " + text + "/" + type + ">";
    }
}
