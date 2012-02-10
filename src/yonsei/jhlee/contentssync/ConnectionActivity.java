package yonsei.jhlee.contentssync;

import android.app.Activity;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ConnectionActivity extends Activity{
	private static final int DISCONNECT_CODE = 2;
    IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
    SMSReceiver smsReceiver = new SMSReceiver();
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connection);
        TextView text_code = (TextView)findViewById(R.id.text1);
        Button button_disconnect = (Button)findViewById(R.id.button_disconnect);
        button_disconnect.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// TODO Auto-generated method stub
				setResult(DISCONNECT_CODE);
				unregisterReceiver(smsReceiver);
				finish();
			}
		});
        if (getIntent().getData()!=null){
        	String mCode = getIntent().getStringExtra("mCode");
        	text_code.setText(mCode);
        }
        registerReceiver(smsReceiver, filter);
    }
}
