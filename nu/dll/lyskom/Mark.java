package nu.dll.lyskom;

public class Mark {
    int text;
    int type;

    public Mark(int text, int type) {
	this.text = text;
	this.type = type;
    }

    public int getText() {
	return text;
    }

    public int getType() {
	return type;
    }
}
