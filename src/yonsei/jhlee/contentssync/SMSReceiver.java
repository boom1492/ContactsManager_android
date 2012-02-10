package yonsei.jhlee.contentssync;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.gsm.SmsManager;
import android.telephony.gsm.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {
	private final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String mMessage = "";
		String mSender = "";
		
		String action = intent.getAction();
		Log.d("receiver", "onReceiver : "+ action);
		
		if(action.equals(ACTION_SMS_RECEIVED)){
			Bundle bundle = intent.getExtras();
			
			if(bundle!=null){
				Object[] pdus = (Object[])bundle.get("pdus");
				
				for(Object pdu:pdus){
					SmsMessage smsMessage = SmsMessage.createFromPdu((byte[])pdu);
					if(mSender.equals("")){
						mSender = smsMessage.getOriginatingAddress();
					}
					mMessage += smsMessage.getMessageBody();
					ContentsSyncActivity.receivedSMS(mSender, mMessage);
					
				}
			}
		}
	}

}
