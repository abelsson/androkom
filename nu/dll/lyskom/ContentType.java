package nu.dll.lyskom;

import java.util.Hashtable;

public class ContentType {

	private static final String TAG = "ContentType";
	private ParameterList contents = null;
	
	public ContentType(String contentTypeString) throws MimeUtility.ParseException {
		// TODO Auto-generated constructor stub
		//Log.d(TAG, "ContentType constr:"+contentTypeString);
		contents = new ParameterList();
		//contents.set("charset", "utf-8");
		if (contentTypeString.endsWith("utf-8")) {
			contents.set("charset", "utf-8");
		} else if (contentTypeString.endsWith("iso-8859-1")) {
			contents.set("charset", "iso-8859-1");
		} else if (contentTypeString.endsWith("iso-2022-jp-2")) {
			contents.set("charset", "iso-2022-jp-2");
		} else {
			contents.set("charset", "iso-8859-1");
		}
	}

	public ParameterList getParameterList() {
		// TODO Auto-generated method stub
		//Log.d(TAG, "getParameterList");
		return contents;
	}

	public boolean match(String string) {
		// TODO Auto-generated method stub
		//Log.d(TAG, "match:"+string);
		return false;
	}

}
