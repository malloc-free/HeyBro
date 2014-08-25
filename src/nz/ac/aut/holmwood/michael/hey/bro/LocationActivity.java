package nz.ac.aut.holmwood.michael.hey.bro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;

/**
 * 
 * @author Michael Holmwood
 * 
 * Location activity handles all of the map related functions: displaying 
 * the current position on a googlemap, showing retrieved locations, and 
 * displaying route information.
 *
 */
@SuppressLint("NewApi")
public class LocationActivity extends Activity
	implements LocationListener{

	//The location manager used by Location Activity. 
	private LocationManager locManager;
	//The googleMap used to display position information.
	private GoogleMap googleMap;
	//The provider used to get location information.
	private String provider;
	//The marker for the current location.
	private MarkerOptions currentLocation;
	//The message key used to receive information from the main activity.
	private String EXTRA_MESSAGE;
	//The colour used for the current location.
	private float markerHue = BitmapDescriptorFactory.HUE_BLUE;
	//The polyline used to represent the current directions route.
	private Polyline currentLine;
	//A list of available directions.
	private List<String> directions;
	//The default zoom level.
	private static final float defaultZoom = 12;
	//The transport used for communication with the server.
	private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
	//The factory used to extract information sent by the google directions server.
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	//The url for the google directions server.
	private String GOOGLE_SERVER;// = "http://maps.googleapis.com/maps/api/directions/json";
	
	//Used to specify the type of camera movement.
	private enum MoveType {
		ANIMATE, DIRECT
	}
	
	//Used to specify the type of transport used for directions.
	//Not currently implemented (fully at least).
	private static final String DRIVE = "driving";
	private static final String WALK = "walking";
	private static final String BIKE = "bicycling";
	
	//Specifies the selected mode of transport (not currently implemented).
	private String transMode;
	
	/**
	 * @see android.app.Activity.onCreate
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		directions = new ArrayList<>();
		GOOGLE_SERVER = getResources().getString(R.string.google_directios_server);
		transMode = DRIVE;
		EXTRA_MESSAGE = getResources().getString(R.string.sent_location);
		Log.i("debug", EXTRA_MESSAGE);
		setContentView(R.layout.activity_location);
		
		locManager =
				(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		provider = LocationManager.GPS_PROVIDER;
		locManager.requestLocationUpdates(provider, 0, 0, this);
		
		if(initilizeMap()) {
			Location location = locManager.getLastKnownLocation(provider);
			LatLng latLng = new LatLng(location.getLatitude(), 
					location.getLongitude());
			currentLocation = new MarkerOptions().position(latLng);
			currentLocation.icon(BitmapDescriptorFactory.defaultMarker(markerHue));
			googleMap.addMarker(currentLocation);
			googleMap.getUiSettings().setMyLocationButtonEnabled(true);
			moveCamera(location, defaultZoom, MoveType.DIRECT);
		}
		
		Intent intent = getIntent();
		
		//If there is a destination included, add this to the map.
		if(intent != null && intent.hasExtra(EXTRA_MESSAGE)) {
			Log.i("debug", "Intent not null");
			
			Object o = intent.getExtras().get(EXTRA_MESSAGE);
			
			if(o != null && o instanceof ArrayList<?> && 
					((ArrayList<?>)o).size() > 0) {
				Log.i("Mike", "Added destination");
				addDestination((ArrayList<String>)o);
			}
		}
	}
	
	/**
	 * This was intended to allow for alternate routes to be selected for
	 * directions. Not currently implemented.
	 * @param select
	 */
	public void selected(int select){
		applyDirections(select);
	}
	
	/**
	 * Initialize the map fragment.
	 * 
	 * @return - False if the map could not be initialized.
	 */
	@SuppressLint("NewApi")
	private boolean initilizeMap() {
		if (googleMap == null) {
			googleMap = 
					((MapFragment)getFragmentManager()
							.findFragmentById(R.id.map)).getMap();
		}
		
		if(googleMap == null) {
			Toast.makeText(getApplicationContext(),
					"Sorry, can't make map!", 
					Toast.LENGTH_SHORT).show();
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Add a destination. Current implementation only allows for a single
	 * destination to be added. The intention was to add multiple destinations.
	 * @param list
	 */
	private void addDestination(ArrayList<String> list) {
		//Get the values for the latitude and longitude from the list.
		double latitude = Double.parseDouble(list.get(0));
		double longitude = Double.parseDouble(list.get(1));
		String title = list.get(2);
		Log.i("debug", list.get(0));
		Log.i("debug", list.get(1));
		Log.i("debug", list.get(2));
		
		//Set up the marker and camera for the destination.
		LatLng destination = new LatLng(latitude, longitude);
		MarkerOptions marker = new MarkerOptions().position(destination).title("Current Location");
		marker.title(title);
		googleMap.addMarker(marker);
		CameraPosition cam = new CameraPosition.Builder().target(destination).zoom(12).build();
		googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cam));
		LatLng origin = locToLatLng(locManager.getLastKnownLocation(provider));
		
		//Add directions for the destination.
		addDirections(origin, destination);
	}
	
	/**
	 * Add a marker to the map using the supplied location and title. Not
	 * currently used.
	 * 
	 * @param location - The location to place the map.
	 * @param title - The title to give the location.
	 */
	private void addMarker(Location location, String title) {
		LatLng latLng = new LatLng(location.getLatitude(),
				location.getLongitude());
		MarkerOptions marker = new MarkerOptions().position(latLng);
	
		googleMap.addMarker(marker);
		
	}
	
	/**
	 * Move the camera to the specified location, apply the specified zoom
	 * and use the movement type directed.
	 * 
	 * @param location - The location to move to.
	 * @param zoom - The level of zoom to use.
	 * @param type - The type of movement to use (direct or animated).
	 */
	private void moveCamera(Location location, float zoom, MoveType type) {
		LatLng latLng = new LatLng(location.getLatitude(),
				location.getLongitude());
		CameraPosition cam = new CameraPosition.Builder()
		.target(latLng).zoom(zoom).build();
		
		if(type == MoveType.ANIMATE) {
			googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cam));
		}
		else{
			googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cam));
		}
	}
	
	/**
	 * Add directions to the map, from the supplied origin to the supplied
	 * destination.
	 * 
	 * @param origin - The origin location.
	 * @param destination - The destination location.
	 */
	private void addDirections(LatLng origin, LatLng destination) {
		String oStr = origin.latitude + "," + origin.longitude;
		String dStr = destination.latitude + "," + destination.longitude;
		
		//Palm off the request to a new thread.
		DirectionsCommunicator comm = new DirectionsCommunicator(){
			
			public void onPostExecute(Void data){
				applyDirections(0);
			}
		};
		comm.execute(oStr, dStr);
	}
	
	/**
	 * Add the directions at the specified index to the map. Not currently 
	 * used. The intention was to use a menu option and a dialog to select
	 * alternate routes. Not complete.
	 * 
	 * @param index - The index of the route to use.
	 */
	private void applyDirections(int index) {
		if(currentLine != null) {
			currentLine.remove();
		}
		
		if(directions.size() > 0 && index < directions.size()) {
			List<LatLng> locations = PolyUtil.decode(directions.get(0));
			PolylineOptions polyLine = new PolylineOptions();
			polyLine.addAll(locations);
			currentLine = googleMap.addPolyline(polyLine);
			
		}
		else{
			Toast.makeText(this, "No directions available", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Convert a location to a LatLng object. Convenience method.
	 * 
	 * @param location - The location to convert.
	 * @return - The resulting LatLng.
	 */
	private LatLng locToLatLng(Location location) {
		return new LatLng(location.getLatitude(), location.getLongitude());
	}
	
	/**
	 * Update the marker with the current location.
	 */
	@Override
	public void onLocationChanged(Location location) {
		LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
		currentLocation.position(latLng);
	
		//moveCamera(location, googleMap.getCameraPosition().zoom);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	private class DirectionsCommunicator extends AsyncTask<String, Integer, Void> {

		/**
		 * Most of this code is thanks to:
		 * http://ddewaele.github.io/GoogleMapsV2WithActionBarSherlock/part5
		 * 
		 * Gets directions from google directions services. The details are
		 * passed into a url, and the result parsed from jSON to strings. Each
		 * result is passed back to the main thread in a List. The three 
		 * objects DirectionsResult, Route and OverviewPolyLine are 
		 * used to parse information from the result.
		 * 
		 * @param params An array comprising of two strings: 
		 * an origin string and a destination string, each with 
		 * the lat/long of their respective locations. 
		 */
		@Override
		protected Void doInBackground(String... params)
		{
			Log.i("debug", "origin = " + params[0]);
			Log.i("debug", "destinaiton = " + params[1]);
			directions.clear();
			
			try {
				//Create an initializer to set the HttpRequest factory
				//to parse data in jSON format.
				HttpRequestInitializer init = new HttpRequestInitializer(){
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
					}
				};
				
				//Setup the request an url.
				HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(init);
				GenericUrl url = new GenericUrl(GOOGLE_SERVER);
				url.put("origin", params[0]);
				url.put("destination", params[1]);
				url.put("sensor", true);
				url.put("alternatives", true);
				url.put("mode", "driving");
				HttpRequest request = requestFactory.buildGetRequest(url);
				HttpResponse response = request.execute();
				
				//Parse the reply.
				DirectionsResult result = response.parseAs(DirectionsResult.class);
				Log.i("debug", "Size of routes = " + result.routes.size());
				
				//Iterate through the results, add to list.
				for(Route r : result.routes) {
					directions.add(r.overviewPolyLine.points);
				}		
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return null;
		}		
	}
	
	/**
	 * Credit: http://ddewaele.github.io/GoogleMapsV2WithActionBarSherlock/part5
	 * 
	 * Class used when parsing jSON with HttpResponse. Holds a list of all
	 * of the routes contained in the result.
	 */
	public static class DirectionsResult {
		@Key("routes")
		public List<Route> routes;
		
	}
	
	/**
	 * Credit: http://ddewaele.github.io/GoogleMapsV2WithActionBarSherlock/part5
	 *
	 * Class used when parsing jSON with HttpResponse. Used to store the data
	 * for the child field "overview_polyline" in each route in the jSON
	 * response.
	 */
	public static class Route {
		@Key("overview_polyline")
		public OverviewPolyLine overviewPolyLine;
	}
	
	/**
	 * Credit: http://ddewaele.github.io/GoogleMapsV2WithActionBarSherlock/part5
	 *
	 * Class used when parsing jSON with HttpResponse. Used to store the
	 * data for the encoded points in the polyline.
	 */
	public static class OverviewPolyLine {
		@Key("points")
		public String points;
	}

}
