/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 2000 by Staffan Malmgren <staffan@ckrakes.com>
 *
 */

package nu.dll.lyskom;

import java.net.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

/*
          Session-Info ::= 
              ( person                  :       Pers-No;
                working-conference      :       Conf-No;
                session                 :       Session-No;
                what-am-i-doing         :       HOLLERITH;
                username                :       HOLLERITH;
                idle-time               :       INT32;
                connection-time         :       Time;
              )

        Session-Info-Ident ::= 
              ( person                  :       Pers-No;
                working-conference      :       Conf-No;
                session                 :       Session-No;
                what-am-i-doing         :       HOLLERITH;
                username                :       HOLLERITH;
                hostname                :       HOLLERITH;
                ident-user              :       HOLLERITH;
                idle-time               :       INT32;
                connection-time         :       Time;
              )

        Static-Session-Info ::=
              (
                username                : HOLLERITH;
                hostname                : HOLLERITH;
                ident-user              : HOLLERITH;
                connection-time         : Time;
              )

        Session-Flags ::= BITSTRING
              ( invisible;
                user_active_used;
                user_absent;
                reserved3;
                reserved4;
                reserved5;
                reserved6;
                reserved7;
              )

        Dynamic-Session-Info ::=
              ( session                 :       Session-No;
                person                  :       Pers-No;
                working-conference      :       Conf-No;
                idle-time               :       INT32;
                flags                   :       Session-Flags;
                what-am-i-doing         :       HOLLERITH;
              )
 
*/  

/**
 * This handles the datatype Dynamic-Session-Info
 */

public class DynamicSessionInfo {
    public int session;
    public int person;
    public int workingConference;
    public int idleTime;
    public Bitstring flags;
    public byte[] whatAmIDoing;
    /**
     * This constructor will probably not be used, as the information
     * doesn't come alone in the single KomToken[] array, but instead
     * as a part of many DynamicSessionInfo's in a big-ass array */
    public DynamicSessionInfo(int offset, KomToken[] tk) {
	this.session = tk[offset++].toInteger();
	this.person = tk[offset++].toInteger();
	this.workingConference = tk[offset++].toInteger();
	this.idleTime = tk[offset++].toInteger();
	this.flags = new Bitstring(tk[offset++]);
	this.whatAmIDoing = tk[offset++].getContents();
    }
    /**
     * This is the one to use
     */
    public DynamicSessionInfo (int session, int person, int workingConference, int idleTime, Bitstring flags, byte[] whatAmIDoing) {
	this.session = session;
	this.person = person;
	this.workingConference = workingConference;
	this.idleTime = idleTime;
	this.flags = flags;
	this.whatAmIDoing = whatAmIDoing;
    }

    public int getSession() {
	return session;
    }
    
    public int getPerson() {
	return person;
    }

    public int getWorkingConference() {
	return workingConference;
    }

    public int getIdleTime() {
	return idleTime;
    }

    public Bitstring getFlags() {
	return flags;
    }

    public byte[] getWhatAmIDoing() {
	return whatAmIDoing;
    }

    public String getWhatAmIDoingString() {
	return new String(whatAmIDoing);
    }





}



