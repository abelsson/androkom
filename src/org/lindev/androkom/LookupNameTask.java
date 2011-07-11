package org.lindev.androkom;

import nu.dll.lyskom.ConfInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

public class LookupNameTask extends AsyncTask<Void, Void, ConfInfo[]> {
    public static final int LOOKUP_CONFERENCES = 1;
    public static final int LOOKUP_USERS = 2;
    public static final int LOOKUP_BOTH = LOOKUP_USERS | LOOKUP_CONFERENCES;

    private final ProgressDialog mDialog;
    private final Activity mActivity;
    private final KomServer mKom;
    private final String mRecip;
    private final int mLookupFlags;
    private final RunOnSuccess mRunnable;

    public interface RunOnSuccess {
        public void run(final ConfInfo conf);
    }

    public LookupNameTask(final Activity activity, final KomServer kom, final String recipient,
            final int lookupFlags, final RunOnSuccess runnable) {
        this.mActivity = activity;
        this.mKom = kom;
        this.mRecip = recipient;
        this.mLookupFlags = lookupFlags;
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
        ConfInfo[] users = null;
        ConfInfo[] confs = null;
        if (0 != (mLookupFlags & LOOKUP_USERS)) {
            users = mKom.getUsers(mRecip);
        }
        if (0 != (mLookupFlags & LOOKUP_CONFERENCES)) {
            confs = mKom.getConferences(mRecip);
        }
        if (users == null || users.length == 0) {
            return confs;
        }
        if (confs == null || confs.length == 0) {
            return users;
        }
        final ConfInfo[] possibleRecip = new ConfInfo[users.length + confs.length];
        System.arraycopy(users, 0, possibleRecip, 0, users.length);
        System.arraycopy(confs, 0, possibleRecip, users.length, confs.length);
        return possibleRecip;
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
