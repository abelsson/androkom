package nu.dll.app.swingkom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import nu.dll.lyskom.*;
import java.io.*;

class ReadingPane extends Box {

    TextContainer parent;
    JDesktopPane desktop = null;
    Text currentText = null;
    
    JTextArea bodyArea;
    TextInfoLabel authorLabel;
    JLabel rcptLabel, subjectLabel;
    JComboBox rcptList;

    CommentListPanel commentedPanel;
    CommentListPanel commentPanel;

    JButton nextUnreadBtn;
    JButton replyBtn;

    JCheckBox reuseBox;
    Session lyskom;
    
    boolean resolve = false;
    
    String[] recipientNames = new String[0];
    
    private JCheckBox reuseButton = null;

    public ReadingPane(TextContainer _parent, Session kom, boolean resolve, Text text) {
        super(BoxLayout.Y_AXIS);
    	this.lyskom = kom;
    	this.resolve = resolve;
    	this.currentText = text;
    	this.parent = _parent;

    	setupGUI();
    	commentPanel = new CommentListPanel("Kommentar i text ", text.getStatInts(TextStat.miscCommIn), lyskom, this,
    	                                    SwingConstants.LEFT);
    	add(commentPanel);
    	JPanel optionsPanel = new JPanel(new BorderLayout());
    	reuseBox = new JCheckBox("Återanvänd inläggsfönster");
    	reuseBox.setSelected(true);
    	optionsPanel.add(reuseBox, BorderLayout.WEST);

	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    	
    	replyBtn = new JButton("Kommentera");
	nextUnreadBtn = new JButton("Nästa olästa >>");
	nextUnreadBtn.setEnabled(false);
    	buttonPanel.add(replyBtn);
	buttonPanel.add(nextUnreadBtn);
	optionsPanel.add(buttonPanel, BorderLayout.EAST);
    	replyBtn.addActionListener(new ActionListener() {
    	        public void actionPerformed(ActionEvent e) {
    	            parent.commentText(currentText.getNo(), true);
    	        }
    	    });
    	add(optionsPanel);
    	showText(text, false);
    	setCursor(null);
    }
        
    public ReadingPane(Session lyskom, boolean resolve) {
        super(BoxLayout.Y_AXIS);
        this.lyskom = lyskom;
        this.resolve = resolve;
	setupGUI();
    }

    private void setupGUI() {
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        setBackground(parent.getBackground());
       
        bodyArea = new JTextArea(20, 72);
        bodyArea.setEditable(false);
        bodyArea.setFont(new Font("Courier", Font.PLAIN, 12));
        Box headerPanel = new Box(BoxLayout.Y_AXIS);
        headerPanel.setBackground(parent.getBackground());
        
        JScrollPane scroll = new JScrollPane(bodyArea);
        
        JPanel fromPanel = new JPanel();
        
        
        fromPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        fromPanel.setBackground(parent.getBackground());
        fromPanel.add(authorLabel = new TextInfoLabel("Text ", 0, null, this, SwingConstants.RIGHT));
        
        headerPanel.add(fromPanel);
        
        int[] commTo = (currentText != null) ? currentText.getStatInts(TextStat.miscCommTo) : new int[0];
        JPanel commp = new JPanel();
        commp.setBackground(parent.getBackground());
        FlowLayout commpLayout = new FlowLayout(FlowLayout.LEFT);
        commpLayout.setVgap(1);
        commp.setLayout(commpLayout);

        commentedPanel = new CommentListPanel("Kommentar till text ", commTo, lyskom, this, 
                SwingConstants.LEFT);

        commp.add(commentedPanel);
        headerPanel.add(commp);

        
        JPanel rcptPanel = new JPanel();
        rcptPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        rcptPanel.setBackground(parent.getBackground());
        rcptPanel.add(new JLabel("Till: "));
        rcptList = new JComboBox();

        rcptLabel = new JLabel();
        rcptPanel.add(rcptLabel);
        headerPanel.add(rcptPanel);
        
        JPanel subjectPanel = new JPanel();
        subjectPanel.setLayout(new FlowLayout(FlowLayout.LEFT));        
        subjectPanel.setBackground(parent.getBackground());
        subjectLabel = new JLabel("Ämne: ");
        subjectPanel.add(subjectLabel);
        headerPanel.add(subjectPanel);
        
        add(headerPanel);
        add(scroll);
    	
    }

   
    
    boolean getReuse() {
        return reuseBox.isSelected();   
    }
    
   
    public ReadingPane() {
        this(null, false);   
    }

    void setResolve(boolean resolve) {
        this.resolve = resolve;   
    }
    
    void setSession(Session lyskom) {
        this.lyskom = lyskom;
 
    }


    class HeaderPanel extends JPanel {
        public Dimension getPreferredSize() {
            return new Dimension(getSize().width, 80);
        }   
    }
        
    public TextContainer getTextContainer() {
        return parent;
    }
    

    
    void showText(Text t, boolean showComments) {
                
        bodyArea.setText(new String(t.getBody()));

        subjectLabel.setText("Ämne: " + new String(t.getSubject()));
        
        final int author = t.getAuthor();
        final int[] rcpts = t.getStatInts(TextStat.miscRecpt);
        
        //authorLabel.setText("" + author);
        authorLabel.setTextNo(t.getNo());
        authorLabel.setKomSession(lyskom);
        
        String a = "";
        for (int i=0; i < rcpts.length; i ++) {
            a = a + rcpts[i] + (i < rcpts.length-1 ? ", " : "");
        }
        rcptLabel.setText(a);    
            
        if (lyskom != null && resolve) {
        /*
            new Thread(new Runnable() {
                public void run() {
        */
                    try {
                        //String authorName = new String(lyskom.getConfName(author));
                        //authorLabel.setText(authorName);
                        authorLabel.paint();
                        String b = "";
                        for (int i=0; i < rcpts.length; i ++) {
                            b = b + new String(lyskom.getConfName(rcpts[i])) +
                                (i < rcpts.length-1 ? ", " : "");
                        }
                        rcptLabel.setText(b);
                        
                    } catch (IOException ex) {
                        System.err.println("IOE: " + ex.getMessage());
                    }
        /*
                }
            }).start();
        */
        } else {
            Debug.println("Not resolving (lyskom=" + lyskom + ", resolve=" + resolve + ")");   
        }
        
        if (showComments) {
            commentPanel.update(t.getStatInts(TextStat.miscCommIn), lyskom);
        }
        
        commentedPanel.update(t.getStatInts(TextStat.miscCommTo), lyskom);
        invalidate();
        
    }
    
    
}
