package org.lindev.androkom;

import java.io.IOException;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.dll.lyskom.AuxItem;
import nu.dll.lyskom.KomToken;
import nu.dll.lyskom.RpcFailure;
import nu.dll.lyskom.Text;

import org.lindev.androkom.KomServer.TextInfo;
import org.lindev.androkom.gui.IMConversationList;
import org.lindev.androkom.gui.MessageLog;
import org.lindev.androkom.gui.TextCreator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

/**
 * Show texts in a LysKOM conference.
 * 
 * @author henrik
 *
 */
public class Conference extends Activity implements OnTouchListener, ServiceConnection
{
    private static final String TAG = "Androkom Conference";

    private static final int SWIPE_MIN_DISTANCE = 150;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    /**
     * Set up activity. Will show individual LysKOM texts
     * with a click anywhere on the display moving to the 
     * next unread text. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate initialize");
        
        setContentView(R.layout.conference);

        if (ConferencePrefs.getUserButtons(getBaseContext())) {
            Button userb1 = (Button) findViewById(R.id.userbutton1);
            int butval = ConferencePrefs.getUserButton1val(getBaseContext());
            Resources res = getResources();
            String val = res.getStringArray(R.array.userbutton_entries_list)[butval-1];
            userb1.setText(val);
            userb1.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    doButtonClick(1);
                }
            });
            Button userb2 = (Button) findViewById(R.id.userbutton2);
            butval = ConferencePrefs.getUserButton2val(getBaseContext());
            val = res.getStringArray(R.array.userbutton_entries_list)[butval-1];
            userb2.setText(val);
            userb2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    doButtonClick(2);
                }
            });
            Button userb3 = (Button) findViewById(R.id.userbutton3);
            butval = ConferencePrefs.getUserButton3val(getBaseContext());
            val = res.getStringArray(R.array.userbutton_entries_list)[butval-1];
            userb3.setText(val);
            userb3.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    doButtonClick(3);
                }
            });
            Button userb4 = (Button) findViewById(R.id.userbutton4);
            butval = ConferencePrefs.getUserButton4val(getBaseContext());
            val = res.getStringArray(R.array.userbutton_entries_list)[butval-1];
            userb4.setText(val);
            userb4.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    doButtonClick(4);
                }
            });
        } else {
            LinearLayout userbuttonslayout = (LinearLayout) findViewById(R.id.userbuttons);
            userbuttonslayout.setVisibility(View.GONE);
        }
        
        mSwitcher = (ViewAnimator)findViewById(R.id.flipper_id);
        
        mSlideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        mSlideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        mSlideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        mSlideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);

        TextView text1h = (TextView)findViewById(R.id.flipper_headers1_id);
        text1h.setMovementMethod(LinkMovementMethod.getInstance());
        TextView text1 = (TextView)findViewById(R.id.flipper_text1_id);
        text1.setMovementMethod(LinkMovementMethod.getInstance());

        TextView text2h = (TextView)findViewById(R.id.flipper_headers2_id);
        text2h.setMovementMethod(LinkMovementMethod.getInstance());
        TextView text2 = (TextView)findViewById(R.id.flipper_text2_id);
        text2.setMovementMethod(LinkMovementMethod.getInstance());

        final Object data = getLastNonConfigurationInstance();
        final Intent intent = getIntent();
        
        int confNo = 0;
        int textNo = 0;

        if (data != null) {
            Log.d(TAG, "Got data!");
            mState = (State) data;
            if (mState.hasCurrent()) {
                try {
                    Spannable spannedText = mState.currentText.elementAt(
                            mState.currentTextIndex).getSpannableBody();
                    addLinks(spannedText, digits, null);
                    TextView tview = getCurrentTextView();
                    tview.setText(spannedText);
                } catch (Exception e) {
                    Log.d(TAG, "onCreate Exception:" + e);
                    //e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Broken error");
            }
        } else {
            mState = new State();
            mState.currentText = new Stack<TextInfo>();
            if(textNo > 0) {
                //mState.currentText.add(mKom.getKomText(textNo)); NULL!
                mState.textQueue = new ArrayBlockingQueue<Integer>(10);
                mState.textQueue.offer(textNo);
            }
            mState.currentTextIndex = -1;
            mState.ShowHeadersLevel = Integer.parseInt(ConferencePrefs
                    .getShowHeadersLevel(getBaseContext()));
            mState.conferenceNo = confNo;
            TextView tview = getCurrentTextView();
            if(tview != null) {
                tview.setText(formatText(getString(R.string.loading_text)+" "));
            }
        }

        if (intent != null) {
            final Bundle extras = intent.getExtras();
            if (extras != null) {
                Object confid = extras.get("conference-id");
                if (confid == null) {
                    confNo = 0;
                } else {
                    confNo = (Integer) confid;
                    mState.conferenceNo = confNo;
                }
                Log.i(TAG, "Got passed conference id: " + confNo);

                Object textNo_obj = extras.get("textNo");
                if (textNo_obj == null) {
                    textNo = 0;
                } else {
                    textNo = (Integer) textNo_obj;
                    if(textNo > 0) {
                        if (mState.textQueue == null) {
                            mState.textQueue = new ArrayBlockingQueue<Integer>(10);
                        }
                        mState.textQueue.offer(textNo);
                    }
                }
                Log.i(TAG, "Got passed text no: " + textNo);
            }
        }

        mGestureDetector = new GestureDetector(new MyGestureDetector());
       
        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate Got a bundle");
            restoreBundle(savedInstanceState);
        }
        getApp().doBindService(this);
    }
    

    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&mKom!=null) {
            mKom.setUser(re_userId, re_userPSW, re_server);
        } else {
            if(mKom==null) {
                Log.d(TAG, "mKom == null");
            }
            if(re_userId<1){
                Log.d(TAG, "no userId");
            }
            if(re_userPSW==null){
                Log.d(TAG, "null password");
            } else {
                if(re_userPSW.length()<1){
                    Log.d(TAG, "short password");
                }
            }
        }
    }
    
	@Override
	protected void onDestroy() {
        getApp().doUnbindService(this);
		super.onDestroy();
		Log.d(TAG, "Destroyed");
	}

    void doButtonClick(int buttonno) {
        Log.d(TAG, "Click button:" + buttonno);
        int butval;
        switch (buttonno) {
        case 1:
            butval = ConferencePrefs.getUserButton1val(getBaseContext());
            break;
        case 2:
            butval = ConferencePrefs.getUserButton2val(getBaseContext());
            break;
        case 3:
            butval = ConferencePrefs.getUserButton3val(getBaseContext());
            break;
        case 4:
            butval = ConferencePrefs.getUserButton4val(getBaseContext());
            break;
        default:
            butval = 1;
            Log.d(TAG, "Unknown button selection");
        }
        Log.d(TAG, "Doing action:" + butval);
        switch (butval) {
        case 1: // Svara
            Intent intent = new Intent(this, TextCreator.class);
            TextInfo currentText = mState.getCurrent();
            if ((currentText != null) && (currentText.getTextNo() > 0)) {
                intent.putExtra(TextCreator.INTENT_SUBJECT,
                        currentText.getSubject());
                intent.putExtra(TextCreator.INTENT_REPLY_TO,
                        currentText.getTextNo());
                startActivity(intent);
            } else {
                Log.d(TAG, "doButtonClick case 1: no current text");
                Toast.makeText(getApplicationContext(),
                        getString(R.string.no_text_loaded), Toast.LENGTH_SHORT)
                        .show();
            }
            break;
        case 2: // Avmarkera
            unmarkCurrentText();
            break;
        case 3: // Markera
            markCurrentText();
            break;
        case 4: // Vilka vänner
            seewhoison(2);
            break;
        case 5: // IM
            intent = new Intent(this, IMConversationList.class);
            startActivity(intent);
            break;
        case 6: // SPC
            moveToNextText(true);
            break;
        default:
            Log.d(TAG, "Unknown action, doing nothing.");
        }
    }

    
    /**
     * Fetch original text asynchronously, and show a progress spinner
     * while the user is waiting.
     */
    private class LoadOriginalPostTask extends AsyncTask<Integer, Void, TextInfo>
    {
        protected void onPreExecute() {
            Log.d(TAG, "LoadOriginalPostTask.onPreExecute");
            setProgressBarIndeterminateVisibility(true);
        }

