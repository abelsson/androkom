package org.lindev.androkom;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

public class imagesactivity extends Activity implements ServiceConnection {
    private static final boolean DEBUG_TITLE = true;
    
    private static final String TAG = "Androkom imagesactivity";
    ImageView imgView;
    
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!DEBUG_TITLE)
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.images);
                
        imgView = (ImageView) findViewById(R.id.imageView1);
        byte[] bilden = getIntent().getByteArrayExtra("bilden");
        bmImg = BitmapFactory.decodeByteArray(bilden, 0, bilden.length);
        if(bmImg != null) {
            imgView.setImageBitmap(bmImg);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.image_decode_failed), Toast.LENGTH_LONG).show();
        }
        
        imgView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                Log.d(TAG, "TOUCH!" + arg1.getX() + " y:" + arg1.getY());
                finish();
                return false;
            }
        });
        getApp().doBindService(this);
    }

    @Override
    protected void onDestroy() {
        getApp().doUnbindService(this);
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG_TITLE) {
            setTitle("Androkom image");
        }
    }
    
    @Override
    public void onRestart() {
        super.onRestart();
        if (DEBUG_TITLE) {
            setTitle("Androkom image");
        }        
    }
    
    App getApp() 
    {
        return (App)getApplication();
    }

    Bitmap bmImg;

    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected start");
        mKom = ((LocalBinder<KomServer>) service).getService();
    }

    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "onServiceDisconnected");
        mKom = null;        
    }
    
    KomServer mKom;   
}
