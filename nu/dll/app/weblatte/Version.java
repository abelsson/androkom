package nu.dll.app.weblatte;

import nu.dll.lyskom.Debug;
import java.io.*;

public class Version {
    static String resource = "/nu/dll/app/weblatte/version";
    static String version = "0.0";
    static {
	try {
	    InputStream is = Version.class.getResourceAsStream(resource);
	    if (is != null) {
		BufferedReader rdr = new BufferedReader(new InputStreamReader(is, "iso-8859-1"));
		String row = rdr.readLine();
		if (row != null) version = row;
		else Debug.println("Version: resource " + resource + " is empty.");
	    } else {
		Debug.println("Version: resource " + resource + " not found.");
	    }
	} catch (IOException ex1) {
	    ex1.printStackTrace();
	}
	Debug.println("Weblatte version is " + version);
    }

    public static String getVersion() {
	return version;
    }
}
