package nz.ac.aut.holmwood.michael.hey.bro;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

/**
 * 
 * @author Michael Holmwood
 * 
 * Handles and reports on the status of a sent SMS. Based
 * on code by Andrew Ensor (just tweaked a bit).
 *
 */
public class SMSHandler extends BroadcastReceiver{

	//Specifies the type of handler this is.
	private HandlerType type;
	
	//Used to specify the handler type.
	public enum HandlerType {
		SMS_RECV, SMS_SENT
	}

	/**
	 * Main constructor for SMSHandler. Only parameter is the 
	 * type.
	 * @param type - Specify type of handler.
	 */
	public SMSHandler(HandlerType type) {
		super();
		this.type = type;
	}
	
	/**
	 * @see android.content.BroadcastReceiver.onReceive
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		switch(type) {
		case SMS_RECV : smsRecv(context, intent); break;
		case SMS_SENT : smsSent(context, intent); break;
		}
	}
	
	/**
	 * Called when an SMS has been sent. Reports status.
	 * 
	 * @see android.content.BroadcastReceiver.smsSent
	 * 
	 * @param context - The associated context.
	 * @param intent - The registered intent.
	 */
	private void smsSent(Context context, Intent intent) {
		String message = "Error not specified";
		
		switch(getResultCode()) {
		case Activity.RESULT_OK : 
			message = "SMS Sent";
			break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE : 
			message = "Generic failure";
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE :
			message = "No service!";
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU : 
			message = "PDU not provided";
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF : 
			message = "Radio off, please switch on!";
			break;
		}
		
		Util.showMessage(message, context);
	}
	
	/**
	 * Not currently implemented.
	 * 
	 * @param context
	 * @param intent
	 */
	private void smsRecv(Context context, Intent intent) {
		
	}

}
