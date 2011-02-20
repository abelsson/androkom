package org.lindev.androkom;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Dialog to enter commands by keyboard.
 * 
 * @author jonas
 *
 */
public class XDialog extends Dialog 
{

	private static final String TAG = "Androkom XDialog";
	
	public XDialog(Context context) {
		super(context);

		myContext = context;
	}
	
	protected void 	onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.xdialog);
        mContent = (TextView) findViewById(R.id.message);
        Button positiveButton = (Button) findViewById(R.id.positiveButton);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { doCommand(); }
        });
        Button negativeButton = (Button) findViewById(R.id.negativeButton);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { dismiss(); }
        });

		setTitle("Lyskom:");
		keyBuffer="";
	}

	void doCommand() {
        Intent intent;
		switch(parse_string_for_command(keyBuffer)) {
		case 0 :
            intent = new Intent(myContext, CreateNewIM.class);    
            myContext.startActivity(intent);
            break;
		case 1 :
            intent = new Intent(myContext, CreateNewText.class);    
            intent.putExtra("recipient_type", 1);
            myContext.startActivity(intent);
            break;
		case 2 :
            intent = new Intent(myContext, CreateNewText.class);    
            intent.putExtra("recipient_type", 2);
            myContext.startActivity(intent);
            break;
		}
		dismiss();
	}
	
	protected void 	onStart() {
		//Log.d(TAG, "onStart");		
	}
	
	protected void 	onStop() {
		//Log.d(TAG, "onStop");		
	}
	
	void onBackPressed() {
		//Log.d(TAG, "back pressed");
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode) {
		case KeyEvent.KEYCODE_DEL :
		case KeyEvent.KEYCODE_BACK :
		case KeyEvent.KEYCODE_CLEAR :
		case KeyEvent.KEYCODE_SOFT_LEFT :
			if (keyBuffer.length()>0) {
				keyBuffer = (String) keyBuffer.subSequence(0, keyBuffer.length()-1);
			}
			break;
		case KeyEvent.KEYCODE_ENTER :
			doCommand();
			break;
		default :
			keyBuffer += event.getDisplayLabel();
		}

		int commandno = parse_string_for_command(keyBuffer);
		if (commandno != -1) {
			mContent.setText(keycommands[commandno]);
		} else {
			mContent.setText(keyBuffer);
		}
		return false;
	}
	
	private int parse_string_for_command(String keys) {
		if(keys.length()<1) {
			return -1;
		}
		for(int i=0; i<keycommands.length; i++) {
			if (keys.equalsIgnoreCase(
					keycommands[i].substring(0,
							min(keys.length(),
									keycommands[i].length())))) {
				return i;
			}
		}
		return -1;
	}
	
	private int min(int length, int length2) {
		if(length < length2)
			return length;
		return length2;
	}

	Context myContext;
	TextView mContent;
	String keyBuffer="";
	
	String[] keycommands = {"Sända meddelande",
			"Skriva ett inlägg",
			"Skicka brev"
			};
}
