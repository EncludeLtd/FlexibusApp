package ie.enclude.flexibus;

import ie.enclude.flexibus.database.DBAdapter;
import ie.enclude.flexibus.util.BusTrip;
import ie.enclude.flexibus.util.Passenger;
import ie.enclude.flexibus.util.PassengerAdapter;

import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class PassengerList extends ListActivity
{
	public static final String DEBUG_TAG="FlexibusLogging";
	private static final int ADD_FREE_MENU_ITEM = Menu.FIRST;
	private static final int ADD_FARE_MENU_ITEM = ADD_FREE_MENU_ITEM + 1;
	
	List<Passenger>passengers;
	List<BusTrip>busTrips;
	String tripTitle;
	String tripID;
	private TextView header;
	int farePayingPassengers=0;
	int freePassengers=0;

	ProgressDialog m_progress;
	
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

   /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) 
    {
    	tripTitle = "";
    	super.onCreate(icicle);
        
    	mRuntimeOrientation = this.getScreenOrientation();
        if (icicle != null)
    	{
    		
    	}
    	
		FlexibusApp gs = (FlexibusApp) getApplication();
		passengers = gs.getCurrentPassengerList();
		farePayingPassengers = gs.getFarePayingPassengers();
		freePassengers = gs.getFreePassengers();
		if (passengers == null)
		{
			m_progress = ProgressDialog.show(this, "Loading...", "Loading today's passengers from Salesforce");
    		mDisableScreenRotation = true;
			new SalesforceLoadProviderSheetTask().execute(gs);
		}
		else
		{
			prepareListView();
		}
    }
    
	protected void onPause ()
	{
		super.onPause();
		FlexibusApp gs = (FlexibusApp) getApplication();
		gs.saveCurrentPassengerList(tripID, passengers, freePassengers, farePayingPassengers);
	}

	// in Android 3.0 will need to invalidate menu
	public boolean onPrepareOptionsMenu (Menu menu)
	{
		menu.clear();
		Resources res = getResources();
		
		// save the text used so that it can be recognised in onOptionsMenuSelected
		menu.add(0, ADD_FREE_MENU_ITEM, 0, String.format(res.getString(R.string.addFreePassenger), freePassengers)).setIcon(R.drawable.ftpmenu);
		menu.add(0, ADD_FARE_MENU_ITEM, 1, String.format(res.getString(R.string.addFarePassenger), farePayingPassengers)).setIcon(R.drawable.euromenu);
		return super.onPrepareOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		FlexibusApp gs = (FlexibusApp) getApplication();
		if (item.getItemId() == ADD_FREE_MENU_ITEM)
		{
			freePassengers++;
			gs.setPassengerListDirty();
		}
		else if (item.getItemId() == ADD_FARE_MENU_ITEM)
		{
			farePayingPassengers++;
			gs.setPassengerListDirty();
		}
		return true;
	}
	
    public class SalesforceLoadProviderSheetTask extends AsyncTask<FlexibusApp, Void, String> implements SalesforceResponseInterface
	{
		@Override
		protected String doInBackground(FlexibusApp... gs) 
		{
			DBAdapter db = gs[0].getDatabase();
			try
			{
				db.open();
				if (CONSTANTS.TESTING)
				{
					//db.InitialiseDatabase(); // REMOVE AFTER TESTING
				}
				if (!db.ContainsTodaysTrips(gs[0].getCurrentBusName()))
				{
					gs[0].getDataHandler().localLogin();
					busTrips = gs[0].getDataHandler().getTodaysBusTrips(this);
					// this will return null if it has to call out - so do the rest of the work in responseReceived
					if (busTrips != null)
					{
						// processTrips (); -- database already populated
					}
					else
					{
						return gs[0].getDataHandler().getLastError();
					}
				}
				processPassengers ();
				return "";
			}
			catch (Exception e)
			{
				Log.v(DEBUG_TAG, "doInBackground in SalesforceLoadProviderSheetTask " + e.getMessage());
				return e.getMessage();
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
    		mDisableScreenRotation = false;
			if (!result.equals(""))
			{
				Toast.makeText(PassengerList.this, result, Toast.LENGTH_SHORT).show();
				finish();
			}
			else
			{
				// processTrips (); - trips and passengers are already in the database
				processPassengers (); // TODO consider a way of refreshing the list of passengers
				if (passengers != null)
				{
					prepareListView();
				}
				else
				{
					final FlexibusApp gs = (FlexibusApp) getApplication();
					String errormsg = gs.getDataHandler().getLastError(); 
					if (errormsg == null || errormsg.equals(""))
					{
						errormsg = "No passengers today";
					}
					Toast.makeText(getApplicationContext(), errormsg, Toast.LENGTH_SHORT).show();
					finish();
				}
			}
		}
	}

    public void processTrips ()
    {
		FlexibusApp gs = (FlexibusApp) getApplication();
		DBAdapter db = gs.getDatabase();
    	db.InitialiseDatabase(gs.getCurrentBusName());
    	busTrips = gs.getDataHandler().getTodaysBusTrips(null);
    	if (busTrips != null)
    	{
			for (BusTrip trip: busTrips)
			{
				db.insertBusTrip(trip);
				passengers = gs.getDataHandler().getPassengerList(trip.salesforce_ID);
				if (passengers != null)
				{
					for (Passenger pass: passengers)
					{
						db.insertPassenger(pass);
					}
				}
			}
    	}
    	else
    	{
			String errormsg = gs.getDataHandler().getLastError(); 
			if (errormsg == null || errormsg.equals(""))
			{
				errormsg = "No bustrips today";
			}
			Toast.makeText(getApplicationContext(), errormsg, Toast.LENGTH_SHORT).show();
    	}
    		
    }

    public void processPassengers ()
    {
		FlexibusApp gs = (FlexibusApp) getApplication();
		DBAdapter db = gs.getDatabase();
		passengers = db.getTodaysPassengers(gs.getCurrentBusTripID());
		if (passengers != null)
		{
			String[] passengerNumbers = db.getPassengerNumbers(db.getCurrentBusTrip());
			if (passengerNumbers != null)
			{
				freePassengers = Integer.parseInt(passengerNumbers[0]);
				farePayingPassengers = Integer.parseInt(passengerNumbers[1]);
			}
		}
		else
		{
			String errormsg = gs.getDataHandler().getLastError(); 
			if (errormsg == null || errormsg.equals(""))
			{
				errormsg = "No passengers today";
			}
			Toast.makeText(getApplicationContext(), errormsg, Toast.LENGTH_SHORT).show();
		}
   }
    
    public String getNote (Passenger thisPassenger)
    {
    	String note="";  
    	if (!thisPassenger.homePhone.equals("null")) note = "Home phone: "+ thisPassenger.homePhone + '\n';
    	if (!thisPassenger.mobilePhone.equals("null")) note += "Mobile: " + thisPassenger.mobilePhone + '\n';
    	if (!thisPassenger.getAddress().equals("null")) note += "Address: " + thisPassenger.getAddress() + '\n';
    	if (!thisPassenger.note.equals("null")) note += thisPassenger.note;
    	
    	if (note == "") note = "No details available about " + thisPassenger.name;
    	return note;
    }
    
    public void prepareListView() 
	{
		if (passengers != null)
		{
			final FlexibusApp gs = (FlexibusApp) getApplication();
			DBAdapter db = gs.getDatabase();
			db.open();
			tripTitle = db.getCurrentBusTripTitle();
			tripID = db.getCurrentBusTrip();

			ListView lv = getListView();
			addTitle (lv);
			lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
//			setListAdapter(new PassengerAdapter(this, android.R.layout.simple_list_item_multiple_choice, passengers));
			lv.setAdapter(new PassengerAdapter(this, android.R.layout.simple_list_item_multiple_choice, passengers));
			
			lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() 
			{
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) 
			    {
					if (position == 0) // do nothing if the header is clicked on
					{
						return true;
					}
			    	// When clicked, show a toast with the note
					int whichPassenger = position - CONSTANTS.LIST_HEADER_OFFSET;
			    	String note = getNote (passengers.get(whichPassenger));
			    	Toast.makeText(PassengerList.this, note, Toast.LENGTH_LONG).show();
			    	return true;
			    }
			});
			
			lv.setOnItemClickListener(new OnItemClickListener() 
			{
			    public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			    {
			    	if (position == 0) // do nothing if the header is clicked on
					{
						return;
					}
			    	FlexibusApp gs = (FlexibusApp) getApplication();
			    	DBAdapter db = gs.getDatabase();
			    	Passenger selectedPassenger = passengers.get(position - CONSTANTS.LIST_HEADER_OFFSET);
			    	// is this a check or an uncheck?
			    	CheckedTextView selectedView = (CheckedTextView)view;
			    	if (!db.isPassengerOnBoard(selectedPassenger))
			    	{
				    	Integer contribution = 0;
				    	if (!selectedPassenger.hasFreeTravel())
				    	{
				    		// TODO ask for a contribution
				    	}
				    	db.registerPassengerStatus (selectedPassenger, contribution, true);
				    	selectedView.setChecked(true);
				    	incrementPassengerCount (selectedPassenger);
			    	}
			    	else
			    	{
				    	decrementPassengerCount (selectedPassenger);
				    	db.registerPassengerStatus (selectedPassenger, 0, false);
			    		selectedView.setChecked(false);
			    	}
			    	selectedView.setText(selectedPassenger.toString());
			    	gs.setPassengerListDirty();
			    	return;
			    }
			  });

			preparePassengersForCheckedListView(db);
		}
	}

	protected void decrementPassengerCount(Passenger selectedPassenger) 
	{
		// decrement by the number of companions + the passenger
		int amountToDecrement = selectedPassenger.getNumberOfCompanions() + 1;
		 if (selectedPassenger.hasFreeTravel())
		 {
			freePassengers-= amountToDecrement;
			 if (freePassengers<0)
			 {
				 freePassengers = 0;
			 }
		 }
		 else
		 {
			 farePayingPassengers-= amountToDecrement;
			 if (farePayingPassengers<0)
			 {
				 farePayingPassengers = 0;
			 }
		 }
	}

	protected void incrementPassengerCount(Passenger selectedPassenger) 
	{
		 if (selectedPassenger.hasFreeTravel())
		 {
			 freePassengers++;
		 }
		 else
		 {
			 farePayingPassengers++;
		 }
	}

	public void preparePassengersForCheckedListView(DBAdapter db) 
	{
		int totalItems = getListView().getCount() - CONSTANTS.LIST_HEADER_OFFSET;
		if (totalItems > 0)
		{
			for (int position=0; position<totalItems; position++)
			{
				Passenger pass = passengers.get(position);
				pass.flagAsOnBoard (db.isPassengerOnBoard(pass));
			}
		}
	}

	private void addTitle(ListView lv) 
	{
		header = new TextView(this);
		header.setText(tripTitle);
		header.setClickable(false);
		lv.addHeaderView(header);
	}
}
