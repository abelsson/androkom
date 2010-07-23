/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
// -*- Mode: Java; c-basic-offset: 4 -*-
package org.lysator.lattekom;

interface RpcReplyReceiver {
	void rpcReply(RpcReply r);
}
