package org.lindev.androkom;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.dll.lyskom.AuxItem;
import nu.dll.lyskom.RpcFailure;
import nu.dll.lyskom.Text;

import org.lindev.androkom.AsyncMessages.AsyncMessageSubscriber;
import org.lindev.androkom.KomServer.TextInfo;
import org.lindev.androkom.gui.IMConversationList;
import org.lindev.androkom.gui.ImgTextCreator;
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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
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
public class Conference extends Activity implements AsyncMessageSubscriber, OnTouchListener, ServiceConnection, OnInitListener
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
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate initialize");
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	
        // Use this when bumping to SdkVersion to 9
        if(!KomServer.RELEASE_BUILD) {
         // Activate StrictMode
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                //.detectAll()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork() 
                 // alternatively .detectAll() for all detectable problems
                .penaltyLog()
                //.penaltyDeath()
                .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                 .detectLeakedSqlLiteObjects()
                // .detectLeakedClosableObjects()  // API-level 11
                // alternatively .detectAll() for all detectable problems
                 //.detectAll()
                .penaltyLog()
                //.penaltyDeath()
                .build());
        }
        
        
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
            Log.d(TAG, "onCreate Got data!");
            mState = (State) data;
            if (mState.hasCurrent()) {
                try {
                    Spannable spannedText = mState.currentText.elementAt(
                            mState.currentTextIndex).getSpannableBody();
                    addLinks(spannedText, digits);
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
            Log.d(TAG, "onCreate Initialize without bundle data");
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
                
                Object textId_obj = extras.get("textListIndex");
                textNo = -1;
                if (textId_obj != null) {
                    Integer textId = (Integer) textId_obj;
                    if(textId >= 0) {
                        textNo = textId;
                    }
                }
                mState.textListIndex  = textNo;
                Log.i(TAG, "Got passed text id: " + textNo);
            }
        }

        mGestureDetector = new GestureDetector(new MyGestureDetector());
       
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // Ignore duplicate messages
                if (!hasMessages(msg.what)) {
                    consumeMessage(msg);
                }
            }
        };

        tts = new TextToSpeech(getBaseContext(), this);

        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate Got a bundle");
            restoreBundle(savedInstanceState);
        }
        Log.d(TAG, "onCreate trying to bind service");
        getApp().doBindService(this);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }
    
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&(mKom!=null)) {
            Log.d(TAG, "onResume resets username");
            mKom.setUser(re_userId, re_userPSW, re_server, re_port, re_useSSL, re_cert_level);
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
            if((mKom!=null) && (mKom.getUserId()>0)) {
                Log.d(TAG, "onResume mKom has username");
            } else {
                Log.d(TAG, "onResume mKom is missing username");
            }
            if ((mKom!=null) && (mState.textListIndex >= 0)) {
                List<TextInfo> textList = mKom.getCurrentTextList();
                if (textList != null) {
                    Log.d(TAG, "onResume textList exists");
                    int textNo = textList.get(mState.textListIndex).getTextNo();
                    mState.textQueue = new ArrayBlockingQueue<Integer>(10);
                    mState.textQueue.offer(textNo);
                } else {
                    Log.d(TAG, "onResume textList is null");
                }
            }
        }
    }
    
	@Override
	protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
	    if(mKom != null) {
	        mKom.interruptPrefetcher();
	    }
        getApp().doUnbindService(this);

        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        
        if (mKom != null) {
            mKom.removeAsyncSubscriber(this);
        }
		Log.d(TAG, "Destroyed");
	}

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        mSwitcher.setDisplayedChild(0);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
        // set current also
        TextInfo currentText = mState.getCurrent();
        if(currentText != null) {
            int textNo = currentText.getTextNo();
            loadMessage(Consts.MESSAGE_TYPE_TEXTNO, textNo, 0);
        } else {
            Log.d(TAG, "onRestart: no current text. bailing out.");
            finish();
        }
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
        Intent intent = null;
        TextInfo currentText = null;
        switch (butval) {
        case 1: // Svara
            intent = new Intent(this, TextCreator.class);
            currentText = mState.getCurrent();
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
        case 7: // Svara Kamera
            currentText = mState.getCurrent();
            if ((currentText != null) && (currentText.getTextNo() > 0)) {
                doCamReply();
            } else {
                Log.d(TAG, "doButtonClick case 7: no current text");
                Toast.makeText(getApplicationContext(),
                        getString(R.string.no_text_loaded), Toast.LENGTH_SHORT)
                        .show();
            }
            break;
        case 8: // Svara Album
            currentText = mState.getCurrent();
            if ((currentText != null) && (currentText.getTextNo() > 0)) {
                intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, IMG_REQUEST);
                Message msg = new Message();
                msg.obj = 0;
                msg.arg1 = currentText.getTextNo();
                msg.arg2 = 0;
                msg.what = Consts.MESSAGE_TYPE_MARKREAD;
                mHandler.sendMessage(msg);
            } else {
                Log.d(TAG, "doButtonClick case 8: no current text");
                Toast.makeText(getApplicationContext(),
                        getString(R.string.no_text_loaded), Toast.LENGTH_SHORT)
                        .show();
            }
            break;
        default:
            Log.d(TAG, "Unknown action, doing nothing.");
        }
    }

    
    protected void consumeMessage(final Message msg) {
        Thread backgroundThread;
        final int textNo;
        final TextInfo text;
	    
        final Object msgobj = msg.obj;
        final int msgwhat = msg.what;
        final int msgarg1 = msg.arg1;
        final int msgarg2 = msg.arg2;

        TextInfo texti;
        switch (msg.what) {
        case Consts.MESSAGE_TYPE_PARENT_TO:
            textNo = msg.arg1;
            Log.i(TAG,
                    "consumeMessage doInBackground Trying to get parent text of"
                            + textNo);
            backgroundThread = new Thread(new Runnable() {
                public void run() {
                    final TextInfo text;
                    final int lmsgarg1 = msgarg1;
                    final int lmsgarg2 = msgarg2;
                    text = mKom.getParentToText(textNo);
                    if ((text != null) && (text.getTextNo() > 0)) {
                        Log.i(TAG, "consumeMessage Got text");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                setMessage(text);
                                Log.i(TAG,
                                        "consumeMessage PARENT_TO:"+lmsgarg1);
                                Message lmsg = new Message();
                                lmsg.obj = 0;
                                lmsg.arg1 = lmsgarg1;
                                lmsg.arg2 = lmsgarg2;
                                lmsg.what = Consts.MESSAGE_TYPE_MARKREAD;
                                mHandler.sendMessage(lmsg);
                            }
                        });
                    } else {
                        Log.d(TAG, "consumeMessage failed to get parent of "
                                + textNo);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        getString(R.string.error_no_parent),
                                        Toast.LENGTH_SHORT).show();
                                setProgressBarIndeterminateVisibility(false);
                            }
                        });
                    }
                }
            });
            backgroundThread.start();
            break;

        case Consts.MESSAGE_TYPE_TEXTNO:
            textNo = msg.arg1;
            Log.i(TAG, "consumeMessage doInBackground Trying to get text "
                    + textNo);
            backgroundThread = new Thread(new Runnable() {
                public void run() {
                    final TextInfo text;
                    text = mKom.getKomText(textNo);
                    if (text != null) {
                        Log.i(TAG, "consumeMessage Got text");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                setMessage(text);
                            }
                        });
                    } else {
                        Log.d(TAG, "consumeMessage failed to get text "
                                + textNo);
                        finish();
                    }
                }
            });
            backgroundThread.start();
            break;

        case Consts.MESSAGE_PREFETCH_NEXT:
            Log.i(TAG, "consumeMessage MESSAGE_PREFETCH_NEXT 1");
            backgroundThread = new Thread(new Runnable() {
                public void run() {
                    Log.i(TAG, "consumeMessage MESSAGE_PREFETCH_NEXT BG 1");
                    TextInfo not_used_text = mKom.getNextUnreadText(true);
                    Log.i(TAG, "consumeMessage MESSAGE_PREFETCH_NEXT BG 2");
                }
            });
            backgroundThread.start();
            Log.i(TAG, "consumeMessage MESSAGE_PREFETCH_NEXT 2");
            break;
            
        case Consts.MESSAGE_TYPE_NEXT:
            Log.i(TAG, "consumeMessage Trying to get next unread text");
            if(msgobj == null) {
                Log.i(TAG, "consumeMessage msgobj == null");
            } else {
                int looplevel = (Integer) msgobj;
                Log.i(TAG, "consumeMessage msgobj/looplevel = "+looplevel);
            }
            backgroundThread = new Thread(new Runnable() {
                final int lmsgarg1 = msgarg1;
                final int lmsgarg2 = msgarg2;
                final Object lmsgobj = msgobj;
                final int lmsgwhat = msgwhat;
                final Handler lmHandler = mHandler;
                
                public void run() {
                    final TextInfo text;
                    Log.i(TAG, "consumeMessage MESSAGE_TYPE_NEXT bg thread begins");
                    
                    if(lmsgobj == null) {
                        Log.i(TAG, "consumeMessage lmsgobj == null");
                    } else {
                        int looplevel = (Integer) lmsgobj;
                        //Log.i(TAG, "consumeMessage lmsgobj = "+looplevel);
                    }

                    Log.i(TAG, "consumeMessage pre session");
                    text = mKom.getNextUnreadText(false);
                    Log.i(TAG, "consumeMessage post session");
                    if (text == null) {
                        int looplevel = 0;
                        if (msgobj != null) {
                            looplevel = (Integer) lmsgobj;
                        }
                        Log.d(TAG, "consumeMessage Failed to get text, loop "
                                + looplevel);
                        if (looplevel > 20) {
                            // Timeout
                            Log.d(TAG, "consumeMessage Could not find text");
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    setProgressBarIndeterminateVisibility(false);
                                    finish();
                                }
                            });
                        } else {
                            if (looplevel > 0) {
                                Log.d(TAG, "consumeMessage sending message");
                                Message msgout = new Message();
                                // msgout.arg1 = lmsgarg1;
                                msgout.arg2 = msg.arg2;
                                Integer ll = looplevel+1;
                                Log.d(TAG, "consumeMessage new looplevel:"+ll);
                                msgout.obj = ll;
                                msgout.what = lmsgwhat;
                                lmHandler.sendMessageDelayed(msgout, 200);
                            } else {
                                Log.d(TAG, "consumeMessage NOT sending message");
                                finish();
                            }
                        }
                    } else if(text.getTextNo() == 0) {
                        Log.d(TAG, "consumeMessage got zero text");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                setProgressBarIndeterminateVisibility(false);
                            }
                        });
                        finish();
                    } else {
                        Log.i(TAG, "consumeMessage Got text");
                        Log.i(TAG, "consumeMessage Got text:"+text.getTextNo());
/*                        runOnUiThread(new Runnable() {
                            public void run() {
                                setMessage(text);
                            }
                        });
*/
                        Message lmsg = new Message();
                        lmsg.obj = text;
                        lmsg.arg1 = 0;
                        lmsg.arg2 = 0;
                        lmsg.what = Consts.MESSAGE_TYPE_SET_TEXT;
                        mHandler.sendMessage(lmsg);
                        //Yield to the UI
/*                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            //e.printStackTrace();
                        }
*/
                        if (text.getTextNo() == lmsgarg1) {
                            Log.i(TAG,
                                    "consumeMessage ERROR trying to readmark visible text");
                        } else if (0 == lmsgarg1) {
                            Log.i(TAG,
                                    "consumeMessage ERROR trying to readmark 0");
                        } else {
                            Log.i(TAG,
                                    "consumeMessage visible text:"+text.getTextNo());
                            Log.i(TAG,
                                    "consumeMessage mark read text:"+lmsgarg1);
                            lmsg = new Message();
                            lmsg.obj = 0;
                            lmsg.arg1 = lmsgarg1;
                            lmsg.arg2 = lmsgarg2;
                            lmsg.what = Consts.MESSAGE_TYPE_MARKREAD;
                            mHandler.sendMessage(lmsg);
                        }
                        updateNumUnreads();

                        Message msgout = new Message();
                        msgout.what = Consts.MESSAGE_PREFETCH_NEXT;
                        //lmHandler.sendMessageDelayed(msgout, 300);
                        lmHandler.sendMessage(msgout);
                   }
                   Log.i(TAG, "consumeMessage MESSAGE_TYPE_NEXT bg thread done");
                }
            });
            Log.i(TAG, "consumeMessage Letting go to background");
            backgroundThread.setPriority(Thread.MAX_PRIORITY);
            backgroundThread.start();
            break;

        case Consts.MESSAGE_TYPE_SET_TEXT:
            Log.d(TAG, "consumeMessage MESSAGE_TYPE_SET_TEXT");
            setMessage((TextInfo) msg.obj);
            // check filters
            TextInfo current = mState.getCurrent();
            if (current != null) {
                int[] confNos = current.getConfNos();
                for (int confNo : confNos) {
                    if (mKom.containsSuperJumpFilter(confNo,
                            current.getSubject())) {
                        int currentText = current.getTextNo();
                        loadMessage(Consts.MESSAGE_TYPE_NEXT, currentText,
                                msg.arg2);
                    }
                }
            } else {
                Log.d(TAG,
                        "consumeMessage MESSAGE_TYPE_MARKREAD hasCurrent no current 1");
                finish();
            }
            Log.d(TAG, "consumeMessage MESSAGE_TYPE_SET_TEXT done");
            break;

        case Consts.MESSAGE_TYPE_MARKREAD:
            Log.d(TAG, "consumeMessage MESSAGE_TYPE_MARKREAD hasCurrent");
            if ((msg.arg2 == 0)
                    && (ConferencePrefs.getMarkTextRead(getBaseContext()))) {
                Log.d(TAG, "consumeMessage doInBackground getMarkTextRead:"
                        + msg.arg1);
                mKom.markTextAsRead(msg.arg1);
            }
            Log.d(TAG, "consumeMessage MESSAGE_TYPE_MARKREAD done");
            break;

        case Consts.MESSAGE_TYPE_ACTIVATEUSER:
            Log.d(TAG, "consumeMessage MESSAGE_TYPE_ACTIVATEUSER");
            mKom.activateUser();
            break;

        case Consts.MESSAGE_TYPE_UPDATENUMUNREADS:
            Log.d(TAG, "updateNumUnreads processing message");
            if(mHandler == null) {
                Log.d(TAG, "updateNumUnreads no handler!");
            }
            mKom.getConferenceUnreadsNo_msg(mHandler);
            break;

        case Consts.MESSAGE_TYPE_UPDATENUMUNREADS_GUI:
            Log.d(TAG, "updateNumUnreads GUI updating GUI : "+msg.arg1);
            // setTitle(mKom.getConferenceName()+":"+msg.arg1);  // not used since it might induce a delay
            runOnUiThread(new Runnable() {
                public void run() {
                    setTitle(getTitle()+":"+msg.arg1);  // may be ugly since it may add a number more than once
                }
            });
            break;
            
        case Consts.MESSAGE_TYPE_SEEORIGINALPOST1:
            textNo = msg.arg1;
            backgroundThread = new Thread(new Runnable() {
                public void run() {
                    if (textNo > 0) {
                        Log.i(TAG, "OP1: Trying to get parent text of "
                                + textNo);

                        TextInfo texto = mKom.getKomText(textNo);
                        if (texto != null) {
                            Message msgout = new Message();
                            msgout.obj = texto;
                            msgout.arg1 = textNo;
                            msgout.what = Consts.MESSAGE_TYPE_SEEORIGINALPOST2;
                            mHandler.sendMessage(msgout);
                        } else {
                            Log.d(TAG, "OP1: No text?");
                        }
                    } else {
                        Log.d(TAG, "OP1: FAIL");
                    }
                }
            });
            backgroundThread.start();
            break;

        case Consts.MESSAGE_TYPE_SEEORIGINALPOST2:
            text = (TextInfo) msg.obj;
            textNo = msg.arg1;
            backgroundThread = new Thread(new Runnable() {
                public void run() {
                    if ((text != null) && (text.getTextNo() > 0)) {
                        final TextInfo parentText = mKom.getParentToText(text
                                .getTextNo());
                        if ((parentText != null)
                                && (parentText.getTextNo() > 0)) {
                            Log.i(TAG, "Trying to get parent text of"
                                    + parentText.getTextNo());
                            int looplevel = msg.arg2;
                            looplevel++;
                            if (looplevel > 100) {
                                // Timeout
                                Log.d(TAG, "Stuck in loop or long thread");
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Message lmsg = new Message();
                                        lmsg.obj = 0;
                                        lmsg.arg1 = mState.getCurrent().getTextNo();
                                        lmsg.arg2 = 0;
                                        lmsg.what = Consts.MESSAGE_TYPE_MARKREAD;
                                        mHandler.sendMessage(lmsg);
                                        setMessage(parentText);
                                    }
                                });
                            } else {
                                Message msgout = new Message();
                                msgout.arg1 = text.getTextNo();
                                msgout.obj = parentText;
                                msgout.what = Consts.MESSAGE_TYPE_SEEORIGINALPOST2;
                                mHandler.sendMessage(msgout);
                            }
                        } else if ((text != null) && (text.getTextNo() > 0)) {
                            Log.i(TAG, "consumeMessage Got text");
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Message lmsg = new Message();
                                    lmsg.obj = 0;
                                    lmsg.arg1 = mState.getCurrent().getTextNo();
                                    lmsg.arg2 = 0;
                                    lmsg.what = Consts.MESSAGE_TYPE_MARKREAD;
                                    mHandler.sendMessage(lmsg);
                                    setMessage(text);
                                }
                            });
                        }
                    }
                }
            });
            backgroundThread.start();
            break;
            
        case Consts.MESSAGE_CONF_INIT:
            Log.d(TAG, "consumeMessage MESSAGE_CONF_INIT");

            if ((mKom != null) && (mState.textListIndex >= 0)) {
                Log.d(TAG, "consumeMessage MESSAGE_CONF_INIT textList");
                List<TextInfo> textList = mKom.getCurrentTextList();
                if (textList != null) {
                    textNo = textList.get(mState.textListIndex).getTextNo();
                    mState.textQueue = new ArrayBlockingQueue<Integer>(10);
                    mState.textQueue.offer(textNo);
                } else {
                    Log.d(TAG, "onResume textList is null");
                }
            }

            Log.d(TAG, "consumeMessage Pre Init Task");
            //new InitConnectionTask().execute();
            initConf();
            Log.d(TAG, "consumeMessage Post Init Task");
            break;

        case Consts.MESSAGE_UPDATE:
            Log.d(TAG, "consumeMessage MESSAGE_UPDATE Pre");
            textNo = mState.getCurrent().getTextNo();
            loadMessage(Consts.MESSAGE_TYPE_TEXTNO, textNo, 0);
            Log.d(TAG, "consumeMessage MESSAGE_UPDATE Post");
            break;
            
        default:
            Log.d(TAG, "consumeMessage ERROR unknown msg.what=" + msg.what);
            break;
        }
    }

    void setMessage(TextInfo text) {
        try {
            Log.d(TAG, "setMessage");
            // int curr = -1;
            // if (mState.hasCurrent()) {
            // curr = mState.getCurrent().getTextNo();
            // }
            if (text != null && text.getTextNo() < 0) {
                Toast.makeText(getApplicationContext(), text.getBody(),
                        Toast.LENGTH_SHORT).show();
                if (text.getTextNo() < -1) {
                    /* error fetching text, probably lost connection */
                    Log.d(TAG,
                            "setMessage error fetching text, probably lost connection");
                    mKom.logout();
                    finish();
                } else {
                    Log.d(TAG, "setMessage error fetching text, recoverable error?");
                }
            } else if (text != null) {
                Log.d(TAG, "setMessage got text");
                mState.currentText.push(text);
                mState.currentTextIndex = mState.currentText.size() - 1;
                // Log.i(TAG, stackAsString());

                // Log.d(TAG, "VHEADERS: "+text.getVisibleHeaders());
                // Log.d(TAG, "AHEADERS: "+text.getAllHeaders());
                // Log.d(TAG, "AUTHOR: "+text.getAuthor());
                // Log.d(TAG, "SUBJECT: "+text.getSubject());
                // Log.d(TAG, "BODY: "+text.getBody());
                if (text.getAllHeaders().contains("ContentType:image/")) {
                    Log.d(TAG, "setMessage image text");
                    final Spannable spannedHeader = text
                            .getSpannableHeaders();
                    addLinks(spannedHeader, digits);

                    TextView tview = getOtherHeadersView();
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
                    mSwitcher.showNext();
                } else {
                    Log.d(TAG, "setMessage show text");
                    final Spannable spannedHeader = text
                            .getSpannableHeaders();
                    addLinks(spannedHeader, digits);

                    TextView tview = getOtherHeadersView();
                    tview.setText(spannedHeader);

                    final Spannable spannedText = text.getSpannableBody();
                    addLinks(spannedText, digits);

                    tview = getOtherTextView();
                    tview.setText(spannedText);
                    resetOtherScroll();
                    setOtherTextSwitch();
                    // Switch conference if the new text is from another
                    // conference
                    final int confNo = mKom.getCurrentConference();
                    int[] confNos = text.getConfNos();
                    boolean foundConf = false;
                    for(int i : confNos) {
                        if(i == confNo) {
                            foundConf = true;
                            break;
                        }
                    }
                    if (!foundConf) {
                        try {
                            Log.d(TAG, "setMessage switching from "+confNo+" to "+ confNos[0]);
                            mKom.setConference(confNos[0]);
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    mSwitcher.showNext();
                }
                if (mState.textListIndex >= 0) {
                    setTitle("(återser)");
                } else {
                    setTitle(mKom.getConferenceName());
                }
           } else {
                Log.d(TAG, "setMessage error fetching text, probably lost connection");
                Log.d(TAG, "setMessage text=null");
                mKom.logout();
                finish();
            }
        } catch (Exception e) {
            Log.d(TAG, "setMessage PostExecute catched exception:" + e);
            e.printStackTrace();
            Log.d(TAG, "setMessage PostExecute bailing out");
            finish();
        }
        setProgressBarIndeterminateVisibility(false);
    }
    
    void loadMessage(int msgType, int textNo, int markTextAsReadint) {
        setProgressBarIndeterminateVisibility(true);

        Message msg = new Message();
        msg.obj = 1;
        msg.arg1 = textNo;
        msg.arg2 = markTextAsReadint;
        msg.what = msgType;
        mHandler.sendMessage(msg);
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
    public final boolean addLinks(Spannable s, Pattern p) {
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

    
    private void dumpWSizes(Window window) {
        Log.d(TAG, "dumpSizes ----------------------");
        Log.d(TAG, "dumpSizes height:" + window.getAttributes().height);
        Log.d(TAG, "dumpSizes width:" + window.getAttributes().width);
        Log.d(TAG, "dumpSizes x:" + window.getAttributes().x);
        Log.d(TAG, "dumpSizes y:" + window.getAttributes().y);
    }
    
    private void dumpChildrenSizes(Window window) {
        //       
    }
    
    private void dumpSizes() {
        Window currentW = this.getWindow();
        while(currentW != null) {
            dumpWSizes(currentW);
            currentW = currentW.getContainer();
        }
        dumpChildrenSizes(this.getWindow());
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
            dumpLog();
            Log.d(TAG, "onSingleTapConfirmed");
            if (!ConferencePrefs.getEnableTapToNext(context)) {
                Log.d(TAG, "Tap disabled");
                return false;
            }
            dumpSizes();
            Display display = getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            // TODO: Eh. Figure out how calculate our height properly (excluding optional buttons).
            int myLimit = mSwitcher.getBaseline() + mSwitcher.getBottom();
            Log.d(TAG, "onSingleTapUp myLimit:"+myLimit);
            Log.d(TAG, "onSingleTapUp width:"+width);
            Log.d(TAG, "onSingleTapUp height:"+height);
            Log.d(TAG, "onSingleTapUp Baseliune:"+mSwitcher.getBaseline());
            Log.d(TAG, "onSingleTapUp PaddingTop:"+mSwitcher.getPaddingTop());
            Log.d(TAG, "onSingleTapUp top:"+mSwitcher.getTop());
            Log.d(TAG, "onSingleTapUp ---");
            int childCount = mSwitcher.getChildCount();          
            //int foo = mSwitcher.getDisplayedChild();
            for (int foo = 0; foo < childCount; foo++) {
                View bar = mSwitcher.getChildAt(foo);
                Rect outRect = new Rect();
                bar.getHitRect(outRect);
                Log.d(TAG, "onSingleTapUp child top:" + outRect.top);
                Log.d(TAG, "onSingleTapUp child height:" + outRect.height());
                Log.d(TAG, "onSingleTapUp child baseline:" + bar.getBaseline());
                Log.d(TAG, "onSingleTapUp child top:" + bar.getTop());
                Log.d(TAG,
                        "onSingleTapUp child padding top:"
                                + bar.getPaddingTop());
                Log.d(TAG, "onSingleTapUp ---");
            }
            Log.d(TAG, "onSingleTapUp e.getRawY():"+e.getRawY());
            Log.d(TAG, "onSingleTapUp e.getRawX():"+e.getRawX());
            Log.d(TAG, "onSingleTapUp e.getY():"+e.getY());
            Log.d(TAG, "onSingleTapUp e.getDownTime():"+e.getDownTime());
            Log.d(TAG, "onSingleTapUp e.getDownTime():"+e.getEventTime());

            int topLimit = 0;
            if (android.os.Build.VERSION.SDK_INT > 10) {
                topLimit = height / 10;
            }
            
            if ((e.getRawY() > topLimit) && (e.getRawY() < myLimit)) {
                if (e.getRawX() > 0.6 * width) {
                    if (ConferencePrefs.getVibrateForTap(context)) {
                        Vibrator vibrator = (Vibrator) context
                                .getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(ConferencePrefs
                                .getVibrateTimeTap(context));
                    }
                    setProgressBarIndeterminateVisibility(true);
                    moveToNextText(true);
                    activateUser();
                    return true;
                }
                if (e.getRawX() < 0.4 * width) {
                    if (ConferencePrefs.getVibrateForTap(context)) {
                        Vibrator vibrator = (Vibrator) context
                                .getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(ConferencePrefs
                                .getVibrateTimeTap(context));
                    }
                    setProgressBarIndeterminateVisibility(true);
                    moveToPrevText();
                    activateUser();
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            Log.d(TAG, "onFling");
            try {
                // Horizontal swipes
                if (Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH) {
                    // right to left swipe
                    if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        setProgressBarIndeterminateVisibility(true);
                        moveToNextText(true);
                        return true;
                    } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        setProgressBarIndeterminateVisibility(true);
                        moveToPrevText();
                        activateUser();
                        return true;
                    }
                }

                // Vertical swipes
                if (Math.abs(e1.getX() - e2.getX()) <= SWIPE_MAX_OFF_PATH) {
                    // top to bottom swipe
                    if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        setProgressBarIndeterminateVisibility(true);
                        moveToParentText();
                        activateUser();
                        return true;
                    }
                }
                Log.d(TAG, "onFling not much movement:");
                Log.d(TAG, "onFling e1.getX():"+e1.getX());
                Log.d(TAG, "onFling e2.getX():"+e2.getX());
                Log.d(TAG, "onFling e1.getY():"+e1.getY());
                Log.d(TAG, "onFling e2.getY():"+e2.getY());
                Log.d(TAG, "onFling velocityX:"+velocityX);
                Log.d(TAG, "onFling velocityY:"+velocityY);
                long downtime = e2.getDownTime()-e1.getDownTime(); 
                double distance = Math.sqrt(e1.getX()*e1.getX()+e2.getX()*e2.getX());
                if((downtime<20) && (distance < 20)) {
                    Log.d(TAG, "onFling trying tap instead");
                    onSingleTapConfirmed(e2);
                } else {
                    Log.d(TAG, "onFling too long downtime "+downtime);
                }
            } catch (Exception e) {
                // nothing
            }
            Log.d(TAG, "onFling done");
            return false;
        }
    }

    private void moveToPrevText() {
        Message lmsg = new Message();
        
        if(mState != null) {
            TextInfo current = mState.getCurrent();
            if(current != null) {
                lmsg.obj = 0;
                lmsg.arg1 = current.getTextNo();
                lmsg.arg2 = 0;
                lmsg.what = Consts.MESSAGE_TYPE_MARKREAD;
                mHandler.sendMessage(lmsg);
            } else {
                Log.d(TAG, "moveToPrevText got current == null");
            }
        } else {
            Log.d(TAG, "moveToPrevText got mState == null");
        }

        if (mState.textListIndex >= 0) {
            moveToPrevListText();
        } else {
            moveToPrevUnreadText();
        }
    }

    private void moveToPrevListText() {
        List<TextInfo> textList = mKom.getCurrentTextList();
        if(mState.textListIndex>0) {
            mState.textListIndex--;
            int textNo = textList.get(mState.textListIndex).getTextNo();
            moveToText(textNo);
        } else {
            Log.d(TAG, "moveToPrevListText already at start of list");
        }
    }

    private void moveToPrevUnreadText() {
        Log.i(TAG, "moving to prev text, cur: " + (mState.currentTextIndex - 1)
                + "/" + mState.currentText.size());

        if (mState.currentTextIndex > 0) {
            mState.currentTextIndex--;
            Log.i(TAG, stackAsString());
            mSwitcher.setInAnimation(mSlideRightIn);
            mSwitcher.setOutAnimation(mSlideRightOut);

            TextInfo text = mState.currentText
                    .elementAt(mState.currentTextIndex);
            setTextInView(text);
        } else {
            Toast.makeText(getApplicationContext(), "No previous text",
                    Toast.LENGTH_SHORT).show();
        }
        setProgressBarIndeterminateVisibility(false);
    }

    public void activateUser() {
        Message msg = new Message();
        msg.what = Consts.MESSAGE_TYPE_ACTIVATEUSER;
        mHandler.sendMessage(msg);
    }

    public void updateNumUnreads() {
        Log.d(TAG, "updateNumUnreads sending message");
        Message msg = new Message();
        msg.what = Consts.MESSAGE_TYPE_UPDATENUMUNREADS;
        mHandler.sendMessage(msg);
    }
    
    private void moveToNextText(boolean markTextAsRead) {
        if(mState.textListIndex >= 0) {
            moveToNextListText(markTextAsRead);
        } else {
            moveToNextUnreadText(markTextAsRead);
        }
    }
    
    private void moveToNextListText(boolean markTextAsRead) {
        List<TextInfo> textList = mKom.getCurrentTextList();
        if((mState.textListIndex+1)<textList.size()) {
            mState.textListIndex++;
            int textNo = textList.get(mState.textListIndex).getTextNo();
            moveToText(textNo);
        } else {
            Log.d(TAG, "moveToNextListText already at end of list");
        }
    }

    private void setTextInView(TextInfo text) {
        TextView tview = getOtherHeadersView();
        if (text.getAllHeaders().contains("ContentType:image/")) {
            Log.d(TAG, "setTextInView image text");
            final Spannable spannedHeader = text.getSpannableHeaders();
            addLinks(spannedHeader, digits);

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
    
    private void moveToNextUnreadText(boolean markTextAsRead) {
        Log.i(TAG, "moving to next text cur:" + mState.currentTextIndex + "/"
                + mState.currentText.size());
        int markTextAsReadint = markTextAsRead ? 0 : 1;
        mSwitcher.setInAnimation(mSlideLeftIn);
        mSwitcher.setOutAnimation(mSlideLeftOut);

        if ((mState.currentTextIndex + 1) >= mState.currentText.size()) {
            // At end of list. load new text from server
            Log.i(TAG, "fetching new text");
            // if (android.os.Build.VERSION.SDK_INT > 12) {
            // new LoadMessageTask().executeOnExecutor(
            // AsyncTask.THREAD_POOL_EXECUTOR, MESSAGE_TYPE_NEXT, 0,
            // markTextAsReadint);
            int currentText = 0;
            TextInfo current = mState.getCurrent();
            if (current != null) {
                currentText = current.getTextNo();
            }
            Log.i(TAG, "moveToNextUnreadText currentText=" + currentText);
            loadMessage(Consts.MESSAGE_TYPE_NEXT, currentText,
                    markTextAsReadint);
        } else {
            Log.i(TAG, "Moving in old fetched text");
            // Display old text, already fetched.
            mState.currentTextIndex++;
            Log.i(TAG, stackAsString());

            setTextInView(mState.currentText.elementAt(mState.currentTextIndex));
            setProgressBarIndeterminateVisibility(false);
        }
    }

    private void moveToText(final int textNo) {
        Log.i(TAG, "fetching text " + textNo);
        mSwitcher.setInAnimation(mSlideLeftIn);
        mSwitcher.setOutAnimation(mSlideLeftOut);
        loadMessage(Consts.MESSAGE_TYPE_TEXTNO, textNo, 0);
    }

    private void moveToParentText() {
        int current = mState.getCurrent().getTextNo();
        Log.i(TAG, "fetching parent to text " + current);

        mSwitcher.setInAnimation(mSlideLeftIn);
        mSwitcher.setOutAnimation(mSlideLeftOut);
        //if(android.os.Build.VERSION.SDK_INT > 12) {
            //new LoadMessageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,  MESSAGE_TYPE_PARENT_TO, current, 0);
        loadMessage(Consts.MESSAGE_TYPE_PARENT_TO, current, 0);
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
			return true;
		case android.view.KeyEvent.KEYCODE_Q:
		case 4: // back in emulator
			finish();
			return true;
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
     * 
     * TODO: deprecated method!
     */
    @Override
    public Object onRetainNonConfigurationInstance() 
    {
        Log.d(TAG, "onRetainNonConfigurationInstance");
        return mState;
    }

    private Bitmap getPic() {
        Bitmap bitmap = null;
        String mCurrentPhotoPath = "dummy";//cameraTempFilename;

        File checkExistFile = new File(mCurrentPhotoPath);
        if (checkExistFile.exists()) {
            // Get the dimensions of the View
            int targetW = (int) Math.sqrt(ConferencePrefs
                    .getMaxImageSizePix(this)) * 4;
            int targetH = (int) Math.sqrt(ConferencePrefs
                    .getMaxImageSizePix(this)) * 4;

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        } else {
            Log.d(TAG, "getPic File does not exist:" + checkExistFile);
        }
        return bitmap;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");

        final Intent img_intent = new Intent(Conference.this,
                ImgTextCreator.class);
        
        if ((requestCode == CAMERA_REQUEST) || (requestCode == IMG_REQUEST)) {
            if (mState == null) {
                Toast.makeText(this,
                        "Internal error, try again. (onActivityResult 1)",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "onActivityResult error. Bailing out 1");
                return;
            }
            TextInfo currentText = mState.getCurrent();
            if (currentText == null) {
                Toast.makeText(this,
                        "Internal error, try again. (onActivityResult 2)",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "onActivityResult error. Bailing out 2");
                return;
            }

            img_intent.putExtra(TextCreator.INTENT_SUBJECT,
                    currentText.getSubject());
            img_intent.putExtra(TextCreator.INTENT_REPLY_TO,
                    currentText.getTextNo());
        }

        switch (requestCode) {
        case CAMERA_REQUEST:
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult got Camera result OK");
                Log.d(TAG, "onActivityResult camera URI:"+mState.cameraTempFilename);
                Bitmap bitmap = null;
                img_intent.putExtra("bild_uri", mState.cameraTempFilename);
                img_intent.putExtra("BitmapImage", bitmap);
                startActivity(img_intent);
            } else {
                Log.d(TAG, "onActivityResult got Camera result cancel");
                Toast.makeText(this, getString(R.string.camera_canceled),
                        Toast.LENGTH_SHORT).show();
            }
            break;
        case IMG_REQUEST:
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult got Gallery result OK");
                Bitmap bitmap = null;
                img_intent.putExtra("bild_uri", data.getData().toString());
                img_intent.putExtra("BitmapImage", bitmap);
                startActivity(img_intent);
            } else {
                Log.d(TAG, "onActivityResult got Gallery result Cancel");
                Toast.makeText(this, getString(R.string.gallery_canceled),
                        Toast.LENGTH_SHORT).show();
            }
            break;
        default:
            Log.d(TAG, "onActivityResult got unknown requestCode:"
                    + requestCode);
        }
        Log.d(TAG, "onActivityResult done");
    }

    @SuppressWarnings("null")
    private String getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {

            storageDir = new File(
                    Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Androkom");

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d(TAG, "getAlmbumDir failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name),
                    "External storage is not mounted READ/WRITE.");
        }

        if(storageDir != null) {
            return storageDir.toString();
        }
        return null;
    }
    
    private void doCamReply() {
        Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                final String EXPORT_DIR_NAME = getAlbumDir(); 
                final String EXPORT_FILE_NAME = EXPORT_DIR_NAME + File.separatorChar+"img.png";
                File tempFile = null;
                tempFile = new File(EXPORT_DIR_NAME);
                tempFile.mkdirs();
                tempFile = new File(EXPORT_FILE_NAME);
                Log.d(TAG, "doCamReply() tempfile = " + tempFile.toString());
                Uri cameraTempFileUri = Uri.fromFile(tempFile);
                mState.cameraTempFilename = cameraTempFileUri.toString();
                Intent intent = new Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraTempFileUri);
                startActivityForResult(intent, CAMERA_REQUEST);
                Message msg = new Message();
                msg.obj = 0;
                msg.arg1 = mState.getCurrent().getTextNo();
                msg.arg2 = 0;
                msg.what = Consts.MESSAGE_TYPE_MARKREAD;
                mHandler.sendMessage(msg);
            }
        });
        backgroundThread.start();
    }

    /**
     * Called when user has selected a menu item from the 
     * menu button popup. 
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	TextView t1 = null;
        Intent intent = null;
        Message msg = null;
    	
    	Log.d(TAG, "onOptionsItemSelected");

    	dumpLog();

        activateUser();
    	
        // Handle item selection
        switch (item.getItemId()) {

        /*
         * A reply to the current text was requested, so show a 
         * CreateText activity. 
         */
        case R.id.reply:
            intent = new Intent(this, TextCreator.class);
            intent.putExtra(TextCreator.INTENT_SUBJECT, mState.getCurrent().getSubject());
            intent.putExtra(TextCreator.INTENT_REPLY_TO, mState.getCurrent().getTextNo());
            startActivity(intent);
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
            return true;

        case R.id.img_reply:
            intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, IMG_REQUEST);
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
            return true;

        case R.id.cam_reply:
            doCamReply();
            return true;


		case R.id.menu_settings_id :
			Log.d(TAG, "Starting menu");
			startActivity(new Intent(this, ConferencePrefs.class));
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
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
			return true;

		case R.id.menu_unmarktext_id:
            unmarkCurrentText();
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
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
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
            return true;

        case R.id.menu_sub_recipient_id:
            subRecipient();
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
            return true;

        case R.id.menu_add_comment_id:
            addComment();
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
            return true;

        case R.id.menu_sub_comment_id:
            subComment();
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
            return true;

        case R.id.menu_gilla_id:
            gilla_current_text();
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
            return true;

        case R.id.menu_next_no_readmark_id:
            moveToNextText(false);
            return true;

        case R.id.menu_sharetext_id:
            shareIt();
            msg = new Message();
            msg.obj = 0;
            msg.arg1 = mState.getCurrent().getTextNo();
            msg.arg2 = 0;
            msg.what = Consts.MESSAGE_TYPE_MARKREAD;
            mHandler.sendMessage(msg);
            return true;

        case R.id.menu_speaktext_id:
            speakText();
            return true;
        case R.id.menu_showimage_id:
            showImage();
            return true;
        case R.id.menu_superhoppa_id:
            superJump();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showImage() {
        File tempFile = null;
        FileOutputStream fos = null;
        File sdDir = Environment.getExternalStorageDirectory();
        final String EXPORT_DIR_NAME = sdDir.getAbsolutePath()
                + "/Android/data/org.lindev.androkom/files";
        final String EXPORT_FILE_NAME = EXPORT_DIR_NAME + "/img.png";
        Log.d(TAG, "TEMP IMAGE: " + EXPORT_FILE_NAME);
        try {
            tempFile = new File(EXPORT_DIR_NAME);
            tempFile.mkdirs();
            tempFile = new File(EXPORT_FILE_NAME);
            tempFile.createNewFile();
            fos = new FileOutputStream(tempFile);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return;
        }
        BufferedOutputStream out = new BufferedOutputStream(fos);

        TextInfo text = mState.getCurrent();
        if (text.getAllHeaders().contains("ContentType:image/")) {
            Log.d(TAG, "Text contains image");
            
            byte[] bilden = text.getRawBody();
            Bitmap bmImg = BitmapFactory.decodeByteArray(bilden, 0,
                    bilden.length);
            if (bmImg != null) {
                bmImg.compress(Bitmap.CompressFormat.PNG, 100, out);
                try {
                    out.flush();
                    out.close();
                    
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(new File(EXPORT_FILE_NAME)), "image/png");
                    startActivity(intent);
                } catch (IOException e) {
                    Log.d(TAG, "IOException "+e);
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.image_decode_failed),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void superJump() {
        TextInfo currentText = mState.getCurrent();      
        mKom.addSuperJumpFilter(mState.conferenceNo, currentText.getSubject());
        loadMessage(Consts.MESSAGE_TYPE_NEXT, currentText.getTextNo(), 0);
    }
    
    private void gilla_current_text() {
        int CurrentText = mState.getCurrent().getTextNo();        
        try {
            mKom.addAuxItem(CurrentText, AuxItem.tagFastReply, "Gilla");
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private class markCurrentTextTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        // worker thread (separate from UI thread)
        @Override
        protected Integer doInBackground(final Void... args) {
            if (mKom == null) {
                return null;
            }
            if (mState == null) {
                return null;
            }

            try {
                TextInfo current = mState.getCurrent();
                if (current != null) {
                    int CurrentTextNo = current.getTextNo();
                    try {
                        mKom.markText(CurrentTextNo);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } catch (RpcFailure e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "markCurrentTextTask RpcFailure");
                // e.printStackTrace();
            }

            Log.d(TAG, "markCurrentTextTask doInBackground Done");
            return 0;
        }

        @Override
        protected void onPostExecute(final Integer value) {
            setProgressBarIndeterminateVisibility(false);
            Log.d(TAG, "markCurrentTextTask Done");
        }
    }

    protected void markCurrentText() {
        new markCurrentTextTask().execute();
    }
    
    protected void unmarkCurrentText() {
        TextInfo currentText = mState.getCurrent();
        if (currentText != null) {
            int CurrentTextNo = currentText.getTextNo();
            try {
                mKom.unmarkText(CurrentTextNo);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Failed to unmark due to null");
        }
    }

    protected void seetextagain() {
    	showDialog(DIALOG_NUMBER_ENTRY);
    }

    protected void seeoriginalpost() {
        setProgressBarIndeterminateVisibility(true);
        
        Message msg = new Message();
        if (mState.hasCurrent()) {
            Log.d(TAG, "hasCurrent");
            TextInfo text = mState.getCurrent();
            int textNo = text.getTextNo();
            Log.d(TAG, "hasCurrent textno " + textNo);
            msg.arg1 = textNo;
            Log.d(TAG, "hasCurrent textno " + msg.arg1);
        } else
            return;
        msg.what = Consts.MESSAGE_TYPE_SEEORIGINALPOST1;
        mHandler.sendMessage(msg);
    }

    protected void seePresentationConf() {
        int textNo = mKom.getConferencePres();
        if (textNo > 0) {
            Log.i(TAG, "fetching text " + textNo);
            loadMessage(Consts.MESSAGE_TYPE_TEXTNO, textNo, 0);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.no_presentation_error),
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void seePresentationUser() {
        final TextInfo currentText = mState.getCurrent();
        int userNum = currentText.getAuthorNo();
        int textNo = 0;
        try {
            textNo = mKom.getUserPres(userNum);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (textNo > 0) {
            Log.i(TAG, "fetching text " + textNo);
            loadMessage(Consts.MESSAGE_TYPE_TEXTNO, textNo, 0);
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
        try {
            mKom.leaveConference(mState.conferenceNo);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
            for (int i = 0; i < allrecipts.length; i++) {
                vals[i] = "(unknown)";
                try {
                    vals[i] = mKom.fetchUsername(allrecipts[i]);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    Log.d(TAG, "subRecipient InterruptedException:"+e);
                }
            }
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
                            String result = "broken error";
                            try {
                                result = mKom.subRecipient(currentTextNo, selectedUser);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
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
            Log.d(TAG, "subRecipient RPcFailure:"+e);
            //e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

        alert.setPositiveButton(getString(R.string.alert_dialog_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String textvalue = input.getText().toString();
                        Log.i(TAG, "trying to parse " + textvalue);
                        int textNo = Integer.parseInt(textvalue);
                        Log.i(TAG, "fetching text " + textNo);

                        mSwitcher.setInAnimation(mSlideLeftIn);
                        mSwitcher.setOutAnimation(mSlideLeftOut);
                        loadMessage(Consts.MESSAGE_TYPE_TEXTNO, textNo, 0);
                    }
                });

        alert.setNegativeButton(getString(R.string.alert_dialog_cancel),
                new DialogInterface.OnClickListener() {
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
                            try {
                                if (item < commented.length) {
                                    result = mKom.subComment(currentTextNo,
                                            selectedComment);
                                } else {
                                    result = mKom.subComment(selectedComment,
                                            currentTextNo);
                                }
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
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
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void shareIt() {
        String shareBody = "Here is the share content body";
        TextInfo currentText = null;
        /*  Should we use share instead/also?
         * 
         * 
         * Intent sharingIntent = new
         * Intent(android.content.Intent.ACTION_SEND);
         * sharingIntent.setType("text/plain"); String shareSubject =
         * getString(R.string.shared_subject);
         * 
         * if (mState.textQueue == null || mState.textQueue.isEmpty()) { if
         * ((mState != null) && (mState.hasCurrent())) { currentText =
         * mState.getCurrent(); shareBody = currentText.getSpannableHeaders() +
         * "\n" + currentText.getSpannableBody();
         * sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
         * shareSubject);
         * sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
         * startActivity(Intent.createChooser(sharingIntent, "Share via")); } }
         */
        currentText = mState.getCurrent();
        shareBody = currentText.getSpannableHeaders() + "\n"
                + currentText.getSpannableBody();

        /*
         * New version of clipboard can probably handle images too:
         * 
         * if(android.os.Build.VERSION.SDK_INT >
         * android.os.Build.VERSION_CODES.ECLAIR_MR1) {
         * android.content.ClipboardManager clipboard =
         * (android.content.ClipboardManager)
         * getSystemService(Context.CLIPBOARD_SERVICE); android.content.ClipData
         * clip = android.content.ClipData.newPlainText("Copied Text",
         * stringYouExtracted); clipboard.setPrimaryClip(clip); } else {
         */
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (currentText.getAllHeaders().contains("ContentType:image/")) {
            Log.d(TAG, "shareit image text");
            byte[] bilden = currentText.getRawBody();
            Bitmap bmImg = BitmapFactory.decodeByteArray(bilden, 0,
                    bilden.length);
            if (bmImg != null) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.clipboard_cant_handle_image),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.image_decode_failed),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            clipboard.setText(shareBody);
        }
    }
    
    private void speakText() {
        String shareBody = "Here is the share content body";
        TextInfo currentText = null;

        currentText = mState.getCurrent();
        shareBody = currentText.getSpannableHeaders() + "\n"
                + currentText.getSpannableBody();

        if (tts != null) {
            tts.speak(shareBody, TextToSpeech.QUEUE_ADD, null);
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
        addLinks(spannedText, Pattern.compile("\\d{5,}"));
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
        }
        return (TextView) findViewById(R.id.flipper_text1_id);
    }

    /**
     * Assume switcher got two childs and return the other, currently not
     * visible, child.
     */
    public ImageView getOtherImgView() {
        int currentViewId = mSwitcher.getCurrentView().getId();

        if (currentViewId == R.id.scrollView1) {
            return (ImageView) findViewById(R.id.flipper_imageView2);
        }
        return (ImageView) findViewById(R.id.flipper_imageView1);
    }

    /**
     * Get the visible TextView child
     * 
     */
    public TextView getCurrentTextView() {
        int currentViewId = mSwitcher.getCurrentView().getId();

        if (currentViewId == R.id.scrollView1) {
            return (TextView) findViewById(R.id.flipper_text1_id);
        }
        return (TextView) findViewById(R.id.flipper_text2_id);
    }

    /**
     * Update theme settings like colours
     * @param view 
     * 
     */
    public void updateTheme(View view) {
        Log.d(TAG, "updateTheme");

        int bgCol = Color.parseColor("black");
        int fgCol = Color.parseColor("white");
        int linkCol = Color.parseColor("blue");
        String bgColString = null;
        String fgColString = null;
        String linkColString = null;

        try {
            bgColString = ConferencePrefs.getBGColour(getBaseContext()).trim();
            bgCol = Color.parseColor(bgColString);
        } catch (IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.illegal_colour)+bgColString, Toast.LENGTH_SHORT)
                    .show();
        }
            
        try {
            fgColString = ConferencePrefs.getFGColour(getBaseContext()).trim();
            fgCol = Color.parseColor(fgColString);
        } catch (IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.illegal_colour)+fgColString, Toast.LENGTH_SHORT)
                    .show();
        }
        
        try {
            linkColString = ConferencePrefs.getLinkColour(getBaseContext()).trim();
            linkCol = Color.parseColor(linkColString);
        } catch (IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.illegal_colour)+linkColString, Toast.LENGTH_SHORT)
                    .show();
        }
        
        /*Log.d(TAG, "updateTheme bgCols="+bgColString);
        Log.d(TAG, "updateTheme fgCols="+fgColString);
        Log.d(TAG, "updateTheme bgCol="+bgCol);
        Log.d(TAG, "updateTheme fgCol="+fgCol);*/
        
        ViewAnimator va = (ViewAnimator)findViewById(R.id.flipper_id);
        va.setBackgroundColor(bgCol);
        
        TextView text1h = (TextView)findViewById(R.id.flipper_headers1_id);
        TextView text1 = (TextView)findViewById(R.id.flipper_text1_id);
        TextView text2h = (TextView)findViewById(R.id.flipper_headers2_id);
        TextView text2 = (TextView)findViewById(R.id.flipper_text2_id);
        
        text1h.setBackgroundColor(bgCol);
        text2h.setBackgroundColor(bgCol);
        text1.setBackgroundColor(bgCol);
        text2.setBackgroundColor(bgCol);
        
        text1h.setLinkTextColor(linkCol);
        text2h.setLinkTextColor(linkCol);
        text1.setLinkTextColor(linkCol);
        text2.setLinkTextColor(linkCol);

        text1h.setTextColor(fgCol);
        text2h.setTextColor(fgCol);
        text1.setTextColor(fgCol);
        text2.setTextColor(fgCol);
        
        float fontSize = ConferencePrefs.getFontSize(getBaseContext());
        text1h.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        text2h.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        text1.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        text2.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        Log.d(TAG, "finished updateTheme");
   }
    
    /**
     * Set switch to show image
     * 
     */
    public void setOtherImgSwitch() {
        Log.d(TAG, "setOtherImgSwitch");
        int currentViewId = mSwitcher.getCurrentView().getId();
        ViewSwitcher switcher=null;
        
        if (currentViewId == R.id.scrollView1) {
            switcher = (ViewSwitcher) findViewById(R.id.profile2Switcher);
        } else {
            switcher = (ViewSwitcher) findViewById(R.id.profile1Switcher);
        }
        switcher.setDisplayedChild(1); // see order in XML
        
        updateTheme(switcher.getChildAt(1));
        Log.d(TAG, "finished setOtherImgSwitch");
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
        updateTheme(switcher.getChildAt(0));
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
        //super.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        if ((re_userId > 0) && (re_userPSW != null)
                && (re_userPSW.length() > 0)) {
            outState.putInt("UserId", re_userId);
            outState.putString("UserPSW", re_userPSW);
            outState.putString("UserServer", re_server);
            outState.putInt("UserServerPortNo", re_port);
            outState.putBoolean("UserUseSSL", re_useSSL);
            outState.putInt("UserCertLevel", re_cert_level);
            Log.d(TAG, "onSaveInstanceState, stored state with local id");
        } else {
            if (mKom != null) {
                int userId = mKom.getUserId();
                if (userId > 0) {
                    Log.d(TAG, "Store userid:" + userId);
                    outState.putInt("UserId", userId);
                    outState.putString("UserPSW", mKom.getUserPassword());
                    outState.putString("UserServer", mKom.getServer());
                    outState.putInt("UserServerPortNo", mKom.getServerPortNo());
                    outState.putBoolean("UserUseSSL", mKom.getUseSSL());
                    outState.putInt("UserCertLevel", mKom.getCertLevel());
                    Log.d(TAG, "onSaveInstanceState, stored state with mKom id");
                } else {
                    Log.d(TAG, "onSaveInstanceState: No userid, bailing out");
                }
            } else {
                Log.d(TAG, "onSaveInstanceState: No mKom, bailing out");
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
            re_port = savedInstanceState.getInt("UserServerPortNo");
            re_useSSL = savedInstanceState.getBoolean("UserUseSSL");
            re_cert_level = savedInstanceState.getInt("UserCertLevel");
            if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&mKom!=null) {
                mKom.setUser(re_userId, re_userPSW, re_server, re_port, re_useSSL, re_cert_level);
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
    
    public void asyncMessage(final Message msg) {
        Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                asyncMessageBG(msg);
            }
        });
        backgroundThread.start();
    }
    
    public void asyncMessageBG(Message msg) {
        int currentTextNo = -1;

        // TODO: Much of this should probably be done for any text in cache and no matter
        // what activity happens to be visible
        
        Log.d(TAG, "asyncMessage received");

        if (mState != null) {
            TextInfo currentText = mState.getCurrent();
            if ((currentText != null) && (currentText.getTextNo() > 0)) {
                currentTextNo = currentText.getTextNo();
                Log.d(TAG, "asyncMessage currentTextNo=" + currentTextNo);
            }
        }
        switch (msg.what) {
        case nu.dll.lyskom.Asynch.new_recipient:
            if (msg.arg1 == currentTextNo) {
                Log.d(TAG, "New recipient added, update view");
                mKom.updateText(currentTextNo);
                Message rmsg = new Message();
                rmsg.what = Consts.MESSAGE_UPDATE;
                mHandler.sendMessage(rmsg);
            } else {
                Log.d(TAG, "New recipient added, NOT update view");
            }
            break;
        case nu.dll.lyskom.Asynch.sub_recipient:
            if (msg.arg1 == currentTextNo) {
                Log.d(TAG, "Recipient removed, update view");
                mKom.updateText(currentTextNo);
                Message rmsg = new Message();
                rmsg.what = Consts.MESSAGE_UPDATE;
                mHandler.sendMessage(rmsg);
            } else {
                Log.d(TAG, "Recipient removed, NOT update view");
            }
            break;
        case nu.dll.lyskom.Asynch.text_aux_changed:
            if (msg.arg1 == currentTextNo) {
                Log.d(TAG, "Aux changed, update view");
                mKom.updateText(currentTextNo);
                Message rmsg = new Message();
                rmsg.what = Consts.MESSAGE_UPDATE;
                mHandler.sendMessage(rmsg);
            } else {
                Log.d(TAG, "Aux changed, NOT update view");
            }
            break;
        case nu.dll.lyskom.Asynch.leave_conf:
            if (msg.arg1 == mState.conferenceNo) {
                Log.d(TAG, "Not a member anymore, leaving");
                finish();
            } else {
                Log.d(TAG, "Not a member anymore, NOT leaving" + msg.arg1 + " "
                        + mState.conferenceNo);
            }
            break;
        case nu.dll.lyskom.Asynch.deleted_text:
            if (msg.arg1 == currentTextNo) {
                Log.d(TAG, "Text deleted, leaving");
                finish();
            } else {
                Log.d(TAG, "Text deleted, NOT leaving");
            }
            break;
        case nu.dll.lyskom.Asynch.new_text:
            Log.d(TAG, "New text #" + msg.arg1);

            Text text = null;
            try {
                text = mKom.getTextbyNo(msg.arg1);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                text = null;
            }
            if (text != null) {
                boolean reloadCurrent = false;
                for (int commented : text.getCommented()) {
                    if (commented == currentTextNo) {
                        reloadCurrent = true;
                    }
                }
                for (int comment : text.getComments()) {
                    if (comment == currentTextNo) {
                        reloadCurrent = true;
                    }
                }
                if (reloadCurrent) {
                    Log.d(TAG, "Current text changed, update view");
                    mKom.updateText(currentTextNo);
                    Message rmsg = new Message();
                    rmsg.what = Consts.MESSAGE_UPDATE;
                    mHandler.sendMessage(rmsg);
                } else {
                    Log.d(TAG, "New text is not related to current text");
                }
            } else {
                Log.d(TAG, "Could not get text");
            }
            break;
        case nu.dll.lyskom.Asynch.login:
            //ignore
            break;
        case nu.dll.lyskom.Asynch.logout:
            //ignore
            break;
        case nu.dll.lyskom.Asynch.sync_db:
            //ignore
            break;
        default:
            Log.d(TAG, "Unknown async message received#" + msg.what);
        }
    }
    
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected start");
        mKom = ((LocalBinder<KomServer>) service).getService();
        mKom.addAsyncSubscriber(this);
        mKom.setShowHeadersLevel(mState.ShowHeadersLevel);
        if(mKom == null) {
            Log.d(TAG, "onServiceConnected Failed to get mKom service");
        } else {
            Log.d(TAG, "onServiceConnected Succeeded to get mKom service");            
        }
        if((re_userId>0)&&(re_userPSW!=null)&&(re_userPSW.length()>0)&&mKom!=null) {
            Log.d(TAG, "onServiceConnected Setting user id from local variables");
            mKom.setUser(re_userId, re_userPSW, re_server, re_port, re_useSSL, re_cert_level);
            if(!mKom.isConnected()) {
                Log.d(TAG, "onServiceConnected Trying to reconnect");
                mKom.reconnect();
            }
        } else {
            if(mKom==null) {
                Log.d(TAG, "onServiceConnected  mKom == null");
            } else {
                Log.d(TAG, "onServiceConnected Missing user id in local variables, trying to restore from service");
                re_userId = mKom.getUserId();
                re_userPSW = mKom.getUserPassword();
                re_server = mKom.getServer();
                re_port = mKom.getServerPortNo();
                re_useSSL = mKom.getUseSSL();
                re_cert_level = mKom.getCertLevel();
                
                setTitle(mKom.getConferenceName());
            }
            if(re_userId<1){
                Log.d(TAG, "no userId");
            } else {
                Log.d(TAG, "got userId:"+re_userId);
            }
            if(re_userPSW==null){
                Log.d(TAG, "null password");
            } else {
                if(re_userPSW.length()<1){
                    Log.d(TAG, "short password");
                }
            }
        }

        Message msg = new Message();
        msg.what = Consts.MESSAGE_CONF_INIT;
        mHandler.sendMessage(msg);

        Log.d(TAG, "onServiceConnected done");
    }

    protected void initConf() {
        {
            Log.d(TAG, "InitConf");

            if (mKom == null) {
                return;
            }

            try {
                Log.d(TAG, "InitConnectionTask setConf 1");
                int oldConf = mKom.getCurrentConference();
                mKom.setConference(mState.conferenceNo);
                if(oldConf != mState.conferenceNo) {
                    Log.d(TAG, "Restart prefetcher");
                    mKom.restartPrefetcher();
                } else {
                    Log.d(TAG, "Start prefetcher");
                    mKom.startPrefetcher();
                }
                Log.d(TAG, "InitConnectionTask setConf 2");
            } catch (RpcFailure e) {
                // TODO Auto-generated catch block
                Log.d(TAG, "InitConnectionTask RpcFailure");
                // e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "InitConnectionTask IOException:" + e);
                e.printStackTrace();
                try {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.connection_lost),
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e2) {
                    Log.d(TAG,
                            "InitConnectionTask IOException failed to toast:"
                                    + e2);
                }
                Thread backgroundThread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            mKom.logout();
                        } catch (InterruptedException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                });
                backgroundThread.start();
                finish();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Log.d(TAG, "InitConnectionTask set Conference Done");

            TextInfo currentText = null;

            if (mState.textQueue == null || mState.textQueue.isEmpty()) {
                if ((mState != null) && (mState.hasCurrent())) {
                    currentText = mState.getCurrent();
                }
                mSwitcher.setInAnimation(mSlideLeftIn);
                mSwitcher.setOutAnimation(mSlideLeftOut);
                if (currentText != null) {
                    Log.d(TAG, "InitConnectionTask Getting current text");
                    loadMessage(Consts.MESSAGE_TYPE_TEXTNO,
                            currentText.getTextNo(), 0);
                } else {
                    Log.d(TAG, "InitConnectionTask Getting next text");
                    loadMessage(Consts.MESSAGE_TYPE_NEXT, 0, 0);
                }
            } else {
                Log.d(TAG, "InitConnectionTask Getting text from queue");
                loadMessage(Consts.MESSAGE_TYPE_TEXTNO,
                        mState.textQueue.poll(), 0);
            }

        }
        Log.d(TAG, "InitConf done");
    }

    public void onServiceDisconnected(ComponentName name) {
		mKom = null;		
	}

    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            if (tts != null) {
                int result = tts.setLanguage(Locale.getDefault());

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "onInit: This Language is not supported");
                } else {
                    Log.d(TAG, "onInit: TTS initialized OK");
                }
            } else {
                Log.d(TAG, "onInit: no TTS found");
            }
        } else {
            Log.e(TAG, "onInit: Initilization Failed!");
        }
    }
    
    void dumpLog() {
        Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                mKom.dumpLog();
            }
        });
        backgroundThread.start();
    }
    
    App getApp() 
    {
        return (App)getApplication();
    }

    private class State {
        public int textListIndex = -1;
        public int conferenceNo;
        int currentTextIndex = -1;
        Stack<TextInfo> currentText;
        Queue<Integer> textQueue;

        boolean hasCurrent() {
            return currentTextIndex >= 0;
        }

        TextInfo getCurrent() {
            if (currentTextIndex >= 0 && currentTextIndex < currentText.size())
                return currentText.elementAt(currentTextIndex);
            return null;
        }

        int ShowHeadersLevel;
        
        private String cameraTempFilename = null;
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
    private int re_port=0; // for reconnect
    private boolean re_useSSL=true; // for reconnect
    private int re_cert_level=0; // for reconnect
    
    private static final int DIALOG_NUMBER_ENTRY = 7;
    
    private TextToSpeech tts=null;
    private static Handler mHandler=null;
    
    private static final int CAMERA_REQUEST = 1;
    private static final int IMG_REQUEST = 2;
}
