/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

public class Person {
    public byte[] username;
    public Bitstring privileges;
    public Bitstring flags;
    public KomTime lastLogin;
    public int userArea;
    public int totalTimePresent;
    public int sessions;
    public int createdLines;
    public int createdBytes;
    public int readTexts;
    public int noOfTextFetches;
    public int createdPersons;
    public int createdConfs;
    public int firstCreatedLocalNo;
    public int noOfCreatedTexts;
    public int noOfMarks;
    public int noOfConfs;

    int number;

    public UConference uconf;

    public Person(int number) {
	this.number = number;
    }

    public int getNo() {
	return number;
    }
    
    public UConference getUConference() {
    	return uconf;	
    }

    public static Person createFrom(int persNo, RpcReply reply) {
	Person person = new Person(persNo);
	int pcount = 0;
	KomToken[] parameters = reply.getParameters();	
	person.username = ((Hollerith) parameters[pcount++]).getContents();
	person.privileges = new Bitstring(parameters[pcount++]);
	person.flags = new Bitstring(parameters[pcount++]);
	person.lastLogin = KomTime.createFrom(pcount, parameters);
	pcount = pcount + KomTime.ITEM_SIZE;
	person.userArea = parameters[pcount++].toInteger();
	person.totalTimePresent = parameters[pcount++].toInteger();
	person.sessions = parameters[pcount++].toInteger();
	person.createdLines = parameters[pcount++].toInteger();
	person.createdBytes = parameters[pcount++].intValue(); // new
	person.readTexts = parameters[pcount++].toInteger();
	person.noOfTextFetches = parameters[pcount++].toInteger();
	person.createdPersons = parameters[pcount++].toInteger();
	person.createdConfs = parameters[pcount++].toInteger();
	person.firstCreatedLocalNo = parameters[pcount++].toInteger();
	person.noOfCreatedTexts = parameters[pcount++].toInteger(); // new
	person.noOfMarks = parameters[pcount++].toInteger();
	person.noOfConfs = parameters[pcount++].toInteger();
	Debug.println("Person " + persNo + " noOfConfs: " + person.noOfConfs);
	return person;
    }

}
	
