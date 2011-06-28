package org.lindev.androkom.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.dll.lyskom.Text;

import org.lindev.androkom.KomServer;
import org.lindev.androkom.KomServer.TextInfo;

import android.os.AsyncTask;
import android.util.Log;

class Prefetcher {
    private static final String TAG = "Androkom";

    private static final Pattern TEXT_LINK_FINDER = Pattern.compile("\\d{5,}");
    private static final int MAX_PREFETCH = 10;
    private static final int ASK_AMOUNT = 2 * MAX_PREFETCH;
    private static boolean ENABLE_CACHE_RELEVANT = false;

    private final KomServer mKom;
    private final TextCache mTextCache;

    private final BlockingQueue<TextConf> mUnreadQueue;
    private final Set<Integer> mRelevantCached;

    private PrefetchNextUnread mPrefetchRunner = null;

    Prefetcher(final KomServer kom, final TextCache textCache) {
        this.mKom = kom;
        this.mTextCache = textCache;
        this.mUnreadQueue = new ArrayBlockingQueue<TextConf>(MAX_PREFETCH);
        this.mRelevantCached = new HashSet<Integer>();
    }

    private class TextConf {
        private final int textNo;
        private final int confNo;

        TextConf (final int textNo, final int confNo) {
            this.textNo = textNo;
            this.confNo = confNo;
        }
    }

    private class PrefetchNextUnread extends Thread {
        private final Queue<Integer> mUnreadConfs;
        private final Set<Integer> mEnqueued;

        private Iterator<Integer> mMaybeUnreadIter = null;
        private int mCurrConf = -1;
        private boolean mIsInterrupted = false;

        PrefetchNextUnread(final int confNo) {
            Log.i(TAG, "PrefetchNextUnread starting in conference " + confNo);
            this.mUnreadConfs = new LinkedList<Integer>();
            this.mEnqueued = new HashSet<Integer>();
            initialize(confNo);
        }

        private void initialize(final int confNo) {
            final List<Integer> unreadConfList = mKom.getSession().getUnreadConfsListCached();
            int startIdx = unreadConfList.indexOf(confNo);
            if (startIdx < 0) {
                startIdx = 0;
            }
            for (int i = startIdx; i < unreadConfList.size(); ++i) {
                Log.i(TAG, "PrefetchNextUnread enqueuing unread conference " + unreadConfList.get(i));
                mUnreadConfs.add(unreadConfList.get(i));
            }
            for (int i = 0; i < startIdx; ++i) {
                Log.i(TAG, "PrefetchNextUnread enqueuing unread conference " + unreadConfList.get(i));
                mUnreadConfs.add(unreadConfList.get(i));
            }
            this.mMaybeUnreadIter = Collections.<Integer>emptyList().iterator();
        }

        private void enqueueAndPrefetch(final int textNo, final int confNo) {
            final boolean notEnqueued = mEnqueued.add(textNo);
            if (notEnqueued && !mKom.isLocalRead(textNo)) {
                try {
                    mUnreadQueue.put(new TextConf(textNo, confNo));
                } catch (final InterruptedException e) {
                    // Someone called interrupt() on this thread. We shouldn't do anything more, just exit.
                    Log.i(TAG, "PrefetchNextUnread was interrupted");
                    return;
                }
                Log.i(TAG, "PrefetchNextUnread prefetching text " + textNo + " in conference " + confNo);
                mTextCache.getText(textNo);
            }
        }

        public Iterator<Integer> askServerForMore() {
            mCurrConf = mUnreadConfs.element();
            List<Integer> maybeUnread;
            try {
                maybeUnread = mKom.getSession().nextUnreadTexts(mCurrConf, false, ASK_AMOUNT);
            } catch (final IOException e) {
                maybeUnread = Collections.<Integer>emptyList();
            }
            Log.i(TAG, "PrefetchNextUnread asked server about conf " + mCurrConf + ", got " + maybeUnread.size() + " texts");
            Log.i(TAG, maybeUnread.toString());

            // If we don't get as may texts as we ask for, we can assume that there are no more in the conference,
            // so we remove the head of mUnreadConfs.
            if (maybeUnread.size() < ASK_AMOUNT) {
                mUnreadConfs.remove();
            }

            return maybeUnread.iterator();
        }

