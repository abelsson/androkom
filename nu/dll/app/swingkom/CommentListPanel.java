package nu.dll.app.swingkom;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import nu.dll.lyskom.*;

import java.util.Vector;
import java.util.Enumeration;

class CommentListPanel extends JPanel {
    Vector labels = new Vector();
    ReadingPane textPanel = null;
    boolean reuse = false;
    String prefix;
    int alignment;
  
    public CommentListPanel(String pref, int[] comments, Session lyskom, ReadingPane textPanel, int alignment) {
        this.alignment = alignment;
        this.textPanel = textPanel;
        this.prefix = pref;
        layout(comments, lyskom);        
    }
    
    
    void layout(int[] comments, Session lyskom) {
        BoxLayout b = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(b);
        if (textPanel != null) setBackground(textPanel.getBackground());
        for (int i=0; i < comments.length; i++) {
            if (lyskom == null) {
                JLabel l = new JLabel(prefix + comments[i], alignment);
                labels.addElement(l);
                add(l);
            } else {
                TextInfoLabel l = new TextInfoLabel(prefix, comments[i], lyskom, textPanel, alignment);
                labels.addElement(l);
                add(l);
            }
        }	
        
    }
    
    public void update(int[] comments, Session lyskom) {
        /*
         Enumeration e = labels.elements();
         while (e.hasMoreElements()) {
            JLabel l = (JLabel) e.nextElement();
            remove(l);
         }
         */
         removeAll();
         labels.removeAllElements();
         
         for (int i=0; i < comments.length; i++) {
	        if (lyskom == null) {
	            add(new JLabel(prefix + comments[i], alignment));
	        } else {
	            add(new TextInfoLabel(prefix,  comments[i], lyskom, textPanel, alignment));
	        }
	    }  
        
    }
    

	
	
}

class TextInfoLabel extends JLabel {
    int textNo = 0;
    Session lyskom;
    ReadingPane textPanel = null;
    SessionFrame rootFrame = null;
    String prefix = null;
    int alignment;
    
    private Session getKomSession() { return lyskom; }    
    int getNo() { return textNo; }    
    ReadingPane getTextPanel() { return textPanel; }

           
    public TextInfoLabel(String pre, int _textNo, Session _lyskom, ReadingPane rpane, int alignment) {
        super(pre + _textNo, alignment);
        this.alignment = alignment;
        this.textPanel = rpane;
        this.textNo = _textNo;
        this.lyskom = _lyskom;
        this.prefix = pre;

        
        if (textPanel != null) setBackground(textPanel.getBackground());
	

	try {	    
	    if (lyskom != null && lyskom.getTextStat(textNo) != null) setCursor(new Cursor(Cursor.HAND_CURSOR));
	} catch (IOException ex) {
	    SwingKOM.panic(ex);
	}

        if (lyskom != null) paint();        
        
            
        addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                boolean reuse = textPanel != null && textPanel.getReuse();
		try {
		    if (lyskom.getTextStat(textNo) == null) {
			return;
		    }
		} catch (IOException ex) {
		    SwingKOM.panic(ex);
		}
                if (textPanel != null) {
                    textPanel.getTextContainer().showText(textNo, reuse);
                } else {
                    if (rootFrame != null) rootFrame.showText(textNo);
                }
        	    
            }
                
            public void mouseEntered(MouseEvent e) {
        	    
            }
               
            public void mouseExited(MouseEvent e) {
        	    
            }
                
            public void mousePressed(MouseEvent e) {
        	    
            }
                
            public void mouseReleased(MouseEvent e) {
        	    
            }
       });
    }   
    
    public void setSessionFrame(SessionFrame frame) {
        rootFrame = frame;   
    }
    
    public void setKomSession(Session lyskom) {
        this.lyskom = lyskom;   
    }
    
    public void setTextNo(int textNo) {
        this.textNo = textNo;   
    }
    
    public void paint() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    if (getKomSession() != null) {
                        TextStat stat = getKomSession().getTextStat(getNo());
                        String s = null; 
                        if (stat != null) {
                            s = prefix + getNo() + " av " +
                                    new String(getKomSession().getConfName(stat.getAuthor()));
                            setText(s);
                        }
                        Debug.println("*** " + s);

                    }
                } catch (IOException ex) {
                    System.err.println("foo " + ex.getMessage());
                    System.exit(42);

                }

            }
        });        
    }

}
