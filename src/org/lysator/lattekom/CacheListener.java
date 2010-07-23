/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

interface CacheListener {
	public void itemAdded(Object o);
	public void itemUpdated(Object o);
	public void itemRemoved(Object o);
}
