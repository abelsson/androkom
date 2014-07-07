package org.lindev.androkom.text;

import nu.dll.lyskom.Text;

import org.lindev.androkom.KomServer;
import org.lindev.androkom.KomServer.TextInfo;

import android.util.Log;

public class TextFetcher {
    private static final String TAG = "Androkom TextFetcher";

    private final KomServer mKom;
    private final TextCache mTextCache;
    private final Prefetcher mPrefetcher;

    public TextFetcher(final KomServer kom)
    {
        this.mKom = kom;
        this.mTextCache = new TextCache(mKom);
        this.mPrefetcher = new Prefetcher(mKom, mTextCache);
    }

    public TextInfo getKomText(final int textNo) {
        final TextInfo text = mTextCache.getDText(textNo);
        if(text!=null) {
            mPrefetcher.doCacheRelevant(text.getTextNo());
        }
        return text;
    }

    public int getNextUnreadTextNo() {
        Log.d(TAG, "getNextUnreadTextNo");
        return mPrefetcher.getNextUnreadTextNo();
    }

    public TextInfo getNextUnreadText(final boolean peekQueue) {
        boolean doCacheRelevant = true;
        Log.d(TAG, "getNextUnreadText");
        return mPrefetcher.getNextUnreadText(doCacheRelevant, peekQueue);
    }

    public TextInfo getParentToText(final int textNo) {
        final Text text;
        try {
            // We can assume this is cached
            Log.d(TAG, "getParentToText:"+textNo);
            text = mKom.getTextbyNo(textNo);
        } catch (final Exception e) {
            Log.d(TAG, "getParentToText " + e);
            //e.printStackTrace();
            //mKom.reconnect();
            return TextInfo.createText(mKom.getBaseContext(), TextInfo.ERROR_FETCHING_TEXT);
        }
        
        if (text != null) {
            final int commented[] = text.getCommented();
            if (commented.length > 0) {
                Log.d(TAG, "number of parents:" + commented.length);
                final TextInfo textInfo = mTextCache.getDText(commented[0]);
                if (textInfo != null) {
                    mPrefetcher.doCacheRelevant(textInfo.getTextNo());
                }
                return textInfo;
            }
        }
        Log.d(TAG, "No parent found");
        return TextInfo.createText(mKom.getBaseContext(), TextInfo.NO_PARENT);
    }

    public void startPrefetcher() {
        try {
            mPrefetcher.start(mKom.getCurrentConference());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void restartPrefetcher() {
        try {
            mPrefetcher.restart(mKom.getCurrentConference());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void interruptPrefetcher() {
        mPrefetcher.interruptPrefetcher();
    }

    public void setShowHeadersLevel(final int mShowHeadersLevel) {
        mTextCache.setShowHeadersLevel(mShowHeadersLevel);
    }

/*    public void removeTextFromCache(int textNo) {
        mTextCache.removeTextFromCache(textNo);
        mPrefetcher.removeTextFromCache(textNo);
    }
    */
}
