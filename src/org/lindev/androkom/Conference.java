package org.lindev.androkom;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lindev.androkom.KomServer.TextInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
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
public class Conference extends Activity implements ViewSwitcher.ViewFactory, OnTouchListener
{
	public static final String TAG = "Androkom Conference";

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
        super.onCreate(savedInstanceState);

        setContentView(R.layout.conference);

        mSwitcher = (TextSwitcher)findViewById(R.id.flipper);
        mSwitcher.setFactory(this);

        mSlideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        mSlideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        mSlideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        mSlideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);

        final Object data = getLastNonConfigurationInstance();
        final int confNo = (Integer) getIntent().getExtras().get("conference-id");

        Log.i("androkom", "Got passed conference id: " + confNo);

        if (data != null) {      	
            mState = (State)data;
            mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
        } else {
            mState = new State();
            mState.currentText = new Stack<TextInfo>();
            mState.currentTextIndex = 0;
            mState.ShowFullHeaders = ConferencePrefs.getShowFullHeaders(getBaseContext());
            getApp().getKom().setShowFullHeaders(mState.ShowFullHeaders);
            getApp().getKom().setConference(confNo);
            new LoadMessageTask().execute();
            
            mSwitcher.setText(formatText(getString(R.string.loading_text)));
        }

        mGestureDetector = new GestureDetector(new MyGestureDetector());
       

    }
    
    /**
     * Fetch new texts asynchronously, and show a progress spinner
     * while the user is waiting.
     * 
     * @author henrik
     *
     */
    private class LoadMessageTask extends AsyncTask<Integer, Integer, TextInfo> 
    {
        private final ProgressDialog dialog = new ProgressDialog(Conference.this);

        protected void onPreExecute() 
        {
            this.dialog.setCancelable(true);
            this.dialog.setIndeterminate(true);
            this.dialog.setMessage(getString(R.string.loading));
            this.dialog.show();
        }

        // worker thread (separate from UI thread)
        protected TextInfo doInBackground(final Integer... args) 
        {
        	if (args.length == 2 && args[0] > 0) {
        		switch (args[0]) {
        		case MESSAGE_TYPE_PARENT_TO:
        			Log.d(TAG, "Trying to get parent text of"+args[1]);
            		return ((App)getApplication()).getKom().getParentToText(args[1]);    
        		case MESSAGE_TYPE_TEXTNO: 
        			Log.d(TAG, "Trying to get text "+args[1]);
            		return ((App)getApplication()).getKom().getKomText(args[1]);
        		default:
        			Log.d(TAG, "LoadMessageTask unknown type:" + args[0]);
        			return null;
        		}
        	}
        	else
        		return ((App)getApplication()).getKom().getNextUnreadText();    
        	
        	
        }

        protected void onPostExecute(final TextInfo text) 
        {
            if (text.getTextNo() < 0) {
                this.dialog.dismiss();
                Toast.makeText(getApplicationContext(), text.getBody(), Toast.LENGTH_SHORT).show();
                return;
            }
            mState.currentText.push(text);
            mState.currentTextIndex = mState.currentText.size() - 1;
            mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
            TextView widget = (TextView)mSwitcher.getCurrentView();
            widget.scrollTo(0, 0);
            setTitle(((App)getApplication()).getKom().getConferenceName());
            this.dialog.dismiss();
        }
    }

    private class MarkTextReadTask extends AsyncTask<Integer, Integer, Void> 
    {
        @Override
        protected Void doInBackground(final Integer... args) 
        {       	
        	((App)getApplication()).getKom().markTextAsRead(args[0]);
			return null;  	      	
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
            
        }  

        @Override  
        public void onClick(View widget) {  
            // TODO Conference.this.onKomLinkClicked(mLinkText);
        	Log.d(TAG, "ClickableSpan onClick");
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

		private void moveToPrevText() {
			Log.i("androkom","moving to prev text, cur: " + (mState.currentTextIndex-1) + "/" + mState.currentText.size());
			
			mState.currentTextIndex--;        
			
			if (mState.currentTextIndex < 0) {
			    mState.currentTextIndex = 0;
			    return;
			}
			
			mSwitcher.setInAnimation(mSlideRightIn);
			mSwitcher.setOutAnimation(mSlideRightOut);
			mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
		}

		private void moveToNextText() {
			Log.i("androkom","moving to next text cur:" + mState.currentTextIndex + "/" + mState.currentText.size()); 

			new MarkTextReadTask().execute(mState.getCurrent().getTextNo());

			mState.currentTextIndex++;
			
			if (mState.currentTextIndex >= mState.currentText.size()) {
			    // At end of list. load new text from server
			    Log.i("androkom", "fetching new text");
			    new LoadMessageTask().execute(-1);

			    mSwitcher.setInAnimation(mSlideLeftIn);
			    mSwitcher.setOutAnimation(mSlideLeftOut);			  
			    mState.currentTextIndex = mState.currentText.size() - 1;
			}
			else {
			    // Display old text, already fetched.
			    mSwitcher.setInAnimation(mSlideLeftIn);
			    mSwitcher.setOutAnimation(mSlideLeftOut);

			    mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
			}
		}
		
		private void moveToParentText()
		{
		    Log.i("androkom", "fetching parent to text " + mState.getCurrent().getTextNo());
		    new LoadMessageTask().execute(MESSAGE_TYPE_PARENT_TO, mState.getCurrent().getTextNo());

		    mSwitcher.setInAnimation(mSlideLeftIn);
		    mSwitcher.setOutAnimation(mSlideLeftOut);	
		}
    }

	private void moveToPrevText() {
		Log.i("androkom","moving to prev text, cur: " + (mState.currentTextIndex-1) + "/" + mState.currentText.size());
		
		mState.currentTextIndex--;        
		
		if (mState.currentTextIndex < 0) {
		    mState.currentTextIndex = 0;
		    return;
		}
		
		mSwitcher.setInAnimation(mSlideRightIn);
		mSwitcher.setOutAnimation(mSlideRightOut);
		mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
	}

	private void moveToNextText() {
		Log.i("androkom","moving to next text cur:" + mState.currentTextIndex + "/" + mState.currentText.size()); 
		
        new MarkTextReadTask().execute(mState.getCurrent().getTextNo());

		mState.currentTextIndex++;
		
		if (mState.currentTextIndex >= mState.currentText.size()) {
		    // At end of list. load new text from server
		    Log.i("androkom", "fetching new text");
		    new LoadMessageTask().execute(-1);

		    mSwitcher.setInAnimation(mSlideLeftIn);
		    mSwitcher.setOutAnimation(mSlideLeftOut);			  
		    mState.currentTextIndex = mState.currentText.size() - 1;
		}
		else {
		    // Display old text, already fetched.
		    mSwitcher.setInAnimation(mSlideLeftIn);
		    mSwitcher.setOutAnimation(mSlideLeftOut);

		    mSwitcher.setText(formatText(mState.currentText.elementAt(mState.currentTextIndex), mState.ShowFullHeaders));
		}
	}

	private void moveToParentText()
	{
	    Log.i("androkom", "fetching parent to text " + mState.getCurrent().getTextNo());
	    new LoadMessageTask().execute(MESSAGE_TYPE_PARENT_TO, mState.getCurrent().getTextNo());

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
    	((App)getApplication()).getKom().markText(CurrentTextNo);
    }
    
    protected void unmarkCurrentText() {
    	int CurrentTextNo = mState.getCurrent().getTextNo();
    	((App)getApplication()).getKom().unmarkText(CurrentTextNo);
    }

    protected void seetextagain() {
    	showDialog(DIALOG_NUMBER_ENTRY);
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


    public static Spannable formatText(TextInfo text, boolean ShowFullHeaders)
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
        
        body.append("<p>");
        for(String line : lines) {
            if (line.startsWith(" ") || line.startsWith("\t"))
                body.append("<br/>");


            if (line.trim().length() == 0)
                body.append("</p><p>");

            line = line.replaceAll("&", "&amp;");
            line = line.replaceAll("<", "&lt;");
            line = line.replaceAll(">", "&gt;");
            body.append(line);
            body.append(" ");
        }
        body.append("</p>");

        Log.i("androkom", body.toString());

        SpannableStringBuilder spannedText = (SpannableStringBuilder)Html.fromHtml(body.toString());       
        Linkify.addLinks(spannedText, Linkify.ALL);
        
        return spannedText;
    }
    
    public static Spannable formatText(String text)
    {
     
        SpannableStringBuilder spannedText = (SpannableStringBuilder)Html.fromHtml(text);       
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
        int currentTextIndex;
        Stack<TextInfo> currentText;   
        TextInfo getCurrent() { return currentText.elementAt(currentTextIndex); }
        boolean ShowFullHeaders;
    };
    
    State mState;

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
}
