package nu.dll.lyskom;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import java.io.IOException;

/**
 * A HollerithMap is a collection of sequentially ordered key-value pairs.
 */
public class HollerithMap extends Hollerith {
	private static final long serialVersionUID = 3182639389779691871L;
	Map<String, Hollerith> map;

    public HollerithMap(String charset) {
        super();
        setCharset(charset);
        map = new HashMap<String, Hollerith>();
    }

    public HollerithMap(Hollerith data) {
        setContents(data.getContents());
        setCharset(data.getCharset());
        map = parse(getContents(), getCharset());
    }

    public HollerithMap(byte[] buf, String charset) {
        setCharset(charset);
        setContents(buf);
        map = parse(buf, charset);
    }

    public Hollerith get(String key) {
        synchronized (map) {
            return (Hollerith) map.get(key);
        }
    }

    public void put(String key, String value) {
        synchronized (map) {
            map.put(key, new Hollerith(value, getCharset()));
            updateContents();
        }
    }

    public void put(String key, Hollerith value) {
        synchronized (map) {
            map.put(key, value);
            updateContents();
        }
    }

    public boolean containsKey(String key) {
        synchronized (map) {
            if (!map.containsKey(key))
                return false;

            String value = ((Hollerith) map.get(key)).getContentString();
            if (value.equals("nil"))
                return false;
            return true;
        }
    }

    private void updateContents() {
        try {
            String charset = getCharset();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            for (Iterator<Entry<String, Hollerith>> i = map.entrySet()
                    .iterator(); i.hasNext();) {
                Map.Entry<String, Hollerith> entry = i.next();
                String key = entry.getKey();
                Hollerith value = entry.getValue();
                os.write(new Hollerith(key, charset).toNetwork());
                os.write(' ');
                os.write(value.toNetwork());
                if (i.hasNext())
                    os.write('\n');
            }
            setContents(os.toByteArray());
        } catch (IOException ex1) {
            throw new RuntimeException("Internal Error: " + ex1.toString(), ex1);
        }
    }

    private static Map<String, Hollerith> parse(byte[] bytes, String charset) {
        try {
            Map<String, Hollerith> data = new HashMap<String, Hollerith>();
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);

            KomToken _nextToken = KomTokenReader.readToken(is, charset);
            while (_nextToken != null) {

                // silently skip non-hollerith tokens
                while (_nextToken != null && !(_nextToken instanceof Hollerith)) {
                    _nextToken = KomTokenReader.readToken(is, charset);
                }
                if (_nextToken == null)
                    continue;
                Hollerith nextToken = (Hollerith) _nextToken;
                String key = nextToken.getContentString();
                Hollerith value = (Hollerith) KomTokenReader.readToken(is,
                        charset);
                data.put(key, value);

                _nextToken = (Hollerith) KomTokenReader.readToken(is, charset);
            }
            return data;
        } catch (IOException ex1) {
            throw new RuntimeException("Internal Error: " + ex1.toString(), ex1);
        }
    }

}
