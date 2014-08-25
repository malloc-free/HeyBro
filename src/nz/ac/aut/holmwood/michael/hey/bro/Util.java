package nz.ac.aut.holmwood.michael.hey.bro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * 
 * @author Michael Holmwood
 *
 * Just a convenience class intended to hold static methods used by
 * all activities/classes.
 */
public class Util {
	
	/**
	 * Display a dialog with the supplied message.
	 * 
	 * @param message - The message to display.
	 * @param context - The context that called the method.
	 */
	public static void showMessage(String message,  Context context){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message);
		builder.setPositiveButton("OK", 
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {	
						
					}
				});
		
		builder.show();
	}
}

