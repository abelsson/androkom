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
* purpose: interface for objects wishing to listen to the console
* version: $Revision: 1.1 $
* notes: 
*
* history:
*
* $Log: ConsoleListener.java,v $
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

public interface ConsoleListener
{

    public void consoleAction(ConsoleEvent ce);


} 
