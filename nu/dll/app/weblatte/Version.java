package nu.dll.app.weblatte;

import nu.dll.lyskom.Debug;
import java.io.*;

public class Version {
    static String version = "0.5";
    static {
	try {
	    InputStream is = Version.class.getResourceAsStream("nu/dll/app/weblatte/cvs-version");
	    if (is != null) {
		BufferedReader rdr = new BufferedReader(new InputStreamReader(is, "iso-8859-1"));
		String row = rdr.readLine();
		if (row != null) version = row;
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
