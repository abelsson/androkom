/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public interface Rpc {
    // errors
    /**
     * No error has occurred. error-status is undefined. This should
     * never happen, but it might.  */
    public static final int E_no_error = 0;

    /**
     * The call has not been implemented yet. error-status is
     * undefined.  */
    public static final int E_not_implemented = 2; 

    /**
     * The call is obsolete and no longer implemented. error-status is
     * undefined.  */
    public static final int E_obsolete_call = 3; 

    /**
     * Attempt to set a password containing illegal characters, or to
     * use an incorrect password.  */
    public static final int E_invalid_password = 4; 

    /**
     * A string was too long (see descriptions of each call.)
     * error-status indicates the maximum string length.  */
    public static final int E_string_too_long = 5; 

    /**
     * Login is required before issuing the call. error-status is
     * undefined.  */
    public static final int E_login_first = 6; 

    /**
     * The system is in single-user mode. You need to be privileged to
     * log in despite this. error-status is undefined.  */
    public static final int E_login_disallowed = 7; 

    /**
     * Attempt to use conference number 0. error-status is undefined.  */
    public static final int E_conference_zero = 8; 

    /**
     * Attempt to access a non-existent or secret
     * conference. error-status contains the conference number in
     * question.  */
    public static final int E_undefined_conference = 9; 

    /**
     * Attempt to access a non-existent or secret person. error-status
     * contains the person number in question.  */
    public static final int E_undefined_person = 10; 

    /**
     * No read/write access to something. This might be returned in
     * response to an attempt to create a text, when the recipient
     * conference and its super conferences are read-only, or when
     * attempting to add a member to a conference without enough
     * permission to do so. error-status indicates the object to which
     * we didn't have enough permissions to.  */
    public static final int E_access_denied = 11; 

    /**
     * Not enough permissions to do something. The exact meaning of
     * this response depends on the call. error-status indicated the
     * object for which permission was lacking, or zero.  */
    public static final int E_permission_denied = 12; 

    /**
     * The call requires the caller to be a member of some conference
     * that the caller is not a member of. error-status indicates the
     * conference in question.  */
    public static final int E_not_member = 13; 

    /**
     * Attempt to access a text that either does not exist or is
     * secret in some way. error-status indicates the text number in
     * question.  */
    public static final int E_no_such_text = 14; 

    /**
     * Attempt to use text number 0. error-status is undefined.
     */
    public static final int E_text_zero = 15; 

    /**
     * Attempt to access a text using a local text number that does
     * not represent an existing text. error-status indicates the
     * offending number.  */
    public static final int E_no_such_local_text = 16; 

    /**
     * Attempt to use local text number zero. error-status is undefined.
     */
    public static final int E_local_text_zero = 17; 

    /**
     * Attempt to use a name that's too long, too short or contains
     * invalid characters. error-status is undefined.  */
    public static final int E_bad_name = 18; 

    /**
     * Attempt to use a number that's out of range. The range and
     * meaning of the numbers depends on the call issued. error-status
     * is undefined unless stated otherwise in the call documentation.  */
    public static final int E_index_out_of_range = 19; 

    /**
     * Attempt to create a conference or person with a name that's
     * already occupied. error-status is undefined.  */
    public static final int E_conference_exists = 20; 

    /**
     * Attempt to create a person with a name that's already
     * occupied. error-status is undefined. This error code is
     * probably not used, but you never know for sure.  */
    public static final int E_person_exists = 21; 

    /**
     * Attempt to give a conference a type with secret bit set and the
     * rd-prot bit unset. This is an error since such a conference
     * type is inconsistent. error-status is undefined.  */
    public static final int E_secret_public = 22; 

    /**
     * Attempt to change the letterbox flag of a
     * conference. error-status indicates the conference number.  */
    public static final int E_letterbox = 23; 

    /**
     * Database is corrupted. error-status is an internal code.  */
    public static final int E_ldb_error = 24; 

    /**
     * Attempt to create an illegal misc item. error-status contains
     * the index of the illegal item.  */
    public static final int E_illegal_misc = 25; 

    /**
     * Attempt to use a Misc-Info type (or Info-Type value) that the
     * server knows nothing about. error-status is the type.  */
    public static final int E_illegal_info_type = 26; 

    /**
     * Attempt to add a recipient that is already a recipient of the
     * same type. error-status contains the recipient that already is.  */
    public static final int E_already_recipient = 27; 

    /**
     * Attempt to add a comment to a text twice over. error-status
     * contains the text number of the text that already is a comment.  */
    public static final int E_already_comment = 28; 

    /**
     * Attempt to add a footnote to a text twice over. error-status
     * contains the text number of the text that already is a
     * footnote.  */
    public static final int E_already_footnote = 29; 

    /**
     * Attempt to remove a recipient that isn't really a
     * recipient. error-status contains the conference number in
     * question.  */
    public static final int E_not_recipient = 30; 

    /**
     * Attempt to remove a comment link that does not
     * exist. error-status contains the text number that isn't a
     * comment.  */
    public static final int E_not_comment = 31; 

    /**
     * Attempt to remove a footnote link that does not
     * exist. error-status contains the text number that isn't a
     * footnote.  */
    public static final int E_not_footnote = 32; 

    /**
     * Attempt to add a recipient to a text that already has the
     * maximum number of recipients. error-status is the text that has
     * the maximum number of recipients.  */
    public static final int E_recipient_limit = 33; 

    /**
     * Attempt to add a comment to a text that already has the maximum
     * number of comments. error-status is the text with the maximum
     * number of comments.  */
    public static final int E_comment_limit = 34; 

    /**
     * Attempt to add a footnote to a text that already has the
     * maximum number of footnote. error-status is the text with the
     * maximum number of footnotes.  */
    public static final int E_footnote_limit = 35; 

    /**
     * Attempt to add a mark to a text that already has the maximum
     * number of marks. error-status is the text with the maximum
     * number of marks.  */
    public static final int E_mark_limit = 36; 

    /**
     * Attempt to manipulate a text in a way that required the user to
     * be the author of the text, when not in fact the
     * author. error-status contains the text number in question.  */
    public static final int E_not_author = 37; 

    /**
     * Currently unused.
     */
    public static final int E_no_connect = 38; 

    /**
     * The server ran out of memory.
     */
    public static final int E_out_of_memory = 39; 

    /**
     * Currently unused.
     */
    public static final int E_server_is_crazy = 40; 

    /**
     * Currently unused.
     */
    public static final int E_client_is_crazy = 41; 

    /**
     * Attempt to access a session that does not exist. error-status
     * contains the offending session number.  */
    public static final int E_undefined_session = 42; 

    /**
     * Error using a regexp. The regexp may be invalid or the server
     * unable to compile it for other reasons. error-status is
     * undefined.  */
    public static final int E_regexp_error = 43; 

    /**
     * Attempt to manipulate a text in a way that requires the text to
     * be marked, when in fact it is not marked. error-status
     * indicates the text in question.  */
    public static final int E_not_marked = 44; 

    /**
     * Temporary failure. Try again later. error-code is undefined.
     */
    public static final int E_temporary_failure = 45; 

    /**
     * An array sent to the server was too long. error-status is
     * undefined.  */
    public static final int E_long_array = 46; 

    /**
     * Attempt to send an anonymous text to a conference that does not
     * accept anonymous texts. error-status is undefined.  */
    public static final int E_anonymous_rejected = 47; 

    /**
     * Attempt to create an invalid aux-item. Probably the tag or data
     * are invalid. error-status contains the index in the aux-item
     * list where the invalid item appears.  */
    public static final int E_illegal_aux_item = 48; 

    /**
     * Attempt to manipulate an aux-item without enough
     * permissions. This response is sent when attempting to delete an
     * item set by someone else or an item that can't be deleted, and
     * when attempting to create an item without permissions to do
     * so. error-status contains the index at which the item appears
     * in the aux-item list sent to the server.  */
    public static final int E_aux_item_permission = 49; 

    /**
     * Sent in response to a request for an asynchronous message the
     * server does not send. The call succeeds, but this is sent as a
     * warning to the client. error-status contains the message type
     * the server did not understand.  */
    public static final int E_unknown_async = 50; 

    /**
     * The server has encountered a possibly recoverable internal
     * error. error-status is undefined.  */
    public static final int E_internal_error = 51; 

    /**
     * Attempt to use a feature that has been explicitly disabled in
     * the server. error-status is undefined.  */
    public static final int E_feature_disabled = 52; 

    /**
     * Attempt to send an asynchronous message failed for some
     * reason. Perhaps the recipient is not accepting messages at the
     * moment or there are no viable recipients for a group
     * message. error-status is undefined.  */
    public static final int E_message_not_sent = 53; 

    /**
     * A requested membership type was not compatible with
     * restrictions set on the server or on a specific
     * conference. error-status is undefined unless specifically
     * mentioned in the documentation for a specific call.  */
    public static final int E_invalid_membership_type = 54; 

    /* obsolete calls replaced by newer ones */
    //public final static int C_get_text_stat_old = 26; // 10 (->90)
    //public final static int C_create_text_old = 28; // 10 (->86)

    /* obsolete calls that still needs to updated */
    //public final static int C_get_membership_old = 46; // 10 (->99)
    //public final static int C_get_created_texts = 47; // 10 (->104*)
    //public final static int C_get_conf_stat_old = 50; // 10 (->91)

    public final static int C_logout = 1;
    public final static int C_change_conference = 2;
    public final static int C_change_name = 3;
    public final static int C_change_what_i_am_doing = 4;
    public final static int C_delete_conf = 11;

    public final static int C_sub_member = 15;
    public final static int C_set_presentation = 16;

    // C_get_conf_stat_old = 13
    public final static int C_get_marks = 23;

    public final static int C_get_text = 25;

    public final static int C_mark_as_read = 27;

    public final static int C_delete_text = 29;

    public final static int C_enable = 42;


    public final static int C_get_person_stat = 49;

    public final static int C_get_unread_confs = 52;
    public final static int C_send_message = 53;

    public final static int C_disconnect = 55;

    public final static int C_find_next_text_no = 60;
    public final static int C_find_previous_text_no = 61;

    public final static int C_login = 62;

    public final static int C_set_client_version = 69;
    public final static int C_get_client_name = 70;
    public final static int C_get_client_version = 71;
    public final static int C_mark_text = 72;
    public final static int C_unmark_text = 73;

    public final static int C_lookup_z_name = 76;
    public final static int C_set_last_read = 77;
    public final static int C_get_uconf_stat = 78;

    public final static int C_user_active = 82;
    public final static int C_who_is_on_dynamic = 83;
    public final static int C_get_static_session_info = 84;

    public final static int C_create_text = 86;

    public final static int C_create_conf = 88;
    public final static int C_create_person = 89;

    public final static int C_get_text_stat = 90;
    public final static int C_get_conf_stat = 91;
    public final static int C_modify_text_info = 92;
    public final static int C_modify_conf_info = 93;

    public final static int C_query_read_texts = 98;
    public final static int C_get_membership = 99;

    public final static int C_add_member = 100;

    public final static int C_local_to_global = 103;

}








