package nu.dll.app.swingkom;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import nu.dll.lyskom.*;
  
class InternalTextFrame extends JInternalFrame implements TextContainer {
    Container contents;
    SessionFrame parent;
    JDesktopPane desktop;
    Session kom;
    
    Box internalContainer;

    public InternalTextFrame(SessionFrame pframe, Session _kom, int textNo, boolean comment) throws IOException {
        parent = pframe;
        this.kom = _kom;
	setTitle((comment ? "Kommentar till text " : "Redigera text ") + textNo);
        desktop = parent.getDesktop();
        layoutContents(new TextComposer(kom, kom.getText(textNo), this, comment));
        addToDesktop();
    }
        
    public InternalTextFrame(SessionFrame pframe, Session _kom, int textNo) throws IOException {
        parent = pframe;
        this.kom = _kom;
        setTitle("Text nummer " + textNo);
        desktop = parent.getDesktop();   
        layoutContents(new ReadingPane(this, kom, true, kom.getText(textNo)));        
        addToDesktop();
    }

    private void addToDesktop() {
        desktop.add(this);
        setVisible(true);
        moveToFront();
	try {
	    setSelected(true);
	} catch (java.beans.PropertyVetoException ex) {
	    Debug.println("Failed to select " + this + ": " + ex.getClass().getName() + ": " +
			  ex.getMessage());
	}
    }
    
    private void layoutContents(Container container) {
        contents = container;
        
        // frame properties
        setBackground(parent.getBackground());
        setClosable(true);
        setResizable(true);
        setIconifiable(true);
        
        container.addKeyListener(new TextFrameKeyListener());
        
        internalContainer = new Box(BoxLayout.Y_AXIS);
        internalContainer.add(container);
        getContentPane().add(internalContainer);
        pack();
        setVisible(true);
    }
    
    
    public void commentText(int number, boolean newFrame) {
        if (!newFrame) throw new RuntimeException("Unimplemented feature");
        try {
            new InternalTextFrame(parent, kom, number, true);
        } catch (IOException ex) {
          SwingKOM.panic(ex);  
        }
    }
    
    public void showText(int number, boolean reuse) {
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        if (!reuse) {
            try {
                InternalTextFrame frame = new InternalTextFrame(parent, kom, number);
            } catch (IOException ex) {
                SwingKOM.panic(ex);   
            }
        } else {
            getContentPane().removeAll();
            try {
                setTitle("Text nummer " + number);
                ReadingPane r = new ReadingPane(this, kom, true, kom.getText(number));
                layoutContents(r);
            } catch (IOException ex) {
                SwingKOM.panic(ex);
            }
        }
        
        setCursor(null);
    }
    
    class TextFrameKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) { Debug.println("keyPressed(): " + e); }
        
        public void keyReleased(KeyEvent e) { Debug.println("keyReleased(): " + e); }
        
        public void keyTyped(KeyEvent e) {
            Debug.println("keyTyped(): " + e);
            switch (e.getKeyChar()) {
                case ' ':
                    parent.notify("*beep!*");

                
                    break;
                
            }
            
        }
        
    }

        
}
  