        @Override
        public void run() {
            while (!mIsInterrupted) {
                if (mMaybeUnreadIter.hasNext()) {
                    final int textNo = mMaybeUnreadIter.next();
                    enqueueAndPrefetch(textNo, mCurrConf);
                }
                else if (!mUnreadConfs.isEmpty()) {
                    // Ask the server for more (possibly) unread texts
                    mMaybeUnreadIter = askServerForMore();
                }
                else {
                    // No more unread in conference, and no more unread conferences
                    break;
                }
            }

            // If the thread wasn't interrupted, we should put an end marker on the queue.
            if (!mIsInterrupted) {
                try {
                    // Enqueue the marker that there are no more unread texts
                    Log.i(TAG, "PrefetchNextUnread found no more unread. Exiting.");
                    mUnreadQueue.put(new TextConf(-1, -1));
                } catch (final InterruptedException e) { }
            }
            else {
                Log.i(TAG, "PrefetchNextUnread is exiting because it was interrupted");
            }
        }
    }

    TextInfo getNextUnreadText(final boolean cacheRelevant) {
        // If mPrefetchRunner is null, we have already reached the end of the queue
        if (mPrefetchRunner == null) {
            return TextInfo.createText(mKom.getBaseContext(), TextInfo.ALL_READ);
        }

        // Get the next unread text from the queue
        final TextConf tc;
        try {
            tc = mUnreadQueue.take();
        } catch (final InterruptedException e) {
            return TextInfo.createText(mKom.getBaseContext(), TextInfo.ERROR_FETCHING_TEXT);
        }

        // This is how the prefetcher marks that there are no more unread texts. mPrefetchRunner should be finished,
        // so we can delete the reference to it.
        if (tc.textNo < 0) {
            mPrefetchRunner = null;
            return TextInfo.createText(mKom.getBaseContext(), TextInfo.ALL_READ);
        }

        // If the text is already locally marked as read, get the next one instead
        if (mKom.isLocalRead(tc.textNo)) {
            return getNextUnreadText(cacheRelevant);
        }

        // Switch conference name
        mKom.setConferenceName(mKom.getConferenceName(tc.confNo));

        // Retrieve the text
        final TextInfo text = mTextCache.getText(tc.textNo);

        // Cache relevant info both for this text and for the next in the queue (if available)
        if (cacheRelevant) {
            doCacheRelevant(tc.textNo);
            final TextConf tcNext = mUnreadQueue.peek();
            if (tcNext != null) {
                doCacheRelevant(tcNext.textNo);
            }
        }

        return text;
        
    }

    void restart(final int confNo) {
        if (mPrefetchRunner != null) {
            Log.i(TAG, "TextFetcher restartPrefetcher(), interrupting old PrefetchRunner");
            mPrefetchRunner.mIsInterrupted = true;
            mPrefetchRunner.interrupt();
        }
        mUnreadQueue.clear();
        mPrefetchRunner = new PrefetchNextUnread(confNo);
        mPrefetchRunner.start();
    }

    private class CacheRelevantTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(final Integer... args) {
            final int textNo = args[0];
            final TextInfo textInfo = mTextCache.getText(textNo);
            final Text text;
            try {
                text = mKom.getSession().getText(textNo);
            } catch (final Exception e) {
                e.printStackTrace();
                return null;
            }

            final List<Integer> texts = new ArrayList<Integer>();
            for (int comment : text.getComments()) {
                Log.i(TAG, "CacheRelevantTask " + comment + " is a comment to " + textNo);
                texts.add(comment);
            }
            for (int footnote : text.getFootnotes()) {
                Log.i(TAG, "CacheRelevantTask " + footnote + " is a footnote to " + textNo);
                texts.add(footnote);
            }
            for (int commented : text.getCommented()) {
                Log.i(TAG, "CacheRelevantTask " + commented + " is a parent to " + textNo);
                texts.add(commented);
            }

            final Matcher m = TEXT_LINK_FINDER.matcher(textInfo.getBody());
            while (m.find()) {
                final String str = textInfo.getBody().substring(m.start(), m.end());
                try {
                    final int linkNo = Integer.valueOf(str);
                    Log.i(TAG, "CacheRelevantTask, text number " + linkNo + " found in body of " + textNo);
                    texts.add(linkNo);
                } catch (final NumberFormatException e) {
                    Log.i(TAG, "CacheRelevantTask, unable to parse " + str + " as text number in body of " + textNo);
                }
            }

            for (final int t : texts) {
                mTextCache.getText(t);
            }

            return null;
        }
    }

    /**
     * Cache all comments and footnotes to a text
     */
    void doCacheRelevant(final int textNo) {
        if (!ENABLE_CACHE_RELEVANT || textNo <= 0) {
            return;
        }
        final boolean needCaching;
        synchronized (mRelevantCached) {
            needCaching = mRelevantCached.add(textNo);
        }
        if (needCaching) {
            new CacheRelevantTask().execute(textNo);
        }
    }
}
