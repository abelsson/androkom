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
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JPasswordField;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.ViewportLayout;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

public class TabClient {
    List clients = null;
    JFrame frame = null;
    JTabbedPane tabPane = null;

    class SessionPanel extends JPanel {
	public SessionPanel(Test2 t2) {
	    setLayout(new FlowLayout());
	    JPanel buttonPanel = new JPanel();
	    buttonPanel.setLayout(new FlowLayout());
	    buttonPanel.add(new JButton("Stäng"));
	    add(buttonPanel);
	    add(new JScrollPane(t2.getConsole()));
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
	    setMaximumSize(getPreferredSize());
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
	tabPane.addTab(tabName, new SessionPanel(t2));
	tabPane.setSelectedIndex(tabPane.getTabCount()-1);
	clients.add(t2);
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
	frame.setSize(800,600);	
	frame.pack();
	frame.setVisible(true);
    }

    public void run() {

    }

    public static void main(String[] argv) {
	TabClient c = new TabClient();
    }

}
