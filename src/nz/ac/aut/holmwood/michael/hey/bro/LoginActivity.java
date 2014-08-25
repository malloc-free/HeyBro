package nz.ac.aut.holmwood.michael.hey.bro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * 
 * @author Michael Holmwood
 * 
 * The purpose of this class is to provide login access to the application
 * and server. This communicates with the UserServlet on the AndroidServer, 
 * and checks credentials before the application can be used.
 *
 */
@SuppressLint("NewApi")
public class LoginActivity extends Activity
	implements OnClickListener{

	//The TextView for entering the users handle.
	private TextView loginHandle;
	//The TextView for entering the users password.
	private TextView loginPassword;
	//The TextView to show the server response.
	private TextView serverResponse;
	//The button used to submit login credentials.
	private Button submitButton;
	//The url for the server.
	private String SERVER_URL;
	
	/**
	 * @see android.app.Activity.onCreate
	 */
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//Set up the various views for this Activity.
		setContentView(R.layout.activity_login);
		SERVER_URL = getResources().getString(R.string.user_server_url);
		loginHandle = (TextView)findViewById(R.id.login_handle);
		loginPassword = (TextView)findViewById(R.id.login_password);
		serverResponse = (TextView)findViewById(R.id.server_response);
		submitButton = (Button)findViewById(R.id.submit_button);
		submitButton.setOnClickListener(this);
		
		//Set the default cookiehandler for the application.
		CookieHandler.setDefault(new CookieManager());
	}

	/**
	 * @see android.app.Activity.onCreate
	 */
	@Override
	public void onClick(View arg0) {
		String handle = loginHandle.getText().toString();
		String password = loginPassword.getText().toString();
		
		//Check to see if we have some valid data, if not, shout.
		if(handle.length() < 1){
			Util.showMessage("Handle not provided!", this);
			return;
		}
		if(password.length() < 1){
			Util.showMessage("Password not provided", this);
			return;
		}
		
		//Build the url for the request.
		URL url = null;
		try {
			StringBuilder builder = new StringBuilder();
			builder.append(SERVER_URL);
			builder.append("?action=login");
			builder.append("&handle=").append(handle);
			builder.append("&password=").append(password);
			url = new URL(builder.toString());
			
			//Fire up the request in another thread.
			AsyncTask<URL, Void, String> task = 
					new LoginCommunicator();
			
			task.execute(url);
		}
		catch(MalformedURLException e) {
			Log.e("debug", "MalformedURLExcetpion: " + e.getMessage());
		}
		
	}

	/**
	 * Called if the login was successful.
	 */
	private void loadMain() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	
	/**
	 * Inner class to handle communication with server. Largely based
	 * on code by Andrew Ensor.
	 *
	 */
	private class LoginCommunicator extends AsyncTask<URL, Void, String> {

		/**
		 * @see android.os.AsyncTask.doInBackground
		 */
		@Override
		protected String doInBackground(URL... urls) {
			
			HttpURLConnection conn = null;
			
			URL url = urls[0];
			
			StringBuilder stringBuilder = new StringBuilder();
			
			try {
				conn = (HttpURLConnection)url.openConnection();
				BufferedReader reader = new BufferedReader
						(new InputStreamReader(conn.getInputStream()));
				String line;
				
				while((line = reader.readLine()) != null) {
					stringBuilder.append(line);
				}	
			}
			catch (IOException e) {
				stringBuilder.append(e.getMessage());
			}
			finally {
				if(conn != null) {
					conn.disconnect();
				}
			}
			
			return stringBuilder.toString();
		}
		
		/**
		 * Handle the login response from the server. If
		 * the reply is accepted, then we can proceed. If
		 * anything else, shout about it.
		 * @see android.os.AsyncTask.onPostExecute
		 */
		@Override
		protected void onPostExecute(String response) {
			serverResponse.setText(response);
			Log.i("debug", response);
			if(response.equals("accepted")) {
				loginHandle.setText("");
				loginPassword.setText("");
				loadMain();
			}
			else {
				Util.showMessage("Login failed!", LoginActivity.this);
			}
		}
		
		
	}

	

}
