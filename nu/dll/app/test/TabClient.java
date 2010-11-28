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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JComboBox;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import nu.dll.lyskom.*;

public class TabClient {
    List clients = null;
    JFrame frame = null;
    JTabbedPane tabPane = null;

    class SessionPanel extends JPanel {
	String tabName;
	Test2 t2;
	public SessionPanel(Test2 t2, String name) {
	    tabName = name;
	    this.t2 = t2;
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
			try {
			    shutdown();
			} catch (IOException ex1) {}
			tabPane.setSelectedIndex(tabPane.getTabCount()-2);
			tabPane.removeTabAt(tabPane.indexOfTab(tabName));
		    }
		});

	    JPanel filler = new JPanel();
	    constr.fill = GridBagConstraints.HORIZONTAL;
	    constr.gridx=1;
	    gridBag.setConstraints(filler, constr);
	    add(filler);

	    //t2.getConsole().setLineWrap(true);
	    JScrollPane consolePane = new JScrollPane(t2.getConsole());	    
	    constr.gridwidth = 2;
	    constr.gridx=0;
	    constr.gridy=1;
	    constr.fill = GridBagConstraints.BOTH;
	    gridBag.setConstraints(consolePane, constr);
	    add(consolePane);
	}
	public void shutdown() throws IOException {
	    t2.shutdown();
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
	JComboBox configChooser = null;
	JTextField configNameField = null;
	JButton configSaveButton = null;
	JButton configDeleteButton = null;
	public SetupPanel() {
	    setLayout(new GridLayout(3, 1));
	    JPanel serverPanel = new JPanel();
	    serverPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
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
	    //passwordField.setEchoChar('¤');
	    settingsPanel.add(passwordField);
	    add(settingsPanel);

	    // set up the listeners after all the widgets has been
	    // instantiated, so that we can pass references of them.
	    SetupActionListener setupActionListener =
		new SetupActionListener(serverField, nameField, passwordField);
	    serverField.addKeyListener(setupActionListener);
	    connectButton.addActionListener(setupActionListener);
	    passwordField.addKeyListener(setupActionListener);
	    nameField.addKeyListener(setupActionListener);

	    JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    configChooser = new JComboBox();
	    configNameField = new JTextField(16);
	    configSaveButton = new JButton("Spara");
	    configDeleteButton = new JButton("Ta bort");
	    configSaveButton.addActionListener(new SaveLoadActionListener());
	    configChooser.addActionListener(new SaveLoadActionListener());
	    configDeleteButton.addActionListener(new SaveLoadActionListener());
	    filePanel.add(configChooser);
	    filePanel.add(configNameField);
	    filePanel.add(configSaveButton);
	    filePanel.add(configDeleteButton);
	    add(filePanel);
	    populateConfigChooser();
	    //setMaximumSize(getPreferredSize());
	    Debug.println("MaximumSize: " + getMaximumSize());
	}
	String suffix = ".session";

	class SaveLoadActionListener implements ActionListener {
	    public void actionPerformed(ActionEvent event) {
		if (event.getSource() == configDeleteButton) {
		    File homeDir = new File(System.getProperty("user.home"));
		    File confDir = new File(homeDir, ".lattekom");
		    File propFile = new File(confDir, configChooser.getSelectedItem().toString() + suffix);
		    propFile.delete();
		    populateConfigChooser();
		}
		if (event.getSource() == configSaveButton) {
		    File homeDir = new File(System.getProperty("user.home"));
		    File confDir = new File(homeDir, ".lattekom");
		    Properties props = new Properties();
		    props.put("server", serverField.getText());
		    props.put("username", nameField.getText());
		    props.put("password", new String(passwordField.getPassword()));
		    try {
			String fileName = configNameField.getText() + suffix;
			FileOutputStream out = new FileOutputStream(new File(confDir, fileName));
			props.save(out, null);
			out.close();
			populateConfigChooser();
		    } catch (IOException ex1) {
			Debug.println(ex1.getMessage());
		    }
		}
		if (event.getSource() instanceof JComboBox) {
		    ConfigProperties props = (ConfigProperties) configChooser.getSelectedItem();
		    if (props != null) {
			serverField.setText(props.server);
			nameField.setText(props.username);
			passwordField.setText(props.password);
			configNameField.setText(props.name);
		    }
		}
	    }
	}
	
	void populateConfigChooser() {
	    File homeDir = new File(System.getProperty("user.home"));
	    File confDir = new File(homeDir, ".lattekom");
	    if (!confDir.exists()) {
		if (confDir.mkdir()) 
		    Debug.println("created lattekom dir");
		else
		    Debug.println("did not create lattekom dir");
	    }
	    String[] files = confDir.list(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
			return name.endsWith(suffix);
		    }
		});

	    configChooser.removeAllItems();
	    for (int i=0; i < files.length; i++) {
		File f = new File(confDir, files[i]);
		String fileName = f.getName();
		String name = fileName.substring(0, fileName.length() - suffix.length());
		ConfigProperties item = new ConfigProperties();
		item.name = name;
		try {
		    Properties confProps = new Properties();
		    confProps.load(new FileInputStream(f));
		    item.server = confProps.getProperty("server");
		    item.username = confProps.getProperty("username");
		    item.password = confProps.getProperty("password");
		    configChooser.addItem(item);
		} catch (IOException ex1) {
		    Debug.println("Error loading " + f + ": " + ex1.getMessage());
		}
	    }
	}

	class ConfigProperties {
	    public String name = null;
	    public String server = null;
	    public String username = null;
	    public String password = null;
	    public String toString() { return name != null ? name : super.toString(); }
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
	JPanel sessionPanelPanel = new JPanel(new GridLayout(1, 1));
	sessionPanelPanel.add(sessionPanel);
	tabPane.addTab(tabName, sessionPanelPanel);
	tabPane.setSelectedIndex(tabPane.getTabCount()-1);
	clients.add(t2);

	TabClientCommands newCommands = new TabClientCommands(this);
	t2.addCommand(newCommands);
	frame.pack();
	t2.setVersion("TabbedGUI", "$Version$");
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
	setupRootPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
	SetupPanel setupPanel = new SetupPanel();
	setupRootPanel.add(setupPanel);
	tabPane.add("Start", setupRootPanel);

	frame.setTitle("LatteKOM/TabbedGUI");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	//frame.setSize(800,600);

	tabPane.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    frame.pack();
		    Object source = e.getSource();
		    if (source instanceof JTabbedPane) {
			JTabbedPane tabPane = (JTabbedPane) source;
			Component nowVisible = tabPane.getSelectedComponent();
			Debug.println("stateChanged(): selected component " + nowVisible.getClass().getName());
			if (nowVisible instanceof SessionPanel) {
			    Debug.println("stateChanged(): requesting console focus");
			    ((SessionPanel) nowVisible).t2.getConsole().requestFocus();
			} else if (nowVisible instanceof TextComposer) {
			    TextComposer tc = (TextComposer) nowVisible;
			    if (tc.isCommentOrFootnote()) tc.bodyArea.requestFocus();
			    else tc.subjectField.requestFocus();
			}
			
		    }
		}
	    });

	frame.pack();
	//frame.setResizable(false);
	frame.setVisible(true);
    }

    public void run() {

    }

    public static void main(String[] argv) {
	TabClient c = new TabClient();
    }

}
