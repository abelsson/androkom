package org.lindev.androkom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.dll.lyskom.RpcFailure;
import nu.dll.lyskom.Text;

import org.lindev.androkom.KomServer.TextInfo;

import android.os.AsyncTask;
import android.util.Log;

public class TextFetcher
{
    private static final String TAG = "Androkom";
    private static final int MAX_PREFETCH = 10;

    private final KomServer mKom;
    private final Set<Integer> mSent;
    private final Map<Integer, TextInfo> mTextCache;
    private final BlockingQueue<TextConf> mUnreadQueue;

    private PrefetchRunner mPrefetchRunner = null;
    private boolean mShowFullHeaders = true;

    private class TextConf {
        private final int textNo;
        private final int confNo;

        private TextConf (final int textNo, final int confNo) {
            this.textNo = textNo;
            this.confNo = confNo;
        }
    }

    public TextFetcher(final KomServer kom)
    {
        this.mKom = kom;
        this.mSent = new HashSet<Integer>();
        this.mTextCache = new ConcurrentHashMap<Integer, TextInfo>();
        this.mUnreadQueue = new ArrayBlockingQueue<TextConf>(MAX_PREFETCH);
    }

    public void setShowFullHeaders(final boolean showFullHeaders) {
        this.mShowFullHeaders = showFullHeaders;
    }

    private class TextFetcherTask extends AsyncTask<Integer, Void, TextInfo> {
        private int mTextNo;

