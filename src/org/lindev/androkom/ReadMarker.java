package org.lindev.androkom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import nu.dll.lyskom.Selection;
import nu.dll.lyskom.TextStat;
import android.util.Log;

public class ReadMarker {
    private static final String TAG = "Android";

    private final KomServer mKom;
    private final Set<Integer> mMarked;
    private final BlockingQueue<Integer> mToMark;

    public ReadMarker(final KomServer kom) {
        this.mKom = kom;
        this.mMarked = new HashSet<Integer>();
        this.mToMark = new LinkedBlockingQueue<Integer>();
        new MarkerThread().start();
    }

    private class MarkerThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                int textNo = -1;
                try {
                    textNo = mToMark.take();
                } catch (final InterruptedException e) {
                    // Someone called interrupt()
                    continue;
                }
                if (textNo > 0) {
                    markToServer(textNo);
                }
            }
        }
    }

    private void markToServer(final int textNo) {
        Log.i(TAG, "Mark as read: " + textNo);
        try {
            final TextStat stat = mKom.getSession().getTextStat(textNo, true);
            final int[] tags = { TextStat.miscRecpt, TextStat.miscCcRecpt, TextStat.miscBccRecpt };
            List<Selection> recipientSelections = new ArrayList<Selection>();
            for (final int tag : tags) {
                recipientSelections.addAll(stat.getMiscInfoSelections(tag));
            }
            for (final Selection selection : recipientSelections) {
                int rcpt = 0;
                for (int tag : tags) {
                    if (selection.contains(tag)) {
                        rcpt = selection.getIntValue(tag);
                    }
                }
                if (rcpt > 0) {
                    int local = selection.getIntValue(TextStat.miscLocNo);
                    Log.i(TAG, "markAsRead: global " + textNo + " rcpt " + rcpt + " local " + local);
                    mKom.getSession().doMarkAsRead(rcpt, new int[] { local });
                }
            }
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Mark as read finished: " + textNo);
    }

    public void mark(final int textNo) {
        final boolean needServerMark;
        synchronized (mMarked) {
            needServerMark = mMarked.add(textNo);
        }
        if (needServerMark) {
            mToMark.add(textNo);
        }
    }

    public boolean isLocalRead(final int textNo) {
        synchronized (mMarked) {
            return mMarked.contains(textNo);
        }
    }

	public void clearCaches() {
		synchronized (mMarked) {
			mMarked.clear();
		}
		synchronized (mToMark) {
			mToMark.clear();
		}
	}
}
