package org.lindev.androkom;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Rot13 {

    private static final Map<Character, Character> map;
    static {
        final char[] lookup1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        final char[] lookup2 = "NOPQRSTUVWXYZABCDEFGHIJKLMnopqrstuvwxyzabcdefghijklm".toCharArray();

        Map<Character, Character> m = new HashMap<Character, Character>();
        for(int i=0; i<lookup1.length; i++) {
            m.put(lookup1[i], lookup2[i]);
        }
        map = Collections.unmodifiableMap(m);
    }

    private Rot13() {}

    /**
     * Converts the input string ROT-13 algorithm.
     * @param inStr The input string.
     * @return The ROT-13ed version of the input string.
     */
    public static String cipher(final String inStr) {
        char[] arr = inStr.toCharArray();
        StringBuilder sb = new StringBuilder(arr.length);

        for(char c: arr) {
            Character out = map.get(c);
            if(out == null) {
                sb.append(c);
            }
            else {
                sb.append(out);
            }
        }

        return sb.toString();
    }
}
