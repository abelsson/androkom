package org.lindev.androkom.text;

import java.io.IOException;
import java.util.List;

import nu.dll.lyskom.AuxItem;
import nu.dll.lyskom.Text;
import nu.dll.lyskom.TextStat;

import org.lindev.androkom.ConferencePrefs;
import org.lindev.androkom.KomServer;
import org.lindev.androkom.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;

public class CreateTextTask extends AsyncTask<Void, Void, Object> {
    private static final String TAG = "Androkom CreateTextTask";
    
    private final ProgressDialog mDialog;
    private final Context mContext;
    private final KomServer mKom;
    private final String mSubject;
    private final double mLat;
    private final double mLon;
    private final double mPrecision;
    private final String mBody;
    private final byte[] imgData;
    private final int mInReplyTo;
    private final List<Recipient> mRecipients;
    private final CreateTextRunnable mRunnable;
    private boolean mUserIsMemberOfSomeConf;

    public interface CreateTextRunnable {
        public void run(final Text text);
    }

    public CreateTextTask(final Context context, final KomServer kom, final String subject, final String body,
            final double loc_lat, final double loc_lon, final double loc_precision,
            final int inReplyTo, final List<Recipient> recipients, final CreateTextRunnable runnable) {
        this.mDialog = new ProgressDialog(context);
        this.mContext = context;
        this.mKom = kom;
        this.mSubject = subject;
        this.mLat = loc_lat;
        this.mLon = loc_lon;
        this.mPrecision = loc_precision;
        this.mBody = body;
        this.imgData = null;
        this.mInReplyTo = inReplyTo;
        this.mRecipients = recipients;
        this.mRunnable = runnable;
        this.mUserIsMemberOfSomeConf = false;
    }

    public CreateTextTask(final Context context, final KomServer kom, final String subject, final String body,
            final int inReplyTo, final List<Recipient> recipients, final CreateTextRunnable runnable) {
        this.mDialog = new ProgressDialog(context);
        this.mContext = context;
        this.mKom = kom;
        this.mSubject = subject;
        this.mLat = 0;
        this.mLon = 0;
        this.mPrecision = 0;
        this.mBody = body;
        this.imgData = null;
        this.mInReplyTo = inReplyTo;
        this.mRecipients = recipients;
        this.mRunnable = runnable;
        this.mUserIsMemberOfSomeConf = false;
    }

    public CreateTextTask(final Context context, final KomServer kom, final String subject, final byte[] imgdata,
            final int inReplyTo, final List<Recipient> recipients, final CreateTextRunnable runnable) {
        this.mDialog = new ProgressDialog(context);
        this.mContext = context;
        this.mKom = kom;
        this.mSubject = subject;
        this.mLat = 0;
        this.mLon = 0;
        this.mPrecision = 0;
        this.mBody = "";
        this.imgData = imgdata;
        this.mInReplyTo = inReplyTo;
        this.mRecipients = recipients;
        this.mRunnable = runnable;
        this.mUserIsMemberOfSomeConf = false;
    }

    @Override
    protected void onPreExecute() {
        mDialog.setCancelable(false);
        mDialog.setIndeterminate(true);
        mDialog.setMessage(mContext.getString(R.string.CTT_creating_text));
        mDialog.show();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Object doInBackground(final Void... args) {
        final Text text = new Text();

        if (mInReplyTo > 0) {
            text.addCommented(mInReplyTo);
        }

        for (final Recipient recipient : mRecipients) {
            try {
                switch (recipient.type) {
                case RECP_TO:
                    text.addRecipient(recipient.recipientId);
                    break;
                case RECP_CC:
                    text.addCcRecipient(recipient.recipientId);
                    break;
                case RECP_BCC:
                    if (text.getStat().hasRecipient(recipient.recipientId)) {
                        throw new IllegalArgumentException(recipient.recipientId + mContext.getString(R.string.CTT_already_recipient));
                    }
                    text.addMiscInfoEntry(TextStat.miscBccRecpt, recipient.recipientId);
                    break;
                }
                if (mKom.isMemberOf(recipient.recipientId)) {
                    mUserIsMemberOfSomeConf = true;
                }
            }
            catch (final IllegalArgumentException e) {
                // Conference is already recipient. Just ignore.
            }
        }

        if (imgData == null) {
            // Regular text
            final byte[] subjectBytes = mSubject.getBytes();
            final byte[] bodyBytes = mBody.getBytes();

            byte[] contents = new byte[subjectBytes.length + bodyBytes.length
                    + 1];
            System.arraycopy(subjectBytes, 0, contents, 0, subjectBytes.length);
            System.arraycopy(bodyBytes, 0, contents, subjectBytes.length + 1,
                    bodyBytes.length);
            contents[subjectBytes.length] = (byte) '\n';

            text.setContents(contents);
            text.getStat().setAuxItem(
                    new AuxItem(AuxItem.tagContentType,
                            "text/x-kom-basic;charset=utf-8"));
        } else {
            // image
            final byte[] subjectBytes = mSubject.getBytes();
            byte[] contents = new byte[subjectBytes.length + imgData.length + 1];
            System.arraycopy(subjectBytes, 0, contents, 0, subjectBytes.length);
            System.arraycopy(imgData, 0, contents, subjectBytes.length + 1,
                    imgData.length);
            contents[subjectBytes.length] = (byte) '\n';

            text.setContents(contents);
            text.getStat().setAuxItem(
                    new AuxItem(AuxItem.tagContentType,
                            "image/jpeg; name=dummy.jpg"));
        }

        if((ConferencePrefs.getIncludeLocation(mContext)) &&
                (mPrecision>0.0)) {
            String tagvalue = ""+mLat+" "+mLon+" "+mPrecision;
            Log.i(TAG, "aux pos="+tagvalue);
            text.getStat().setAuxItem(new AuxItem(AuxItem.tagCreationLocation, tagvalue)); 
        }
        return text;
    }

    @Override
    protected void onPostExecute(final Object arg) {
        try {
            mDialog.dismiss();
        } catch (Exception e) {
            Log.d(TAG, "Failed to dismiss dialog " + e);
        }
        if (arg instanceof String) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle((String) arg);
            builder.setPositiveButton(mContext.getString(R.string.alert_dialog_ok), null);
            builder.create().show();
        }
        final Text text = (Text) arg;
        if (mUserIsMemberOfSomeConf) {
            if (mRunnable != null) {
                mRunnable.run(text);
            }
        }
        else {
            final OnClickListener listener = new OnClickListener() {
                public void onClick(final DialogInterface dialog, final int which) {
                    if (which == AlertDialog.BUTTON_NEUTRAL) {
                        return;
                    }
                    if (which == AlertDialog.BUTTON_POSITIVE) {
                        text.addRecipient(mKom.getUserId());
                    }
                    if (mRunnable != null) {
                        mRunnable.run(text);
                    }
                }
            };
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(mContext.getString(R.string.CTT_not_member));
            builder.setPositiveButton(mContext.getString(R.string.yes), listener);
            builder.setNegativeButton(mContext.getString(R.string.no), listener);
            builder.setNeutralButton(mContext.getString(R.string.cancel), listener);
            builder.create().show();
        }
    }
}
