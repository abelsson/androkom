package nu.dll.lyskom;

import java.io.IOException;

/**
 * A "Lazy" text doesn't read it's contents from the server until needed.
 *
 */
public class LazyText extends Text {
    Session session;
    protected LazyText(Session session, int textNo) {
	this.textNo = textNo;
	this.session = session;
    }

    public byte[] getContents() throws RpcFailure {
	int textSize = getStat().getSize();
	if (contents.length != textSize) {
	    try {
		if (Debug.ENABLED)
		    Debug.println("LazyText.getContents(): filling in " + textSize + " bytes for text " + textNo);
		RpcCall call = new RpcCall(session.count(), Rpc.C_get_text).
		    add(new KomToken(textNo)).add("0").
		    add(new KomToken(textSize));
		session.writeRpcCall(call);
		RpcReply reply = session.waitFor(call);
		if (!reply.getSuccess()) throw reply.getException();
		setContents(reply.getParameters()[0].getContents());
	    } catch (IOException ex1) {
		throw new RuntimeException("I/O error", ex1);
	    } 
	}
	return contents;
    }
}
