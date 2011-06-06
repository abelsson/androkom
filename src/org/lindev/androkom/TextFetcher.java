package org.lindev.androkom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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

    private final KomServer mKom;
    private final Set<Integer> mSent;
    private final Map<Integer, TextInfo> mTextCache;
    private final Map<Integer, SortedSet<Integer>> mKnownUnread;
    private boolean mShowFullHeaders = true;

    public TextFetcher(final KomServer kom)
    {
        this.mKom = kom;
        this.mSent = new HashSet<Integer>();
        this.mTextCache = new ConcurrentHashMap<Integer, TextInfo>();
        this.mKnownUnread = new HashMap<Integer, SortedSet<Integer>>();
    }

    public void setShowFullHeaders(final boolean showFullHeaders) {
        this.mShowFullHeaders = showFullHeaders;
    }

    private class TextFetcherTask extends AsyncTask<Integer, Void, TextInfo> {
        private int mTextNo;

        private TextInfo getTextFromServer(final int textNo) {
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
            String HeadersString = "";
            if (mShowFullHeaders) {
                int[] items;
                items = text.getRecipients();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        HeadersString += "Mottagare: ";
                        try {
                            nu.dll.lyskom.Conference confStat = mKom.getSession()
                                    .getConfStat(items[i]);
                            HeadersString += confStat.getNameString();
                        } catch (Exception e) {
                            username = mKom.getString(R.string.person) + authorid
                                    + mKom.getString(R.string.does_not_exist);
                        }
                        HeadersString += "\n";
                    }
                }
                items = text.getCcRecipients();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        HeadersString += "Kopiemottagare: ";
                        try {
                            nu.dll.lyskom.Conference confStat = mKom.getSession()
                                    .getConfStat(items[i]);
                            HeadersString += confStat.getNameString();
                        } catch (Exception e) {
                            username = mKom.getString(R.string.person) + authorid
                                    + mKom.getString(R.string.does_not_exist);
                        }
                        HeadersString += "\n";
                    }
                }
                items = text.getCommented();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        HeadersString += "Kommentar till: " + items[i] + "\n";
                    }
                }
                items = text.getComments();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        HeadersString += "Kommentar i: " + items[i] + "\n";
                    }
                }
                items = text.getFootnotes();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        HeadersString += "Fotnot i: " + items[i] + "\n";
                    }
                }
                items = text.getFootnoted();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        HeadersString += "Fotnot till: " + items[i] + "\n";
                    }
                }
            }
            return new TextInfo(textNo, username, CreationTimeString, HeadersString, SubjectString, BodyString);
        }

        protected TextInfo doInBackground(final Integer... args) {
            mTextNo = args[0];
            TextInfo text = getTextFromServer(mTextNo);
            if (text == null) {
                text = new TextInfo(-1, "", "", "", "", mKom.getString(R.string.error_fetching_text));
            }
            return text;
        }

        protected void onPostExecute(final TextInfo text) {
            mTextCache.put(mTextNo, text);
            synchronized(mTextCache) {
                mTextCache.notifyAll();
            }
        }
    }

    private class TextPrefetchTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(final Integer... args) {
            for (final Integer textNo : args) {
                Log.i(TAG, "TextPrefetcherTask prefetching " + textNo);
                getText(textNo);
            }
            return null;
        }
    }

    private SortedSet<Integer> getKnownUnreadForConf(final int conf) {
        SortedSet<Integer> knownUnread;
        synchronized (mKnownUnread) {
            knownUnread = mKnownUnread.get(conf);
            if (knownUnread == null) {
                knownUnread = new TreeSet<Integer>();
                mKnownUnread.put(conf, knownUnread);
            }
        }
        return knownUnread;
    }

    private Integer nextUnreadInConf(final int conf) {
        final SortedSet<Integer> knownUnread = getKnownUnreadForConf(conf);
        Integer nextUnread = null;
        synchronized (knownUnread) {
            if (knownUnread.isEmpty()) {
                refillUnreads(knownUnread, conf);
            }
            if (!knownUnread.isEmpty()) {
                nextUnread = knownUnread.first();
                knownUnread.remove(nextUnread);
            }
        }
        return nextUnread;
    }

    private static final int MAX_PREFETCH = 20;
    private void refillUnreads(final SortedSet<Integer> knownUnread, final int conf) {
        try {
            // possibleUnreads might contain texts that are read which the server isn't aware of
            final List<Integer> possibleUnreads = mKom.getSession().nextUnreadTexts(conf, false, MAX_PREFETCH);

            // newUnreads are the really unread texts
            final List<Integer> newUnreads = new ArrayList<Integer>();
            for (Integer textNo : possibleUnreads) {
                if (!mKom.isLocalRead(textNo)) {
                    newUnreads.add(textNo);
                    knownUnread.add(textNo);
                }
            }

            // Prefetch the new texts
            if (newUnreads.size() > 0) {
                new TextPrefetchTask().execute(newUnreads.toArray(new Integer[newUnreads.size()]));
            }
        } catch (final IOException e) {
            e.printStackTrace();
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
        doGetText(textNo);

        TextInfo text = mTextCache.get(textNo);

        while (text == null) {
            Log.i(TAG, "TextFetcher getText(), waiting on text " + textNo);
            synchronized(mTextCache) {
                try {
                    mTextCache.wait(500);
                } catch (final InterruptedException e) { }
            }
            text = mTextCache.get(textNo);
        }

        Log.i(TAG, "TextFetcher getText(), got text " + textNo);
        return text;
    }

    public TextInfo getNextUnreadText() {
        final int conf = mKom.getSession().getCurrentConference();
        final Integer nextUnread = nextUnreadInConf(conf);

        if (nextUnread == null) {
            // No more unread in the conference
            try {
                int newConf = mKom.getSession().nextUnreadConference(true);
                if (newConf < 0) {
                    // No more unread conferences
                    return new TextInfo(-1, "", "", "", "", mKom.getString(R.string.all_read));
                }
                return getNextUnreadText();
            } catch (final IOException e) {
                e.printStackTrace();
                return new TextInfo(-1, "", "", "", "", mKom.getString(R.string.error_fetching_unread_text));
            }
        }

        return getText(nextUnread);
    }

    public TextInfo getParentToText(final int textNo) {
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
        mKom.reconnect();
        return new TextInfo(-1, "", "", "", "", "[error fetching parent text]");
    }

    private static final Pattern TEXT_LINK_FINDER = Pattern.compile("\\d{5,}");
    private class CacheRelevantTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(final Integer... args) {
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

            return null;
        }
    }

    /**
     * Cache all comments and footnotes to a text
     * 
     * @param textNo
     */
    public void doCacheRelevant(final int textNo) {
        new CacheRelevantTask().execute(textNo);
    }
}
