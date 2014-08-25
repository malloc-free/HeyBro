package nz.ac.aut.holmwood.michael.hey.bro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import nz.ac.aut.holmwood.michael.hey.bro.SMSHandler.HandlerType;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * 
 * @author Michael Holmwood
 * 
 * Handles most of the important stuff. Sends location data to the 
 * server, retrieves data from a specified id. Also shows raw location
 * data on the screen, mostly for debugging purposes.
 *
 */
@SuppressLint("NewApi")
public class MainActivity extends Activity
	implements LocationListener{
	
	//TextView to show raw location data.
	private TextView locationTextView;
	//TextView to show server responses.
	private TextView responseTextView;
	//TextView to enter recipient phone number.
	private TextView phoneTextView;
	//TextView to enter an id for a location.
	private TextView idTextView;
	//Button to show googlemap view of location.
	private Button viewLocButton;
	//Button to submit location to the server.
	private Button sendLocButton;
	//Button to submit id to the server.
	private Button submitIdButton;
	//The location manager for MainActivity.
	private LocationManager locManager;
	//The string for GPS provider.
	private String provider;
	//The sms manager for MainActivity.
	private SmsManager messManager;
	//Handles and reports SMS status.
	private SMSHandler sentHandler;
	//The url for the location server.
	private static final String serverUrl = "http://192.168.0.10:8080/AndroidServer/LocationServlet";
	//The string used to pass information between MainActivity and 
	//LocationActivity
	private String EXTRA_MESSAGE;
	
	/**
	 * @see android.app.Activity.onCreate
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EXTRA_MESSAGE = getResources().getString(R.string.sent_location);
		Log.i("debug", EXTRA_MESSAGE);
		
		//Setup the view.
		setContentView(R.layout.activity_main);
		locationTextView = (TextView)findViewById(R.id.location_text);
		responseTextView = (TextView)findViewById(R.id.response_text);
		viewLocButton = (Button)findViewById(R.id.view_loc_button);
		sendLocButton = (Button)findViewById(R.id.send_loc_button);
		submitIdButton = (Button)findViewById(R.id.submit_id_button);
		phoneTextView = (TextView)findViewById(R.id.phone_submit);
		idTextView = (TextView)findViewById(R.id.location_id);
		
		//Setup the location manager.
		locManager = 
        		(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        provider = LocationManager.GPS_PROVIDER;
        locManager.requestLocationUpdates(provider, 0, 0, this);
        
        //Set OnClickListeners for various button activities.
		viewLocButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				viewLocation();	
			}	
		});
		
		sendLocButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				sendLocation();
			}	
		});
		
		submitIdButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				sendId();
			}
			
		});
		
		messManager = SmsManager.getDefault();
		
	}
	
	/**
	 * Register the SMSHandler with the system.
	 * 
	 * @see android.app.Activity.onStart()
	 */
	public void onStart() {
		super.onStart();
		sentHandler = new SMSHandler(HandlerType.SMS_SENT);
		registerReceiver(sentHandler, 
				new IntentFilter("SMS_SENT"));
	}
	
	/**
	 * Deregister the SMSHandler with the system.
	 */
	public void onStop() {
		super.onStop();
		Log.i("debug", "deregister handler");
		if(sentHandler != null) {
			unregisterReceiver(sentHandler);
		}
	}
	
	/**
	 * Send the current location to the specified phone number.
	 */
	private void sendLocation() {
		final String phoneNumber = phoneTextView.getText().toString();
		
		//Check the number - if not present, shout about it.
		if(phoneNumber.length() < 1){
			Util.showMessage("No phone number specified", this);
			return;
		}
		
		//Get the current location, and embed it in a url.
		URL url = null;
		Location location = locManager.getLastKnownLocation(provider);
		try {
			StringBuilder builder = new StringBuilder();
			builder.append(serverUrl);
			builder.append("?action=submitnew");
			builder.append("&latitude=").append(Double.toString(location.getLatitude()));
			builder.append("&longitude=").append(Double.toString(location.getLongitude()));
			url = new URL(builder.toString());
			
			//Palm it off to another thread to deal with.
			ActivityCommunicator task = new ActivityCommunicator(){;
			
				@Override
				protected void onPostExecute(ArrayList<String> data) {
					responseTextView.setText(data.get(0));
					sendSms(phoneNumber, data.get(0));
				}
			};
			
			task.execute(url);
		}
		catch(MalformedURLException e) {
			Log.e("debug", "MalformedException: " + e.getMessage());
		}	
	}
	
	/**
	 * Send a SMS with the reference id for the location, provided
	 * by the server.
	 * @param phoneNumber - The phone number to send the message to.
	 * @param id - The id of the reference to send.
	 */
	private void sendSms(String phoneNumber, String id) {
		
		StringBuilder builder = new StringBuilder();
		builder.append(getResources().getString(R.string.sms_message));
		builder.append("\n").append(id);
		PendingIntent sentIntent = 
				PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), 0);
		messManager.sendTextMessage(phoneNumber, null, builder.toString(), sentIntent, null);
		phoneTextView.setText("");
	}
	
	/**
	 * Send the specified id to the server, to retrieve associated 
	 * location.
	 */
	private void sendId() {
		URL url = null;
		String id = idTextView.getText().toString();
		
		//Check the id, if not cool, shout about it.
		if(id.length() < 1){
			Util.showMessage("No id specified!", this);
			return;
		}
		
		Log.i("debug", id);
		
		//Embed the id in a url, for submission to the server.
		try {
			StringBuilder builder = new StringBuilder();
			builder.append(serverUrl);
			builder.append("?action=submitid");
			builder.append("&id=").append(id);
			url = new URL(builder.toString());
			
			//Pass off to another thread.
			AsyncTask<URL, Void, ArrayList<String>> task = 
					new ActivityCommunicator(){
				
				protected void onPostExecute(ArrayList<String> data) {
					displayLocation(data);
				}
			};
			
			task.execute(url);
		}
		catch(MalformedURLException e) {
			Log.e("debug", "MalformedURL");
		}
	}
	
	/**
	 * Pass the information off to LocationActivity to display.
	 * If the response from the server was "failed" then the reference
	 * was bung, and shout about it.
	 * 
	 * @param data - The data received from the server.
	 */
	private void displayLocation(ArrayList<String> data) {
		
		//Check the response.
		if(data.get(0).equals("failed")){
			Util.showMessage("No such location id", this);
			return;
		}
		
		idTextView.setText("");
		
		//Pass to LocationActivity.
		Intent intent = new Intent(this, LocationActivity.class);
		intent.putExtra(EXTRA_MESSAGE, data);
		startActivity(intent);
	}
	
	/**
	 * Show the current location in LocationActivity.
	 */
	private void viewLocation() {
		Intent intent = new Intent(this, LocationActivity.class);
		startActivity(intent);
	}
	
	@Override
	public void onLocationChanged(Location arg0) {
		locationTextView.setText(arg0.toString());
		
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Class to handle communication with the server.
	 *
	 */
	private class ActivityCommunicator extends AsyncTask<URL, Void, ArrayList<String>> {
		@Override
		protected ArrayList<String> doInBackground(URL... urls) {
			
			HttpURLConnection conn = null;
			ArrayList<String> list = new ArrayList<>();
			URL url = urls[0];
			
			try {
				conn = (HttpURLConnection)url.openConnection();
				BufferedReader reader = new BufferedReader
						(new InputStreamReader(conn.getInputStream()));
				String line;
				
				while((line = reader.readLine()) != null) {
					list.add(line);
				}
			}
			catch (IOException e) {
				list.add(e.getMessage());
				Log.e("debug", "IOException: " + e.getMessage());
			}
			finally {
				if(conn != null) {
					conn.disconnect();
				}
			}
			
			return list;
		}
	}
}
