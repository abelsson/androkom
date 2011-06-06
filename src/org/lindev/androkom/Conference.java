package org.lindev.androkom;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lindev.androkom.KomServer.TextInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import android.widget.EditText;
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
    private static final String TAG = "Androkom";

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
        getApp().doBindService(this);

        setContentView(R.layout.conference);

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
            mState = (State)data;
            mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
        } else {
            mState = new State();
            mState.currentText = new Stack<TextInfo>();
            mState.currentTextIndex = -1;
            mState.ShowFullHeaders = ConferencePrefs.getShowFullHeaders(getBaseContext());        
            mState.conferenceNo = confNo;
            mSwitcher.setText(formatText(getString(R.string.loading_text)));
        }

        mGestureDetector = new GestureDetector(new MyGestureDetector());
       
    }
    

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getApp().doUnbindService(this);
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
            setProgressBarIndeterminateVisibility(true);
        }

        // worker thread (separate from UI thread)
        protected TextInfo doInBackground(final Integer... args) {
            Log.d(TAG, "LoadMessageTask doInBackground BEGIN");
            TextInfo text = null;

            if (mState.hasCurrent()) {
                mKom.markTextAsRead(mState.getCurrent().getTextNo());
            }

            Log.d(TAG, "LoadMessageTask doInBackground curent text is marked as read");
            Log.d(TAG, "LoadMessageTask doInBackground case "+args[0]);

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
            if (text != null && text.getTextNo() < 0) {
                Toast.makeText(getApplicationContext(), text.getBody(), Toast.LENGTH_SHORT).show();
            }
            else if (text != null) {
                mState.currentText.push(text);
                mState.currentTextIndex = mState.currentText.size() - 1;
                Log.i(TAG, stackAsString());
                mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
                TextView widget = (TextView) mSwitcher.getCurrentView();
                widget.scrollTo(0, 0);
                setTitle(mKom.getConferenceName());
            } else {
            	Log.d(TAG, "LoadMessageTask onPostExecute text=null");
            }
            setProgressBarIndeterminateVisibility(false);
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
        public boolean onSingleTapUp(MotionEvent e)
        {
        	Display display = getWindowManager().getDefaultDisplay();
        	int width = display.getWidth();  

        	if (e.getRawX() > 0.8*width && e.getDownTime() > 500) {
	             moveToNextText();
	             return true;
        	}     	
        	if (e.getRawX() < 0.2*width && e.getDownTime() > 500) {
	             moveToPrevText();
	             return true;
        	}  
        	return false;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) 
        {
            try {
	            // Horizontal swipes
            	if (Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH) {	                
	                // right to left swipe
	                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)            
	                    moveToNextText();     
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
            mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
        }
    }

    private void moveToNextText() {
        Log.i(TAG, "moving to next text cur:" + mState.currentTextIndex + "/" + mState.currentText.size());

        if ((mState.currentTextIndex + 1) >= mState.currentText.size()) {
            // At end of list. load new text from server
            Log.i(TAG, "fetching new text");
            new LoadMessageTask().execute(MESSAGE_TYPE_NEXT, 0);
            mSwitcher.setInAnimation(mSlideLeftIn);
            mSwitcher.setOutAnimation(mSlideLeftOut);
        }
        else {
            // Display old text, already fetched.
            mState.currentTextIndex++;
            mSwitcher.setInAnimation(mSlideLeftIn);
            mSwitcher.setOutAnimation(mSlideLeftOut);
            Log.i(TAG, stackAsString());
            mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
        }
    }

    private void moveToParentText() {
        int current = mState.getCurrent().getTextNo();
        Log.i(TAG, "fetching parent to text " + current);
        new LoadMessageTask().execute(MESSAGE_TYPE_PARENT_TO, current);

        mSwitcher.setInAnimation(mSlideLeftIn);
        mSwitcher.setOutAnimation(mSlideLeftOut);
    }

    private void moveToText(final int textNo) {
        Log.i(TAG, "fetching text " + textNo);
        new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, textNo);
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
		switch (keyCode) {
		case android.view.KeyEvent.KEYCODE_B:
			moveToPrevText();
			return true;
		case android.view.KeyEvent.KEYCODE_P:
			moveToParentText();
			return true;
		case android.view.KeyEvent.KEYCODE_F:
		case android.view.KeyEvent.KEYCODE_SPACE:
			moveToNextText();
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
        // return onTouchEvent(event);
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
        // Handle item selection
        switch (item.getItemId()) {

        /*
         * A reply to the current text was requested, so show a 
         * CreateText activity. 
         */
        case R.id.reply:
            Intent intent = new Intent(this, CreateText.class);    
            intent.putExtra("in-reply-to", mState.getCurrent().getTextNo());
            intent.putExtra("subject-line", mState.getCurrent().getSubject());
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

		case R.id.menu_createnewtext_id:
            intent = new Intent(this, CreateNewText.class);    
            intent.putExtra("recipient_type", 1);
            startActivity(intent);
			return true;

		case R.id.menu_createnewmail_id:
            intent = new Intent(this, CreateNewText.class);    
            intent.putExtra("recipient_type", 2);
            startActivity(intent);
			return true;

		case R.id.menu_createnewIM_id:
            intent = new Intent(this, CreateNewIM.class);    
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
			seewhoison();
			return true;

        case R.id.menu_message_log_id:
            intent = new Intent(this, MessageLog.class);
            startActivity(intent);
            return true;
            
		default:
            return super.onOptionsItemSelected(item);
        }
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
    	int CurrentTextNo = mState.getCurrent().getTextNo();
    	mKom.unmarkText(CurrentTextNo);
    }

    protected void seetextagain() {
    	showDialog(DIALOG_NUMBER_ENTRY);
    }

    protected void seepresentation() {
        int textNo = mKom.getConferencePres();
        if (textNo > 0) {
            Log.i(TAG, "fetching text " + textNo);
            new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, textNo);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.no_presentation_error),
                    Toast.LENGTH_SHORT).show();
        }
    }

	protected void seewhoison() {
        Intent intent = new Intent(this, WhoIsOn.class);    
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
		  new LoadMessageTask().execute(MESSAGE_TYPE_TEXTNO, textNo);

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


    public Spannable formatText(TextInfo text, boolean ShowFullHeaders)
    {
        String[] lines = text.getBody().split("\n");
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
				String[] headerlines = text.getHeaders().split("\n");
				for (String line : headerlines) {
					body.append(line);
					body.append("<br/>");
				}
			}
            
            body.append("<b>Subject: ");
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
        FillMessage fm = new FillMessage(text.getBody());
        body.append(fm.run());

        //Log.i(TAG, body.toString());

        Spannable spannedText = (Spannable) Html.fromHtml(body.toString());
        addLinks(spannedText, Pattern.compile("\\d{5,}"), null);
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
        TextView t = new TextView(this);
        t.setText("[no text loaded]", TextView.BufferType.SPANNABLE);
        t.setMovementMethod(LinkMovementMethod.getInstance());
        t.setGravity(Gravity.TOP | Gravity.LEFT);
        t.setTextColor(ColorStateList.valueOf(Color.WHITE));

        // TODO: Eh. Figure out how calculate our height properly.	
        t.setMaxHeight(getWindowManager().getDefaultDisplay().getHeight()-40); 
      
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

    public void onServiceConnected(ComponentName name, IBinder service) {
        mKom = ((KomServer.LocalBinder)service).getService();
        mKom.setShowFullHeaders(mState.ShowFullHeaders);
        mKom.setConference(mState.conferenceNo);
        new LoadMessageTask().execute(MESSAGE_TYPE_NEXT, 0);
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

    private static final int DIALOG_NUMBER_ENTRY = 7;
    private static final int MESSAGE_TYPE_PARENT_TO = 1;
    private static final int MESSAGE_TYPE_TEXTNO = 2;
    private static final int MESSAGE_TYPE_NEXT = 3;
}
