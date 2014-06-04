package ie.enclude.flexibus;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.ui.sfnative.SalesforceActivity;

import ie.enclude.salesforce.operation.DataHandleFactory;
import ie.enclude.flexibus.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class FlexibusActivity extends SalesforceActivity implements SalesforceResponseInterface
{
    /** Called when the activity is first created. */

    public static final String DEBUG_TAG="FlexibusLogging";
	
	private static final int CHANGE_BUS_MENU_ITEM = Menu.FIRST;
	private static final int SAVE_PASSENGER_LIST_MENU_ITEM = CHANGE_BUS_MENU_ITEM + 1;
	private static final int LOGOUT_MENU_ITEM = CHANGE_BUS_MENU_ITEM + 2;
	
	private static Button m_selectBusButton; // this is only used if no bus is selected
	public FlexibusActivity m_Activity = this;
	
	TextView m_statusText, m_busOdoText, m_busRegText;
	EditText m_odometerReading;
	Button m_sendButton;
	Button m_busListButton;
	Button m_dailyCheckButton;
	Button m_driverListButton;
	Button m_fuelPurchaseButton;
	Button m_passengerListButton;
	ProgressDialog m_progress;
	Context m_context;
	
	private int mRuntimeOrientation;
	private boolean mDisableScreenRotation=false;
	protected int getScreenOrientation() 
	{
		Display display = getWindowManager().getDefaultDisplay();
	    int orientation = display.getOrientation();
	    if (orientation == Configuration.ORIENTATION_UNDEFINED) 
	    {
	         orientation = getResources().getConfiguration().orientation;
	         if (orientation == Configuration.ORIENTATION_UNDEFINED) 
	         {
	            if (display.getWidth() == display.getHeight())
	            {
	            	orientation = Configuration.ORIENTATION_SQUARE;
	            }
	            else if(display.getWidth() < display.getHeight())
	            {
	            	orientation = Configuration.ORIENTATION_PORTRAIT;
	            }
	            else
	            {
	               orientation = Configuration.ORIENTATION_LANDSCAPE;
	            }
	         }
	      }
	      return orientation;
	 }

   @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
       if (mDisableScreenRotation) 
       {
          super.onConfigurationChanged(newConfig);
          this.setRequestedOrientation(mRuntimeOrientation);
       }
       else 
       {
          mRuntimeOrientation = this.getScreenOrientation();
          super.onConfigurationChanged(newConfig);
       }
    }
	   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	
    	m_context=getApplicationContext();

        mRuntimeOrientation = this.getScreenOrientation();
        setContentView(R.layout.main);
		
        FlexibusApp gs = (FlexibusApp) getApplication();
        
        m_busOdoText = (TextView) findViewById(R.id.busOdoTextView);
        m_busRegText = (TextView) findViewById(R.id.busRegTextView);
        m_statusText = (TextView) findViewById(R.id.statusText);
        
        // rotation destroys the activity
        if (gs.getDataHandler() == null)
        {
        	gs.setDataHandler (new DataHandleFactory(this));
        }
        
        prepareOdoReadingSendButton();
        prepareOdometerEntryField();
        prepareSelectBusButton();
        prepareDailyCheckButton();
        preparePassengerListButton();
        prepareFuelPurchaseButton();
        RetrievePersistantState();
   }

 	private void RetrievePersistantState() 
    {
    	FlexibusApp gs = (FlexibusApp) getApplication();
    	if (gs.getCurrentBusName() == CONSTANTS.UNDEFINED_BUS_NAME) // then at the start of the app - otherwise seems to be called every time a Rest call returns
    	{
	    	SharedPreferences persistanceData = getPreferences(MODE_PRIVATE);
	    	String busName = persistanceData.getString("LastBusName", CONSTANTS.UNDEFINED_BUS_NAME);
	    	if (!busName.equals(CONSTANTS.UNDEFINED_BUS_NAME))
	    	{
	    		m_progress = ProgressDialog.show(this, "Starting...", "Loading previous state");
	    		mDisableScreenRotation = true;
	    	       
	    		String savedOdoReading = persistanceData.getString("LastBusOdoReading", "0");
		     	gs.setSavedBusState (busName, savedOdoReading);
		     	if (gs.getDataHandler().initialiseSelectedBus(gs.getSavedBusName(), gs.getSavedBusOdoReading(), this) == false)
		     	{
		     		m_progress.dismiss();
		     		m_progress = null;
		     	}
	    	}
    	}
	}

    private void StorePersistantState()
    {
     	FlexibusApp gs = (FlexibusApp) getApplication();
     	String busName = gs.getCurrentBusName();
     	if (!busName.equals(CONSTANTS.UNDEFINED_BUS_NAME))
     	{
	     	SharedPreferences persistanceData = getPreferences(MODE_PRIVATE);
	     	SharedPreferences.Editor prefEditor = persistanceData.edit();
    		prefEditor.putString("LastBusName", busName);
    		prefEditor.putString("LastBusOdoReading", (String) gs.getCurrentBusOdoReading());
     		prefEditor.commit();
     	}
    }
    
	@Override 
	public void onResume() 
	{
     	super.onResume();
   		updateCurrentBusStatus();
   }

	@Override
	public void onResume(RestClient client) {
        // Keeping reference to rest client
		FlexibusApp.client = client; 
        RetrievePersistantState();
	}

	@Override 
	public void onPause ()
	{
		super.onPause();
		StorePersistantState();
		if (m_progress != null)
		{
			m_progress.dismiss();
			m_progress = null;
		}
	}
	
	public void updateCurrentBusStatus() 
	{
		if (noBusSelected())
    	{
    		m_statusText.setText(R.string.selectBusText);
    		m_selectBusButton.setVisibility(View.VISIBLE);
    		m_busOdoText.setText("");
    		m_busRegText.setText("");
    	}
    	else
    	{
    		FlexibusApp gs = (FlexibusApp) getApplication();
    		m_busOdoText.setText ((String)gs.getCurrentBusOdoReading());
    		m_busRegText.setText(gs.getCurrentBusName());
    		m_fuelPurchaseButton.setClickable(true);
     		m_dailyCheckButton.setClickable(true);
     		m_passengerListButton.setClickable(true);
    		m_selectBusButton.setVisibility(View.GONE);
    		m_statusText.setText(R.string.initialText);
    	}
	}
    
	private boolean noBusSelected() 
	{
		FlexibusApp gs = (FlexibusApp) getApplication();
    	return gs.getCurrentBusName().equals(CONSTANTS.UNDEFINED_BUS_NAME);
	}

	// in Android 3.0 will need to invalidate menu
	public boolean onPrepareOptionsMenu (Menu menu)
	{
		menu.clear();
		if (!noBusSelected())
		{
			menu.add(0, CHANGE_BUS_MENU_ITEM, 0, R.string.changeBusText).setIcon(R.drawable.changebus);
		}
   		FlexibusApp gs = (FlexibusApp) getApplication();
   		if (gs.isPassengerListDirty())
   		{
   			menu.add(0, SAVE_PASSENGER_LIST_MENU_ITEM, 0, R.string.saveTripReportsText).setIcon(R.drawable.savepassengers);
   		}
   		
   		menu.add(0, LOGOUT_MENU_ITEM, 0, "Logout");
		return super.onPrepareOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == CHANGE_BUS_MENU_ITEM)
		{
			launchBusList();
		}
		else if (item.getItemId() == SAVE_PASSENGER_LIST_MENU_ITEM)
		{
			sendTripReports();
		}
		else if (item.getItemId() == LOGOUT_MENU_ITEM)
		{
			SalesforceSDKManager.getInstance().logout(this);
		}
			
		return true;
	}

	private void prepareOdometerEntryField() {
		m_odometerReading = (EditText) findViewById(R.id.odometer);
        m_odometerReading.setFilters(new InputFilter[] {
        		new InputFilter.LengthFilter(CONSTANTS.MAX_ODOMETER_SIZE)
        });
        m_odometerReading.addTextChangedListener (new TextWatcher() {
        	
        	public void afterTextChanged(Editable s) 
        	{
        		if (odometerReadingIsAcceptable())
        		{
        			m_sendButton.setClickable(true);
        			m_statusText.setText(R.string.readytosend);
        		}
        		else
        		{
        			m_sendButton.setClickable(false);
        			m_statusText.setText(R.string.initialText);
        		}
        	}
        	
            public void onTextChanged(CharSequence s, int start, int before, int count) {
        	}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        m_odometerReading.setOnEditorActionListener(new OnEditorActionListener() 
        {
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) 
			{
				if ((actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == 66)) && odometerReadingIsAcceptable()) 
				{
	        		prepareToSendOdoReading();
		        }
				return false;
			}
        });
	}

	protected void sendTripReports() 
	{
   		FlexibusApp gs = (FlexibusApp) getApplication();
		m_progress = ProgressDialog.show(this, "Sending...", "Sending passenger trip details to Salesforce");
		mDisableScreenRotation=true;
		new SalesforceSendTripReports().execute(gs);
	}

	protected boolean odometerReadingIsAcceptable() 
	{
		return m_odometerReading.length() >= CONSTANTS.MIN_ODOMETER_SIZE && !noBusSelected();
	}

	private void prepareOdoReadingSendButton() {
		m_sendButton = (Button) findViewById(R.id.sendOdoReading);
        m_sendButton.setOnClickListener(new View.OnClickListener() { 
        	public void onClick(View v) 
        	{
        		prepareToSendOdoReading();
        	}
        });
        m_sendButton.setClickable(false);
	}
    
	private void prepareSelectBusButton() 
	{
		m_selectBusButton = (Button) findViewById(R.id.selectBusButton);
		m_selectBusButton.setOnClickListener(new View.OnClickListener() { 
        	public void onClick(View v) 
        	{
        		launchBusList();
           		m_selectBusButton.setVisibility(View.GONE);
           		m_fuelPurchaseButton.setClickable(true);
           		m_dailyCheckButton.setClickable(true);
           		m_passengerListButton.setClickable(true);
        	}
        });
		m_selectBusButton.setClickable(true);
	}
	
	public void launchBusList()
	{
		Intent i = new Intent(this, BusListView.class);
		startActivity(i);
	}
	
  	private void prepareFuelPurchaseButton() 
  	{
  		m_fuelPurchaseButton = (Button) findViewById(R.id.recordFuelPurchaseButton);
  		m_fuelPurchaseButton.setOnClickListener(new View.OnClickListener() { 
        	public void onClick(View v) 
        	{
        		launchRecordFuelPurchase();
        	}
        });
  		m_fuelPurchaseButton.setClickable(!noBusSelected());
	}

	protected void launchRecordFuelPurchase() 
	{
		Intent i = new Intent(this, RecordFuelPurchase.class);
		startActivity(i);
	}

	private void prepareDailyCheckButton() 
	{
		m_dailyCheckButton = (Button) findViewById(R.id.dailyCheckButton);
		m_dailyCheckButton.setOnClickListener(new View.OnClickListener() { 
        	public void onClick(View v) 
        	{
        		launchDailyCheckList();
        	}
        });
		m_dailyCheckButton.setClickable(!noBusSelected());
	}
	
	public void launchDailyCheckList()
	{
		// could wipe previous picklist values here
		Intent i = new Intent(this, DriversDailyCheck.class);
		startActivityForResult(i, CONSTANTS.DRIVER_DAILY_CHECKLIST);
	}

	private void preparePassengerListButton()
	{
		m_passengerListButton = (Button) findViewById(R.id.passengerListButton);
		m_passengerListButton.setOnClickListener(new View.OnClickListener() { 
        	public void onClick(View v) 
        	{
        		launchServiceList();
        	}
        });
    	m_passengerListButton.setClickable(!noBusSelected());
	}
	
	public void launchPassengerList()
	{
		// could wipe previous picklist values here
		Intent i = new Intent(this, PassengerList.class);
		startActivity(i);
	}
	
	public void launchServiceList()
	{
		Intent i = new Intent(this, TodaysBusServiceList.class);
		startActivity(i);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if (requestCode == CONSTANTS.DRIVER_DAILY_CHECKLIST)
		{
			if (resultCode == CONSTANTS.AT_LEAST_ONE_FAULT)
			{
				showDialog (CONSTANTS.DIALOG_ADD_NOTE);

			}
			else if (resultCode == CONSTANTS.NO_FAULTS)
			{
				sendCheckListToSalesforce();
			}
		}
	}
	
	protected void addNoteToCheckList() 
	{
		showDialog (CONSTANTS.DIALOG_ENTER_NOTE);
		
			
	}

	protected void sendCheckListToSalesforce() 
	{
		m_progress = ProgressDialog.show(this, "Sending...", "Sending startup checklist to Salesforce");
		mDisableScreenRotation=true;
   		FlexibusApp gs = (FlexibusApp) getApplication();
   		new SalesforceSendChecklistTask().execute(gs);
	}


	public void prepareToSendOdoReading()
	{
		// need to hide the soft keyboard, once the number is entered!
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(m_odometerReading.getWindowToken(), 0);
		
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
			Log.v(DEBUG_TAG, "prepareToSendOdoReading " + e.getMessage());
			// just send the reading
			sendOdoReading();
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
			sendOdoReading();
		}
	}
	
	public void sendOdoReading()
    {
		m_progress = ProgressDialog.show(this, "Sending...", "Sending Odometer reading to Salesforce");
		mDisableScreenRotation=true;
  		String odoReading = m_odometerReading.getText().toString();
   		FlexibusApp gs = (FlexibusApp) getApplication();
   		gs.setSavedBusState(gs.getSavedBusName(), odoReading);
		gs.getDataHandler().addOdometerReadingToSelectedBus(odoReading, this);
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
	                    	sendOdoReading();
	                    }
	                })
	                .setNegativeButton(R.string.alertNoButtonText, new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) 
	                    {

	                        /* User clicked Cancel so do some stuff */
	                    }
	                })
	                .create();
	        case CONSTANTS.DIALOG_ADD_NOTE:
	        	return new AlertDialog.Builder(this)
                .setTitle(R.string.alertAddNoteTitle)
                .setMessage(R.string.alertAddNoteMessage)
                .setPositiveButton(R.string.alertYesButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                    	addNoteToCheckList();
                    }

	                })
                .setNegativeButton(R.string.alertAbortButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                    }
                })
               .setNeutralButton(R.string.alertNoButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                     	sendCheckListToSalesforce();
                   }
                })
                .create();
	        case CONSTANTS.DIALOG_ENTER_NOTE:
	        {
	        	final Dialog enterNoteDialog = new Dialog(this);

	        	enterNoteDialog.setContentView(R.layout.addnote);
	        	enterNoteDialog.setTitle(R.string.addNoteTitle);
	        	
	        	final EditText noteEdit = (EditText) enterNoteDialog.findViewById(R.id.editNoteText);
	        	Button sendButton = (Button) enterNoteDialog.findViewById(R.id.sendButton);
	        	sendButton.setOnClickListener(new View.OnClickListener() { 
	            	public void onClick(View v) 
	            	{
			        	
	            		String faultReport = noteEdit.getText().toString();
	            		if (faultReport.length() > 0)
	            		{
	            			FlexibusApp gs = (FlexibusApp) getApplication();
	            			gs.getDataHandler().addFaultReportTextToCheckList(faultReport);
	            		}
	            		sendCheckListToSalesforce();
	            		enterNoteDialog.dismiss();
	            	}
	            });
	    		
	    		return enterNoteDialog;

	        }
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
	
	@Override
	public void responseReceived(String result) 
	{
        if (m_progress != null)
        {
        	m_progress.dismiss();
			m_progress = null;
        }
        mDisableScreenRotation=false;

        if (result != "")
        {
        	m_statusText.setText(result);
        	Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
        }
    	updateCurrentBusStatus();
	}
	
	public class SalesforceSendChecklistTask extends AsyncTask<FlexibusApp, Void, String> implements SalesforceResponseInterface
	{
		@Override
		protected String doInBackground(FlexibusApp... gs) 
		{
			gs[0].getDataHandler().localLogin();
			return gs[0].getDataHandler().sendStartupCheckListToSalesforce(this);
		}

		@Override
		public void responseReceived(String result)
		{
	        if (m_progress != null)
	        {
	        	m_progress.dismiss();
				m_progress = null;
	        }
	        mDisableScreenRotation=false;
	        if (result != "")
	        {
	        	m_statusText.setText(result);
	        	Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
	        }
		}

	}

	public class SalesforceInitialiseBusTask extends AsyncTask<FlexibusApp, Void, String> implements SalesforceResponseInterface
	{
		@Override
		protected String doInBackground(FlexibusApp... gs) 
		{
			gs[0].getDataHandler().localLogin();
			if (gs[0].getDataHandler().initialiseSelectedBus(gs[0].getSavedBusName(), gs[0].getSavedBusOdoReading(), this))
			{
				return "";
			}
			else
			{
				return gs[0].getDataHandler().getLastError();
			}
		}

		@Override
		public void responseReceived(String result)
		// if result is blank, then the selected bus has been updated in the LocalDataHandler.setOneBusDetails
		{
	        if (m_progress != null)
	        {
	        	m_progress.dismiss();
				m_progress = null;
	        }
	        mDisableScreenRotation=false;

			if (!result.equals(""))
			{
				Toast.makeText(FlexibusActivity.this, result, Toast.LENGTH_SHORT).show();
				finish();
			}
			else
			{
				updateCurrentBusStatus();
			}
		}
	}
	
	public class SalesforceSendTripReports extends AsyncTask<FlexibusApp, Void, String> 
	{
		@Override
		protected String doInBackground(FlexibusApp... gs) 
		{
			gs[0].getDataHandler().localLogin();
			return gs[0].getDataHandler().sendTripReports(gs[0].getDatabase());
		}

		protected void onPostExecute(String result) 
		{
	        if (m_progress != null)
	        {
	        	m_progress.dismiss();
				m_progress = null;
	        }
	        mDisableScreenRotation=false;
	        if (result != "")
	        {
	        	m_statusText.setText(result);
	        	Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
	        }
	        FlexibusApp gs = (FlexibusApp) getApplication();
	   		gs.clearPassengerListFlag();
		}
	}
}