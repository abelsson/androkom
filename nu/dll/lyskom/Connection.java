/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package nu.dll.lyskom;

import java.net.*;
import java.io.*;

import java.util.LinkedList;


class Connection {

    boolean debug = Boolean.getBoolean("lattekom.debug-writer");

    private LinkedList writeQueue = new LinkedList();
    private Socket sock;
    private InputStream input;
    private OutputStream output;
    private Session session;
    String server;
    int port;

    Thread queuedWriter = null;
    boolean keepRunning = true;

    public Connection(Session session)
    throws IOException, UnknownHostException {
	this.session = session;
	this.server = session.getServer();
	this.port = session.getPort();

	sock = new Socket(server, port);
	input = sock.getInputStream();
	output = sock.getOutputStream();

	queuedWriter = new Thread(new Runnable() {
		public void run() {
		    Debug.println("Queued writer start.");
		    while (keepRunning) {
			try {
			    synchronized (writeQueue) {
				if (writeQueue.isEmpty()) {
				    if (debug) Debug.println("Write queue empty.");
				    writeQueue.wait();
				}
				synchronized (output) {
				    while (!writeQueue.isEmpty()) {
					byte[] bytes = (byte[]) writeQueue.removeFirst();
					output.write(bytes);
					if (Debug.ENABLED) {
					    String s;
					    if (bytes[bytes.length-1] == '\n') {
						s = new String(bytes, 0, bytes.length-1);
					    } else {
						s = new String(bytes);
					    }
					    if (debug) Debug.println("wrote: " + s);
					}
				    }
				}
			    }

			} catch (IOException ex1) {
			    Debug.println("I/O error during write: " + ex1.getMessage());
			    keepRunning = false;
			} catch (InterruptedException ex2) {
			    Debug.println("Interrupted during wait(): " + ex2.getMessage());
			}
		    }
		    Debug.println("Queued writer exit.");
		}
	    });
	queuedWriter.setName("QueuedWriter-" + writerThreadCount++);
	queuedWriter.setDaemon(true);
	queuedWriter.start();

    }
    static int writerThreadCount = 0;

    public String getServer() {
	return server;
    }

    public int getPort() {
	return port;
    }
    
    public void close()
    throws IOException {
	sock.close();
	keepRunning = false;
	queuedWriter.interrupt();
    }

    public InputStream getInputStream() {
	return input;
    }

    public OutputStream getOutputStream() {
	return output;
    }

    public void queuedWrite(String s)
    throws IOException {
	synchronized (writeQueue) {
	    if (!keepRunning) {
		throw new IllegalStateException("Connection has been terminated.");
	    }
	    if (session.listener.getException() != null) {
		Exception ex1 = session.listener.getException();
		throw new IOException("Exception in listener: " + ex1.toString());
	    }
	    try {
		writeQueue.addLast(s.getBytes(session.serverEncoding));
	    } catch (UnsupportedEncodingException ex1) {
		throw new RuntimeException("Unsupported server encoding: " + ex1.getMessage());
	    }
	    writeQueue.notifyAll();
	}
    }

    public void queuedWrite(byte[] b) {
	synchronized (writeQueue) {
	    writeQueue.addLast(b);
	    writeQueue.notifyAll();
	}
    }
    
    /**
     *
     * @deprecated use writeLine() or queuedWrite() instead
     */ 
    public void write(char c)
    throws IOException {
	synchronized (output) {
	    output.write(c);
	}
    }

    public void writeLine(byte[] b) 
    throws IOException {
	synchronized (output) {
	    output.write(b);
	    output.write('\n');
	}
    }

    public void writeLine(String s)
    throws IOException {
	synchronized (output) {
	    try {
		byte[] bytes = s.getBytes(session.serverEncoding);
		byte[] line;
		// append \n if necessary
		if (bytes[bytes.length-1] != '\n') {
		    line = new byte[bytes.length+1];
		    System.arraycopy(bytes, 0, line, 0, bytes.length);
		    line[bytes.length] = (byte) '\n';
		} else {
		    line = bytes;
		}
		output.write(line);
	    } catch (UnsupportedEncodingException ex1) {
		throw new RuntimeException("Unsupported server encoding: " + ex1.getMessage());
	    }
	    output.write('\n');
	}
    }

    /*
     * Reads until "\n" is encountered.
     */

    public String readLine(String s) 
    throws IOException {
	ByteArrayOutputStream os = new ByteArrayOutputStream(80);
	byte b = (byte) input.read();
	while (b != -1 && b != '\n') {	    
	    os.write(b);
	    b = (byte) input.read();
	}

	switch (b) {
	case -1:
	    Debug.println("Connection.readLine(): EOF from stream");
	    break;
	case 0:
	    Debug.println("Connection.readLine(): \\0 from stream");
	    break;
	}
	
	return new String(os.toByteArray(), session.serverEncoding);
    }

	
    
}
