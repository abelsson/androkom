package org.lindev.androkom;

/**
 * Small helper class which maps conference names and LysKOM id's.
 */
public class ConferenceInfo {
	public int id;
	public String name;
	public int numUnread;

	@Override
	public String toString() {
		return name + " <" + id + ">";
	}
}