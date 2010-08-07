package org.lindev.androkom;

import java.util.Stack;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.Touch;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

/**
 * Show texts in a LysKOM conference.
 * 
 * @author henrik
 *
 */
public class Conference extends Activity implements ViewSwitcher.ViewFactory
{
	

    private static final int SWIPE_MIN_DISTANCE = 120;
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
        	mSwitcher.setText(Html.fromHtml(mState.currentText.elementAt(mState.currentTextIndex)));
        	
        } else {    
        	mState = new State();
        	mState.currentText = new Stack<String>();
        	mState.currentTextIndex = 0;
        	getApp().getKom().setConference(confNo);
        	mState.currentText.push(getApp().getKom().getNextUnreadText());
        	
        	Spanned text = Html.fromHtml(mState.currentText.peek());
        	mState.currentTextIndex = 0;
        	
        	mSwitcher.setText(text);
        }
 
        mGestureDetector = new GestureDetector(new MyGestureDetector());
        mGestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (mGestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
        
    }

    class MyGestureDetector extends SimpleOnGestureListener {        

        @Override
        public boolean onScroll (MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
        	Log.i("androkom","got scroll event "+distanceX + " " + distanceY);
        	if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH)
        		return false;
        	  	
        	TextView widget = (TextView)mSwitcher.getCurrentView();

        	// Constrain to top of text widget.
        	int newX = Math.max((int)(widget.getScrollX()+distanceX),0);
        	int newY = Math.max((int)(widget.getScrollY()+distanceY),0);
            
        	// TODO: Implement momentum scrolling.
        	Touch.scrollTo(widget, widget.getLayout(), newX,  newY);

			return true;       	
        }
		@Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                	
                     
                	mState.currentTextIndex++;
                	Log.i("androkom","moving to next text cur:" + mState.currentTextIndex + "/" + mState.currentText.size()); 
                	if (mState.currentTextIndex >= mState.currentText.size()) {
                		Log.i("androkom", "fetching new text");
                		mState.currentText.push(getApp().getKom().getNextUnreadText());                		
                	}
                		
                	
                	Spanned text = Html.fromHtml(mState.currentText.elementAt(mState.currentTextIndex));
                                	
                	mSwitcher.setInAnimation(mSlideLeftIn);
                    mSwitcher.setOutAnimation(mSlideLeftOut);
                    mSwitcher.setText(text);
                    return true;
                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                	Log.i("androkom","left swipe detected");
                	mState.currentTextIndex--;        
                    if (mState.currentTextIndex <= 0) {
                    	mState.currentTextIndex = 0;
                    	return true;
                    }
                	
                    Spanned text = Html.fromHtml(mState.currentText.elementAt(mState.currentTextIndex));
                    
                	mSwitcher.setInAnimation(mSlideRightIn);
                    mSwitcher.setOutAnimation(mSlideRightOut);
                    mSwitcher.setText(text);
                    return true;
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event))
	        return true;
	    else
	    	return false;
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {    	
    	return mState;
    }

   
    /**
     * Called when user has selected a menu item from the 
     * menu button popup. 
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        
        /*
         * A reply to the current text was requested, so show a 
         * CreateText activity. 
         */
        case R.id.reply:
            Intent intent = new Intent(this, CreateText.class);    
            intent.putExtra("in-reply-to", getApp().getKom().getLastTextNo());
            startActivity(intent);
            return true;
       
        default:
            return super.onOptionsItemSelected(item);
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
    

    /**
     * Return TextViews for switcher.
     */
	public View makeView() {
        TextView t = new TextView(this);
        t.setText("[no text loaded]", TextView.BufferType.SPANNABLE);
        t.setGravity(Gravity.TOP | Gravity.LEFT);
        return t;
	}

    App getApp() 
    {
        return (App)getApplication();
    }
    
    private class State {
        int currentTextIndex;
        Stack<String> currentText;        
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
    
    
}
