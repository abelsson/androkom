package nu.dll.app.swingkom;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import nu.dll.lyskom.*;

class ActionPanel extends JPanel {
    SessionFrame parent;
    Session kom;
    JTextField displayTextField;
    JCheckBox readRecursive;
    JCheckBox markAsRead;
    public ActionPanel(SessionFrame frame, Session _kom) {
        this.parent = frame;
        this.kom = _kom;
        
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JLabel("Globalt inläggsnummer:"));
        
        displayTextField = new JTextField(10);
        displayTextField.setText("161876");
        add(displayTextField);
        JButton displayTextButton = new JButton("visa text");
        add(displayTextButton);
        
        Box checkBoxBox = new Box(BoxLayout.Y_AXIS);
        
        readRecursive = new JCheckBox("återse rekursivt");
        markAsRead = new JCheckBox("markera som oläst");
        readRecursive.setSelected(true);
        markAsRead.setSelected(true);
        checkBoxBox.add(markAsRead);
        checkBoxBox.add(readRecursive);
        add(checkBoxBox);
        
        frame.readRecursive = readRecursive.isSelected();
        frame.markAsRead = markAsRead.isSelected();
        
        
        
        readRecursive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                parent.readRecursive = readRecursive.isSelected();
            }
        });

        markAsRead.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                parent.markAsRead = markAsRead.isSelected();
            }
        });
        
        
        
        displayTextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    new InternalTextFrame(parent, kom, Integer.parseInt(displayTextField.getText()));
                } catch (NumberFormatException ex) {
                    parent.notifyError("Ogiltigt globalt textnummer.");
                } catch (IOException ex) {
                    parent.notifyError("I/O-fel: " + ex.getMessage());   
                }
            }     
        });
        
        
    }
	
}