<%@ page language='java' import='nu.dll.lyskom.*' %>
<%@ page pageEncoding='iso-8859-1' contentType='text/html; charset=utf-8' %>
<%@ include file='kom.jsp' %>
<%!
    class ConfNameComparator implements Comparator {
	Session lyskom;
	public ConfNameComparator(Session lyskom) {
	    this.lyskom = lyskom;
	}
	public int compare(Object o1, Object o2) {
	    try {
		return komStrip(lyskom.toString(((UConference) o1).getName())).toLowerCase().
		compareTo(komStrip(lyskom.toString(((UConference) o2).getName())).toLowerCase());
	    } catch (Exception ex1) {
		ex1.printStackTrace();
		throw new RuntimeException(ex1.toString());
	    }

	}
    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Meddelandeskickarformulär</title>
    <link rel="stylesheet" href="lattekom.css" />
  </head>
  <body>
<%
    SortedSet people = (SortedSet) session.getAttribute("lyskom.chat.people");
    if (request.getParameter("reset") != null || people == null) {
        people = new TreeSet(new ConfNameComparator(lyskom));
    	DynamicSessionInfo[] who = lyskom.whoIsOnDynamic(true, false, 3600);
    	for (int i=0; i < who.length; i++) {
	    people.add(lyskom.getUConfStat((who[i].getPerson())));
      	}
        session.setAttribute("lyskom.chat.people", people);
    }
    Integer defaultRecipientObj = (Integer) session.getAttribute("lyskom.chat.default-recipient");
    if (request.getParameter("default") != null)
	defaultRecipientObj = new Integer(request.getParameter("default"));

    int defaultRecipient = defaultRecipientObj != null ? defaultRecipientObj.intValue() : 0;
    int me = lyskom.getMyPerson().getNo();

    if (request.getParameter("btnS") != null) {
	String recipientName = request.getParameter("recipient_name");
	String message = request.getParameter("message");
        List messages = (List) session.getAttribute("lyskom.chat.messages");
	if (!message.trim().equals("") && recipientName != null && !"".equals(recipientName.trim())) {
	    try {
		ConfInfo recipient = lookupName(lyskom, recipientName, true, true);
;
		defaultRecipient = recipient.getNo();
		List tmp = new LinkedList();
		tmp.add(new Integer(recipient.getNo()));
		tmp.add(message);
		if (messages != null) {
		    synchronized (messages) {
		        messages.add(tmp);
		    }	
		}
		try {
		    lyskom.sendMessage(recipient.getNo(), message);
		    if (recipient.getNo() != me) {
		    	synchronized (messages) {
		            messages.notifyAll();
			}
		    }
		} catch (RpcFailure ex1) {
		    synchronized (messages) {
		        messages.remove(tmp);
		    }			    
		    if (ex1.getError() == Rpc.E_message_not_sent) {
			out.println("<script>window.alert('Meddelandet kunde inte skickas.');</script>");
		    } else {
			throw ex1;
		    }
		}

	    } catch (AmbiguousNameException ex1) {
		out.println("<script language=\"JavaScript1.2\">window.alert('Namnet du angav är flertydigt.');</script>");
	    }
	} else {
	    int recipientNo = Integer.parseInt(request.getParameter("recipient"));
	    defaultRecipient = recipientNo;
	    List tmp = new LinkedList();
	    tmp.add(new Integer(recipientNo));
	    tmp.add(message);
	    if (messages != null) {
	    	synchronized (messages) {
		    messages.add(tmp);
	    	}
	    }

	    try {
		lyskom.sendMessage(recipientNo, message);
		if (recipientNo != me) {
		    synchronized (messages) {
		        messages.notifyAll();
		    }
		}
	    } catch (RpcFailure ex1) {
		synchronized (messages) {
		    messages.remove(tmp);
		}
		if (ex1.getError() == Rpc.E_message_not_sent) {
		    out.println("<script>window.alert('Meddelandet kunde inte skickas.');</script>");
		} else {
		    throw ex1;
		}
	    }

	}
    }
    if (defaultRecipient != 0) people.add(lyskom.getUConfStat(defaultRecipient));


%>
    <form method="post" action="<%=basePath%>/chat_sender.jsp">
	Mottagare: <select name="recipient">
	<option value="0">&lt;Alla&gt; (alarmmeddelande)
<%
    synchronized (people) {
	for (Iterator i = people.iterator(); i.hasNext();) {
	    UConference conf = (UConference) i.next();
	    int confNo = conf.getNo();
	    String name = lyskom.toString(conf.getName());
	    out.print("<option value=\"" + confNo + "\"");
	    if (confNo == defaultRecipient)
		out.print(" selected ");
	    out.print(">");
	    out.println(htmlize(name));
	}
    }
%>
	</select>
	/
	<input type="text" size="40" name="recipient_name" /><br />
	<input type="text" size="80" name="message" />
	<input type="submit" name="btnS" value="skicka" /><br />
    (<a href="<%=basePath%>/chat_sender.jsp?reset">uppdatera namnlista</a>,
     <a target="_top" href="<%=basePath%>?stopChat">avsluta chatläge</a>)
    </form>
  </body>
</html>
