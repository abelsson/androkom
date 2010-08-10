/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package org.lysator.lattekom;

/**
 * @deprecated Applications should catch the RpcFailure instead. This exception
 *             is never thrown by LatteKOM.
 */
@SuppressWarnings("serial")
class LoginFailedException extends Exception {
}
