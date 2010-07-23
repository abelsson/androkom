/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

public class SessionInfo {
	byte[] username;
	byte[] hostname;
	byte[] identUser;
	KomTime connectionTime;

	public SessionInfo(int offset, KomToken[] data) {
		int i = offset;
		username = data[i++].getContents();
		hostname = data[i++].getContents();
		identUser = data[i++].getContents();
		connectionTime = KomTime.createFrom(i, data);
	}

	public byte[] getUsername() {
		return username;
	}

	public byte[] getHostname() {
		return hostname;
	}

	public byte[] getIdentUser() {
		return identUser;
	}

	public KomTime getConnectionTime() {
		return connectionTime;
	}

}
