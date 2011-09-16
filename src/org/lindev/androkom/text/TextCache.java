package org.lindev.androkom.text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nu.dll.lyskom.AuxItem;
import nu.dll.lyskom.RpcFailure;
import nu.dll.lyskom.Text;

import org.lindev.androkom.KomServer;
import org.lindev.androkom.KomServer.TextInfo;
import org.lindev.androkom.R;

import android.os.AsyncTask;
import android.util.Log;

class TextCache {
    private static final String TAG = "Androkom";

    private final KomServer mKom;
    private final Set<Integer> mSent;
    private final Map<Integer, TextInfo> mTextCache;

    private boolean mShowFullHeaders = true;

    TextCache(final KomServer kom) {
        this.mKom = kom;
        this.mSent = new HashSet<Integer>();
        this.mTextCache = new ConcurrentHashMap<Integer, TextInfo>();
    }

    private String getAuthorName(int textNo) {
        Text text = null;
        try {
            Log.d(TAG, "getAuthorName:" + textNo);
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
                nu.dll.lyskom.Conference confStat = mKom
                        .getSession().getConfStat(authorid);
                username = confStat.getNameString();
            } catch (final Exception e) {
                username = mKom.getString(R.string.person)
                        + authorid
                        + mKom
                                .getString(R.string.does_not_exist);
            }
        } else {
            Log.d(TAG, "Text " + textNo + " authorid:"
                    + authorid);
            username = mKom.getString(R.string.anonymous);
        }
        return username;
    }

    private class TextFetcherTask extends AsyncTask<Integer, Void, Void> {
        private int mTextNo;

        private TextInfo getTextFromServer(final int textNo) {
            Text text = null;
            try {
                Log.d(TAG, "TextFetcherTask:"+textNo);
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
                Log.d(TAG, "Text "+textNo+" authorid:"+authorid);
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
                        headersString.append(mKom.getString(R.string.androkom_header_recipient));
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
                        headersString.append(mKom.getString(R.string.header_cc_recipient));
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
                        headersString.append(mKom.getString(R.string.header_comment_to));
                        headersString.append(items[i]);
                        headersString.append(" by " + getAuthorName(items[i]));
                        headersString.append('\n');
                    }
                }
                items = text.getComments();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom.getString(R.string.header_comment_in));
                        headersString.append(items[i]);
                        headersString.append(" by " + getAuthorName(items[i]));
                        headersString.append('\n');
                    }
                }
                items = text.getFootnotes();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom.getString(R.string.header_footnote_in));
                        headersString.append(items[i]);
                        headersString.append('\n');
                    }
                }
                items = text.getFootnoted();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom.getString(R.string.header_footnote_to));
                        headersString.append(items[i]);
                        headersString.append('\n');
                    }
                }
                List<AuxItem> aux_items = text.getAuxItems(AuxItem.tagCreationLocation);
                if (aux_items.size() > 0) {
                    for (int i = 0; i < aux_items.size(); i++) {
                        headersString.append(mKom.getString(R.string.header_aux_location));
                        headersString.append(aux_items.get(i));
                        headersString.append('\n');
                    }
                }
            }
            return new TextInfo(mKom.getBaseContext(), textNo, username, CreationTimeString, headersString.toString(),
                    SubjectString, BodyString, mShowFullHeaders);
        }

        protected Void doInBackground(final Integer... args) {
            mTextNo = args[0];
            Log.i(TAG, "TextFetcherTask fetching text " + mTextNo);
            TextInfo text = getTextFromServer(mTextNo);
            if (text == null) {
                text = TextInfo.createText(mKom.getBaseContext(), TextInfo.ERROR_FETCHING_TEXT);
            }
            mTextCache.put(mTextNo, text);
            synchronized(mTextCache) {
                mTextCache.notifyAll();
            }
            return null;
        }
    }

    /**
     * Spawn a new task to fetch a text, unless it's already cached or there's another task fetching it.
     *
     * @param textNo global text number to fetch
     */
    void doGetText(final int textNo) {
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
    TextInfo getText(final int textNo) {
        TextInfo text = mTextCache.get(textNo);
        if (text == null) {
            doGetText(textNo);
        }

        final Thread currentThread = Thread.currentThread();
        while (!currentThread.isInterrupted() && text == null) {
            synchronized(mTextCache) {
                text = mTextCache.get(textNo);
                if (text == null) {
                    try {
                        mTextCache.wait(1000);
                    } catch (final InterruptedException e) {
                        return null;
                    }
                }
            }
        }

        return text;
    }

    void setShowFullHeaders(final boolean showFullHeaders) {
        this.mShowFullHeaders = showFullHeaders;
    }
}
