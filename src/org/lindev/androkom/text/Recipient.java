package org.lindev.androkom.text;

import org.lindev.androkom.R;

import android.content.Context;

public class Recipient {
    public final int recipientId;
    public final String recipientStr;
    public final RecipientType type;
    private Context mContext;
    
    public enum RecipientType {
        RECP_TO,
        RECP_CC,
        RECP_BCC,
    }

    public Recipient(final Context context, final int recipientId, final String recipientStr, final RecipientType type) {
        this.mContext = context;
        this.recipientId = recipientId;
        this.recipientStr = recipientStr;
        this.type = type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        switch (type) {
        case RECP_TO:
            sb.append(mContext.getString(R.string.recipient_to));
            break;
        case RECP_CC:
            sb.append(mContext.getString(R.string.recipient_cc));
            break;
        case RECP_BCC:
            sb.append(mContext.getString(R.string.recipient_bcc));
            break;
        }
        sb.append(recipientStr);
        return sb.toString();
    }
}
