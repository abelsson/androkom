package nu.dll.app.test;

class Match {
    public Match(String c, int s, int o) {
	command = c; score = s; paramOffset = o;
    }
    String command; int score, paramOffset;
    public String toString() {
	return "command: " +  command+  ", score: " + score +  ", paramOffset: " + paramOffset;
    }
}

