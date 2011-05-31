<%@ page language='java' import='nu.dll.lyskom.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ include file='kom.jsp' %>
<%@ include file='prefs_inc.jsp' %>
<%!
    static class ChatMessageReceiver implements AsynchMessageReceiver {
	List list;
	public ChatMessageReceiver(List list) {
	    this.list = list;
	}

	public void asynchMessage(AsynchMessage m) {
	    synchronized (list) {
		list.add(m);
		list.notifyAll();
	    }
	}

    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Persistent läsare för asynkrona KOM-meddelanden</title>
    <link rel="stylesheet" href="lattekom.css" />
  </head>
  <script language="JavaScript1.2">
    function rescroll() {
	scroll(1,10000000);
    }
  </script>
  <body>
<%
    LinkedList messages = new LinkedList();
    List oldMessages = (List) session.getAttribute("lyskom.messages");
    if (oldMessages != null) {
	synchronized (oldMessages) {
	    for (Iterator i = oldMessages.iterator(); i.hasNext();) {
		messages.add(i.next());
		i.remove();		
	    }
	}
    }
    lyskom.addAsynchMessageReceiver(new ChatMessageReceiver(messages));
    boolean chatRunning = true;
    session.setAttribute("lyskom.chat.messages", messages);
    session.setAttribute("lyskom.chat-running", Boolean.TRUE);
    int me = lyskom.getMyPerson().getNo();
    do {
	out.println("<script>rescroll()</script>");
	out.flush();
	Object nextMessage = null;
	synchronized (messages) {
	    while (chatRunning && messages.size() == 0) {
		messages.wait();
	    }
	    nextMessage = messages.size() > 0 ? messages.removeFirst() : null;
	}
	if (nextMessage != null) {
	    if (!(nextMessage instanceof AsynchMessage)) {
		List sentMessage = (List) nextMessage;
		int recipient = ((Integer) sentMessage.remove(0)).intValue();
		String message = (String) sentMessage.remove(0);
	        out.println("<pre class=\"chatMessageSent\">");
		if (recipient == 0) {
		    out.println("<b>Ditt alarmmeddelande</b>:");
		} else {
		    out.println("Ditt meddelande till <b>" +
			lookupName(lyskom, recipient, true) + "</b>:");
		}
		out.println(htmlize(message));
		out.println("\n</pre>");
		continue;
 	    }
	    AsynchMessage m = (AsynchMessage) nextMessage;
	    if (m.getNumber() != Asynch.send_message) {
		continue;
	    }
	    int recipient = m.getParameters()[0].intValue();
	    int sender    = m.getParameters()[1].intValue();
	    String text   = lyskom.toString(m.getParameters()[2].getContents());

	    if (preferences.getBoolean("chat-hide-messages-from-me") && me == sender) {
		continue;
	    }
	    out.println("<pre class=\"chatMessage\">");
	    if (recipient == 0) {
		out.print("<b>Alarmmeddelande</b>");
	    } else {
		if (recipient == me) {
		    out.print("<b>Personligt meddelande</b>");
		} else {
		    out.print("<b>Gruppmeddelande</b> till <b>" +
			lookupName(lyskom, recipient, true) + "</b>");
		}
	    }
            out.println(" från <b>" + lookupName(lyskom, sender, true) + "</b>:");
	    out.println(htmlize(text));
	    out.println("<small>" + df.format(m.getArrivalTime()) + "</small>");
	    out.print("\n</pre>");
	}

    	Boolean chatRunningObj = (Boolean) session.getAttribute("lyskom.chat-running");
    	chatRunning = session.getAttribute("lyskom") != null && lyskom.getLoggedIn() && chatRunningObj != null && chatRunningObj.booleanValue();
    } while (chatRunning);
%>
  </body>
</html>
