/*
 * The orgleenjava license
 * Version $Revision: 1.1 $ 
 * Date $Date: 2002/04/01 16:32:24 $
 *
 *
 * Copyright (c) 1996-2001 Thomas Leen.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All modifications of source from org.leen.java must be provided to
 *    Thomas Leen (thomasleen@hotmail.com).
 *
 * 4. The name "org.leen.java" or "orgleenjava" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact thomasleen@hotmail.com.
 *
 * 5. All packages included in the distribution must likewise 
 *    have their licenses obeyed.  		
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */
 /*
 * --------------------------------------------------------------------
 *	
 *	orgleenjava 
 *
 * This code is part of the orgleenjava distribution, enjoy!
 * all questions comments source ect ect may be sent to 
 * thomasleen@hotmail.com
 *
 * Only even numbered dot releases should be used in production.
 * some code is still ugly, broken and/or sparsely commented. some 
 * code is fine, others, well...beware. take a look at the javadoc
 * to see what is safe
 *
 *
 * Copyright (c) 1996-2001 Thomas Leen.  All rights 
 * reserved.
 *  --------------------------------------------------------------------
 */




package nu.dll.app.test;


/******************************************************
*
* creator: thomas leen
* created: 2001.03.24
* purpose: this class takes care of making input and output look like a console
* version: $Revision: 1.1 $
* notes: needs to emulate history
* in general my implementation of this class sucks. the whole thing is hackey, it needs to be fixed
* the way in which i detect new code lines is half-ass, i base it on the index of the console_thingies which may be erased, or modified through the set and cause the last event to be fragged. must fix that...
*
* history:
*
* $Log: Console.java,v $
* Revision 1.1  2002/04/01 16:32:24  pajp
* Another major overhaul of both the Test2 client and the class library.
*
* A great deal of javadoc comments added. Many classes and method changed
* from 'public' to 'protected', mostly to make the javadoc easier to grasp.
*
* A primitive JUnit test suite added (nu.dll.lyskom.test), that only test
* a small portion of the code, but at least a start.
*
* Many new calls added to Session, and implemented in the Test2 class.
*
* The Test2 client now has support for non-console platforms using a Swing
* console terminal. The Console* classes come from the orgleenjava package
* (orgleenjava.sourceforge.net), with a small modification.
*
* Most string-to-bytes and bytes-to-string conversions now explicitly uses
* the encoding specified by the system property "lyskom.encoding" (default
* iso-8859-1) instead of using the default platform encoding.
*
* Rpc.java and Asynch.java are now interfaces rather than abstract classes.
*
* Plus, a lot of other bug fixes and so on.
*
* Revision 1.2  2001/03/24 21:42:25  holzp
* moved loadwatcher
*
*
******************************************************/

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.Vector;
import java.util.Enumeration;

