package org.lindev.androkom;

import java.io.IOException;
import java.util.Stack;
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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
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
import android.view.Gravity;
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
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

/**
 * Show texts in a LysKOM conference.
 * 
 * @author henrik
 *
 */
public class Conference extends Activity implements ViewSwitcher.ViewFactory, OnTouchListener, ServiceConnection
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
        
        mSwitcher = (TextSwitcher)findViewById(R.id.flipper);
        mSwitcher.setFactory(this);

        mSlideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        mSlideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        mSlideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        mSlideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);

        final Object data = getLastNonConfigurationInstance();
        final int confNo = (Integer) getIntent().getExtras().get("conference-id");

        Log.i(TAG, "Got passed conference id: " + confNo);

        if (data != null) {
            Log.d(TAG, "Got data!");
            mState = (State) data;
            if (mState.hasCurrent()) {
                try {
                    Spannable spannedText = mState.currentText.elementAt(
                            mState.currentTextIndex).getSpannable();
                    addLinks(spannedText, digits, null);
                    mSwitcher.setText(spannedText);
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
            mState.currentTextIndex = -1;
            mState.ShowFullHeaders = ConferencePrefs
                    .getShowFullHeaders(getBaseContext());
            mState.conferenceNo = confNo;
            mSwitcher.setText(formatText(getString(R.string.loading_text)+" "));
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
            intent.putExtra(TextCreator.INTENT_SUBJECT, mState.getCurrent()
                    .getSubject());
            intent.putExtra(TextCreator.INTENT_REPLY_TO, mState.getCurrent()
                    .getTextNo());
            startActivity(intent);
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
                mState.currentText.push(text);
                mState.currentTextIndex = mState.currentText.size() - 1;
                //Log.i(TAG, stackAsString());

                //Log.d(TAG, "VHEADERS: "+text.getVisibleHeaders());
                //Log.d(TAG, "AHEADERS: "+text.getAllHeaders());
                //Log.d(TAG, "AUTHOR: "+text.getAuthor());
                //Log.d(TAG, "SUBJECT: "+text.getSubject());
                //Log.d(TAG, "BODY: "+text.getBody());
                if (text.getAllHeaders().contains("ContentType:image/")) {
                    mSwitcher.setText("Text "+text.getTextNo()+getString(R.string.is_image));
                    
                    Intent intent = new Intent(getApplicationContext(), imagesactivity.class);
                    intent.putExtra("bilden", text.getRawBody());
                    startActivity(intent);
                    
                    TextView widget = (TextView) mSwitcher.getCurrentView();
                    widget.scrollTo(0, 0);
                } else {
                    final Spannable spannedText = text.getSpannable();
                    addLinks(spannedText, digits, null);
                    mSwitcher.setText(spannedText);

                    TextView widget = (TextView) mSwitcher.getCurrentView();
                    widget.scrollTo(0, 0);
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
     * Only a skeleton for now. 
     * 
     * @author henrik
     *
     */
    class KomInternalURLSpan extends ClickableSpan {
        String mLinkText;
        
        public KomInternalURLSpan(String mLinkText) {
            this.mLinkText = mLinkText;
        }

        @Override
        public void onClick(View widget) {
            int textNo;
            try {
                textNo = Integer.valueOf(mLinkText);
            } catch (NumberFormatException e)
            {
                Log.i(TAG, "Illegal textNo: " + mLinkText);
                return;
            }
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
            activateUser();

            Display display = getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            // TODO: Eh. Figure out how calculate our height properly (excluding optional buttons).
            int myLimit = mSwitcher.getBaseline() + mSwitcher.getBottom();
            if (e.getRawY() < myLimit) {
                if (e.getRawX() > 0.8 * width && e.getDownTime() > 500) {
                    moveToNextText(true);
                    return true;
                }
                if (e.getRawX() < 0.2 * width && e.getDownTime() > 500) {
                    moveToPrevText();
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) 
        {
            activateUser();     

            try {
	            // Horizontal swipes
            	if (Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH) {	                
	                // right to left swipe
	                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)            
	                    moveToNextText(true);     
	                 else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) 
	                    moveToPrevText();  	                
                }
            	
	            // Vertical swipes
	            if (Math.abs(e1.getX() - e2.getX()) <= SWIPE_MAX_OFF_PATH) {	                
	                // top to bottom swipe
	                if(e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY)            
	                    moveToParentText();     
	                      
                }
	            
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }

    private void moveToPrevText() {
        Log.i(TAG, "moving to prev text, cur: " + (mState.currentTextIndex-1) + "/" + mState.currentText.size());

        if (mState.currentTextIndex > 0) {
            mState.currentTextIndex--;
            Log.i(TAG, stackAsString());
            mSwitcher.setInAnimation(mSlideRightIn);
            mSwitcher.setOutAnimation(mSlideRightOut);
            mSwitcher.setText(mState.currentText.elementAt(mState.currentTextIndex).getSpannable());
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
        Log.i(TAG, "moving to next text cur:" + mState.currentTextIndex + "/" + mState.currentText.size());
        int markTextAsReadint = markTextAsRead ? 0 : 1;
        
        if ((mState.currentTextIndex + 1) >= mState.currentText.size()) {
            // At end of list. load new text from server
            Log.i(TAG, "fetching new text");
            new LoadMessageTask().execute(MESSAGE_TYPE_NEXT, 0, markTextAsReadint);
            mSwitcher.setInAnimation(mSlideLeftIn);
            mSwitcher.setOutAnimation(mSlideLeftOut);
        }
        else {
            // Display old text, already fetched.
            mState.currentTextIndex++;
            mSwitcher.setInAnimation(mSlideLeftIn);
            mSwitcher.setOutAnimation(mSlideLeftOut);
            Log.i(TAG, stackAsString());
            mSwitcher.setText(mState.currentText.elementAt(mState.currentTextIndex).getSpannable());
        }
    }

    private void moveToParentText() {
        int current = mState.getCurrent().getTextNo();
        Log.i(TAG, "fetching parent to text " + current);
        new LoadMessageTask().execute(MESSAGE_TYPE_PARENT_TO, current, 0);

        mSwitcher.setInAnimation(mSlideLeftIn);
        mSwitcher.setOutAnimation(mSlideLeftOut);
    }

    private void moveToText(final int textNo) {
        Log.i(TAG, "fetching text " + textNo);
        new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, textNo, 0);
        mSwitcher.setInAnimation(mSlideLeftIn);
        mSwitcher.setOutAnimation(mSlideLeftOut);
    }

	private void scrollPageUp() {
		TextView t = (TextView) mSwitcher.getCurrentView();

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
		TextView t = (TextView) mSwitcher.getCurrentView();

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
			t1 = (TextView) mSwitcher.getChildAt(0);
			newtextsize = (float) (t1.getTextSize()*1.1);
			t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newtextsize);
			t1.invalidate();

			t1 = (TextView) mSwitcher.getChildAt(1);
			t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newtextsize);
			t1.invalidate();
			
			storeFontSize(newtextsize);
			return true;

		case R.id.menu_smallerfontsize_id :
			Log.d(TAG, "Change fontsize-");
			t1 = (TextView) mSwitcher.getChildAt(0);
			newtextsize = (float) (t1.getTextSize()*0.9);
			t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newtextsize);
			t1.invalidate();

			t1 = (TextView) mSwitcher.getChildAt(1);
			t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newtextsize);
			t1.invalidate();
			
			storeFontSize(newtextsize);
			return true;

		case R.id.menu_monospaced_id :
            Log.d(TAG, "Change to monospaced");
            t1 = (TextView) mSwitcher.getChildAt(0);
            t1.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
            t1.invalidate();
            return true;

        case R.id.menu_rot13_id :
            Log.d(TAG, "Toggle rot13");
            t1 = (TextView) mSwitcher.getChildAt(0);
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

		case R.id.menu_seepresentation_id:
			seepresentation();
			return true;

		case R.id.menu_seewhoison_id:
			seewhoison(1);
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
        // TODO Auto-generated method stub
        int CurrentText = mState.getCurrent().getTextNo();
        
        mKom.addAuxItem(CurrentText, AuxItem.tagFastReply, "Gilla");
    }


    protected void storeFontSize(float size) {
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("conference_body_textsize", size);    	
    }

    protected void setCurrentFontSize() {
        SharedPreferences prefs =  getPreferences(MODE_PRIVATE);
    	
		TextView t1 = (TextView) mSwitcher.getChildAt(0);
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

    protected void seepresentation() {
        int textNo = mKom.getConferencePres();
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
                            // TODO Auto-generated method stub
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
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);

    	alert.setTitle(getString(R.string.seetextagain_label));
    	alert.setMessage(getString(R.string.alert_dialog_text_entry));

    	// Set an EditText view to get user input 
    	final EditText input = new EditText(this);
    	alert.setView(input);

    	alert.setPositiveButton(getString(R.string.alert_dialog_ok), new DialogInterface.OnClickListener() {
    	public void onClick(DialogInterface dialog, int whichButton) {
    	  String textvalue = input.getText().toString();
  		  Log.i(TAG, "trying to parse " + textvalue);
		  int textNo = Integer.parseInt(textvalue);
		  Log.i(TAG, "fetching text " + textNo);
		  new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, textNo, 0);

		  mSwitcher.setInAnimation(mSlideLeftIn);
		  mSwitcher.setOutAnimation(mSlideLeftOut);
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
    public static Spannable formatText(Context context, TextInfo text, boolean ShowFullHeaders)
    {
        //String[] lines = text.getBody().split("\n");
        StringBuilder body = new StringBuilder();

        // Some simple heuristics to reflow and htmlize KOM texts.
        // TODO: Deal with quoted blocks prefixed with '>'.

        if (text.getTextNo() > 0) {   
        	body.append(text.getTextNo());       	
            body.append(" <b>");
            body.append(text.getAuthor());
            body.append("</b> ");
        	body.append(text.getDate());
            body.append("<br/>");

			if (ShowFullHeaders) {
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
        
        /*
        body.append("<p>");
        for(String line : lines) {
            if (line.startsWith(" ") || line.startsWith("\t"))
                body.append("<br/>");


            if (line.trim().length() == 0)
                body.append("</p><p>");

            
            body.append(line);
            body.append(" ");
        }
        body.append("</p>");
        */

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
     * Return TextViews for switcher.
     */
    public View makeView() {
        Log.d(TAG, "makeView");
        TextView t = new TextView(this);
        t.setText(getString(R.string.no_text_loaded), TextView.BufferType.SPANNABLE);
        t.setMovementMethod(LinkMovementMethod.getInstance());
        t.setGravity(Gravity.TOP | Gravity.LEFT);
        t.setTextColor(ColorStateList.valueOf(Color.WHITE));

        // TODO: Eh. Figure out how calculate our height properly (excluding optional buttons).
        //t.setMaxHeight(getWindowManager().getDefaultDisplay().getHeight()-40); 
        return t;
    }

    App getApp() 
    {
        return (App)getApplication();
    }

    private class State {
        public int conferenceNo;
		int currentTextIndex;
        Stack<TextInfo> currentText;
        boolean hasCurrent() { return currentTextIndex >= 0; }
        TextInfo getCurrent() { return currentText.elementAt(currentTextIndex); }
        boolean ShowFullHeaders;
    };
    
    State mState;

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
                    finish();
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
        mKom.setShowFullHeaders(mState.ShowFullHeaders);
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
        try {
            mKom.setConference(mState.conferenceNo);
        } catch (RpcFailure e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "onServiceConnected RpcFailure");
            //e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.d(TAG, "onServiceConnected IOException");
            //e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.connection_lost), Toast.LENGTH_SHORT).show();
            mKom.logout();
            finish();
        }

        TextInfo currentText = null;

        if((mState!=null)&&(mState.hasCurrent())) {
            currentText = mState.getCurrent();
        }
        if(currentText!=null) {
            Log.d(TAG, "Getting current text");
            new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, currentText.getTextNo(), 0);
        } else {
            Log.d(TAG, "Getting next text");
            new LoadMessageTask().execute(MESSAGE_TYPE_NEXT, 0, 0);
        }
        Log.d(TAG, "onServiceConnected done");
    }

	public void onServiceDisconnected(ComponentName name) {
		mKom = null;		
	}
	
	KomServer mKom;   
    // For gestures and animations

    private GestureDetector mGestureDetector;
    View.OnTouchListener mGestureListener;
    private Animation mSlideLeftIn;
    private Animation mSlideLeftOut;
    private Animation mSlideRightIn;
    private Animation mSlideRightOut;
    private TextSwitcher mSwitcher;

    private int re_userId = 0;
    private String re_userPSW = null;
    private String re_server = null;
    
    private static final int DIALOG_NUMBER_ENTRY = 7;
    private static final int MESSAGE_TYPE_PARENT_TO = 1;
    private static final int MESSAGE_TYPE_TEXTNO = 2;
    private static final int MESSAGE_TYPE_NEXT = 3;
}
