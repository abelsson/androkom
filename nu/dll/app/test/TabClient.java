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
import java.awt.Font;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JPasswordField;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

public class TabClient {
    List clients = null;
    JFrame frame = null;
    JTabbedPane tabPane = null;
    class SetupPanel extends JPanel {
	class SetupActionListener extends KeyAdapter implements ActionListener {
	    JTextField textField = null;
	    public SetupActionListener(JTextField _textField) {
		textField = _textField;
	    }
	    boolean connectTo(String server) {
		if (server.trim().equals("")) return false;
		addConnection(server);
		return true;
	    }
	    public void keyReleased(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_ENTER)
		    connectTo(textField.getText());
	    }
	    public void actionPerformed(ActionEvent event) {
		connectTo(textField.getText());
	    }
	}
	
	JTextField serverField = null;
	public SetupPanel() {
	    setLayout(new FlowLayout());
	    add(new JLabel("Anslut till server: "));
	    serverField = new JTextField(30);
	    serverField.addKeyListener(new SetupActionListener(serverField));
	    add(serverField);
	    JButton connectButton = new JButton("Anslut");
	    connectButton.addActionListener(new SetupActionListener(serverField));
	    add(connectButton);
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
    void addConnection(String server) {
	Test2 t2 = new Test2(true, server);
	Console console = t2.getConsole();

	String tabName = server;
	int count = 1;
	if (tabPane.indexOfTab(tabName) != -1) {
	    count = ((Integer) serverNames.get(server)).intValue()+1;
	    tabName = server + " #" + count;
	}
	serverNames.put(server, new Integer(count));
	tabPane.addTab(tabName, new JScrollPane(console));
	tabPane.setSelectedIndex(tabPane.getTabCount()-1);
	frame.pack();
	clients.add(t2);
	new Thread(t2, tabName+"_Thread").start();
    }

    private void init() {
	clients = new ArrayList();
	frame = new JFrame();
	Container contentPane = frame.getContentPane();
	tabPane = new JTabbedPane(JTabbedPane.LEFT);
	contentPane.add(tabPane);
	tabPane.add("Start", new SetupPanel());

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
