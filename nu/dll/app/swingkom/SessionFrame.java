package nu.dll.app.swingkom;

import nu.dll.lyskom.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class SessionFrame extends JInternalFrame
implements Runnable, RpcEventListener {
    int id;
    static int frameCount = 0;
    
    JTextField hostField, portField;
    JTextField userNameField;
    JTextField passwordField;

    public final static int DEBUG = 255;

    public final static int STATE_NONE = 0;
    public final static int STATE_LOGIN = 1;
    public final static int STATE_CONNECT = 2;
    public final static int STATE_GET_MEMBERSHIP = 3;
    int threadState = STATE_NONE;

    JButton connectButton;
    JButton abortButton; // cancel or disconnect

    JTextArea logArea;

    JTabbedPane mainSessionTabPane;

    Session kom;

    static String defaultServer = "kom.lysator.liu.se";
    static int defaultPort = 4894;

    MembershipList membershipList;

    int currentConference = 0;

    String server;
    int port;

    public SessionFrame(int id) {
	super("Connection "+id, true, true, true, true);
	this.id = id;
	frameCount++;

	kom = new Session();

	membershipList = new MembershipList(kom);

	kom.addRpcEventListener(this);


	mainSessionTabPane = new JTabbedPane();
	mainSessionTabPane.addTab("Setup", null, setupSetupPanel(),
				  "Session setup options");
	mainSessionTabPane.addTab("Conferences", null, setupConfSelector(),
				  "Conference membership list");
	mainSessionTabPane.addTab("Log", null, setupLogPanel(),
				  "Session log");
	//Panel p = new Panel(new GridLayout(1,1));
	//p.add(mainSessionTabPane);


	JPanel mainPanel = new JPanel(new FlowLayout());
	mainPanel.add(mainSessionTabPane);
	setContentPane(mainPanel);
	//passwordField.setSize(50, 50);
	pack();

	//setBounds(30, 30, 400, getHeight());
	setLocation(30, 30);

	//setBounds(30, 30, 400, 100);

    }

    JPanel setupLogPanel() {
	JPanel log = new JPanel(new GridLayout(1,1));
	logArea = new JTextArea();
	log.add(logArea);
	return log;
    }

    void log(String s) {
	logArea.append(s+"\n");
    }

    JPanel setupSetupPanel() {
	JPanel fields;
	fields = new JPanel();
	fields.setBorder(new EmptyBorder(10, 10, 10, 10));
	fields.setLayout(new GridLayout(3, 1));
	fields.add(setupHostPanel());
	fields.add(setupUserPanel());
	fields.add(setupButtonPanel());
	return fields;
    }
    void setStatus(String s) {
	setTitle("Connection "+id+": "+s);
    }

    JPanel setupButtonPanel() {
	JPanel p = new JPanel();
	p.setLayout(new FlowLayout(FlowLayout.RIGHT));

	connectButton = new JButton("Connect");
	abortButton = new JButton("Cancel");

	p.add(connectButton);
	p.add(abortButton);

	ActionListener l = new ButtonListener();
	connectButton.addActionListener(l);
	abortButton.addActionListener(l);

	return p;
    }    



    JPanel setupUserPanel() {

	passwordField = new JPasswordField(12);

	JPanel passwordPanel = new JPanel();
	passwordPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
	passwordPanel.add(new JLabel("Password: ", JLabel.RIGHT));
	passwordPanel.add(passwordField);

	userNameField = new JTextField(25);
	
	JPanel namePanel = new JPanel();
	namePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
	namePanel.add(new JLabel("User name: ", JLabel.RIGHT));
	namePanel.add(userNameField);

	JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	userPanel.add(namePanel);
	userPanel.add(passwordPanel);
	return userPanel;
    }

    JPanel setupHostPanel() {
	JPanel hostPortPanel = new JPanel();
	FlowLayout mainLayout = new FlowLayout(FlowLayout.LEFT);
	mainLayout.setHgap(10);
	hostPortPanel.setLayout(mainLayout);

	portField = new JTextField(""+defaultPort, 4);
	hostField = new JTextField(defaultServer, 40);

	JPanel portPanel = new JPanel();
	portPanel.setLayout(new LabeledPairLayout());
	portPanel.add(new JLabel("Port: ", JLabel.RIGHT), "label");
	portPanel.add(portField, "field");

	JPanel hostPanel = new JPanel();
	hostPanel.setLayout(new LabeledPairLayout());
	hostPanel.add(new JLabel("Hostname: ", JLabel.RIGHT), "label");
	hostPanel.add(hostField, "field");

	hostPortPanel.add(hostPanel);
	hostPortPanel.add(portPanel);
	return hostPortPanel;
    }

    JPanel setupConfSelector() {
	JPanel confSel = new JPanel();
	confSel.setLayout(new GridLayout(1, 1));
	JList confList = new JList(membershipList);
	JScrollPane scrollPane = new JScrollPane();
	scrollPane.getViewport().setView(confList);
	confSel.add(scrollPane);

	confList.addListSelectionListener(new ListSelectionListener() {
	    public void valueChanged(ListSelectionEvent e) {
		Membership m = (Membership)
		    membershipList.getList()[e.getFirstIndex()];
		if (m.conference != currentConference) {
		    changeConference(m);
		}
	    }
	});
	    
	
	return confSel;
    }

    /* used for conferences the user already is a member of */
    public void changeConference(Membership m) {
	try {
	    kom.doChangeConference(m.conference).addAux((Object) m);
	} catch (IOException ex) {
	    networkError(ex.getMessage());
	}
    }

    public void rpcEvent(RpcEvent e) {
	switch (e.getOp()) {
	case Rpc.C_get_membership:
	    membershipList.setList(Membership.createFrom(e.getReply()));
	    break;
	case Rpc.C_change_conference:
	    if (!e.getSuccess()) {
		log("Failed changing conference");
		break;
	    }

	    Membership m = (Membership) e.getCall().getAux(0);
	    if (m == null) {
		try {
		    m = kom.getMembership(kom.getMyPerson().getNo(),
					  ((KomToken)
					   e.getCall().getParameterElements().
					   nextElement()).toInteger(), 1,
					  new Bitstring("0"))[0];
		} catch (IOException ex) {
		    networkError(ex.getMessage());
		    return;
		}
		    
	    }

	    currentConference = m.conference;
	    log("Changed conference to " + m);
	    
	    break;
	default:
	    if (DEBUG>0)
		System.err.println("Unhandled RPC event: "+e);
	}

    }

    void notify(String s) {
	JOptionPane.showInternalMessageDialog(this, s, "Notification",
					      JOptionPane.INFORMATION_MESSAGE);
    }
    void networkError(String s) {
	JOptionPane.showInternalMessageDialog(this, s, "Network error",
					      JOptionPane.ERROR_MESSAGE);
	setStatus("network error: "+s);
    }
    void notifyError(String s) {
	JOptionPane.showInternalMessageDialog(this, s, "Error",
					      JOptionPane.ERROR_MESSAGE);
    }

    public void run() {
	if (DEBUG>0)
	System.err.println("Starting new thread ("+Thread.currentThread()+")");
	switch(threadState) {
	case STATE_CONNECT:
	    if (DEBUG>0)
		System.err.println("STATE_CONNECT");
	    int uport = 4894;
	    try { port = uport = Integer.parseInt(portField.getText()); }
	    catch (NumberFormatException ex) {}
	    try {
		kom.connect(server = hostField.getText(), uport);
	    } catch (IOException ex) {
		networkError("Connect failed: " + ex.getMessage());
		gotDisconnected();
	    }
	    if (kom.getConnected()) {
		gotConnected();
	    }
	
	    break;
	case STATE_LOGIN:
	    if (DEBUG>0)
		System.err.println("STATE_LOGIN");
	    setStatus("connected, logging in...");	    
	    try {
		
		// look up the names
		ConfInfo names[] = kom.lookupName(userNameField.getText(),
						  true, false);
		if (names.length > 1) {
		    StringBuffer msg = new StringBuffer("The name specified is ambigous. Possible matches are:\n");
		    for (int i=0;i<names.length;i++) {
			msg.append(new String(names[i].confName) + " (" + 
					      names[i].confNo + ")\n");
		    }
		    notify(msg.toString());
		} else if (names.length == 0) {
		    notifyError("No user names matching " +
				userNameField.getText());
		} else {
		    kom.login(names[0].confNo, passwordField.getText(), false);
		}
	    } catch (IOException ex) {
		networkError("During login: " + ex.getMessage());
		gotDisconnected();
	    }
	    if (kom.getLoggedIn()) {
		abortButton.setText("Disconnect");
		connectButton.setText("Reconnect");
		userNameField.setText(new String(kom.getMyPerson().
						 uconf.name));
		gotLoggedIn();
	    }
	    break;
	case STATE_GET_MEMBERSHIP:
	    if (DEBUG>0)
		System.err.println("STATE_GET_MEMBERSHIP");	    
	    try {
		kom.doGetMembership(kom.getMyPerson().getNo());
	    } catch (IOException ex) {
		networkError("While getting membership: "+ex.getMessage());
		gotDisconnected();
	    }
	    setStatus(kom.getMyPerson().getNo() +
		      "@"+server+":"+port);
	    break;
	}
    }

    void gotDisconnected() {
	threadState = STATE_NONE;
	abortButton.setText("Cancel");
	connectButton.setText("Connect");
	kom = null;	
    }

    void gotLoggedIn() {
	threadState = STATE_GET_MEMBERSHIP;
	new Thread(this).start();
	// meanwhile, do something useful..
	//sessionSetupPanel.setVisible(false);
	//setContentPane(setupConfSelector());

    }
    void gotConnected() {
	threadState = STATE_LOGIN;
	new Thread(this).start();
	
	
    }

    void connect() {
	threadState = STATE_CONNECT;
	new Thread(this).start();

    }

    class ButtonListener implements ActionListener {
	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == connectButton) {
		connect();
	    } else if (e.getSource() == abortButton) {
		System.err.println("abort-button");
		try {
		    if (kom.getConnected())
			kom.disconnect(true);
		} catch (IOException ex) {
		    System.err.println(ex.toString());
		}
	    }
	    if (DEBUG>0) System.err.println("<-- actionPerformed()");
	}
    }

    class LabeledPairLayout implements LayoutManager {

	Vector labels = new Vector();
	Vector fields = new Vector();
	
	int yGap = 2;
	int xGap = 2;
	
	public void addLayoutComponent(String s, Component c) {
	    if (s.equals("label")) {
		labels.addElement(c);
	    }  else {
		fields.addElement(c);
	    }
	}
	
	public void layoutContainer(Container c) {
	    Insets insets = c.getInsets();
	    
	    int labelWidth = 0;
	    Enumeration labelIter = labels.elements();
	    while(labelIter.hasMoreElements()) {
		JComponent comp = (JComponent)labelIter.nextElement();
		labelWidth = Math.max( labelWidth, comp.getPreferredSize().width );
	    }
	    
	    int yPos = insets.top;
	    
	    Enumeration fieldIter = fields.elements();
	    labelIter = labels.elements();
	    while(labelIter.hasMoreElements() && fieldIter.hasMoreElements()) {
		JComponent label = (JComponent)labelIter.nextElement();
		JComponent field = (JComponent)fieldIter.nextElement();
		int height = Math.max(label.getPreferredSize().height, field.getPreferredSize().height);
		label.setBounds( insets.left, yPos, labelWidth, height ); 
		field.setBounds( insets.left + labelWidth + xGap, 
				 yPos, 
				 c.getSize().width - (labelWidth +xGap + insets.left + insets.right), 
				 height ); 
		yPos += (height + yGap);
	    }
	    
	}

      public Dimension minimumLayoutSize(Container c) {
	  Insets insets = c.getInsets();
	  
	  int labelWidth = 0;
	  Enumeration labelIter = labels.elements();
	  while(labelIter.hasMoreElements()) {
	      JComponent comp = (JComponent)labelIter.nextElement();
	      labelWidth = Math.max( labelWidth, comp.getPreferredSize().width );
	  }

	  int yPos = insets.top;

	  labelIter = labels.elements();
	  Enumeration fieldIter = fields.elements();
	  while(labelIter.hasMoreElements() && fieldIter.hasMoreElements()) {
	      JComponent label = (JComponent)labelIter.nextElement();
	      JComponent field = (JComponent)fieldIter.nextElement();
	      int height = Math.max(label.getPreferredSize().height, field.getPreferredSize().height);
	      yPos += (height + yGap);
	  }
	  return new Dimension( labelWidth * 3 , yPos );
      }

	public Dimension preferredLayoutSize(Container c) {
	    Dimension d = minimumLayoutSize(c);
	    d.width *= 2;
	    return d;
	}
	
	public void removeLayoutComponent(Component c) {}
    }
    

}



