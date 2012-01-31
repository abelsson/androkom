package org.lindev.androkom;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

public class imagesactivity extends Activity {
    private static final boolean DEBUG_TITLE = false;
    
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
            Toast.makeText(getApplicationContext(), "Failed to decode image", Toast.LENGTH_LONG).show();
        }
    }

    public void onStart() {
        super.onStart();
        if (DEBUG_TITLE) {
            setTitle("Androkom image");
        }
    }
    
    public void onRestart() {
        super.onRestart();
        if (DEBUG_TITLE) {
            setTitle("Androkom image");
        }        
    }
    
    Bitmap bmImg;
}