        // worker thread (separate from UI thread)
        protected TextInfo doInBackground(final Integer... args) {
            Log.d(TAG, "LoadOriginalPostTask doInBackground BEGIN");
            TextInfo text = null;
            TextInfo startText = null;

            if (mState.hasCurrent()) {
                Log.d(TAG, "hasCurrent");
                text = mState.getCurrent();
                startText = text;
                Log.d(TAG, "hasCurrent textno"+text.getTextNo());
            } else
                return null;
            
            int maxChainLength = 100;
            TextInfo parentText = mKom.getParentToText(text.getTextNo());
            while((parentText != null) && (parentText.getTextNo()>0) && (maxChainLength>0)) {
                maxChainLength--;
                text = parentText;
                Log.i(TAG, "Trying to get parent text of" + text.getTextNo());
                parentText = mKom.getParentToText(text.getTextNo());
            }
            if (startText.getTextNo() != text.getTextNo()) {
                return text;
            }
            return null;
        }

        protected void onPostExecute(final TextInfo text) 
        {
            Log.d(TAG, "LoadOriginalPostTask.onPostExecute");
            setProgressBarIndeterminateVisibility(false);
            
            if(text != null) {
                new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, text.getTextNo(), 0);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.loadoriginalpost_failed), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Fetch new texts asynchronously, and show a progress spinner
     * while the user is waiting.
     * 
     * @author henrik
     */
    private class LoadMessageTask extends AsyncTask<Integer, Void, TextInfo>
    {
        protected void onPreExecute() {
            Log.d(TAG, "LoadMessageTask.onPreExecute");
            setProgressBarIndeterminateVisibility(true);
        }

        // worker thread (separate from UI thread)
        // args[0] = text type
        // args[1] = text#
        // args[2] = mark current text as read
        protected TextInfo doInBackground(final Integer... args) {
            Log.d(TAG, "LoadMessageTask doInBackground BEGIN");
            TextInfo text = null;

            if (mState.hasCurrent()) {
                Log.d(TAG, "hasCurrent");
                if((args[2]==0) && (ConferencePrefs.getMarkTextRead(getBaseContext()))) {
                    Log.d(TAG, "getMarkTextRead");
                    mKom.markTextAsRead(mState.getCurrent().getTextNo());
                }
            }

            switch (args[0]) {
            case MESSAGE_TYPE_PARENT_TO:
                Log.i(TAG, "Trying to get parent text of" + args[1]);
                text = mKom.getParentToText(args[1]);
                break;

            case MESSAGE_TYPE_TEXTNO: 
                Log.i(TAG, "Trying to get text " + args[1]);
                text = mKom.getKomText(args[1]);
                break;

            case MESSAGE_TYPE_NEXT:
                Log.i(TAG, "Trying to get next unread text");
                /* runOnUiThread(new Runnable() {
                    public void run() {
                        int textNo = mKom.getNextUnreadTextNo();
                        if (textNo > 0) {
                            mSwitcher.setText(formatText(getString(R.string.loading_text)+" "+textNo));
                        }
                    }
                });*/

                text = mKom.getNextUnreadText();
                break;

            default:
                Log.d(TAG, "LoadMessageTask unknown type:" + args[0]);
                text = null;
            }

            return text;
        }