import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.text.Keymap; 


 public class Console extends JTextArea implements KeyListener
 {

     protected final static String DEFAULT_CONSOLE_THINGIES = ">>> ";
     protected String console_thingies = DEFAULT_CONSOLE_THINGIES;
     protected Vector commandHistory = null;
     protected Vector consoleListeners = null;
     protected int new_command_index = 0; //keeps track where in the JTextArea contents the next command begins
     protected int command_history_index = 0; //where in the history vector we are currently located
 
     public Console(int rows, int columns)
     {
	 super(rows,columns);
	 this.addKeyListener(this); //you arent seeing this.
      
	 commandHistory = new Vector();
	 consoleListeners = new Vector();

	
     }

     /*
       ---
       could this be any more of a hack?
       ---
     */

     protected void reset_new_command_index()
     {
	 new_command_index = getText().length();
	 //System.out.println("new command starts at " + new_command_index);
     }

     protected void handleEnter()
     {
	 //System.out.println("enter.");
	 String command = getText().substring(new_command_index).trim();
	 
	 //notify listeners
	 ConsoleEvent ce = new ConsoleEvent(ConsoleEvent.COMMAND_ENTERED,command,this);
	 fireConsoleEvent(ce);
	 reset_new_command_index();
	 commandHistory.addElement(command);
	 command_history_index = commandHistory.size();
     }

     public void addConsoleListener(ConsoleListener cl)
     {
	 consoleListeners.addElement(cl);
     }

     public void removeConsoleListener(ConsoleListener cl)
     {
	 consoleListeners.removeElement(cl);
     }

     protected void fireConsoleEvent(ConsoleEvent ce)
     {
	 Enumeration elements = consoleListeners.elements();

	 while(elements.hasMoreElements())
	     {
		 ConsoleListener cl = (ConsoleListener)(elements.nextElement());
		 cl.consoleAction(ce);
	     }
     }
     /*
       ---
       now basically i need to trap keystrokes. if it is behind last command, dont let it go through. if it is up-arrow, show history back, if it is down-arrow, go forward in history
       ---
     */
     
     
     /*
       ---
       im wondering if i can do it where the event is fired off...

       god this control structure is fuckin ugly. need to find more elegance.
       ---
     */

     protected boolean acceptableKeyEvent(KeyEvent ke)
     {
	 //ack! makes me want to puke
	 //comp.lang.java.retch

	 boolean acceptable = ((ke.getKeyCode() == KeyEvent.VK_LEFT) || (ke.getKeyCode() == KeyEvent.VK_RIGHT) || (ke.getKeyCode() == KeyEvent.VK_UP) || (ke.getKeyCode() == KeyEvent.VK_DOWN));

	 acceptable = (acceptable || (getCaretPosition() >= new_command_index));
	 acceptable = (acceptable || ( (getCaretPosition() == (new_command_index - 1)) && (ke.getKeyCode() == KeyEvent.VK_LEFT)));

	 return acceptable;

     }
     
     public void processKeyEvent(KeyEvent ke)
     {
	 
	 
	 if(acceptableKeyEvent(ke))
	     { 
		 
		 super.processKeyEvent(ke);
	     }
     }

     /*
       ---
       implements the KeyListener interface
       ---
     */
     public void keyReleased(KeyEvent ke)
     {
	  switch (ke.getKeyCode())
		     {
			 case KeyEvent.VK_ENTER:
			     handleEnter();
			     break;

			 case  KeyEvent.VK_DOWN:
			     //if we can move up in history and display
			     moveHistory(1);
			     showHistory(command_history_index);
			     break;

			 case  KeyEvent.VK_UP:
			     //if we can move down in history and display
			     moveHistory(-1);
			     showHistory(command_history_index);
			     break;
		     }
     }

     public void keyPressed(KeyEvent ke){}

     public void keyTyped(KeyEvent ke){}

     /*
       ---
       helpers for dealing with the command history
       ---
      */
	 
     protected void moveHistory(int howmuch)
     {
	 command_history_index = command_history_index + howmuch;

	 if((command_history_index > (commandHistory.size() - 1)) || (command_history_index < 0))
	     {
		 //oops, out of bounds, reverse operation
		 command_history_index = command_history_index - howmuch; 
	     }
	 //System.out.println("moved history to : " + command_history_index);
     }

     protected void showHistory(int index)
     {
	 //System.out.println("show history: " + index);
	 String command = (String)commandHistory.elementAt(index);
	 replaceRange(command,new_command_index,getText().length());
	 setCaretPosition(getText().length());
     }

     public void setConsoleThingies(String thingies)
     {
	 console_thingies = thingies;
     }

     public void append(String text)
     {
	 super.append(text + "\n" + console_thingies);
	 reset_new_command_index();
     }

     // added by rasmus@dll.nu -- 2002-03-28
     public void append(String text, boolean newLine) 
     {
	 if (newLine) {
	     append(text);
	     return;
	 }	 
	 super.append(text);
	 reset_new_command_index();
     }
     
     public void newLine()
     {
	 append("");
     }
 
 }









