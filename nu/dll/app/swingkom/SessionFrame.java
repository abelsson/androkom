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

    JButton defaultCommandButton;

    JDesktopPane desktop;

    public final static int DEBUG = 255;

    public final static int STATE_NONE = 0;
    public final static int STATE_LOGIN = 1;
    public final static int STATE_CONNECT = 2;
    public final static int STATE_GET_MEMBERSHIP = 3;
    public final static int STATE_ONLINE = 4;

    int threadState = STATE_NONE;

    int nextUnread = 0;

    volatile boolean markAsRead = false;
    volatile boolean readRecursive = false;

    JButton connectButton;
    JButton abortButton; // cancel or disconnect

    JTextArea logArea;

    JTabbedPane mainSessionTabPane;
    JScrollPane indexScrollPane, readerScrollPane;
    
    Container unreadIndexBox;
    UnreadDisplay unreadDisplay;
    
    Dimension tabPaneDimension = new Dimension(700, 100);

    Stack toRead = new Stack();

    Session kom;

    static String defaultServer = "sno.pp.se";
    static int defaultPort = 4894;

    MembershipList membershipList;

    int currentConference = 0;

    String server;
    int port;

    public SessionFrame(int id) {
	super("Connection "+id, true, true, true, true);
	Debug.println("--> SessionFrame()");
	this.id = id;
	frameCount++;

	kom = new Session();

	membershipList = new MembershipList(kom);

	kom.addRpcEventListener(this);


	mainSessionTabPane = new JTabbedPane();
	mainSessionTabPane.addTab("Inställningar", null, setupSetupPanel(),
				  "Sessionsinställningar");
	mainSessionTabPane.addTab("Möten", null, setupConfSelector(),
				  "Medlemskapslista");
	mainSessionTabPane.addTab("Logg", null, setupLogPanel(),
				  "Sessionslogg");
	
	unreadIndexBox = new Box(BoxLayout.Y_AXIS);

	indexScrollPane = new JScrollPane(unreadIndexBox);
	indexScrollPane.setPreferredSize(new Dimension(0, 100));

	JPanel bottomPanel = new JPanel();
	bottomPanel.setLayout(new BorderLayout());
	bottomPanel.add(indexScrollPane, BorderLayout.CENTER);
	bottomPanel.add(setupCommandPanel(), BorderLayout.SOUTH);

	JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					      mainSessionTabPane, bottomPanel);
	mainPanel.setDividerLocation(160);

	setContentPane(mainPanel);
	
	pack();

	setPreferredSize(new Dimension(640, 640));
	setLocation(0,0);
	
	show();

    }

    JComponent setupLogPanel() {
	
	logArea = new JTextArea();
	logArea.setText("");
	JScrollPane scroller = new JScrollPane(logArea);
	scroller.setMaximumSize(tabPaneDimension);
	return scroller;
    }

    void log(String s) {
	logArea.append(s+"\n");
    }

    JPanel setupCommandPanel() {
	JPanel panel = new JPanel();
	panel.setLayout(new FlowLayout(FlowLayout.LEFT));
	
	JButton dcButton = new JButton("Nästa olästa inlägg");
	dcButton.setEnabled(false);
	dcButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    showText(nextUnread);
		}
	    });
	
       	panel.add(dcButton);
	defaultCommandButton = dcButton;
	return panel;
    }

    class UnreadDisplay extends JPanel {
        SessionFrame parent;
        Session kom;
        JList list;
        MembershipList mlist;

        Component infoLabel = null;
        public UnreadDisplay(SessionFrame _parent, Session _kom) throws IOException {
            parent = _parent;
            kom = _kom;          
            setLayout(new BorderLayout());
            mlist = new MembershipList(kom);
            list = new JList(mlist);
            list.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    Membership m = (Membership) mlist.getList()[list.getSelectedIndex()];
                    try {
                        updateInfoLabel(m);
                    } catch (IOException ex) {
                        SwingKOM.panic(ex);
                    }
               } 
            });
            
            kom.updateUnreads();
            
            mlist.setList(kom.getUnreadMembership());
            Membership m = null;
            if (list.getSelectedIndex() != -1) m = mlist.getList()[list.getSelectedIndex()];
            updateInfoLabel(m);
            add(list, BorderLayout.NORTH);
        }

	void update() throws IOException {
	    kom.updateUnreads();
	    mlist.setList(kom.getUnreadMembership());
	    Membership m = null;
	    if (list.getSelectedIndex() != -1) m = mlist.getList()[list.getSelectedIndex()];
	    updateInfoLabel(m);
	}
        
        void updateInfoLabel(Membership m) throws IOException {
            if (m != null) {
                if (infoLabel != null) { Debug.println("removing " + infoLabel); remove(infoLabel); }
                infoLabel = new ConfUnreadInfoLabel(m);
		Debug.println("adding " + infoLabel);
                add(infoLabel, BorderLayout.SOUTH);
            } else {
                if (infoLabel == null) add(infoLabel = new JLabel("***"), BorderLayout.SOUTH);
            }
	    repaint();
        
        }
        
        class ConfUnreadInfoLabel extends JPanel {
            Membership membership;
            public ConfUnreadInfoLabel(Membership m) throws IOException {
                membership = m;
                setLayout(new FlowLayout(FlowLayout.LEFT));
                int confNo = membership.getNo();
                add(new JLabel(new String(kom.getConfName(confNo)) + ": "));
                int lastTextRead = membership.getLastTextRead();
                UConference uconf = kom.getUConfStat(confNo);
                TextMapping unreadMap = kom.localToGlobal(confNo, lastTextRead + 1, 5);
                if (unreadMap.hasMoreElements()) {
		    nextUnread = ((Integer) unreadMap.nextElement()).intValue();
                    TextInfoLabel t = new TextInfoLabel("nästa olästa: ", nextUnread,
                                          kom, null, SwingConstants.LEFT);
		    defaultCommandButton .setEnabled(true);
		    kom.changeConference(confNo);
                    t.setSessionFrame(parent);
                    add(t);
		    repaint();
                } else {
		    JLabel t = new JLabel("inga olästa inlägg");
		    defaultCommandButton.setEnabled(false);
		    nextUnread = 0;
		    add(t);
		    repaint();
		}

            }   
        }
    }


    Container setupUnreadIndexBox() throws IOException {
        return unreadDisplay = new UnreadDisplay(this, kom);
        
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

    public JDesktopPane getDesktop() {
        return desktop;   
    }

    public void setDesktop(JDesktopPane desktop) {
        this.desktop = desktop;   
    }

    void setStatus(String s) {
	setTitle("LysKOM-session "+id+": "+s);
    }

    JPanel setupButtonPanel() {
	JPanel p = new JPanel();
	p.setLayout(new FlowLayout(FlowLayout.RIGHT));

	connectButton = new JButton("Anslut");
	abortButton = new JButton("Avbryt");

	p.add(connectButton);
	p.add(abortButton);

	ActionListener l = new ButtonListener();
	connectButton.addActionListener(l);
	abortButton.addActionListener(l);

	return p;
    }    



    JPanel setupUserPanel() {

	passwordField = new JPasswordField(12);
	passwordField.setEditable(true);
	passwordField.setText("seven11");

	JPanel passwordPanel = new JPanel();
	passwordPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
	passwordPanel.add(new JLabel("Lösenord: ", JLabel.RIGHT));
	passwordPanel.add(passwordField);

	userNameField = new JTextField(25);
	userNameField.setEditable(true);
	userNameField.setText("Testgubbe");

	JPanel namePanel = new JPanel();
	namePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
	namePanel.add(new JLabel("Namn: ", JLabel.RIGHT));
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
	hostPanel.add(new JLabel("Servernamn: ", JLabel.RIGHT), "label");
	hostPanel.add(hostField, "field");

	hostPortPanel.add(hostPanel);
	hostPortPanel.add(portPanel);
	return hostPortPanel;
    }

    JPanel setupConfSelector() {
	JPanel confSel = new JPanel();
	confSel.setLayout(new BoxLayout(confSel, BoxLayout.Y_AXIS));
	JList confList = new JList(membershipList);
	JScrollPane scrollPane = new JScrollPane();
	scrollPane.getViewport().setView(confList);
	confSel.add(scrollPane);

	JButton getMembershipBtn = new JButton("Hämta medlemskapslista");
	confSel.add(getMembershipBtn);

	getMembershipBtn.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    try {
			kom.doGetMembership(kom.getMyPerson().getNo());
		    } catch (IOException ex) {
			SwingKOM.panic(ex);
		    }
		}
	    });

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
    
    public void changeConference(int confNo) {
	try {
	    kom.changeConference(confNo);
	    currentConference = confNo;
	    log("Changed conference to " + confNo);
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

            Object aux = e.getCall().getAux(0);
            if (threadState == STATE_GET_MEMBERSHIP) {
                Membership m = (Membership) aux;
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

	    }

	    
	    
	    break;
	default:
	    if (DEBUG>0)
		Debug.println("SwingKOM unhandled RPC event: "+e);
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
	//setCursor(new Cursor(Cursor.WAIT_CURSOR));
	switch(threadState) {
	case STATE_CONNECT:
	threadState = STATE_CONNECT;
	    Debug.println("STATE_CONNECT");
	    int uport = 4894;
	    try { port = uport = Integer.parseInt(portField.getText()); }
	    catch (NumberFormatException ex) {}
	    try {
		kom.connect(server = hostField.getText(), uport);
	    } catch (IOException ex) {
		networkError("Anslutningen misslyckades: " + ex.getMessage());
		gotDisconnected();
	    }
	    if (kom.getConnected()) {
		gotConnected();
	    }
	
	    break;
	case STATE_LOGIN:

	    Debug.println("STATE_LOGIN");
	    setStatus("ansluten - loggar in i LYSKOM");	    
	    try {
		
		// look up the names
		ConfInfo names[] = kom.lookupName(userNameField.getText(),
						  true, false);
		if (names.length > 1) {
		    StringBuffer msg = new StringBuffer("Angett namn är flertydigt. Möjliga namn är:\n");
		    for (int i=0;i<names.length;i++) {
			msg.append(new String(names[i].confName) + " (" + 
					      names[i].confNo + ")\n");
		    }
		    notify(msg.toString());
		} else if (names.length == 0) {
		    notifyError("Det finns inget namn som matchar \"" +
				userNameField.getText() + "\"");
		} else {
		    if (!kom.login(names[0].confNo, passwordField.getText(), false)) {
			notifyError("Inloggningen misslyckades");
		    }
		}
	    } catch (IOException ex) {
		networkError("I/O-fel under inloggning: " + ex.getMessage());
		gotDisconnected();
	    }
	    if (kom.getLoggedIn()) {
		abortButton.setText("Koppla ned");
		connectButton.setText("Återanslut");
		userNameField.setText(new String(kom.getMyPerson().
						 getUConference().getName()));
		gotLoggedIn();
	    }
	    break;
	case STATE_GET_MEMBERSHIP:

            System.err.println("STATE_GET_MEMBERSHIP");	    
            //kom.doGetMembership(kom.getMyPerson().getNo());
            //kom.updateUnreads();
            setCursor(null);
	
            threadState = STATE_ONLINE;

            new Thread(this).start();

	    setStatus(kom.getMyPerson().getNo() +
		      "@"+server+":"+port);
	    setCursor(null);
 	    
            
	    break;
	case STATE_ONLINE:
	    try {
                indexScrollPane.setViewportView(unreadIndexBox = setupUnreadIndexBox());
		kom.changeWhatIAmDoing("leker hacker");
            } catch (IOException ex) {
                networkError("I/O-fel: " + ex.getMessage());
            }
                
            
            break;
	}
	Debug.println("--- connect thread finished");
    }
    
    public void showText(int textNo) {
        try {	    
            new InternalTextFrame(this, kom, textNo);
	    Text t = kom.getText(textNo);
	    int cc = kom.getCurrentConference();
	    int[] _local = { t.getLocal(cc) };
	    kom.markAsRead(kom.getCurrentConference(), _local); // xxx

	    unreadDisplay.update();

	    
        } catch (IOException ex) {
            networkError("I/O-fel: " + ex.getMessage());   
        }
    }

    public int nextUnreadConference(boolean change) throws IOException {
        int c = kom.nextUnreadConference(change);
        return c;
    }
    
    public int nextUnreadText(boolean markRead) throws IOException {
        int t = kom.nextUnreadText(markRead);
        return t;
    }

    public synchronized int popText() {
        Integer i = (Integer) toRead.pop();
        
        return i == null ? -1 : i.intValue();
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