        protected void onPostExecute(final TextInfo text) 
        {
            Log.d(TAG, "LoadMessageTask.onPostExecute");
            setProgressBarIndeterminateVisibility(false);
            //int curr = -1;
            //if (mState.hasCurrent()) {
            //    curr = mState.getCurrent().getTextNo();
            //}
            if (text != null && text.getTextNo() < 0) {
                Toast.makeText(getApplicationContext(), text.getBody(), Toast.LENGTH_SHORT).show();
                if(text.getTextNo() < -1) {
                    /* error fetching text, probably lost connection */
                    Log.d(TAG, "error fetching text, probably lost connection");
                    mKom.logout();
                    finish();
                } else {
                    Log.d(TAG, "error fetching text, recoverable error?");
                }
            }
            else if (text != null) {
                Log.d(TAG, "LoadMessageTask.onPostExecute got text");
                mState.currentText.push(text);
                mState.currentTextIndex = mState.currentText.size() - 1;
                //Log.i(TAG, stackAsString());

                //Log.d(TAG, "VHEADERS: "+text.getVisibleHeaders());
                //Log.d(TAG, "AHEADERS: "+text.getAllHeaders());
                //Log.d(TAG, "AUTHOR: "+text.getAuthor());
                //Log.d(TAG, "SUBJECT: "+text.getSubject());
                //Log.d(TAG, "BODY: "+text.getBody());
                if (text.getAllHeaders().contains("ContentType:image/")) {
                    Log.d(TAG, "LoadMessageTask.onPostExecute image text");
                    final Spannable spannedHeader = text.getSpannableHeaders();
                    addLinks(spannedHeader, digits, null);

                    TextView tview = getOtherHeadersView();
                    tview.setText(spannedHeader);

                    ImageView imgView = getOtherImgView();
                    byte[] bilden = text.getRawBody();
                    Bitmap bmImg = BitmapFactory.decodeByteArray(bilden, 0, bilden.length);
                    if(bmImg != null) {
                        imgView.setImageBitmap(bmImg);
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.image_decode_failed), Toast.LENGTH_LONG).show();
                    }
                    setOtherImgSwitch();
                    mSwitcher.showNext();
                } else {
                    Log.d(TAG, "LoadMessageTask.onPostExecute show text");
                    final Spannable spannedHeader = text.getSpannableHeaders();
                    addLinks(spannedHeader, digits, null);

                    TextView tview = getOtherHeadersView();
                    tview.setText(spannedHeader);

                    final Spannable spannedText = text.getSpannableBody();
                    addLinks(spannedText, digits, null);

                    tview = getOtherTextView();
                    tview.setText(spannedText);
                    resetOtherScroll();
                    setOtherTextSwitch();
                    mSwitcher.showNext();
                }
                setTitle(mKom.getConferenceName());                
            } else {
                Log.d(TAG, "error fetching text, probably lost connection");
                Log.d(TAG, "LoadMessageTask onPostExecute text=null");
                mKom.logout();
                finish();
            }
//            if (curr > 0) {
//                if (ConferencePrefs.getMarkTextRead(getBaseContext()))
//                {
//                    mKom.markTextAsRead(curr);
//                }
//            }
        }
    }

    /**
     * Class for handling internal text number links. 
     * 
     * @author henrik
     *
     */
    class KomInternalURLSpan extends ClickableSpan {
        String mLinkText;
        
        public KomInternalURLSpan(String mLinkText) {
            this.mLinkText = mLinkText;
            Log.d(TAG, "KomInternalURLSpan link for "+mLinkText);
        }

        @Override
        public void onClick(View widget) {
            int textNo;
            Log.d(TAG, "KomInternalURLSpan onClick stored text:"+mLinkText);
            try {
                textNo = Integer.valueOf(mLinkText);
            } catch (NumberFormatException e)
            {
                Log.i(TAG, "KomInternalURLSpan onClick Illegal textNo: " + mLinkText);
                return;
            }
            Log.i(TAG, "KomInternalURLSpan onClick move to textNo: " + textNo);
            moveToText(textNo);
        }
    }  
    
 
    /**
     *  Applies a regex to a Spannable turning the matches into
     *  links. To be used with the class above.
     */ 
    public final boolean addLinks(Spannable s, Pattern p, String scheme) {
        // This could possibly be a new Conference activity, and these links are for a specifik instance. So we
        // remove all old links first.
        for (KomInternalURLSpan span : s.getSpans(0, s.length(), KomInternalURLSpan.class)) {
            s.removeSpan(span);
        }

        boolean hasMatches = false;
        Matcher m = p.matcher(s);

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            String url = m.group(0);

            KomInternalURLSpan span = this.new KomInternalURLSpan(url);
            s.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            hasMatches = true;
        }

        return hasMatches;
    }
    
    /**
     * A gesture detector that is used to navigate within and between texts.
     * 
     * @author henrik
     *
     */
    class MyGestureDetector extends SimpleOnGestureListener 
    {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(TAG, "onSingleTapUp");
            return false;
        }
        
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Context context = getBaseContext();
            activateUser();
            Log.d(TAG, "onSingleTapConfirmed");
            if (!ConferencePrefs.getEnableTapToNext(context)) {
                Log.d(TAG, "Tap disabled");
                return false;
            }
            Display display = getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            // TODO: Eh. Figure out how calculate our height properly (excluding optional buttons).
            int myLimit = mSwitcher.getBaseline() + mSwitcher.getBottom();
            //Log.d(TAG, "onSingleTapUp myLimit:"+myLimit);
            //Log.d(TAG, "onSingleTapUp width:"+width);
            //Log.d(TAG, "onSingleTapUp e.getRawY():"+e.getRawY());
            //Log.d(TAG, "onSingleTapUp e.getRawX():"+e.getRawX());
            //Log.d(TAG, "onSingleTapUp e.getDownTime():"+e.getDownTime());

            if (e.getRawY() < myLimit) {
                if (e.getRawX() > 0.6 * width) {
                    if (ConferencePrefs.getVibrateForTap(context)) {
                        Vibrator vibrator = (Vibrator) context
                                .getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(ConferencePrefs.getVibrateTimeTap(context));
                    }
                    moveToNextText(true);
                    return true;
                }
                if (e.getRawX() < 0.4 * width) {
                    if (ConferencePrefs.getVibrateForTap(context)) {
                        Vibrator vibrator = (Vibrator) context
                                .getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(ConferencePrefs.getVibrateTimeTap(context));
                    }
                    moveToPrevText();
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            activateUser();
            Log.d(TAG, "onFling");
            try {
                // Horizontal swipes
                if (Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH) {
                    // right to left swipe
                    if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        moveToNextText(true);
                        return true;
                    } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        moveToPrevText();
                        return true;
                    }
                }

                // Vertical swipes
                if (Math.abs(e1.getX() - e2.getX()) <= SWIPE_MAX_OFF_PATH) {
                    // top to bottom swipe
                    if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        moveToParentText();
                        return true;
                    }
                }

            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }

    private void moveToPrevText() {
        Log.i(TAG, "moving to prev text, cur: " + (mState.currentTextIndex - 1)
                + "/" + mState.currentText.size());

        if (mState.currentTextIndex > 0) {
            mState.currentTextIndex--;
            Log.i(TAG, stackAsString());
            mSwitcher.setInAnimation(mSlideRightIn);
            mSwitcher.setOutAnimation(mSlideRightOut);

            TextView tview = getOtherHeadersView();
            TextInfo text = mState.currentText
                    .elementAt(mState.currentTextIndex);
            if (text.getAllHeaders().contains("ContentType:image/")) {
                Log.d(TAG, "LoadMessageTask.onPostExecute image text");
                final Spannable spannedHeader = text.getSpannableHeaders();
                addLinks(spannedHeader, digits, null);

                tview.setText(spannedHeader);

                ImageView imgView = getOtherImgView();
                byte[] bilden = text.getRawBody();
                Bitmap bmImg = BitmapFactory.decodeByteArray(bilden, 0,
                        bilden.length);
                if (bmImg != null) {
                    imgView.setImageBitmap(bmImg);
                } else {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.image_decode_failed),
                            Toast.LENGTH_LONG).show();
                }
                setOtherImgSwitch();
            } else {
                Spannable spannableHeaders = mState.currentText.elementAt(
                        mState.currentTextIndex).getSpannableHeaders();
                tview.setText(spannableHeaders);

                tview = getOtherTextView();
                tview.setText(mState.currentText.elementAt(
                        mState.currentTextIndex).getSpannableBody());
                setOtherTextSwitch();
            }
            mSwitcher.showNext();
        }
    }

    public void activateUser() {
        new ActivateUserTask().execute();
    }

    /**
     * No need to wait for activate
     * 
     */
    private class ActivateUserTask extends AsyncTask<KomToken, Void, Void> {
        protected void onPreExecute() {
            Log.d(TAG, "ActivateUserTask.onPreExecute");
        }

        // worker thread (separate from UI thread)
        protected Void doInBackground(final KomToken... args) {
            try {
                mKom.activateUser();
            } catch (Exception e1) {
                Log.i(TAG, "Failed to activate user exception:"+e1);
                //e1.printStackTrace();
                mKom.logout();
            }
            return null;
        }
    }

    private void moveToNextText(boolean markTextAsRead) {
        Log.i(TAG, "moving to next text cur:" + mState.currentTextIndex + "/"
                + mState.currentText.size());
        int markTextAsReadint = markTextAsRead ? 0 : 1;
        mSwitcher.setInAnimation(mSlideLeftIn);
        mSwitcher.setOutAnimation(mSlideLeftOut);

        if ((mState.currentTextIndex + 1) >= mState.currentText.size()) {
            // At end of list. load new text from server
            Log.i(TAG, "fetching new text");
            new LoadMessageTask().execute(MESSAGE_TYPE_NEXT, 0,
                    markTextAsReadint);
        } else {
            Log.i(TAG, "Moving in old fetched text");
            // Display old text, already fetched.
            mState.currentTextIndex++;
            Log.i(TAG, stackAsString());

            TextView tview = getOtherHeadersView();
            TextInfo text = mState.currentText
                    .elementAt(mState.currentTextIndex);
            if (text.getAllHeaders().contains("ContentType:image/")) {
                Log.d(TAG, "LoadMessageTask.onPostExecute image text");
                final Spannable spannedHeader = text.getSpannableHeaders();
                addLinks(spannedHeader, digits, null);

                tview.setText(spannedHeader);

                ImageView imgView = getOtherImgView();
                byte[] bilden = text.getRawBody();
                Bitmap bmImg = BitmapFactory.decodeByteArray(bilden, 0,
                        bilden.length);
                if (bmImg != null) {
                    imgView.setImageBitmap(bmImg);
                } else {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.image_decode_failed),
                            Toast.LENGTH_LONG).show();
                }
                setOtherImgSwitch();
            } else {
                tview.setText(mState.currentText.elementAt(
                        mState.currentTextIndex).getSpannableHeaders());

                tview = getOtherTextView();
                tview.setText(mState.currentText.elementAt(
                        mState.currentTextIndex).getSpannableBody());
                setOtherTextSwitch();
            }
            mSwitcher.showNext();
        }
    }

    private void moveToText(final int textNo) {
        Log.i(TAG, "fetching text " + textNo);
        mSwitcher.setInAnimation(mSlideLeftIn);
        mSwitcher.setOutAnimation(mSlideLeftOut);
        new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, textNo, 0);
    }

    private void moveToParentText() {
        int current = mState.getCurrent().getTextNo();
        Log.i(TAG, "fetching parent to text " + current);

        mSwitcher.setInAnimation(mSlideLeftIn);
        mSwitcher.setOutAnimation(mSlideLeftOut);
        new LoadMessageTask().execute(MESSAGE_TYPE_PARENT_TO, current, 0);
    }

	private void scrollPageUp() {
		TextView t = getCurrentTextView();

		if ( t!= null) {
			int visibleHeight = t.getHeight()
			-t.getTotalPaddingBottom()
			-t.getTotalPaddingTop()
			-t.getCompoundPaddingBottom()
			-t.getCompoundPaddingTop()
			-2*t.getLineHeight();
			int newY = t.getScrollY() - visibleHeight;
			if(newY < 0) {
				newY = 0;
			}
			t.scrollTo(0, newY);
		}
	}
	
	private void scrollPageDown() {
		TextView t = getCurrentTextView();

		if (t != null) {
			int visibleHeight = t.getHeight()
			-t.getTotalPaddingBottom()
			-t.getTotalPaddingTop()
			-t.getCompoundPaddingBottom()
			-t.getCompoundPaddingTop()
			-2*t.getLineHeight();
			int newY = t.getScrollY() + visibleHeight;
			if(newY > t.getLineCount()*t.getLineHeight()-visibleHeight) {
				newY = t.getLineCount()*t.getLineHeight()-visibleHeight;
			}
			t.scrollTo(0, newY);
		}
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
        activateUser();

		switch (keyCode) {
		case android.view.KeyEvent.KEYCODE_B:
			moveToPrevText();
			return true;
		case android.view.KeyEvent.KEYCODE_P:
			moveToParentText();
			return true;
		case android.view.KeyEvent.KEYCODE_F:
		case android.view.KeyEvent.KEYCODE_SPACE:
			moveToNextText(true);
			return true;
		case android.view.KeyEvent.KEYCODE_U:
		case 115: //PgUp on Toshiba AC100-10D
			scrollPageUp();
			return true;
		case android.view.KeyEvent.KEYCODE_D:
		case 116: //PgDn on Toshiba AC100-10D
			scrollPageDown();
			return true;
		case android.view.KeyEvent.KEYCODE_X:
			// skicka med mState.getCurrent().getTextNo() ?
			XDialog dialog = new XDialog(this);
			dialog.show();
		case android.view.KeyEvent.KEYCODE_Q:
		case 4: // back in emulator
			finish();
		default:
			Log.d(TAG, "onKeyup unknown key:" + keyCode + " " + event);
		}
		return false;
	}

    /**
     * This one is called when we, ourselves, have been touched.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        if (mGestureDetector.onTouchEvent(event))
        	return true;
        
        return super.dispatchTouchEvent(event);          
    }

    /**
     * This one is called when our child TextView has been touched.
     */
    public boolean onTouch(View v, MotionEvent event) 
    {
        if (mGestureDetector.onTouchEvent(event))
            return true;
        
        return false;
    }

    /**
     * When we're being temporarily destroyed, due to, for example 
     * the user rotating the screen, save our state so we can restore
     * it again.
     */
    @Override
    public Object onRetainNonConfigurationInstance() 
    {
        Log.d(TAG, "onRetainNonConfigurationInstance");
        return mState;
    }

    /**
     * Called when user has selected a menu item from the 
     * menu button popup. 
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	TextView t1;
    	float newtextsize;
    	
    	Log.d(TAG, "onOptionsItemSelected");
        activateUser();
    	
        // Handle item selection
        switch (item.getItemId()) {

        /*
         * A reply to the current text was requested, so show a 
         * CreateText activity. 
         */
        case R.id.reply:
            Intent intent = new Intent(this, TextCreator.class);
            intent.putExtra(TextCreator.INTENT_SUBJECT, mState.getCurrent().getSubject());
            intent.putExtra(TextCreator.INTENT_REPLY_TO, mState.getCurrent().getTextNo());
            startActivity(intent);
            return true;

		case R.id.menu_settings_id :
			Log.d(TAG, "Starting menu");
			startActivity(new Intent(this, ConferencePrefs.class));
			return true;

		case R.id.menu_biggerfontsize_id :
			Log.d(TAG, "Change fontsize+");
			t1 = getCurrentTextView();
			newtextsize = (float) (t1.getTextSize()*1.1);
			t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newtextsize);
			t1.invalidate();

			t1 = getOtherTextView();
			t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newtextsize);
			
			storeFontSize(newtextsize);
			return true;

		case R.id.menu_smallerfontsize_id :
            Log.d(TAG, "Change fontsize-");
            t1 = getCurrentTextView();
            newtextsize = (float) (t1.getTextSize() * 0.9);
            t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newtextsize);
            t1.invalidate();

            t1 = getOtherTextView();
            t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newtextsize);

            storeFontSize(newtextsize);
            return true;

		case R.id.menu_monospaced_id :
            Log.d(TAG, "Change to monospaced");
            t1 = getCurrentTextView();
            t1.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
            t1.invalidate();
            return true;

        case R.id.menu_rot13_id :
            Log.d(TAG, "Toggle rot13");
            t1 = getCurrentTextView();
            CharSequence content = t1.getText();
            CharSequence rot13_content = Rot13.cipher(content.toString());
            t1.setText(rot13_content);
            t1.invalidate();
            return true;

		case R.id.menu_createnewtext_id:
            intent = new Intent(this, TextCreator.class);
            startActivity(intent);
			return true;

		case R.id.menu_createnewmail_id:
            intent = new Intent(this, TextCreator.class);
            intent.putExtra(TextCreator.INTENT_IS_MAIL, true);
            startActivity(intent);
			return true;

		case R.id.menu_messaging_id:
            intent = new Intent(this, IMConversationList.class);
            startActivity(intent);
			return true;

		case R.id.menu_marktext_id:
            markCurrentText();
			return true;

		case R.id.menu_unmarktext_id:
            unmarkCurrentText();
			return true;

		case R.id.menu_seetextagain_id:
			seetextagain();
			return true;

		case R.id.menu_seepresentationconf_id:
			seePresentationConf();
			return true;

        case R.id.menu_seepresentationuser_id:
            seePresentationUser();
            return true;

		case R.id.menu_seewhoison_id:
			seewhoison(1);
			return true;

        case R.id.menu_seeoriginalpost_id:
            seeoriginalpost();
            return true;
            
        case R.id.menu_leaveconference_id:
            leaveconference();
            return true;

        case R.id.menu_message_log_id:
            intent = new Intent(this, MessageLog.class);
            startActivity(intent);
            return true;
            
        case R.id.menu_add_recipient_id:
            addRecipient();
            return true;

        case R.id.menu_sub_recipient_id:
            subRecipient();
            return true;

        case R.id.menu_add_comment_id:
            addComment();
            return true;

        case R.id.menu_sub_comment_id:
            subComment();
            return true;

        case R.id.menu_gilla_id:
            gilla_current_text();
            return true;

        case R.id.menu_next_no_readmark_id:
            moveToNextText(false);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void gilla_current_text() {
        int CurrentText = mState.getCurrent().getTextNo();        
        mKom.addAuxItem(CurrentText, AuxItem.tagFastReply, "Gilla");
    }


    protected void storeFontSize(float size) {
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("conference_body_textsize", size);
        editor.commit();
    }

    protected void setCurrentFontSize() {
        SharedPreferences prefs =  getPreferences(MODE_PRIVATE);
    	
		TextView t1 = getCurrentTextView();
		t1.setTextSize(prefs.getFloat("conference_body_textsize", 12));
    }

    protected void markCurrentText() {
    	int CurrentTextNo = mState.getCurrent().getTextNo();
    	mKom.markText(CurrentTextNo);
    }
    
    protected void unmarkCurrentText() {
        TextInfo currentText = mState.getCurrent();
        if (currentText != null) {
            int CurrentTextNo = currentText.getTextNo();
            mKom.unmarkText(CurrentTextNo);
        } else {
            Log.d(TAG, "Failed to unmark due to null");
        }
    }

    protected void seetextagain() {
    	showDialog(DIALOG_NUMBER_ENTRY);
    }

    protected void seeoriginalpost() {
        new LoadOriginalPostTask().execute();
    }

    protected void seePresentationConf() {
        int textNo = mKom.getConferencePres();
        if (textNo > 0) {
            Log.i(TAG, "fetching text " + textNo);
            new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, textNo, 0);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.no_presentation_error),
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void seePresentationUser() {
        final TextInfo currentText = mState.getCurrent();
        int userNum = currentText.getAuthorNo();
        int textNo = mKom.getUserPres(userNum);
        if (textNo > 0) {
            Log.i(TAG, "fetching text " + textNo);
            new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, textNo, 0);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.no_presentation_error),
                    Toast.LENGTH_SHORT).show();
        }
    }

	protected void seewhoison(int type) {
		Intent intent = new Intent(this, WhoIsOn.class);
		intent.putExtra("who_type", type);
		startActivity(intent);
	}

    protected void leaveconference() {
        mKom.leaveConference(mState.conferenceNo);
        finish();
    }

    public static int[] concatAll(int[] first, int[] second) {
        int totalLength = first.length + second.length;
        int[] result = new int[totalLength];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static int[] concatAll(int[] first, int[] second, int[] third) {
        int totalLength = first.length + second.length + third.length;
        int[] result = new int[totalLength];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        System.arraycopy(third, 0, result, first.length + second.length,
                third.length);
        return result;
    }

    /* show user the recipients of the current text and allow user to select
     * which recipient to remove.
     */
    protected void subRecipient() {
        try {
            final int currentTextNo = mState.getCurrent().getTextNo();
            Text text = mKom.getTextbyNo(currentTextNo);
            int[] recipts = text.getRecipients();
            int[] ccrecipts = text.getCcRecipients();
            int[] bccrecipts = text.getBccRecipients();
            final int[] allrecipts = concatAll(recipts, ccrecipts, bccrecipts);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(Conference.this);
            builder.setTitle(getString(R.string.pick_a_name));
            final String[] vals = new String[allrecipts.length];
            for (int i = 0; i < allrecipts.length; i++)
                vals[i] = mKom.fetchUsername(allrecipts[i]);
            builder.setSingleChoiceItems(vals, -1,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.cancel();
                            int selectedUser = allrecipts[item];
                            Log.d(TAG, "Selected user:" + selectedUser + ":"
                                    + vals[item]);
                            doSubRecipient(selectedUser);
                        }

                        private void doSubRecipient(int selectedUser) {
                            String result = mKom.subRecipient(currentTextNo, selectedUser);
                            if (result != "") {
                                Toast.makeText(getApplicationContext(),
                                        result, Toast.LENGTH_SHORT)
                                        .show();                                
                            }
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } catch (RpcFailure e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "subRecipient RPcFailure:"+e);
            //e.printStackTrace();
        }
    }


    /* start new intent to let user add new recipient to existing text
     */
    protected void addRecipient() {
        final int currentTextNo = mState.getCurrent().getTextNo();
        
        Intent intent = new Intent(this, AddNewRecipientToText.class);
        intent.putExtra(AddNewRecipientToText.INTENT_TEXTNO, currentTextNo);
        startActivity(intent);
    }
    
    protected Dialog onCreateDialog(int id) {
        // Assume id = DIALOG_NUMBER_ENTRY for now 
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);

    	alert.setTitle(getString(R.string.seetextagain_label));
    	alert.setMessage(getString(R.string.alert_dialog_text_entry));

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    	alert.setView(input);

    	alert.setPositiveButton(getString(R.string.alert_dialog_ok), new DialogInterface.OnClickListener() {
    	public void onClick(DialogInterface dialog, int whichButton) {
    	  String textvalue = input.getText().toString();
  		  Log.i(TAG, "trying to parse " + textvalue);
		  int textNo = Integer.parseInt(textvalue);
		  Log.i(TAG, "fetching text " + textNo);

		  mSwitcher.setInAnimation(mSlideLeftIn);
		  mSwitcher.setOutAnimation(mSlideLeftOut);
		  new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, textNo, 0);
    	  }
    	});

    	alert.setNegativeButton(getString(R.string.alert_dialog_cancel), new DialogInterface.OnClickListener() {
    	  public void onClick(DialogInterface dialog, int whichButton) {
    	    // Canceled.
    	  }
    	});

    	return alert.create();
    }

    /* start new intent to let user add new comment to existing text
     */
    protected void addComment() {
        final int currentTextNo = mState.getCurrent().getTextNo();
        
        Intent intent = new Intent(this, AddNewCommentToText.class);
        intent.putExtra(AddNewCommentToText.INTENT_TEXTNO, currentTextNo);
        startActivity(intent);
    }
    
    /* show user the comments of the current text and allow user to select
     * which comment to remove.
     */
    protected void subComment() {
        try {
            final int currentTextNo = mState.getCurrent().getTextNo();
            Text text = mKom.getTextbyNo(currentTextNo);
            final int[] commented = text.getCommented();
            final int[] comments = text.getComments();
            final int[] allComments = concatAll(commented, comments);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(Conference.this);
            builder.setTitle(getString(R.string.pick_a_name));
            final String[] vals = new String[allComments.length];
            for (int i=0;i<allComments.length;i++) {
                vals[i]=Integer.toString(allComments[i]);
            }
            builder.setSingleChoiceItems(vals, -1,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.cancel();
                            int selectedComment = allComments[item];
                            Log.d(TAG, "Selected comment:" + selectedComment);
                            String result="no result";
                            if(item<commented.length) {
                                result = mKom.subComment(currentTextNo, selectedComment);
                            } else {
                                result = mKom.subComment(selectedComment, currentTextNo);                                
                            }
                            if (result != "") {
                                Toast.makeText(getApplicationContext(),
                                        result, Toast.LENGTH_SHORT)
                                        .show();                                
                            }
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } catch (RpcFailure e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "subComment RPcFailure:"+e);
            //e.printStackTrace();
        }
    }


    /**
     * The menu key has been pressed, instantiate the requested
     * menu.
     */
    @Override 
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.conference, menu);
        return true;
    }

    private static final Pattern digits = Pattern.compile("\\d{5,}");
    public static Spannable formatHeaders(Context context, TextInfo text,
            int showHeadersLevel) {
        StringBuilder body = new StringBuilder();

        if (text.getTextNo() > 0) {   
            body.append(text.getTextNo());          
            body.append(" <b>");
            body.append(text.getAuthor());
            body.append("</b> ");
            body.append(text.getDate());
            body.append("<br/>");

            if (showHeadersLevel>0) {
                String[] headerlines = text.getVisibleHeaders().split("\n");
                for (String line : headerlines) {
                    body.append(line);
                    body.append("<br/>");
                }
            }
            
            body.append("<b>"+context.getString(R.string.subject));
            body.append(text.getSubject());
            body.append("</b>");
        }
        
        Spannable spannedText = (Spannable) Html.fromHtml(body.toString());
        Linkify.addLinks(spannedText, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        
        return spannedText;
    }
    
    public static Spannable formatText(Context context, TextInfo text)
    {
        StringBuilder body = new StringBuilder();

        // Some simple heuristics to reflow and htmlize KOM texts.
        // TODO: Deal with quoted blocks prefixed with '>'.

        String rawbody = text.getBody();
        String cookedbody;
        
        boolean force646decode = ConferencePrefs.getforce646decode(context);
        if (force646decode) {
            cookedbody = rawbody.replace("$", "¤")
                    .replace("@", "É")
                    .replace("[", "Ä")
                    .replace("\\", "Ö")
                    .replace("[", "Ä")
                    .replace("]", "Å")
                    .replace("^", "Ü")
                    .replace("`", "é")
                    .replace("{", "ä")
                    .replace("|", "ö")
                    .replace("}", "å")
                    .replace("~", "ü");
        } else {
            cookedbody = rawbody;
        }
        FillMessage fm = new FillMessage(cookedbody);
        body.append(fm.run());

        //Log.i(TAG, body.toString());

        Spannable spannedText = (Spannable) Html.fromHtml(body.toString());
        Linkify.addLinks(spannedText, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        
        return spannedText;
    }
    
    public Spannable formatText(String text)
    {
        SpannableStringBuilder spannedText = (SpannableStringBuilder)Html.fromHtml(text);       
        addLinks(spannedText, Pattern.compile("\\d{5,}"), null);
        Linkify.addLinks(spannedText, Linkify.ALL);
        
        return spannedText;
    }

    /**
     * Assume switcher got two childs and set the other, currently not
     * visible, child.
     */
    public void resetOtherScroll() {
        int currentViewId = mSwitcher.getCurrentView().getId();

        ScrollView sv = null;

        if (currentViewId == R.id.scrollView1) {
            sv = (ScrollView) findViewById(R.id.scrollView2);
        } else {
            sv = (ScrollView) findViewById(R.id.scrollView1);
        }
        if (sv != null) {
            sv.scrollTo(0, 0);
        }
    }

    /**
     * Assume switcher got two childs and return the other, currently not
     * visible, child.
     */
    public TextView getOtherHeadersView() {
        int currentViewId = mSwitcher.getCurrentView().getId();

        if (currentViewId == R.id.scrollView1) {
            return (TextView) findViewById(R.id.flipper_headers2_id);
        } else {
            return (TextView) findViewById(R.id.flipper_headers1_id);
        }
    }

    /**
     * Assume switcher got two childs and return the other, currently not
     * visible, child.
     */
    public TextView getOtherTextView() {
        int currentViewId = mSwitcher.getCurrentView().getId();

        if (currentViewId == R.id.scrollView1) {
            return (TextView) findViewById(R.id.flipper_text2_id);
        } else {
            return (TextView) findViewById(R.id.flipper_text1_id);
        }
    }

    /**
     * Assume switcher got two childs and return the other, currently not
     * visible, child.
     */
    public ImageView getOtherImgView() {
        int currentViewId = mSwitcher.getCurrentView().getId();

        if (currentViewId == R.id.scrollView1) {
            return (ImageView) findViewById(R.id.flipper_imageView2);
        } else {
            return (ImageView) findViewById(R.id.flipper_imageView1);
        }
    }

    /**
     * Get the visible TextView child
     * 
     */
    public TextView getCurrentTextView() {
        int currentViewId = mSwitcher.getCurrentView().getId();

        if (currentViewId == R.id.scrollView1) {
            return (TextView) findViewById(R.id.flipper_text1_id);
        } else {
            return (TextView) findViewById(R.id.flipper_text2_id);
        }
    }

    /**
     * Set switch to show image
     * 
     */
    public void setOtherImgSwitch() {
        int currentViewId = mSwitcher.getCurrentView().getId();
        ViewSwitcher switcher=null;
        
        if (currentViewId == R.id.scrollView1) {
            switcher = (ViewSwitcher) findViewById(R.id.profile2Switcher);
        } else {
            switcher = (ViewSwitcher) findViewById(R.id.profile1Switcher);
        }
        switcher.setDisplayedChild(1); // see order in XML
    }

    /**
     * Set switch to show text
     * 
     */
    public void setOtherTextSwitch() {
        int currentViewId = mSwitcher.getCurrentView().getId();
        ViewSwitcher switcher=null;
        
        if (currentViewId == R.id.scrollView1) {
            switcher = (ViewSwitcher) findViewById(R.id.profile2Switcher);
        } else {
            switcher = (ViewSwitcher) findViewById(R.id.profile1Switcher);
        }
        switcher.setDisplayedChild(0); // see order in XML
    }

    private String stackAsString()
    {
        String str = "STACK: ";
        for (int i = 0; i < mState.currentText.size(); ++i)
        {
            int textNo = mState.currentText.elementAt(i).getTextNo();
            if (i == mState.currentTextIndex)
            {
                str += " [" + textNo + "]";
            }
            else
            {
                str += " " + textNo;
            }
        }
        return str;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        if ((re_userId > 0) && (re_userPSW != null)
                && (re_userPSW.length() > 0)) {
            outState.putInt("UserId", re_userId);
            outState.putString("UserPSW", re_userPSW);
            outState.putString("UserServer", re_server);
        } else {
            if (mKom != null) {
                int userId = mKom.getUserId();
                if (userId > 0) {
                    Log.d(TAG, "Store userid:" + userId);
                    outState.putInt("UserId", userId);
                    outState.putString("UserPSW", mKom.getUserPassword());
                    outState.putString("UserServer", mKom.getServer());
                } else {
                    Log.d(TAG, "No userid, bailing out");
                }
            }
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");

        if (savedInstanceState != null) {
            Log.d(TAG, "on RestoreInstanceState got a bundle");
            restoreBundle(savedInstanceState);
        }
    }

    private void restoreBundle(Bundle savedInstanceState) {
        Log.d(TAG, "restoreBundle start");
        if (savedInstanceState != null) {
            Log.d(TAG, "restoreBundle got a bundle");
            // Restore UI state from the savedInstanceState.
            // This bundle has also been passed to onCreate.
            re_userId = savedInstanceState.getInt("UserId");
            re_userPSW = savedInstanceState.getString("UserPSW");
            re_server = savedInstanceState.getString("UserServer");
            if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&mKom!=null) {
                mKom.setUser(re_userId, re_userPSW, re_server);
            } else {
                if(mKom==null) {
                    Log.d(TAG, "mKom == null");
                }
                if(re_userId<1){
                    Log.d(TAG, "no userId");
                }
                if(re_userPSW==null){
                    Log.d(TAG, "null password");
                } else {
                    if(re_userPSW.length()<1){
                        Log.d(TAG, "short password");
                    }
                }
            }
        }        
    }
    
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected start");
        mKom = ((LocalBinder<KomServer>) service).getService();
        mKom.setShowHeadersLevel(mState.ShowHeadersLevel);
        if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&mKom!=null) {
            mKom.setUser(re_userId, re_userPSW, re_server);
            if(!mKom.isConnected()) {
                mKom.reconnect();
            }
        } else {
            if(mKom==null) {
                Log.d(TAG, "mKom == null");
            }
            if(re_userId<1){
                Log.d(TAG, "no userId");
            }
            if(re_userPSW==null){
                Log.d(TAG, "null password");
            } else {
                if(re_userPSW.length()<1){
                    Log.d(TAG, "short password");
                }
            }
        }
        new InitConnectionTask().execute();
        Log.d(TAG, "onServiceConnected done");
    }

    private class InitConnectionTask extends
            AsyncTask<Void, Void, Integer> {
        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        // worker thread (separate from UI thread)
        @Override
        protected Integer doInBackground(final Void... args) {
            {
                if (mKom == null) {
                    return null;
                }

                try {
                    mKom.setConference(mState.conferenceNo);
                } catch (RpcFailure e) {
                    // TODO Auto-generated catch block
                    Log.d(TAG, "onServiceConnected RpcFailure");
                    // e.printStackTrace();
                } catch (IOException e) {
                    Log.d(TAG, "onServiceConnected IOException:" + e);
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.connection_lost),
                            Toast.LENGTH_SHORT).show();
                    mKom.logout();
                    finish();
                }

                TextInfo currentText = null;

                if (mState.textQueue == null || mState.textQueue.isEmpty()) {
                    if ((mState != null) && (mState.hasCurrent())) {
                        currentText = mState.getCurrent();
                    }
                    mSwitcher.setInAnimation(mSlideLeftIn);
                    mSwitcher.setOutAnimation(mSlideLeftOut);
                    if (currentText != null) {
                        Log.d(TAG, "Getting current text");
                        new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO,
                                currentText.getTextNo(), 0);
                    } else {
                        Log.d(TAG, "Getting next text");
                        new LoadMessageTask().execute(MESSAGE_TYPE_NEXT, 0, 0);
                    }
                } else {
                    Log.d(TAG, "Getting text from queue");
                    new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO,
                            mState.textQueue.poll(), 0);
                }

            }
            return 0;
        }

        @Override
        protected void onPostExecute(final Integer value) {
            setProgressBarIndeterminateVisibility(false);
        }
    }

    public void onServiceDisconnected(ComponentName name) {
		mKom = null;		
	}

    App getApp() 
    {
        return (App)getApplication();
    }

    private class State {
        public int conferenceNo;
        int currentTextIndex;
        Stack<TextInfo> currentText;
        Queue<Integer> textQueue;
        boolean hasCurrent() { return currentTextIndex >= 0; }
        TextInfo getCurrent() { return currentText.elementAt(currentTextIndex); }
        int ShowHeadersLevel;
    };
    
    State mState;

	KomServer mKom;   
    // For gestures and animations

    private GestureDetector mGestureDetector;
    View.OnTouchListener mGestureListener;
    private Animation mSlideLeftIn;
    private Animation mSlideLeftOut;
    private Animation mSlideRightIn;
    private Animation mSlideRightOut;
    private ViewAnimator mSwitcher;

    private int re_userId = 0;
    private String re_userPSW = null;
    private String re_server = null;
    
    private static final int DIALOG_NUMBER_ENTRY = 7;
    private static final int MESSAGE_TYPE_PARENT_TO = 1;
    private static final int MESSAGE_TYPE_TEXTNO = 2;
    private static final int MESSAGE_TYPE_NEXT = 3;
}
