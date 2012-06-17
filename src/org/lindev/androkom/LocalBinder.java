package org.lindev.androkom;

import java.lang.ref.WeakReference;

import android.os.Binder;

/**
 * A generic implementation of Binder to be used for local services
 * @author Geoff Bruckner  12th December 2009
 *
 * @param <S> The type of the service being bound
 */

public class LocalBinder<S> extends Binder {
    private String TAG = "LocalBinder";
    private  WeakReference<S> mService;
   
   
    public LocalBinder(S service){
        mService = new WeakReference<S>(service);
    }

   
    public S getService() {
        return mService.get();
    }
}
