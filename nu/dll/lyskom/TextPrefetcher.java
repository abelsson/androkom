package nu.dll.lyskom;

import java.io.IOException;
import java.util.List;

class TextPrefetcher implements Runnable {
    List texts;
    Session session;
    public TextPrefetcher(Session session, List texts) {
	this.session = session;
	this.texts = texts;
    }

    public void run() {
	Debug.println("TextPrefetcher: " + texts.size() + " texts to be fetched");
	while (!texts.isEmpty()) {
	    int textNo;
	    synchronized (texts) {
		textNo = ((Integer) texts.remove(0)).intValue();
	    }
	    try {
		session.getText(textNo, true);
		Debug.println("Fetched text number " + textNo);
	    } catch (IOException ex1) {
		Debug.println("I/O error during pre-fetch: " + ex1.getMessage());
	    }
	}
	Debug.println("Prefetch list emtpy");
    }
}
