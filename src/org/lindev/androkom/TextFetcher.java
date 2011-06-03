package org.lindev.androkom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nu.dll.lyskom.RpcFailure;
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
    private boolean showFullHeaders = true;

    public TextFetcher(final KomServer kom)
    {
        this.mKom = kom;
        this.mSent = new HashSet<Integer>();
        this.mTextCache = new ConcurrentHashMap<Integer, TextInfo>();
    }

    public void setShowFullHeaders(final boolean showFullHeaders) {
        this.showFullHeaders = showFullHeaders;
    }

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
        if (showFullHeaders) {
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

    private class TextFetcherTask extends AsyncTask<Integer, Void, TextInfo> {
        private int mTextNo;

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
            Log.i(TAG, "TextFetcher.getText, waiting on text " + textNo);
            synchronized(mTextCache) {
                try {
                    mTextCache.wait(500);
                } catch (final InterruptedException e) { }
            }
            text = mTextCache.get(textNo);
        }

        return text;
    }

    public TextInfo getNextUnreadText() {
        try {
            int textNo = mKom.getSession().nextUnreadText(false);
            if (textNo < 0) {
                mKom.getSession().nextUnreadConference(true);
                textNo = mKom.getSession().nextUnreadText(false);
                if (textNo < 0) {
                    return new TextInfo(-1, "", "", "", "", mKom.getString(R.string.all_read));
                }
            }
            return getText(textNo);
        } catch (final Exception e) {
            Log.d(TAG, "getNextUnreadText "+e);
            e.printStackTrace();
        }
        mKom.reconnect();
        return new TextInfo(-1, "", "", "", "", mKom.getString(R.string.error_fetching_unread_text));
    }

    public TextInfo getParentToText(final int textNo) {
        try {
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

    /**
     * Cache all comments and footnotes to a text
     * 
     * @param textNo
     */
    public void doCacheComments(final int textNo) {
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
