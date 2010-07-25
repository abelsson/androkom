package org.lindev.androkom;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CreateText extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.createtext);
		
		inReplyTo = (Integer) getIntent().getExtras().get("in-reply-to");
		mSubject = (EditText) findViewById(R.id.subject);
		mBody = (EditText) findViewById(R.id.body);
		Button confirmButton = (Button) findViewById(R.id.send);
		Button cancelButton = (Button) findViewById(R.id.cancel);
		
		confirmButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) { sendMessage(); }
		});
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) { cancelMessage(); }
		});
	}
	
	private void sendMessage()
	{
		String subject = mSubject.getText().toString();
		String body = mBody.getText().toString();
		getApp().getKom().createText(subject, body, inReplyTo);
		finish();
	}
	
	private void cancelMessage()
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
