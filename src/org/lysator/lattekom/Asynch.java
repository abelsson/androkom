/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

/**
 * Interface containing constants for asynchronous message types.
 */
public interface Asynch {
	/**
	 * A new text has been created. The first parameter is the text number, the
	 * second is the text-stat information.
	 */
	public final static int new_text_old = 0;
	/**
	 * @deprecated replaced by "logout"
	 */
	public final static int i_am_off = 1; // deprecated
	/**
	 * @deprecated replaced by "i-am-on"
	 */
	public final static int i_am_on_obsolete = 2; // deprecated

	/**
	 * A conference has changed it's name. The first parameter is the
	 * conference's number, the second it's old name and the third it's new
	 * name.
	 */
	public final static int new_name = 5;
	/**
	 * This message is sent when a session's working conference, what-i-am-doing
	 * string or username changes. The parameters are a Who-Info struct.
	 */
	public final static int i_am_on = 6;
	/**
	 * This message is sent by the server once before it blocks to save its
	 * database and once just after it blocks.
	 */
	public final static int sync_db = 7;
	/**
	 * This message is sent to a user when the user's membership in the working
	 * conference is removed for any reason, such as the conference being
	 * deleted or a session successfully issued a sub-member call.
	 * 
	 * The parameter is the conference number.
	 */
	public final static int leave_conf = 8;
	/**
	 * This message is sent when someone logs onto the LysKOM server. The
	 * parameter is the person's number.
	 */
	public final static int login = 9;

	/**
	 * @deprecated replaced by "send-message"
	 */
	public final static int broadcast = 10; // deprecated

	/**
	 * This message is sent when someone fails to log in because the maximum
	 * number of allowed connections has been reached. You might want to
	 * disconnect from the server if your session is idle.
	 */
	public final static int rejected_connection = 11;
	/**
	 * This message is sent when someone sends a message. The first parameter is
	 * the recipient conference, or zero if it is a broadcast message to all
	 * users. The second parameter is the sender. The third parameter is a
	 * Hollerith containing the message.
	 */
	public final static int send_message = 12;
	/**
	 * This message is sent when a session is logged out.
	 */
	public final static int logout = 13;
	/**
	 * This message is sent when a text has been deleted.
	 */
	public final static int deleted_text = 14;
	/**
	 * This message is sent when a new text has been created.
	 */
	public final static int new_text = 15;
	/**
	 * This message is sent when a new recipient has been added to a text.
	 */
	public final static int new_recipient = 16;
	/**
	 * This message is sent when a recipient has been removed from a text.
	 */
	public final static int sub_recipient = 17;
	/**
	 * This message is sent when a conference has been added to a person's
	 * membership.
	 */
	public final static int new_membership = 18;

}
