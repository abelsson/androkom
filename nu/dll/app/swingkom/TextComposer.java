package nu.dll.app.swingkom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import nu.dll.lyskom.*;
import java.io.*;

class TextComposer extends Box {
    Session kom;
    Text commentedText;
    Text newText;
    JTextArea bodyArea;
    JTextField subjectField;
    JLabel rcptLabel;
    InternalTextFrame frame;
    
    public TextComposer(Session lyskom, Text text, InternalTextFrame _frame, boolean comment) {
        super(BoxLayout.Y_AXIS);
        this.kom = lyskom;
        this.commentedText = text;
        this.frame = _frame;

        newText = new Text(new String(text.getSubject()), "");
        
        bodyArea = new JTextArea(20, 72);
        bodyArea.setEditable(true);
        bodyArea.setFont(new Font("Courier", Font.PLAIN, 12));
        Box headerPanel = new Box(BoxLayout.Y_AXIS);
       
        JScrollPane scroll = new JScrollPane(bodyArea);

        int[] commTo = { text.getNo() };
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

        int[] rcpts = commentedText.getStatInts(TextStat.miscRecpt);
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
        
        JPanel optionsPanel = new JPanel(new BorderLayout());
        
        JButton submitText = new JButton("Skicka");
        submitText.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    int textNo = kom.reply(commentedText.getNo(), new Text(subjectField.getText(), bodyArea.getText()));
                    Debug.println("** Session.reply() returned " + textNo);
                    if (textNo > 0) {
                        frame.dispose();
                    }
                    frame.setCursor(null);
                } catch (IOException ex) {
                    System.err.println("** I/O error: " + ex.getMessage());
                    System.exit(42);   
                    
                }
            }
        });
        optionsPanel.add(submitText, BorderLayout.EAST);
        add(optionsPanel);
        
        
        
        
    }    
    
}