package org.lindev.androkom.text;

public class Recipient {
    public final int recipientId;
    public final String recipientStr;
    public final RecipientType type;

    public enum RecipientType {
        RECP_TO,
        RECP_CC
    }

    public Recipient(final int recipientId, final String recipientStr, final RecipientType type) {
        this.recipientId = recipientId;
        this.recipientStr = recipientStr;
        this.type = type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        switch (type) {
        case RECP_TO:
            sb.append("To: ");
            break;
        case RECP_CC:
            sb.append("Cc: ");
            break;
        }
        sb.append(recipientStr);
        return sb.toString();
    }
}
