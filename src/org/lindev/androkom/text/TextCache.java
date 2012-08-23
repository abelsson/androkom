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
        }
        if (text != null) {
            String username;
            int authorid = text.getAuthor();
            if (authorid > 0) {
                try {
                    nu.dll.lyskom.Conference confStat = mKom
                            .getConfStat(authorid);
                    username = confStat.getNameString();
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
            Log.d(TAG, "getTextFromServer textno:"+textNo);
            if(!mKom.isConnected()) {
                Log.d(TAG, " getTextFromServer not connected");
                return null;
            }
            Text text = null;
            try {
                Log.d(TAG, "TextFetcherTask:"+textNo);
                text = mKom.getTextbyNo(textNo);
            } catch (final RpcFailure e) {
                Log.d(TAG, "getTextFromServer: "+e);
                //e.printStackTrace();
                return null;
            }

            String username=mKom.getString(R.string.anonymous);
            int authorid = text.getAuthor();
            if (authorid > 0) {
                try {
                    nu.dll.lyskom.Conference confStat = mKom.getConfStat(authorid);
                    if(confStat!=null) {
                        username = confStat.getNameString();
                    } else {
                        Log.d(TAG, "getTextFromServer Failed to get username");
                    }
                } catch (final Exception e) {
                    username = mKom.getString(R.string.person) + authorid + mKom.getString(R.string.does_not_exist);
                }
            } else {
                Log.d(TAG, "Text "+textNo+" authorid:"+authorid);
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
            int[] items;
            if (mShowHeadersLevel > 0) {
                
                int marks = text.getMarks();
                if (marks>0) {
                    headersString.append(mKom.getString(R.string.marked_by)+marks+mKom.getString(R.string.marked_by_persons));
                }
                items = text.getRecipients();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom.getString(R.string.androkom_header_recipient));
                        try {
                            nu.dll.lyskom.Conference confStat = mKom.getConfStat(items[i]);
                            if(confStat!=null) {
                                headersString.append(confStat.getNameString());
                            } else {
                                Log.d(TAG, "Failed to append header");
                            }
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
                            nu.dll.lyskom.Conference confStat = mKom.getConfStat(items[i]);
                            if(confStat!=null) {
                                headersString.append(confStat.getNameString());
                            } else {
                                Log.d(TAG, "Failed to appen headers2");
                            }
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
                        conditionalAppend(headersString, mKom.getString(R.string.by_author), getAuthorName(items[i]));
                        headersString.append('\n');
                    }
                }
                items = text.getComments();
                if (items.length > 0) {
                    for (int i = 0; i < items.length; i++) {
                        headersString.append(mKom.getString(R.string.header_comment_in));
                        headersString.append(items[i]);
                        conditionalAppend(headersString,mKom.getString(R.string.by_author), getAuthorName(items[i]));
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
                List<AuxItem> aux_item = text.getAuxItems(AuxItem.tagMxAuthor);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.MxAuthor_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxCc);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.MxCC_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxDate);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.MxDate_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxFrom);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.MxFrom_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxInReplyTo);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.MxInReplyTo_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxReplyTo);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.MxReplyTo_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagMxTo);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.MxTo_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                aux_item = text.getAuxItems(AuxItem.tagCreationLocation);
                if (aux_item.size() > 0) {
                    for (int i = 0; i < aux_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.location_aux_label));
                        headersString.append(aux_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
            }
                if (mShowHeadersLevel > 1) {
                List<AuxItem> contentType_item = text.getAuxItems(AuxItem.tagContentType);
                if (contentType_item.size() > 0) {
                    for (int i = 0; i < contentType_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.contentType_aux_label));
                        headersString.append(contentType_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                List<AuxItem> creatorsw_item = text.getAuxItems(AuxItem.tagCreatingSoftware);
                if (creatorsw_item.size() > 0) {
                    for (int i = 0; i < creatorsw_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.creatorsw_aux_label));
                        headersString.append(creatorsw_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
                List<AuxItem> faq_item = text.getAuxItems(AuxItem.tagFaqText);
                if (faq_item.size() > 0) {
                    for (int i = 0; i < faq_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.faq_aux_label));
                        headersString.append(faq_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
            }
                List<AuxItem> fast_item = text.getAuxItems(AuxItem.tagFastReply);
                    if (fast_item.size() > 0) {
                        for (int i = 0; i < fast_item.size(); i++) {
                        headersString.append(mKom.getString(R.string.fast_aux_label));
                        headersString.append(fast_item.get(i).getDataString());
                        headersString.append('\n');
                    }
                }
            StringBuilder allHeadersString = new StringBuilder();
            allHeadersString.append("ContentType:"+text.getContentType());
            
            Log.d(TAG, "getTextFromServer returning");
            return new TextInfo(mKom.getBaseContext(), textNo, username, authorid, CreationTimeString, allHeadersString.toString(),
                    headersString.toString(),
                    SubjectString, BodyString, text.getBody(), mShowHeadersLevel);
        }

        private void conditionalAppend(StringBuilder headersString,
                String string, String authorName) {
            if(string == null) 
                return;
            if(authorName == null)
                return;
            if(string.length()<1)
                return;
            if(authorName.length()<1)
                return;
            headersString.append(string+authorName);
        }

        protected Void doInBackground(final Integer... args) {
            TextInfo text = null;
            mTextNo = args[0];
            Log.i(TAG, "TextFetcherTask fetching text " + mTextNo);
            if(!mKom.isConnected()) {
                Log.d(TAG, " TextFetcherTask not connected");
                return null;
            }
            try {
                text = getTextFromServer(mTextNo);
            } catch (Exception e) {
                Log.d(TAG, "TextFetcherTask.background caught error:"+e);
                //e.printStackTrace();
                text = null;
            }
            if (text == null) {
                text = TextInfo.createText(mKom.getBaseContext(), TextInfo.ERROR_FETCHING_TEXT);
                clearCacheStat();
            } else {
                mTextCache.put(mTextNo, text);
                synchronized(mTextCache) {
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
        int MaxWaits = 140;
        while (!currentThread.isInterrupted() && text == null && MaxWaits>0) {
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
                        return null;
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

    void setShowHeadersLevel(final int mShowHeadersLevel) {
        this.mShowHeadersLevel = mShowHeadersLevel;
    }
    
    void clearCacheStat() {
        synchronized (mTextCache) {
            mTextCache.clear();
            mTextCache.notifyAll();
        }
        synchronized (mSent) {
            mSent.clear();
        }
    }
}
