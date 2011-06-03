package org.lindev.androkom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nu.dll.lyskom.Text;

import org.lindev.androkom.KomServer.TextInfo;

import android.os.AsyncTask;
import android.util.Log;

public class TextFetcher
{
    private static final String TAG = "Android";

    private final KomServer mKom;
    private final Set<Integer> mSent;
    private final Map<Integer, TextInfo> mTextCache;
    private boolean ShowFullHeaders = true;

    public TextFetcher(final KomServer kom)
    {
        this.mKom = kom;
        this.mSent = new HashSet<Integer>();
        this.mTextCache = new ConcurrentHashMap<Integer, TextInfo>();
    }

    private class TextFetcherTask extends AsyncTask<Integer, Void, TextInfo> {
        private int mTextNo;

        protected TextInfo doInBackground(final Integer... args) {
            mTextNo = args[0];
            try {
                Text text = mKom.getSession().getText(mTextNo);
                String username;
                int authorid = text.getAuthor();
                if (authorid > 0) {
                    try {
                        nu.dll.lyskom.Conference confStat = mKom.getSession().getConfStat(authorid);
                        username = confStat.getNameString();
                    } catch (Exception e) {
                        username = mKom.getString(R.string.person)+authorid+
                        mKom.getString(R.string.does_not_exist);
                    }
                } else {
                    username = mKom.getString(R.string.anonymous);
                }
                String CreationTimeString = text.getCreationTimeString();
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
                if (ShowFullHeaders) {
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
                return new TextInfo(mTextNo, username, CreationTimeString, HeadersString, SubjectString, BodyString);
            } catch (final Exception e) {
                Log.d(TAG, "getTextAsHTML "+e);
                e.printStackTrace();
            }

            return new TextInfo(-1, "", "", "", "", mKom.getString(R.string.error_fetching_text));
        }

        protected void onPostExecute(final TextInfo text) {
            mTextCache.put(mTextNo, text);
            synchronized(mTextCache) {
                mTextCache.notifyAll();
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
        doGetText(textNo);

        TextInfo text = mTextCache.get(textNo);

        while (text == null) {
            synchronized(mTextCache) {
                try {
                    mTextCache.wait(500);
                } catch (final InterruptedException e) { }
            }
            text = mTextCache.get(textNo);
        }

        return text;
    }

    /**
     * Cache all comments and footnotes to a text
     * 
     * @param textNo
     */
    public void cacheComments(final int textNo) {
        try {
            // We can assume this is cached (if textNo has been fetched before)
            final Text text = mKom.getSession().getText(textNo);

            for (int commentNo : text.getComments()) {
                Log.i(TAG, "Trying to cache text " + commentNo + ", which is a comment to " + textNo);
                doGetText(commentNo);
            }

            for (int footnoteNo : text.getFootnotes()) {
                Log.i(TAG, "Trying to cache text " + footnoteNo + ", which is a footnote to " + textNo);
                doGetText(footnoteNo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
