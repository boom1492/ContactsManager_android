package yonsei.jhlee.contentssync;

import java.net.URI;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.client.android.integration.IntentIntegrator;
import com.google.zxing.client.android.integration.IntentResult;

import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.SwitchboardConfig;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class ContentsSyncActivity extends Activity {
    private static final String mHost = "mobilesw.yonsei.ac.kr";
	private static final SwitchboardConfig mSbConfig = new XMPPSwitchboardConfig(mHost);
	private static final JunctionMaker mMaker = JunctionMaker.getInstance(mSbConfig);
	
	private static Junction jx;
	private static final int REQUEST_CODE = 0;
	private static final int DISCONNECT_CODE = 2;
	private static JunctionActor actor;
	
	public static void receivedSMS(String sender, String message){
		JSONObject msg = new JSONObject();
		try {
			msg.put("service", "receivedsms");
			msg.put("phoneNumber", sender);
			msg.put("message", message);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		actor.sendMessageToSession(msg);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode==REQUEST_CODE){
			if(resultCode==DISCONNECT_CODE){
				JSONObject msg = new JSONObject();
				try {
					msg.put("service", "disconnect");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				actor.sendMessageToSession(msg);
				actor.leave();
				jx.disconnect();
				
				//
				actor = null;
				jx = null;
				//
			}
		}else{
			IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
			Uri uri = Uri.parse(result.getContents());
			String mCode = uri.getQueryParameter("sessionId");
			MyTask mTask = new MyTask();
			mTask.execute(mCode);
		}
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        if (getIntent().getData() != null) {
			Uri data = getIntent().getData();
			String mCode = data.getQueryParameter("sessionId");
			MyTask mTask = new MyTask();
			mTask.execute(mCode);
        }
        Button button_qrscanner = (Button)findViewById(R.id.button_qrscanner);
        button_qrscanner.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// TODO Auto-generated method stub
				IntentIntegrator.initiateScan(ContentsSyncActivity.this);
			}
		});
        Button button_enter = (Button)findViewById(R.id.button_enter);
        final TextView text_code = (TextView)findViewById(R.id.text_code);
		final String mCode = text_code.getText().toString();
        button_enter.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub				
				MyTask mTask = new MyTask();
				mTask.execute(mCode);
			}
		});
    }
    public void editContact(String name, String phone, String email){
    
    }
    public JSONArray getContacts(){
    	Cursor cur = managedQuery(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
    	JSONArray mContactList = new JSONArray();
    	while(cur.moveToNext()){
    		String name;
    		String ContactID;
    		int nameColumn = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
    		int idColumn = cur.getColumnIndex(ContactsContract.Contacts._ID);
    		
    		name = cur.getString(nameColumn);
    		ContactID = cur.getString(idColumn);
    			
    		Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID+"="+ContactID, null, null);
    		while(phones.moveToNext()){
    	    	JSONObject mContact = new JSONObject();
    			String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
    			phoneNumber = phoneNumber.replace("-", "");
    			//System.out.println("name=" + name + "phone num = " + phoneNumber);
    			try {
					mContact.put("name", name);
					mContact.put("phone", phoneNumber);
					mContactList.put(mContact);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		phones.close();
    	}
//    	System.out.println(mContactList);
    	return mContactList;
    }
    class MyTask extends AsyncTask<String, Void, Boolean>{
		private ProgressDialog mDialog;
		@Override
		protected Boolean doInBackground(String... params) {
			// TODO Auto-generated method stub
			JSONArray contacts = getContacts();
			TelephonyManager mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String myPhoneNumber = mTelephonyManager.getLine1Number();
			JSONObject msg = new JSONObject();
			try {
				msg.put("service", "init");
				msg.put("contacts", contacts);
				msg.put("phoneNumber", myPhoneNumber);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			
			
			try {
				actor = new SyncActor();
				jx = mMaker.newJunction(URI.create("http://"+mHost+"/"+params[0]), actor);
			} catch (JunctionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			actor.sendMessageToSession(msg);
			return true;
		}
		@Override
		protected void onPreExecute() {
			if (mDialog == null) {
				mDialog = new ProgressDialog(ContentsSyncActivity.this);
				mDialog.setMessage("접속중입니다.");
				mDialog.setIndeterminate(true);
				mDialog.setCancelable(true);
				mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface arg0) {
						//actor.leave();
					}
				});
				mDialog.show();	
			}
		}

		protected void onPostExecute(Boolean bool) {
			mDialog.hide();
			if(bool){
				Toast.makeText(ContentsSyncActivity.this, "접속에 성공하였습니다.", Toast.LENGTH_SHORT).show();
				Intent intent = new Intent(ContentsSyncActivity.this, ConnectionActivity.class);
				startActivityForResult(intent, REQUEST_CODE);
			} else{
				Toast.makeText(ContentsSyncActivity.this, "접속에 실패하였습니다.", Toast.LENGTH_SHORT).show();
			}
		};
	};

    class SyncActor extends JunctionActor{
    	
    	public SyncActor(){
    		super("mobile");
    	}
    	
    	@Override
    	public void onMessageReceived(MessageHeader header, JSONObject message) {
    		// TODO Auto-generated method stub
    		try {
    			String service = message.getString("service");
    			
    			if(service.equals("initsms")){
    		        //debug code
    		        JSONArray SMSList = new JSONArray();
    		        Uri SMS_URI = Uri.parse("content://sms/");
    		        String phoneNumber = message.getString("phoneNumber");
    		        Cursor cursor = getContentResolver().query(SMS_URI, null, null, null, null);
    				
    				while(cursor.moveToNext()){
    					if(cursor.getString(cursor.getColumnIndex("address")).equals(phoneNumber)){

    						JSONObject SMSInstance = new JSONObject();
    						String address = cursor.getString(cursor.getColumnIndex("address"));
    						String body = cursor.getString(cursor.getColumnIndex("body"));
    						int type = cursor.getInt(cursor.getColumnIndex("type"));
    						long date = cursor.getLong(cursor.getColumnIndex("date"));
    						try {
    							SMSInstance.put("address", address);
    							SMSInstance.put("body", body);
    							SMSInstance.put("type", type);
    							SMSInstance.put("date", date);
    						} catch (JSONException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
    						SMSList.put(SMSInstance);
    					}
    				}
    				Log.d("sms", SMSList.toString());
    				
    		        JSONObject msg = new JSONObject();
    		        msg.put("service", "ackinitsms");
    		        msg.put("smslist", SMSList);
    		        sendMessageToSession(msg);
    				//
    			}
    			else if(service.equals("getsmslist")){
    				// 모든 sms목록을 불러온다.
    		        JSONArray SMSList = new JSONArray();
    		        Uri SMS_URI = Uri.parse("content://sms/");
    		        Cursor cursor = getContentResolver().query(SMS_URI, null, null, null, null);
    				
    				while(cursor.moveToNext()){
						JSONObject SMSInstance = new JSONObject();
						String address = cursor.getString(cursor.getColumnIndex("address"));
						String body = cursor.getString(cursor.getColumnIndex("body"));
						int type = cursor.getInt(cursor.getColumnIndex("type"));
						long date = cursor.getLong(cursor.getColumnIndex("date"));
						try {
							SMSInstance.put("address", address);
							SMSInstance.put("body", body);
							SMSInstance.put("type", type);
							SMSInstance.put("date", date);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						SMSList.put(SMSInstance);
    				}
    				Log.d("sms", SMSList.toString());
    				
    		        JSONObject msg = new JSONObject();
    		        msg.put("service", "ackgetsmslist");
    		        msg.put("smslist", SMSList);
    		        sendMessageToSession(msg);
    			}
    			else if(service.equals("sendsms")){
    				// sms를 보냄
    				String phoneNumber = message.getString("phoneNumber");
    				String mMessage = message.getString("message");
    				PendingIntent sendPI = PendingIntent.getBroadcast(ContentsSyncActivity.this, 0, new Intent("SMS_SENT"), 0);
    				PendingIntent deliveredPI = PendingIntent.getBroadcast(ContentsSyncActivity.this, 0, new Intent("SMS_DELIVERED"), 0);
    				
    				registerReceiver(new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							// TODO Auto-generated method stub
							switch(getResultCode()){
							case Activity.RESULT_OK:
								Toast.makeText(ContentsSyncActivity.this, "SMS Sent", Toast.LENGTH_LONG).show();
								break;
							case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
								Toast.makeText(ContentsSyncActivity.this, "Generic failure", Toast.LENGTH_LONG).show();
								break;
							case SmsManager.RESULT_ERROR_NO_SERVICE:
								Toast.makeText(ContentsSyncActivity.this, "No service", Toast.LENGTH_LONG).show();
								break;
							case SmsManager.RESULT_ERROR_NULL_PDU:
								Toast.makeText(ContentsSyncActivity.this, "Null PDU", Toast.LENGTH_LONG).show();
								break;
							case SmsManager.RESULT_ERROR_RADIO_OFF:
								Toast.makeText(ContentsSyncActivity.this, "Radio off", Toast.LENGTH_LONG).show();
								break;
							}
						}
					}, new IntentFilter("SMS_SENT"));
						
    				registerReceiver(new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							// TODO Auto-generated method stub
							switch(getResultCode()){
							case Activity.RESULT_OK:
								Toast.makeText(getBaseContext(), "SMS Delivered", Toast.LENGTH_LONG).show();
								break;
							case Activity.RESULT_CANCELED:
								Toast.makeText(getBaseContext(), "SMS not delivered", Toast.LENGTH_LONG).show();
								break;
							}
						}
					}, new IntentFilter("SMS_DELIVERED"));
    				SmsManager sms = SmsManager.getDefault();
    				sms.sendTextMessage(phoneNumber, null, mMessage, sendPI, deliveredPI);
    			}
    			else if(service.equals("editcontacts")){
    				// 연락처 정보를 수정
    			}
    			else if(service.equals("disconnect")){
    				// 연결 끊기
    			}
    		} catch (JSONException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
    }
}