        private TextInfo getTextFromServer(final int textNo) {
        	Log.d(TAG, "TextFetcherTask getTextFromServer");

            Text text = null;
            try {
                text = mKom.getSession().getText(textNo);
            } catch (final RpcFailure e) {
                e.printStackTrace();
                return null;
            } catch (final IOException e) {
                e.printStackTrace();
                return null;
            }
            String username;
            int authorid = text.getAuthor();
            if (authorid > 0) {
                try {
                    nu.dll.lyskom.Conference confStat = mKom.getSession().getConfStat(authorid);
                    username = confStat.getNameString();
                } catch (final Exception e) {
                    username = mKom.getString(R.string.person) + authorid + mKom.getString(R.string.does_not_exist);
                }
            } else {
                username = mKom.getString(R.string.anonymous);
            }
            Date CreationTime = text.getCreationTime();
            SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm]");
            String CreationTimeString = sdf.format(CreationTime);
            String SubjectString = null;
            try {
                SubjectString = text.getSubjectString();
            } catch (UnsupportedEncodingException e) {
                Log.d(TAG, "UnsupportedEncodingException"+e);
                SubjectString = text.getSubjectString8();
            }
            String BodyString = null;
            try {
                BodyString = text.getBodyString();
            } catch (UnsupportedEncodingException e) {
                Log.d(TAG, "UnsupportedEncodingException"+e);
                BodyString = text.getBodyString8();
            }
            StringBuilder headersString = new StringBuilder();
            if (mShowFullHeaders) {
                int[] items;
                items = text.getRecipients();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append("Mottagare: ");
                        try {
                            nu.dll.lyskom.Conference confStat = mKom.getSession()
                                    .getConfStat(items[i]);
                            headersString.append(confStat.getNameString());
                        } catch (Exception e) {
                            username = mKom.getString(R.string.person) + authorid
                                    + mKom.getString(R.string.does_not_exist);
                        }
                        headersString.append('\n');
                    }
                }
                items = text.getCcRecipients();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append("Kopiemottagare: ");
                        try {
                            nu.dll.lyskom.Conference confStat = mKom.getSession()
                                    .getConfStat(items[i]);
                            headersString.append(confStat.getNameString());
                        } catch (Exception e) {
                            username = mKom.getString(R.string.person) + authorid
                                    + mKom.getString(R.string.does_not_exist);
                        }
                        headersString.append('\n');
                    }
                }
                items = text.getCommented();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append("Kommentar till: ");
                        headersString.append(items[i]);
                        headersString.append('\n');
                    }
                }
                items = text.getComments();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append("Kommentar i: ");
                        headersString.append(items[i]);
                        headersString.append('\n');
                    }
                }
                items = text.getFootnotes();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append("Fotnot i: ");
                        headersString.append(items[i]);
                        headersString.append('\n');
                    }
                }
                items = text.getFootnoted();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append("Fotnot till: ");
                        headersString.append(items[i]);
                        headersString.append('\n');
                    }
                }
            }
            Log.d(TAG, "TextFetcherTask getTextFromServer done");
            return new TextInfo(textNo, username, CreationTimeString, headersString.toString(),
                    SubjectString, BodyString);
        }

        protected TextInfo doInBackground(final Integer... args) {
        	Log.d(TAG, "TextFetcherTask doInBackground");
            mTextNo = args[0];
            Log.i(TAG, "TextFetcherTask fetching text " + mTextNo);
            TextInfo text = getTextFromServer(mTextNo);
            if (text == null) {
                text = new TextInfo(-1, "", "", "", "", mKom.getString(R.string.error_fetching_text));
            }
        	Log.d(TAG, "TextFetcherTask doInBackground done");
            return text;
        }

        protected void onPostExecute(final TextInfo text) {
        	Log.d(TAG, "TextFetcherTask onPostExecute");
            mTextCache.put(mTextNo, text);
            synchronized(mTextCache) {
                mTextCache.notifyAll();
            }
        	Log.d(TAG, "TextFetcherTask onPostExecute done");
        }
    }

    private class PrefetchRunner extends Thread {
        private static final int ASK_AMOUNT = 2 * MAX_PREFETCH;

        private final Queue<Integer> mUnreadConfs;
        private final Set<Integer> mEnqueued;

        private Iterator<Integer> mMaybeUnreadIter = null;
        private int mCurrConf = -1;
        private boolean mIsInterrupted = false;

        private PrefetchRunner(final int confNo) {
            Log.i(TAG, "PrefetchRunner starting in conference " + confNo);
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
                Log.i(TAG, "PrefetchRunner enqueuing unread conference " + unreadConfList.get(i));
                mUnreadConfs.add(unreadConfList.get(i));
            }
            for (int i = 0; i < startIdx; ++i) {
                Log.i(TAG, "PrefetchRunner enqueuing unread conference " + unreadConfList.get(i));
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
                    Log.i(TAG, "PrefetchRunner was interrupted");
                    return;
                }
                Log.i(TAG, "PrefetchRunner prefetching text " + textNo + " in conference " + confNo);
                getText(textNo);
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
                    Log.i(TAG, "PrefetchRunner found no more unread. Exiting.");
                    mUnreadQueue.put(new TextConf(-1, -1));
                } catch (final InterruptedException e) { }
            }
            else {
                Log.i(TAG, "PrefetchRunner is exiting because it was interrupted");
            }
        }
    }

    /**
     * Spawn a new task to fetch a text, unless it's already cached or there's another task fetching it.
     *
     * @param textNo global text number to fetch
     */
    public void doGetText(final int textNo) {
        boolean needFetch;

        synchronized (mSent) {
            needFetch = mSent.add(textNo);
        }

        if (needFetch) {
            new TextFetcherTask().execute(textNo);
        }
    }

    /**
    * Fetch a text (if needed), and return it
    *
    * @param textNo global text number to fetch
    */
    public TextInfo getText(final int textNo) {
    	if(textNo<1) {
    		Log.d(TAG, "There are no negative text numbers.");
    		return new TextInfo(-1, "", "", "", "", mKom.getString(R.string.error_fetching_unread_text));
    	}
        final Thread currentThread = Thread.currentThread();

        doGetText(textNo);
        TextInfo text = mTextCache.get(textNo);

        while (!currentThread.isInterrupted() && text == null) {
            //Log.i(TAG, "TextFetcher getText(), waiting on text " + textNo);
            synchronized(mTextCache) {
                try {
                    mTextCache.wait(500);
                } catch (final InterruptedException e) {
                    return null;
                }
            }
            text = mTextCache.get(textNo);
        }

        return text;
    }

    public TextInfo getNextUnreadText() {
        final int confNo = mKom.getSession().getCurrentConference();
        if (mPrefetchRunner == null) {
            return new TextInfo(-1, "", "", "", "", mKom.getString(R.string.all_read));
        }
        final TextConf tc;
        try {
            tc = mUnreadQueue.take();
        } catch (final InterruptedException e) {
            return new TextInfo(-1, "", "", "", "", mKom.getString(R.string.error_fetching_unread_text));
        }
        if (tc.textNo < 0) {
            mPrefetchRunner = null;
            return new TextInfo(-1, "", "", "", "", mKom.getString(R.string.all_read));
        }
        if (mKom.isLocalRead(tc.textNo)) {
            return getNextUnreadText();
        }
        if (tc.confNo != confNo) {
            try {
                mKom.getSession().changeConference(tc.confNo);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return getText(tc.textNo);
    }

    public TextInfo getParentToText(final int textNo) {
    	Log.d(TAG, "Trying to get parent to "+textNo);
        try {
            // We can assume this is cached
            Text t = mKom.getSession().getText(textNo);
            final int arr[] = t.getCommented();
            if (arr.length > 0) {
                return getText(arr[0]);
            } else {
                return new TextInfo(-1, "", "", "", "", "Text has no parent");
            }
        } catch (final RpcFailure e) {
            Log.d(TAG, "getParentToText "+e);
            e.printStackTrace();
        } catch (final Exception e) {
            Log.d(TAG, "getParentToText "+e);
            e.printStackTrace();
        }
        return new TextInfo(-1, "", "", "", "", "[error fetching parent text]");
    }

    public void restartPrefetcher() {
        final int confNo = mKom.getSession().getCurrentConference();
        if (mPrefetchRunner != null) {
            Log.i(TAG, "TextFetcher restartPrefetcher(), interrupting old PrefetchRunner");
            mPrefetchRunner.mIsInterrupted = true;
            mPrefetchRunner.interrupt();
        }
        mTextCache.clear();
        mUnreadQueue.clear();
        mPrefetchRunner = new PrefetchRunner(confNo);
        mPrefetchRunner.start();
    }

    private static final Pattern TEXT_LINK_FINDER = Pattern.compile("\\d{5,}");
    private class CacheRelevantTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(final Integer... args) {
        	Log.d(TAG, "CacheRelevantTask doInBackground");
            final int textNo = args[0];
            final TextInfo textInfo = getText(textNo);
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
					Log.i(TAG, "CacheRelevantTask " + linkNo
							+ " is a found int body of " + textNo);
					texts.add(linkNo);
				} catch (java.lang.NumberFormatException e) {
					Log.d(TAG, "Not a number error:" + str);
				}
            }

            for (final int t : texts) {
                getText(t);
            }

        	Log.d(TAG, "CacheRelevantTask doInBackground done");
            return null;
        }
    }

    /**
     * Cache all comments and footnotes to a text
     * 
     * @param textNo
     */
    public void doCacheRelevant(final int textNo) {
        if (textNo > 0) {
            new CacheRelevantTask().execute(textNo);
        }
    }
}
