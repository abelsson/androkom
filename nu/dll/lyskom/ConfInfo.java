/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;
/**
 * Class representing a Conf-Info structure
 * @author rasmus@sno.pp.se
 * @version 0.1
 */
public class ConfInfo {

    // Conf-Type ::= BITSTRING
    public int confNo = -1;
    public byte[] confName;
    public ConfType confType;

    public ConfInfo(byte[] confName,  ConfType confType, int confNo) {
	this.confName = confName;
	this.confType = confType;
	this.confNo = confNo;
    }

    public int getNo() {
	return confNo;
    }

    /**
     * Return the name of this Conference
     *
     */
    public String getNameString() {
	try {
	    return new String(confName, Session.serverEncoding);
	} catch (java.io.UnsupportedEncodingException e) {
	    throw new RuntimeException("ConfInfo.getNameString(): Unsupported encoding: " + e.getMessage());
	}
    }

    public String toString() {
	return "ConfInfo(confNo: "+confNo+"; confName: "+new String(confName)+
	    "; confType: "+new String(confType.getContents())+")";
    }
   
}
    
