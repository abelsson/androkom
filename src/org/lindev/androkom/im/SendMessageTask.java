package org.lindev.androkom.im;

import java.io.IOException;

import nu.dll.lyskom.RpcFailure;

import org.lindev.androkom.KomServer;
import org.lindev.androkom.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class SendMessageTask extends AsyncTask<Void, Void, String> {
    private final ProgressDialog dialog;
    private final Context mContext;
    private final KomServer mKom;
    private final int mConfNo;
    private final String mConfStr;
    private final String mMsg;
    private final Runnable mRunnable;

    public SendMessageTask(final Context context, final KomServer kom, final int confNo, final String confStr,
            final String msg, final Runnable runnable) {
        this.dialog = new ProgressDialog(context);
        this.mContext = context;
        this.mKom = kom;
        this.mConfNo = confNo;
        this.mConfStr = confStr;
        this.mMsg = msg;
        this.mRunnable = runnable;
    }

    @Override
    protected void onPreExecute() {
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.setMessage(mContext.getString(R.string.im_sending_message));
        dialog.show();
    }

    @Override
    protected String doInBackground(final Void... args) {
        try {
            mKom.sendMessage(mConfNo, mMsg, true);
        }
        catch (final RpcFailure e) {
            return mConfStr + mContext.getString(R.string.im_isnt_logged_in);
        }
        catch (final IOException e) {
            return mContext.getString(R.string.im_network_error);
        }
        return null;
    }

    @Override
    protected void onPostExecute(final String errorMsg) {
        dialog.dismiss();
        if (errorMsg != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(errorMsg);
            builder.setPositiveButton(mContext.getString(R.string.alert_dialog_ok), null);
            builder.create().show();
            return;
        }
        if (mRunnable != null) {
            mRunnable.run();
        }
    }
}
