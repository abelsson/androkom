package org.lindev.androkom.text;

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

// Is this cach really needed? Lattekom got a cache too.

class TextCache {
    private static final String TAG = "Androkom TextCache";

    private final KomServer mKom;
    private final Set<Integer> mSent;
    private final Map<Integer, TextInfo> mTextCache;

    private int mShowHeadersLevel = 1;

    TextCache(final KomServer kom) {
        this.mKom = kom;
        this.mSent = new HashSet<Integer>();
        this.mTextCache = new ConcurrentHashMap<Integer, TextInfo>();
    }

    private String getConfName(int id) throws InterruptedException {
        return mKom.getConferenceName(id);
    }
    

    private String getAuthorName(int textNo) {
        Log.d(TAG, "getAuthorName:"+textNo);
        if (!mKom.isConnected()) {
            Log.d(TAG, " getAuthorName not connected");
            return null;
        }
        Text text = null;
        try {
            Log.d(TAG, "getAuthorName:" + textNo);
            text = mKom.getTextbyNo(textNo);
        } catch (final RpcFailure e) {
            Log.d(TAG, "getAuthorName: " + e);
            // e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (text != null) {
            String username;
            int authorid = text.getAuthor();
            if (authorid > 0) {
                try {
                    username = getConfName(authorid);
                } catch (final Exception e) {
                    username = mKom.getString(R.string.person) + authorid
                            + mKom.getString(R.string.does_not_exist);
                }
            } else {
                Log.d(TAG, "Text " + textNo + " authorid:" + authorid);
                username = mKom.getString(R.string.anonymous);
            }
            return username;
        } else {
            Log.d(TAG, "Could not get a authorname for textNo:" + textNo);
            return "";
        }
    }

    private class TextFetcherTask extends AsyncTask<Integer, Void, Void> {
        private int mTextNo;

        private TextInfo getTextFromServer(final int textNo) {
            Log.d(TAG, "TextFetcherTask getTextFromServer textno:" + textNo);
            if (!mKom.isConnected()) {
                Log.d(TAG, "TextFetcherTask getTextFromServer not connected");
                return null;
            }
            Text text = null;
            StringBuilder allHeadersString;
            String username = mKom.getString(R.string.anonymous);
            String authorname = mKom.getString(R.string.anonymous);
            int authorid;
            String CreationTimeString;
            StringBuilder headersString;
            String SubjectString = null;
            String BodyString = null;
            int[] emptyarr = {-1};
            
            try {
                Log.d(TAG, "TextFetcherTask get textno:" + textNo);
                text = mKom.getTextbyNo(textNo);
            } catch (final RpcFailure e) {
                Log.d(TAG, "TextFetcherTask getTextFromServer RpcFailure: " + e);
                // e.printStackTrace();
                if(e.getError()==14) {
                    Log.d(TAG, "TextFetcherTask Error 14 no such text or not allowed to read");
                    //Text does not exist or we are not allowed to read.
                    return new TextInfo(mKom.getBaseContext(), textNo, emptyarr, username,
                            0, "-", "FINNS EJ",
                            "FINNS EJ", "FINNS EJ", "FINNS EJ",
                            null, 0);                    
                }
                return new TextInfo(mKom.getBaseContext(), textNo, emptyarr, username,
                        0, "-", "FINNS EJ",
                        "FINNS EJ", "FINNS EJ", "RpcFailure"+e.getError(),
                        ("RpcFailure"+e.getError()).getBytes(), 0);                    
            } catch (InterruptedException e) {
                Log.d(TAG, "TextFetcherTask InterruptedException");
                e.printStackTrace();
            }

            if(text == null) {
                Log.d(TAG, "TextFetcherTask failed to get text: ");
                return new TextInfo(mKom.getBaseContext(), textNo, emptyarr, username,
                        0, "-", "FINNS EJ",
                        "FINNS EJ", "FINNS EJ", "FINNS EJ",
                        null, 0);                    
            } else {
                Log.d(TAG, "TextFetcherTask got Text: "+text.getNo());
            }

            authorid = text.getAuthor();
            if (authorid > 0) {
                try {
                    authorname = getConfName(authorid);
                } catch (final Exception e) {
                    authorname = mKom.getString(R.string.person) + authorid
                            + mKom.getString(R.string.does_not_exist);
                }
            } else {
                Log.d(TAG, "Text " + textNo + " authorid:" + authorid);
            }
            Date CreationTime = text.getCreationTime();
            SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm]");
            CreationTimeString = sdf.format(CreationTime);

            try {
                SubjectString = text.getSubjectString();
            } catch (UnsupportedEncodingException e) {
                Log.d(TAG, "UnsupportedEncodingException" + e);
                SubjectString = text.getSubjectString8();
            }

