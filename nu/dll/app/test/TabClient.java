package nu.dll.app.test;

import javax.swing.JFrame;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JPasswordField;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.ViewportLayout;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.IOException;
import nu.dll.lyskom.*;

public class TabClient {
    List clients = null;
    JFrame frame = null;
    JTabbedPane tabPane = null;

    class SessionPanel extends JPanel {
	String tabName;
	public SessionPanel(Test2 t2, String name) {
	    tabName = name;
	    GridBagLayout gridBag = new GridBagLayout();
	    GridBagConstraints constr = new GridBagConstraints();
	    setLayout(gridBag);

	    JButton closeButton = new JButton("Stäng session");
	    constr.fill = GridBagConstraints.NONE;
	    constr.gridx=0;
	    constr.gridy=0;
	    gridBag.setConstraints(closeButton, constr);
	    add(closeButton);

	    closeButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent event) {
			tabPane.remove(tabPane.indexOfTab(tabName));
		    }
		});

	    JPanel filler = new JPanel();
	    constr.fill = GridBagConstraints.HORIZONTAL;
	    constr.gridx=1;
	    gridBag.setConstraints(filler, constr);
	    add(filler);

	    JScrollPane consolePane = new JScrollPane(t2.getConsole());
	    constr.gridwidth = 2;
	    constr.gridx=0;
	    constr.gridy=1;
	    constr.fill = GridBagConstraints.BOTH;
	    gridBag.setConstraints(consolePane, constr);
	    add(consolePane);
	}
    }

    class SetupPanel extends JPanel {
	class SetupActionListener extends KeyAdapter implements ActionListener {
	    JTextField textField = null;
	    JTextField nameField = null;
	    JPasswordField passwordField = null;
	    public SetupActionListener(JTextField _textField, JTextField _nameField,
				       JPasswordField _passwordField) {
		textField = _textField;
		nameField = _nameField;
		passwordField = _passwordField;
	    }

	    boolean connect() {
		if (textField.getText().trim().equals("")) return false;
		addConnection(textField.getText(), nameField.getText(),
			      new String(passwordField.getPassword()));
		return true;
	    }
	    public void keyReleased(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_ENTER)
		    connect();
	    }
	    public void actionPerformed(ActionEvent event) {
		connect();
	    }
	}
	
	JTextField serverField = null;
	JTextField nameField = null;
	JPasswordField passwordField = null;

	public SetupPanel() {
	    setLayout(new GridLayout(2, 1));
	    JPanel serverPanel = new JPanel();
	    serverPanel.setLayout(new FlowLayout());
	    serverPanel.add(new JLabel("Anslut till server: "));
	    serverField = new JTextField(30);
	    serverPanel.add(serverField);
	    JButton connectButton = new JButton("Anslut");
	    serverPanel.add(connectButton);

	    add(serverPanel);
	    
	    JPanel settingsPanel = new JPanel();
	    settingsPanel.add(new JLabel("Namn: "));
	    nameField = new JTextField(30);
	    settingsPanel.add(nameField);
	    settingsPanel.add(new JLabel("Lösenord: "));
	    passwordField = new JPasswordField(8);
	    passwordField.setEchoChar('¤');
	    settingsPanel.add(passwordField);
	    add(settingsPanel);

	    // set up the listeners after all the widgets has been
	    // instantiated, so that we can pass references of them.
	    serverField.addKeyListener(new SetupActionListener(serverField, nameField,
							       passwordField));
	    connectButton.addActionListener(new SetupActionListener(serverField, nameField,
								    passwordField));
	    passwordField.addKeyListener(new SetupActionListener(serverField, nameField,
								 passwordField));
	    nameField.addKeyListener(new SetupActionListener(serverField, nameField,
							     passwordField));

	    //setMaximumSize(getPreferredSize());
	    Debug.println("MaximumSize: " + getMaximumSize());

	}
    }

    class StatusPanel extends JPanel {

	public StatusPanel() {
	    setLayout(new FlowLayout());
	}
	
    }

    public TabClient() {
	init();
    }

    Map serverNames = new HashMap();
    int editCount = 1;

    void addConnection(String server, String username, String password) {
	Test2 t2 = new Test2(true, server);
	if (!username.trim().equals("")) t2.setDefaultUser(username);
	if (!password.trim().equals("")) t2.setDefaultPassword(password);
	String tabName = server;
	int count = 1;
	if (tabPane.indexOfTab(tabName) != -1) {
	    count = ((Integer) serverNames.get(server)).intValue()+1;
	    tabName = server + " #" + count;
	}
	serverNames.put(server, new Integer(count));
	SessionPanel sessionPanel = new SessionPanel(t2, tabName);
	tabPane.addTab(tabName, sessionPanel);
	tabPane.setSelectedIndex(tabPane.getTabCount()-1);
	clients.add(t2);

	t2.addCommand("k", new AbstractCommand() {
		public int doCommand(String s, String parameters)
		throws IOException, CmdErrException {
		    int textNo = 0;
		    Text text = null;
		    boolean footnote = false;
		    StringTokenizer st = null;
		    
		    if (parameters != null) {
			st = new StringTokenizer(parameters, " ");
			try {
			    if (st.hasMoreTokens())
				textNo = Integer.parseInt(st.nextToken());
			} catch (NumberFormatException ex1) {
			    textNo = -1;
			}
		    }
		    if (textNo < 1) {
			if (footnote && application.getLastSavedText() != null)
			    textNo = application.getLastSavedText().getNo();
			else
			    if (application.getLastText() != null)
				textNo = application.getLastText().getNo();
		    }
		    
		    if (textNo < 1) {
			throw new CmdErrException("Du måste ange ett giltigt textnummer eller läsa/skriva en text först");
		    }
		    
		    text = session.getText(textNo);
		    
		    if (text == null) {
			text = session.getText(textNo);
			if (text == null) throw new CmdErrException("Hittade inget inlägg");
		    }

		    TextComposer composer = new TextComposer(session, text, true);
		    String tabName = "edit";
		    editCount++;
		    if (tabPane.indexOfTab(tabName) != -1) {
			tabName = "edit-" + editCount;
		    }
		    tabPane.addTab(tabName, composer);
		    tabPane.setSelectedIndex(tabPane.getTabCount()-1);
		    composer.waitForAction();
		    tabPane.remove(composer);

		    int newTextNo = 0;
		    try {
			newTextNo = session.createText(composer.getNewText());
			application.setLastSavedText(session.getText(newTextNo));
		    } catch (RpcFailure ex1) {
			application.consoleWrite("%Fel: kunde inte skapa kommentar/fotnot: ");
			switch (ex1.getError()) {
			case Rpc.E_not_author:
			    application.consoleWriteLn("du är inte författare till text " + ex1.getErrorStatus());
			    break;
			default:
			    throw new CmdErrException("Okänt fel: " + ex1.getMessage());
			}
			return Command.ERROR;
		    }
		    if (newTextNo > 0) {
			application.consoleWriteLn("text nummer " + newTextNo + " skapad.");
			if (!application.dontMarkOwnTextsAsRead) {
			    application.markAsRead(newTextNo);
			}
		    }

		    editCount--;
		    return Command.OK;
		}
		public String[] getCommandDescriptions() {
		    return new String[0];
		}
	    });
	frame.pack();
	new Thread(t2, "connMain-" + connCount++).start();
    }
    static int connCount = 0;

    private void init() {
	clients = new ArrayList();
	frame = new JFrame();
	Container contentPane = frame.getContentPane();
	tabPane = new JTabbedPane(JTabbedPane.LEFT);
	contentPane.add(tabPane);
	JPanel setupRootPanel = new JPanel();
	setupRootPanel.setLayout(new FlowLayout());
	SetupPanel setupPanel = new SetupPanel();
	setupRootPanel.add(setupPanel);
	tabPane.add("Start", setupRootPanel);

	frame.setTitle("LatteKOM/T2");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	//frame.setSize(800,600);

	tabPane.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    frame.pack();
		}
	    });

	frame.pack();
	frame.setResizable(false);
	frame.setVisible(true);
    }

    public void run() {

    }

    public static void main(String[] argv) {
	TabClient c = new TabClient();
    }

}
