package nu.dll.lyskom;

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
}
