package nu.dll.lyskom; 

import java.util.LinkedList;

class AsynchInvoker extends Thread {
    LinkedList runnables = new LinkedList();

    public AsynchInvoker() {
	setName("AsynchInvokerThread");
    }

    public void run() {
	boolean run = true;
	while (run) {
	    try {
		while (!runnables.isEmpty()) {
		    Runnable nextRunnable = null;
		    synchronized (runnables) {
			nextRunnable = (Runnable) runnables.removeFirst();			
		    }
		    Debug.println("Executing " + nextRunnable.toString());
		    nextRunnable.run();
		}
		synchronized (runnables) {
		    Debug.println("Waiting.");
		    runnables.wait();
		}
	    } catch (InterruptedException ex1) {
		Debug.println("Interrupted");
	    }
	}
    }

    public void enqueue(Runnable r) {
	synchronized(runnables) {
	    runnables.addLast(r);
	    runnables.notifyAll();
	}
    }

}
