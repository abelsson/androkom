package org.lindev.androkom.text;

import java.io.IOException;
import java.util.List;

import nu.dll.lyskom.AuxItem;
import nu.dll.lyskom.Text;

import org.lindev.androkom.KomServer;
import org.lindev.androkom.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;

public class CreateTextTask extends AsyncTask<Void, Void, Object> {
    private final ProgressDialog mDialog;
    private final Context mContext;
    private final KomServer mKom;
    private final String mSubject;
    private final String mBody;
    private final int mInReplyTo;
    private final List<Recipient> mRecipients;
    private final CreateTextRunnable mRunnable;
    private boolean mUserIsMemberOfSomeConf;

    public interface CreateTextRunnable {
        public void run(final Text text);
    }

    public CreateTextTask(final Context context, final KomServer kom, final String subject, final String body,
            final int inReplyTo, final List<Recipient> recipients, final CreateTextRunnable runnable) {
        this.mDialog = new ProgressDialog(context);
        this.mContext = context;
        this.mKom = kom;
        this.mSubject = subject;
        this.mBody = body;
        this.mInReplyTo = inReplyTo;
        this.mRecipients = recipients;
        this.mRunnable = runnable;
        this.mUserIsMemberOfSomeConf = false;
    }

    @Override
    protected void onPreExecute() {
        mDialog.setCancelable(false);
        mDialog.setIndeterminate(true);
        mDialog.setMessage("Creating text ...");
        mDialog.show();
    }

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
                }
                if (mKom.getSession().isMemberOf(recipient.recipientId)) {
                    mUserIsMemberOfSomeConf = true;
                }
            }
            catch (final IOException e) {
                return "IOException";
            }
            catch (final IllegalArgumentException e) {
                // Conference is already recipient. Just ignore.
            }
        }

        final byte[] subjectBytes = mSubject.getBytes();
        final byte[] bodyBytes = mBody.getBytes();

        byte[] contents = new byte[subjectBytes.length + bodyBytes.length + 1];
        System.arraycopy(subjectBytes, 0, contents, 0, subjectBytes.length);
        System.arraycopy(bodyBytes, 0, contents, subjectBytes.length + 1, bodyBytes.length);
        contents[subjectBytes.length] = (byte) '\n';

        text.setContents(contents);
        text.getStat().setAuxItem(new AuxItem(AuxItem.tagContentType, "text/x-kom-basic;charset=utf-8")); 

        return text;
    }

    @Override
    protected void onPostExecute(final Object arg) {
        mDialog.dismiss();
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
            builder.setTitle("You aren't member of any recipient. Add yourself?");
            builder.setPositiveButton(mContext.getString(R.string.yes), listener);
            builder.setNegativeButton(mContext.getString(R.string.no), listener);
            builder.setNeutralButton(mContext.getString(R.string.cancel), listener);
            builder.create().show();
        }
    }
}
