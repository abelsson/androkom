/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;
/**
 * Class representing a Conf-Info structure
 * 
 * @author rasmus@sno.pp.se
 * @version 0.1
 */
public class ConfInfo {

	// Conf-Type ::= BITSTRING
	public int confNo = -1;

	/** @deprecated use the Hollerith confNameH instead */
	public byte[] confName; // kept for backward-compatibility
	Hollerith confNameH;
	public ConfType confType;

	protected ConfInfo(Hollerith confName, ConfType confType, int confNo) {
		this.confName = confName.getContents();
		this.confNameH = confName;
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
		return confNameH.getContentString();
	}

	public String toString() {
		return "ConfInfo(confNo: " + confNo + "; confName: "
				+ new String(confName) + "; confType: "
				+ new String(confType.getContents()) + ")";
	}

}
