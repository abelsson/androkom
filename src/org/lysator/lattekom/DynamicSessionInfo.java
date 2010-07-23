/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 2000 by Staffan Malmgren <staffan@ckrakes.com>
 *
 */

package org.lysator.lattekom;

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
 * This handles the LysKOM datatype Dynamic-Session-Info, which contains
 * information about sessions that might be changed over time (such as current
 * conference and what-am-i-doing).
 * 
 * @author Staffan Malmgren
 */
public class DynamicSessionInfo {
	public int session;
	public int person;
	public int workingConference;
	public int idleTime;
	public Bitstring flags;
	public byte[] whatAmIDoing;

	/**
	 * This constructor will probably not be used, as the information doesn't
	 * come alone in the single KomToken[] array, but instead as a part of many
	 * DynamicSessionInfo's in a big-ass array
	 */
	protected DynamicSessionInfo(int offset, KomToken[] tk) {
		this.session = tk[offset++].intValue();
		this.person = tk[offset++].intValue();
		this.workingConference = tk[offset++].intValue();
		this.idleTime = tk[offset++].intValue();
		this.flags = new Bitstring(tk[offset++]);
		this.whatAmIDoing = tk[offset++].getContents();
	}

	/**
	 * This is the one to use
	 */
	protected DynamicSessionInfo(int session, int person,
			int workingConference, int idleTime, Bitstring flags,
			byte[] whatAmIDoing) {
		this.session = session;
		this.person = person;
		this.workingConference = workingConference;
		this.idleTime = idleTime;
		this.flags = flags;
		this.whatAmIDoing = whatAmIDoing;
	}

	/**
	 * Returns the session number for this session
	 */
	public int getSession() {
		return session;
	}

	/**
	 * Returns the person number logged in into this session
	 */
	public int getPerson() {
		return person;
	}

	/**
	 * Returns the current working conference for this session
	 */
	public int getWorkingConference() {
		return workingConference;
	}

	/**
	 * Returns the number of seconds this session has been idle
	 */
	public int getIdleTime() {
		return idleTime;
	}

	/**
	 * Returns the session flags (see spec elsewhere)
	 */
	public Bitstring getFlags() {
		return flags;
	}

	/**
	 * Returns what the session's client has reported as what-i-am-doing.
	 */
	public byte[] getWhatAmIDoing() {
		return whatAmIDoing;
	}

	/**
	 * Translated the result from getWhatAmIDoing() into a String according to
	 * the current platform's default character encoding.
	 * 
	 * @deprecated the string should be decoded using Session.toString()
	 */
	public String getWhatAmIDoingString() {
		return new String(whatAmIDoing);
	}

}
