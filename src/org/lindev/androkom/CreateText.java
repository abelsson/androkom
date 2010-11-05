package org.lindev.androkom;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Activity showing a dialog in which the user can create 
 * new texts. Currently assumes that each text is in reply 
 * to another text.
 * 
 * @author henrik
 *
 */
public class CreateText extends Activity 
{

    /**
     * Create activity. Just a plain old dialog with
     * a subject, body and cancel and post buttons.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.createtext);

        inReplyTo = (Integer) getIntent().getExtras().get("in-reply-to");
        String subject = (String)  getIntent().getExtras().get("subject-line");
        mSubject = (EditText) findViewById(R.id.subject);
        mBody = (EditText) findViewById(R.id.body);

        mSubject.setText(subject);
        
        Button confirmButton = (Button) findViewById(R.id.send);
        Button cancelButton = (Button) findViewById(R.id.cancel);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { createText(); }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { cancelText(); }
        });
    }

    /**
     * Request a new message be posted.
     */
    private void createText()
    {
        String subject = mSubject.getText().toString();
        String body = mBody.getText().toString();
        getApp().getKom().createText(subject, body, inReplyTo, true);
        finish();
    }

    /**
     * New text canceled. Just finish up and die.
     */
    private void cancelText()
    {
        finish();
    }

    App getApp() 
    {
        return (App)getApplication();
    }

    private int inReplyTo;
    private EditText mSubject;
    private EditText mBody;
}
