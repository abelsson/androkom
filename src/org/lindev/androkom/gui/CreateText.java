package org.lindev.androkom.gui;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.List;

import nu.dll.lyskom.AuxItem;
import nu.dll.lyskom.Text;
import nu.dll.lyskom.TextStat;

import org.lindev.androkom.KomServer;
import org.lindev.androkom.text.Recipient;

import android.util.Log;

public class CreateText {
    private static final String TAG = "Androkom CreateTextTask";
    
    private final KomServer mKom;
    private final String mSubject;
    private final double mLat;
    private final double mLon;
    private final double mPrecision;
    private final String mBody;
    private final String imgFilename;
    private final byte[] imgData;
    private final int mInReplyTo;
    private final List<Recipient> mRecipients;
    private boolean mUserIsMemberOfSomeConf;

    public CreateText(final KomServer kom, final String subject, final String body,
            final double loc_lat, final double loc_lon, final double loc_precision,
            final int inReplyTo, final List<Recipient> recipients) {
        this.mKom = kom;
        this.mSubject = subject;
        this.mLat = loc_lat;
        this.mLon = loc_lon;
        this.mPrecision = loc_precision;
        this.mBody = body;
        this.imgFilename = null;
        this.imgData = null;
        this.mInReplyTo = inReplyTo;
        this.mRecipients = recipients;
        this.mUserIsMemberOfSomeConf = false;
    }

    public CreateText(final KomServer kom, final String subject, final String body,
            final int inReplyTo, final List<Recipient> recipients) {
        this.mKom = kom;
        this.mSubject = subject;
        this.mLat = 0;
        this.mLon = 0;
        this.mPrecision = 0;
        this.mBody = body;
        this.imgFilename = null;
        this.imgData = null;
        this.mInReplyTo = inReplyTo;
        this.mRecipients = recipients;
        this.mUserIsMemberOfSomeConf = false;
    }

    public CreateText(final KomServer kom, final String subject,
            final String imgFilename, final byte[] imgdata,
            final int inReplyTo, final List<Recipient> recipients) {
        this.mKom = kom;
        this.mSubject = subject;
        this.mLat = 0;
        this.mLon = 0;
        this.mPrecision = 0;
        this.mBody = "";
        this.imgFilename = imgFilename;
        this.imgData = imgdata;
        this.mInReplyTo = inReplyTo;
        this.mRecipients = recipients;
        this.mUserIsMemberOfSomeConf = false;
    }

    protected Text process(final boolean includeLocation) {
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
                        throw new IllegalArgumentException(recipient.recipientId + "CTT_already_recipient");
                    }
                    text.addMiscInfoEntry(TextStat.miscBccRecpt, recipient.recipientId);
                    break;
                }
                try {
                    if (mKom.isMemberOf(recipient.recipientId)) {
                        mUserIsMemberOfSomeConf = true;
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
            byte[] subjectBytes = null;
                
            try {
                // Returns a charset object for the named charset.
                Charset charset = Charset.forName("ISO-8859-1");

                // Constructs a new encoder for this charset.
                CharsetEncoder encoder = charset.newEncoder();
                encoder.replaceWith("?".getBytes("ISO-8859-1"));
                encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
                // Wrap the character sequence into a buffer.
                CharBuffer uCharBuffer = CharBuffer.wrap(mSubject);

                // Encode the remaining content of a single input character
                // buffer to a new byte buffer.
                // Converts to ISO-8859-1 bytes and stores them to the byte
                // buffer
                ByteBuffer bbuf = encoder.encode(uCharBuffer);
                subjectBytes = bbuf.array();
            } catch (CharacterCodingException e) {
                e.printStackTrace();
                System.out.println("Character Coding Error: " + e.getMessage());
                subjectBytes = mSubject.getBytes();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                System.out.println("UnsupportedEncodingException: " + e.getMessage());
                subjectBytes = mSubject.getBytes();
            }
                               
            byte[] contents = new byte[subjectBytes.length + imgData.length + 1];
            System.arraycopy(subjectBytes, 0, contents, 0, subjectBytes.length);
            System.arraycopy(imgData, 0, contents, subjectBytes.length + 1,
                    imgData.length);
            contents[subjectBytes.length] = (byte) '\n';

            text.setContents(contents);
            AuxItem auxItem = null;
            if(imgFilename == null) {
                auxItem = new AuxItem(AuxItem.tagContentType,
                        "image/jpeg; name=dummy.jpg");
            } else {
                auxItem = new AuxItem(AuxItem.tagContentType,
                        "image/jpeg; name="+imgFilename);
            }
            text.getStat().setAuxItem(auxItem);
        }

        if (includeLocation && (mPrecision > 0.0)) {
            String tagvalue = "" + mLat + " " + mLon + " " + mPrecision;
            Log.i(TAG, "aux pos=" + tagvalue);
            text.getStat().setAuxItem(
                    new AuxItem(AuxItem.tagCreationLocation, tagvalue));
        }

        /*try {
            if (text instanceof String) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(
                        mContext);
                builder.setTitle((String) arg);
                builder.setPositiveButton(
                        mContext.getString(R.string.alert_dialog_ok), null);
                builder.create().show();
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to show dialog " + e);
        }*/

        return text;
 /*       if (mUserIsMemberOfSomeConf) {
            if (mRunnable != null) {
                mRunnable.run(text);
            }
        } else {
            final OnClickListener listener = new OnClickListener() {
                public void onClick(final DialogInterface dialog,
                        final int which) {
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
            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    mContext);
            builder.setTitle(mContext.getString(R.string.CTT_not_member));
            builder.setPositiveButton(mContext.getString(R.string.yes),
                    listener);
            builder.setNegativeButton(mContext.getString(R.string.no), listener);
            builder.setNeutralButton(mContext.getString(R.string.cancel),
                    listener);
            try {
                builder.create().show();
            } catch (Exception e) {
                Log.d(TAG, "Failed to show dialog2 " + e);
            }
        }*/
    }
}
