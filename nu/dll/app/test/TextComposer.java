package nu.dll.app.test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import nu.dll.lyskom.*;
import nu.dll.app.swingkom.CommentListPanel;
import java.io.*;

class TextComposer extends Box {
    Session kom;
    Text commentedText;
    Text newText;
    JTextArea bodyArea;
    JTextField subjectField;
    JLabel rcptLabel;
    
    Object actionLock = new Object();

    boolean abort = false;

    public TextComposer(Session lyskom, Text text, boolean comment) throws IOException {
        super(BoxLayout.Y_AXIS);
        this.kom = lyskom;
        this.commentedText = text;

        newText = new Text(new String(text.getSubject()), "");
        
        bodyArea = new JTextArea(20, 72);
        bodyArea.setEditable(true);
        bodyArea.setFont(new Font("Courier", Font.PLAIN, 12));
        Box headerPanel = new Box(BoxLayout.Y_AXIS);
       
        JScrollPane scroll = new JScrollPane(bodyArea);

        int[] commTo = { text.getNo() };
	newText.addCommented(text.getNo());
        JPanel commp = new JPanel();
        FlowLayout commpLayout = new FlowLayout(FlowLayout.LEFT);
        commpLayout.setVgap(1);
        commp.setLayout(commpLayout);

        CommentListPanel commentedPanel = new CommentListPanel("Kommentar till text ", commTo, kom, null, 
                                                               SwingConstants.LEFT);

        commp.add(commentedPanel);
        headerPanel.add(commp);
        
        JPanel rcptPanel = new JPanel();
        rcptPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        rcptPanel.add(new JLabel("Till: "));
        rcptLabel = new JLabel();
        rcptPanel.add(rcptLabel);

	int[] recipients = text.getRecipients();
	for (int i=0; i < recipients.length; i++) {
	    Conference conf = lyskom.getConfStat(recipients[i]);
	    if (conf.getType().original()) {
		int superconf = conf.getSuperConf();
		if (superconf > 0) {
		    newText.addRecipient(superconf);
		} else {
		    abort = true;
		    actionLock.notify();
		}
	    } else {
		newText.addRecipient(recipients[i]);
	    }
	}


        int[] rcpts = newText.getStatInts(TextStat.miscRecpt);
        String a = "";
        try {
            for (int i=0; i < rcpts.length; i ++) {
                a = a + new String(kom.getConfName(rcpts[i])) + (i < rcpts.length-1 ? ", " : "");
            }
        } catch (IOException ex) {
            System.err.println("** I/O error: " + ex.getMessage());
            System.exit(42);   
            
        }
        rcptLabel.setText(a);    
        
        
        headerPanel.add(rcptPanel);
        
        JPanel subjectPanel = new JPanel();
        subjectPanel.setLayout(new FlowLayout(FlowLayout.LEFT));        
        JLabel subjectLabel = new JLabel("Ämne: ");
        subjectPanel.add(subjectLabel);
        subjectField = new JTextField(new String(text.getSubject()), 72);
        subjectField.setFont(new Font("Courier", Font.PLAIN, 12));
        subjectPanel.add(subjectField);
        headerPanel.add(subjectPanel);
        
        add(headerPanel);
        add(scroll);
        
	GridBagLayout gridBag = new GridBagLayout();
	GridBagConstraints constr = new GridBagConstraints();
        JPanel optionsPanel = new JPanel(gridBag);
        JPanel fillPanel = new JPanel();
	constr.fill = GridBagConstraints.HORIZONTAL;
	gridBag.setConstraints(fillPanel, constr);
	optionsPanel.add(fillPanel);
	constr.gridx = 1;
	constr.fill = GridBagConstraints.NONE;
        JButton submitText = new JButton("Skicka");
	gridBag.setConstraints(submitText, constr);
        submitText.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
		synchronized (actionLock) {
		    actionLock.notifyAll();
		}
            }
        });
        optionsPanel.add(submitText);
        add(optionsPanel);
    }

    public Text getNewText() {
	try {
	    newText.setContents((getSubject() + "\n" +
				 getBody().trim()).getBytes(Session.serverEncoding));
	} catch (UnsupportedEncodingException ex1) {
	    throw new RuntimeException("Unsupported server encoding: " + ex1.getMessage());
	}
	return newText;
    }

    public void waitForAction() {
	if (abort) return;
	try {
	    synchronized (actionLock) {
		actionLock.wait();
	    }
	} catch (InterruptedException ex1) {}
    }

    public String getBody() {
	return bodyArea.getText();
    }

    public String getSubject() {
	return subjectField.getText();
    }
    
}