            try {
                BodyString = text.getBodyString();
            } catch (UnsupportedEncodingException e) {
                Log.d(TAG, "UnsupportedEncodingException" + e);
                BodyString = text.getBodyString8();
            }
            headersString = new StringBuilder();
            int[] items;
            if (mShowHeadersLevel > 0) {

                int marks = text.getMarks();
                if (marks > 0) {
                    headersString.append(mKom.getString(R.string.marked_by)
                            + marks
                            + mKom.getString(R.string.marked_by_persons));
                }
                items = text.getRecipients();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom
                                .getString(R.string.androkom_header_recipient));
                        try {
                            username = getConfName(items[i]);
                        } catch (Exception e) {
                            username = mKom.getString(R.string.person)
                                    + authorid
                                    + mKom.getString(R.string.does_not_exist);
                        }
                        headersString.append(username);
                        headersString.append('\n');
                    }
                }
                items = text.getCcRecipients();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom
                                .getString(R.string.header_cc_recipient));
                        try {
                            username = getConfName(items[i]);
                        } catch (Exception e) {
                            username = mKom.getString(R.string.person)
                                    + authorid
                                    + mKom.getString(R.string.does_not_exist);
                        }
                        headersString.append(username);
                        headersString.append('\n');
                    }
                }
                items = text.getCommented();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom
                                .getString(R.string.header_comment_to));
                        headersString.append(items[i]);
                        conditionalAppend(headersString,
                                mKom.getString(R.string.by_author),
                                getAuthorName(items[i]));
                        headersString.append('\n');
                    }
                }
                items = text.getComments();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom
                                .getString(R.string.header_comment_in));
                        headersString.append(items[i]);
                        conditionalAppend(headersString,
                                mKom.getString(R.string.by_author),
                                getAuthorName(items[i]));
                        headersString.append('\n');
                    }
                }
                items = text.getFootnotes();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom
                                .getString(R.string.header_footnote_in));
                        headersString.append(items[i]);
                        headersString.append('\n');
                    }
                }
                items = text.getFootnoted();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom
                                .getString(R.string.header_footnote_to));
                        headersString.append(items[i]);
                        headersString.append('\n');
                    }
                }
                List<AuxItem> aux_item = text.getAuxItems(AuxItem.tagMxAuthor);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.MxAuthor_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxCc);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.MxCC_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxDate);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.MxDate_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxFrom);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.MxFrom_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxInReplyTo);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.MxInReplyTo_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxReplyTo);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.MxReplyTo_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxTo);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.MxTo_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagCreationLocation);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.location_aux_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
            }
            if (mShowHeadersLevel > 1) {
                List<AuxItem> contentType_item = text
                        .getAuxItems(AuxItem.tagContentType);
                if (contentType_item.size() > 0) {
                    for (int i = 0; i < contentType_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.contentType_aux_label));
                        headersString.append(contentType_item.get(i)
                                .getDataString());
                        headersString.append('\n');
                    }
                }
                List<AuxItem> creatorsw_item = text
                        .getAuxItems(AuxItem.tagCreatingSoftware);
                if (creatorsw_item.size() > 0) {
                    for (int i = 0; i < creatorsw_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.creatorsw_aux_label));
                        headersString.append(creatorsw_item.get(i)
                                .getDataString());
                        headersString.append('\n');
                    }
                }
                List<AuxItem> faq_item = text.getAuxItems(AuxItem.tagFaqText);
                if (faq_item.size() > 0) {
                    for (int i = 0; i < faq_item.size(); i++) {
                        headersString.append(mKom
                                .getString(R.string.faq_aux_label));
                        headersString.append(faq_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
            }
            List<AuxItem> fast_item = text.getAuxItems(AuxItem.tagFastReply);
            if (fast_item.size() > 0) {
                for (int i = 0; i < fast_item.size(); i++) {
                    headersString.append(mKom
                            .getString(R.string.fast_aux_label));
                    headersString.append(fast_item.get(i).getDataString());
                    headersString.append('\n');
                }
            }
            
            allHeadersString = new StringBuilder();
            allHeadersString.append("ContentType:" + text.getContentType());
            
            // TODO: what is really current conf? Kind of philosophical question?

            Log.d(TAG, "getTextFromServer returning");
            return new TextInfo(mKom.getBaseContext(), textNo, text.getRecipients(), authorname,
                    authorid, CreationTimeString, allHeadersString.toString(),
                    headersString.toString(), SubjectString, BodyString,
                    text.getBody(), mShowHeadersLevel);
        }

        private void conditionalAppend(StringBuilder headersString,
                String string, String authorName) {
            if (string == null)
                return;
            if (authorName == null)
                return;
            if (string.length() < 1)
                return;
            if (authorName.length() < 1)
                return;
            headersString.append(string + authorName);
        }

        protected Void doInBackground(final Integer... args) {
            TextInfo text = null;
            mTextNo = args[0];
            Log.i(TAG, "TextFetcherTask fetching text " + mTextNo);
            if (!mKom.isConnected()) {
                Log.d(TAG, " TextFetcherTask not connected");
                return null;
            }
            try {
                text = getTextFromServer(mTextNo);
            } catch (Exception e) {
                Log.d(TAG, "TextFetcherTask.background caught error:" + e);
                e.printStackTrace();
                text = null;
            }
            if (text == null) {
                text = TextInfo.createText(mKom.getBaseContext(),
                        TextInfo.ERROR_FETCHING_TEXT);
                clearCacheStat();
            } else {
                mTextCache.put(mTextNo, text);
                synchronized (mTextCache) {
                    mTextCache.notifyAll();
                }
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
            Log.d(TAG, " doGetText mSent");
            needFetch = mSent.add(textNo);
        }
        Log.d(TAG, " doGetText mSent done");

        if (needFetch) {
            Log.d(TAG, " doGetText needFetch");
            //if(android.os.Build.VERSION.SDK_INT > 12) {
                //new TextFetcherTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, textNo);
            
            new TextFetcherTask().execute(textNo);
        }
        Log.d(TAG, " doGetText Done");
    }

    /**
    * Fetch a text (if needed), and return it
    *
    * @param textNo global text number to fetch
    */
    TextInfo getDText(final int textNo) {
        if(!mKom.isConnected()) {
            Log.d(TAG, " getText not connected");
            return null;
        }
        Log.d(TAG, "getText:"+textNo);
        TextInfo text = mTextCache.get(textNo);
        if (text == null) {
            Log.d(TAG, "getText doGetText:"+textNo);
            doGetText(textNo);
        } else {
            Log.d(TAG, "getText gotText, returning");
            return text;
        }

        final Thread currentThread = Thread.currentThread();
        int MaxWaits = 80;
        
        while (!currentThread.isInterrupted() && text == null && MaxWaits>0 && (textNo>0)) {
            synchronized(mTextCache) {
                Log.d(TAG, "getText waiting for mTextCache:"+textNo+" loop "+MaxWaits);
                if(!mKom.isConnected()) {
                    Log.d(TAG, " getText not connected in loop");
                    currentThread.interrupt();
                    MaxWaits=0;
                }
                text = mTextCache.get(textNo);
                if (text == null) {
                    try {
                        mTextCache.wait(1000);
                    } catch (final InterruptedException e) {
                        Log.d(TAG, " getText InterruptedException. Continue...");
                        //return null;
                    }
                }
            }
            MaxWaits--;
        }
        if(MaxWaits<1) {
            Log.d(TAG, "MaxWaits:"+MaxWaits);
        }
        if(text==null) {
            Log.d(TAG, "Could not find text");
            clearCacheStat();
        }
        Log.d(TAG, "getText returning");
        return text;
    }

    /**
    * Fetch a text for cache
    *
    * @param textNo global text number to fetch
    */
    void getCText(final int textNo) {
        if(!mKom.isConnected()) {
            Log.d(TAG, " getCText not connected");
            return;
        }
        Log.d(TAG, "getCText:"+textNo);
        TextInfo text = mTextCache.get(textNo);
        if (text == null) {
            Log.d(TAG, "getCText TEXT MISSING IN CACHE:"+textNo);
            Log.d(TAG, "cache size "+mTextCache.size());
            //Set<Entry<Integer, TextInfo>> allTexts = mTextCache.entrySet();
            //for (Entry<Integer, TextInfo>s : allTexts) {
            //    Integer textNum = s.getKey();
            //    Log.d(TAG, "CACHE CONTAINS " + textNum);
            //}
            Log.d(TAG, "getCText doGetText:"+textNo);
            doGetText(textNo);
        } else {
            Log.d(TAG, "getCText gotText, returning");
            return;
        }

        Log.d(TAG, "getCText returning");
    }


    void setShowHeadersLevel(final int mShowHeadersLevel) {
        this.mShowHeadersLevel = mShowHeadersLevel;
    }
    
    void clearCacheStat() {
        Log.d(TAG, "Clearing cache");
        synchronized (mTextCache) {
            mTextCache.clear();
            mTextCache.notifyAll();
        }
        synchronized (mSent) {
            mSent.clear();
        }
    }

/*    public void removeTextFromCache(int textNo) {
        synchronized (mTextCache) {
            mTextCache.remove(textNo);
        }
    }
    */
}
