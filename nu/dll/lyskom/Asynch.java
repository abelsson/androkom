/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;


// don't remember why this is an abstract class instead of an interface...
public abstract class Asynch {
    public final static int new_text_old = 0;
    public final static int i_am_off = 1; // deprecated
    public final static int i_am_on_obsolete = 2; // deprecated
    public final static int new_name = 5;
    public final static int i_am_on = 6;
    public final static int sync_db = 7;
    public final static int leave_conf = 8;
    public final static int login = 9;
    public final static int broadcast = 10; // deprecated
    public final static int rejected_connection = 11;
    public final static int send_message = 12;
    public final static int logout = 13;
    public final static int deleted_text = 14;
    public final static int new_text = 15;
    public final static int new_recipient = 16;
    public final static int sub_recipient = 17;
    public final static int new_membership = 18;

}



