package nu.dll.app.test;

import javax.swing.*;
import java.awt.event.*;
import nu.dll.lyskom.*;
import nu.dll.app.swingkom.CommentListPanel;
import java.io.*;
import java.awt.*;
import java.util.*;

class TextComposer extends Box {
    Session kom;
    Text commentedText;
    Text newText;
    JTextArea bodyArea;
    JTextField subjectField;
    JLabel rcptLabel;
    
    Object actionLock = new Object();
    boolean comment = false;
    boolean footnote = false;
    boolean abort = false;

    public TextComposer(Session lyskom, Text text, boolean comment, boolean footnote) throws IOException {
        super(BoxLayout.Y_AXIS);
        this.kom = lyskom;
        this.commentedText = text;
	this.comment = comment;
	this.footnote = footnote;
	if (comment) {
	    newText = new Text(new String(text.getSubject(), kom.getServerEncoding()), "");
	    
	} else {
	    newText = (Text) text.clone();
	}
        
        bodyArea = new JTextArea(20, 72);
        bodyArea.setEditable(true);
	bodyArea.setColumns(72);
	bodyArea.setLineWrap(true);
        bodyArea.setFont(new Font("Courier", Font.PLAIN, 12));
	bodyArea.addKeyListener(new KeyAdapter() {
		public void keyPressed(KeyEvent event) {
		    if (event.getKeyCode() == KeyEvent.VK_ENTER && event.isControlDown()) {
			synchronized (actionLock) {
			    actionLock.notifyAll();
			}
		    }
		}
	    });

        Box headerPanel = new Box(BoxLayout.Y_AXIS);
       
        JScrollPane scroll = new JScrollPane(bodyArea);

        int[] commTo = comment ?  new int[] { text.getNo() } : new int[0];
	if (comment && !footnote) newText.addCommented(text.getNo());
	if (comment && footnote) newText.addFootnoted(text.getNo());
        JPanel commp = new JPanel();
        FlowLayout commpLayout = new FlowLayout(FlowLayout.LEFT);
        commpLayout.setVgap(1);
        commp.setLayout(commpLayout);

        CommentListPanel commentedPanel = new CommentListPanel((footnote ? "Fotnot" : "Kommentar") +
							       " till text ", commTo, kom, null, 
                                                               SwingConstants.LEFT);
        commp.add(commentedPanel);
        headerPanel.add(commp);
        
        JPanel rcptPanel = new JPanel();
        rcptPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        rcptPanel.add(new JLabel("Till: "));
        rcptLabel = new JLabel();
        rcptPanel.add(rcptLabel);

	int[] recipients = text.getRecipients();
	for (int i=0; (comment || footnote) && i < recipients.length; i++) {
	    Conference conf = lyskom.getConfStat(recipients[i]);
	    if (!footnote && conf.getType().original()) {
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
                a = a + new String(kom.getConfName(rcpts[i]), kom.getServerEncoding()) + (i < rcpts.length-1 ? ", " : "");
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
	try {
	    subjectField = new JTextField(new String(text.getSubject(), kom.getServerEncoding()), 72);
	} catch (UnsupportedEncodingException ex1) {
	    throw new RuntimeException("Unsupported server envoding: " + ex1);
	}
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
	constr.gridx=2;
	JButton abortButton = new JButton("Avbryt");
	abortButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
		abort = true;
		synchronized (actionLock) {
		    actionLock.notifyAll();
		}
            }
	    });
	gridBag.setConstraints(abortButton, constr);

	JButton wrapButton = new JButton("Bryt text");
	wrapButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent event) {
		    try {
			wrapText();
		    } catch (UnsupportedEncodingException ex1) {
			Debug.println(ex1.toString());
		    }
		}
	    });
	
	constr.gridx=3;
	gridBag.setConstraints(wrapButton, constr);

	optionsPanel.add(abortButton);
        optionsPanel.add(submitText);
	optionsPanel.add(wrapButton);
        add(optionsPanel);
    }

    int rightMargin = Integer.getInteger("lattekom.t2.linewrap", new Integer(69)).intValue();
    void wrapText() throws UnsupportedEncodingException {
	bodyArea.setText(bodyArea.getText().trim());
	newText.setContents((getSubject() + "\n" +
			     getBody().trim()).getBytes(kom.getServerEncoding()));
	java.util.List rows = newText.getBodyList();
	java.util.List newRows = new LinkedList();

	Iterator i = rows.iterator();
	while (i.hasNext()) {
	    String row = (String) i.next();
	    while (row.length() > rightMargin) {
		int cutAt = row.lastIndexOf(' ', rightMargin);
		String wrappedRow = row.substring(0, cutAt);
		row = row.substring(cutAt+1);
		newRows.add(wrappedRow);
	    }
	    newRows.add(row);
	}

	i = newRows.iterator();
	StringBuffer newBody = new StringBuffer();
	while (i.hasNext()) {
	    String row = (String) i.next();
	    newBody.append(row + "\n");
	}
	bodyArea.setText(newBody.toString().trim());
    }

    public Text getNewText() {
	try {
	    wrapText();

	} catch (UnsupportedEncodingException ex1) {
	    throw new RuntimeException("Unsupported server encoding: " + ex1.getMessage());
	}
	return newText;
    }

    public boolean isAborted() {
	return abort;
    }

    public boolean isCommentOrFootnote() {
	return comment || footnote;
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
