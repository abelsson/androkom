package org.lindev.androkom;

import nu.dll.lyskom.ConfInfo;
import nu.dll.lyskom.RpcFailure;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

public class LookupNameTask extends AsyncTask<Void, Void, ConfInfo[]> {
    private final ProgressDialog mDialog;
    private final Activity mActivity;
    private final KomServer mKom;
    private final String mRecip;
    private final LookupType mLookupType;
    private final RunOnSuccess mRunnable;

    public enum LookupType {
        LOOKUP_CONFERENCES,
        LOOKUP_USERS,
        LOOKUP_BOTH
    }

    public interface RunOnSuccess {
        public void run(final ConfInfo conf);
    }

    public LookupNameTask(final Activity activity, final KomServer kom, final String recipient,
            final LookupType lookupType, final RunOnSuccess runnable) {
        this.mActivity = activity;
        this.mKom = kom;
        this.mRecip = recipient;
        this.mLookupType = lookupType;
        this.mRunnable = runnable;
        this.mDialog = new ProgressDialog(mActivity);
    }

    @Override
    protected void onPreExecute() {
        mDialog.setCancelable(true);
        mDialog.setIndeterminate(true);
        mDialog.setMessage(mActivity.getString(R.string.im_resolving_recipient));
        mDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                LookupNameTask.this.cancel(true);
            }
        });
        mDialog.show();
    }

    @Override
    protected ConfInfo[] doInBackground(final Void... args) {
        try {
            switch (mLookupType) {
            case LOOKUP_USERS:
                return mKom.lookupName(mRecip, true, false);
            case LOOKUP_CONFERENCES:
                return mKom.lookupName(mRecip, false, true);
            case LOOKUP_BOTH:
                return mKom.lookupName(mRecip, true, true);
            default:
                return null;
            }
        }
        catch (final RpcFailure e) {
            return null;
        }
        catch (final NullPointerException e) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(final ConfInfo[] possibleRecip) {
        mDialog.dismiss();
        if (possibleRecip == null || possibleRecip.length == 0) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(mActivity.getString(R.string.im_no_such_recipient) + mRecip);
            builder.setPositiveButton(mActivity.getString(R.string.alert_dialog_ok), null);
            builder.create().show();
        }
        else if (possibleRecip.length == 1) {
            if (mRunnable != null) {
                mRunnable.run(possibleRecip[0]);
            }
        }
        else {
            final String[] items = new String[possibleRecip.length];
            for (int i = 0; i < items.length; ++i) {
                items[i] = possibleRecip[i].getNameString();
            }
            final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(mActivity.getString(R.string.pick_a_name));
            builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int item) {
                    dialog.dismiss();
                    if (mRunnable != null) {
                        mRunnable.run(possibleRecip[item]);
                    }
                }
            });
            builder.create().show();
        }
    }
}
