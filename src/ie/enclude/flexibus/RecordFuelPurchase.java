package ie.enclude.flexibus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class RecordFuelPurchase extends Activity
{
	TextView m_statusText;
	Button m_sendButton;
	Button m_cancelButton;
	EditText m_odometerReading;
	EditText m_fuelPurchased;
	ProgressDialog m_progress;

	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recordfuelpurchase);

        m_statusText = (TextView) findViewById(R.id.statusText);
        prepareButtons();
        prepareEditFields();
	}
	
	protected void onResume ()
    {
    	super.onResume();
   		m_statusText.setText(R.string.initialFuelPurchaseText);
    }

	private void prepareButtons() {
		m_sendButton = (Button) findViewById(R.id.sendButton);
        m_sendButton.setOnClickListener(new View.OnClickListener() { 
        	public void onClick(View v) 
        	{
        		prepareToSendFuelPurchase();
        	}
        });
        m_sendButton.setClickable(false);
        
        m_cancelButton = (Button) findViewById(R.id.cancelButton);
        m_cancelButton.setOnClickListener(new View.OnClickListener() { 
        	public void onClick(View v) 
        	{
        		finish();
        	}
        });
        m_cancelButton.setClickable(true);

	}

	private void prepareEditFields() 
	{
		m_odometerReading = (EditText) findViewById(R.id.odometer);
        m_odometerReading.setFilters(new InputFilter[] {
        		new InputFilter.LengthFilter(CONSTANTS.MAX_ODOMETER_SIZE)
        });
        listenForTextChanging(m_odometerReading);
        onEnterPressed(m_odometerReading);
        
        m_fuelPurchased = (EditText) findViewById(R.id.fuelEdit);
        m_fuelPurchased.setFilters(new InputFilter[] {
        		new InputFilter.LengthFilter(CONSTANTS.MAX_FUEL_SIZE)
        });
        listenForTextChanging(m_fuelPurchased);
        onEnterPressed(m_fuelPurchased);

 	}

	public void listenForTextChanging(EditText textField) 
	{
		textField.addTextChangedListener (new TextWatcher() {
        	
        	public void afterTextChanged(Editable s) 
        	{
        		checkStatusOfButtons();
        	}
        	
            public void onTextChanged(CharSequence s, int start, int before, int count) {
        	}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
	}

	public void onEnterPressed(EditText textField) 
	{
		textField.setOnEditorActionListener(new OnEditorActionListener() 
        {
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) 
			{
				if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == 66)) 
				{
					prepareToSendFuelPurchase();
		        }
				return false;
			}
        });
	}

	protected void checkStatusOfButtons() 
	{
   		if (odometerReadingIsAcceptable() && fuelReadingIsAcceptable())
		{
			m_sendButton.setClickable(true);
			m_statusText.setText(R.string.readytosendFuelPurchase);
		}
		else
		{
			m_sendButton.setClickable(false);
			m_statusText.setText(R.string.initialFuelPurchaseText);
		}
	}

	private boolean fuelReadingIsAcceptable() 
	{
	   	FlexibusApp gs = (FlexibusApp) getApplication();
		return m_fuelPurchased.length() >= CONSTANTS.MIN_FUEL_SIZE && !gs.getCurrentBusName().equals(CONSTANTS.UNDEFINED_BUS_NAME);
	}

	protected boolean odometerReadingIsAcceptable() 
	{
    	FlexibusApp gs = (FlexibusApp) getApplication();
		return m_odometerReading.length() >= CONSTANTS.MIN_ODOMETER_SIZE && !gs.getCurrentBusName().equals(CONSTANTS.UNDEFINED_BUS_NAME);
	}

	public void prepareToSendFuelPurchase()
	{
		// need to hide the soft keyboard, once the number is entered!
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(m_odometerReading.getWindowToken(), 0);
		imm.hideSoftInputFromWindow(m_fuelPurchased.getWindowToken(), 0);
		
		if (odometerReadingIsAcceptable() && fuelReadingIsAcceptable())
		{
			FlexibusApp gs = (FlexibusApp) getApplication();
			
			int currentOdoReading=0;
			int proposedOdoReading=0;
			try
			{
				currentOdoReading= Integer.parseInt(gs.getDataHandler().getCurrentBusOdoReading());
				proposedOdoReading = Integer.parseInt(m_odometerReading.getText().toString());
			}
			catch (NumberFormatException e)
			{
				sendFuelPurchase(); // probably means we don't have a current reading
				return;
			}
			int odoDifference = proposedOdoReading - currentOdoReading;
			if (odoDifference < 0)
			{
				Resources res = getResources();
				gs.setAlertDialogMessage (String.format(res.getString(R.string.alertOdometerLowMessage), proposedOdoReading, currentOdoReading));
				showDialog (CONSTANTS.DIALOG_CHECK_ODOMETER);
			}
			else if (odoDifference > CONSTANTS.MAX_ODO_READING_DIFFERENCE)
			{
				Resources res = getResources();
				gs.setAlertDialogMessage (String.format(res.getString(R.string.alertOdometerHighMessage), proposedOdoReading, currentOdoReading));
				showDialog (CONSTANTS.DIALOG_CHECK_ODOMETER);
			}
			else
			{
				sendFuelPurchase();
			}
		}
	}
	
	public void sendFuelPurchase()
    {
		m_progress = ProgressDialog.show(this, "Sending...", "Sending fuel purchased details to Salesforce");
  		String odoReading = m_odometerReading.getText().toString();
  		String fuelPurchased = m_fuelPurchased.getText().toString();
   		FlexibusApp gs = (FlexibusApp) getApplication();
   		gs.setSavedBusState(gs.getSavedBusName(), odoReading, fuelPurchased);
   		new SalesforceSendFuelPurchasedTask().execute(gs);
    }

	public class SalesforceSendFuelPurchasedTask extends AsyncTask<FlexibusApp, Void, String> 
	{
		@Override
		protected String doInBackground(FlexibusApp... gs) 
		{
			gs[0].getDataHandler().localLogin();
			return gs[0].getDataHandler().recordFuelPurchased(gs[0]);
		}

		protected void onPostExecute(String result) 
		{
	        if (m_progress != null)
	        {
	        	m_progress.dismiss();
				m_progress = null;
	        }

	        m_statusText.setText(result);
	        Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
	        RecordFuelPurchase.this.finish();
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) 
	{
		switch (id) 
		{
	        case CONSTANTS.DIALOG_CHECK_ODOMETER:
	            return new AlertDialog.Builder(this)
	                .setTitle(R.string.alertOdometerTitle)
	                .setMessage("Something wrong with odometer reading")
	                .setPositiveButton(R.string.alertYesButtonText, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) 
	                    {
	                    	sendFuelPurchase();
	                    }
	                })
	                .setNegativeButton(R.string.alertNoButtonText, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) 
	                    {

	                        /* User clicked Cancel so do some stuff */
	                    }
	                })
	                .create();
	        
	            default:
	            	return null;
		}
	}

	protected void onPrepareDialog (int id, Dialog dialog)
	{
		if (id == CONSTANTS.DIALOG_CHECK_ODOMETER)
		{
			FlexibusApp gs = (FlexibusApp) getApplication();
			((AlertDialog) dialog).setMessage(gs.getCurrentAlertDialogMessage());
		}
	}
	
 
}
