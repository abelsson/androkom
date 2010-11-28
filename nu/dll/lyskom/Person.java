/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class Person {
	protected byte[] username;
	protected Bitstring privileges;
	protected Bitstring flags;
	protected KomTime lastLogin;
	protected int userArea;
	protected int totalTimePresent;
	protected int sessions;
	protected int createdLines;
	protected int createdBytes;
	protected int readTexts;
	protected int noOfTextFetches;
	protected int createdPersons;
	protected int createdConfs;
	protected int firstCreatedLocalNo;
	protected int noOfCreatedTexts;
	protected int noOfMarks;
	protected int noOfConfs;

	int number;

	protected UConference uconf;

	Person(int number) {
		this.number = number;
	}

	/**
	 * Returns the number of this person.
	 */
	public int getNo() {
		return number;
	}

	/**
	 * Returns the UConference object representing this person's letterbox.
	 */
	public UConference getUConference() {
		return uconf;
	}

	/**
	 * Returns the username of this person.
	 */
	public byte[] getUsername() {
		return username;
	}

	/**
	 * Returns this person's previlege flags.
	 */
	public Bitstring getPrivileges() {
		return privileges;
	}

	/**
	 * Returns this person's flags.
	 */
	public Bitstring getFlags() {
		return flags;
	}

	/**
	 * Returns a KomTime object representing when this person last logged onto
	 * the LysKOM server.
	 */
	public KomTime getLastLogin() {
		return lastLogin;
	}

	/**
	 * Returns the text number containing this person's user area. Currently,
	 * LatteKOM contains no code to deal with the user area.
	 */
	public int getUserArea() {
		return userArea;
	}

	/**
	 * Returns the total number of seconds this person has been logged onto the
	 * LysKOM server.
	 */
	public int getTotalTimePresent() {
		return totalTimePresent;
	}
	/**
	 * Returns the total number of sessions this person has initiated.
	 */
	public int getSessions() {
		return sessions;
	}

	/**
	 * Returns the total number of text lines this person has produced.
	 */
	public int getCreatedLines() {
		return createdLines;
	}

	/**
	 * Returns the total number of text bytes this person has produced.
	 */
	public int getCreatedBytes() {
		return createdBytes;
	}

	/**
	 * Returns the total number of texts read by this person.
	 */
	public int getReadTexts() {
		return readTexts;
	}

	/**
	 * Returns the total number of text fetches this person has done.
	 */
	public int getNoOfTextFetches() {
		return noOfTextFetches;
	}

	/**
	 * Returns the number of persons this person has created.
	 */
	public int getCreatedPersons() {
		return createdPersons;
	}

	/**
	 * Returns the number of conferences created by this person.
	 */
	public int getCreatedConferences() {
		return createdConfs;
	}

	/**
	 * From the spec: "The local number of the earliest article written by the
	 * person. The local number applies to a local-to-global mapping containing
	 * all articles written by the person."
	 */
	public int getFirstCreatedLocalNo() {
		return firstCreatedLocalNo;
	}

	/**
	 * Returns the number of texts created by this person.
	 */
	public int getNoOfCreatedTexts() {
		return noOfCreatedTexts;
	}

	/**
	 * Returns the number of text-marks this person holds.
	 */
	public int getNoOfMarks() {
		return noOfMarks;
	}

	/**
	 * Returns the number of conferences this person is a member of.
	 */
	public int getNoOfConfs() {
		return noOfConfs;
	}

	/**
	 * Creates a person object out of an RpcReply containing the reply to a
	 * get-person-stat call.
	 */
	public static Person createFrom(int persNo, RpcReply reply) {
		Person person = new Person(persNo);
		int pcount = 0;
		KomToken[] parameters = reply.getParameters();
		person.username = ((Hollerith) parameters[pcount++]).getContents();
		person.privileges = new Bitstring(parameters[pcount++]);
		person.flags = new Bitstring(parameters[pcount++]);
		person.lastLogin = KomTime.createFrom(pcount, parameters);
		pcount = pcount + KomTime.ITEM_SIZE;
		person.userArea = parameters[pcount++].intValue();
		person.totalTimePresent = 1; // TODO:parameters[pcount++].intValue(); crash on value like 3821863021
		person.sessions = parameters[pcount++].intValue();
		person.createdLines = parameters[pcount++].intValue();
		person.createdBytes = parameters[pcount++].intValue(); // new
		person.readTexts = parameters[pcount++].intValue();
		person.noOfTextFetches = parameters[pcount++].intValue();
		person.createdPersons = parameters[pcount++].intValue();
		person.createdConfs = parameters[pcount++].intValue();
		person.firstCreatedLocalNo = parameters[pcount++].intValue();
		person.noOfCreatedTexts = parameters[pcount++].intValue(); // new
		person.noOfMarks = parameters[pcount++].intValue();
		person.noOfConfs = parameters[pcount++].intValue();
		Debug.println("Person " + persNo + " noOfConfs: " + person.noOfConfs);
		return person;
	}

}
