package ie.enclude.flexibus;

import ie.enclude.flexibus.PassengerList.SalesforceLoadProviderSheetTask;
import ie.enclude.flexibus.database.DBAdapter;
import ie.enclude.flexibus.util.BusTrip;
import ie.enclude.flexibus.util.BusTripAdapter;
import ie.enclude.flexibus.util.Passenger;
import ie.enclude.flexibus.util.PassengerAdapter;

import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
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

public class TodaysBusServiceList extends ListActivity
{
	public static final String DEBUG_TAG="FlexibusLogging";
	List<BusTrip>busTrips;
	private TextView header;

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
    	super.onCreate(icicle);
        
    	mRuntimeOrientation = this.getScreenOrientation();
        if (icicle != null)
    	{
    		
    	}
    	
		FlexibusApp gs = (FlexibusApp) getApplication();
		busTrips = gs.getCurrentServiceList();
		if (busTrips == null)
		{
			m_progress = ProgressDialog.show(this, "Loading...", "Loading today's services from Salesforce");
    		mDisableScreenRotation = true;
			new SalesforceLoadProviderSheetTask().execute(gs);
		}
		else
		{
			prepareListView();
		}
    }
    
    public class SalesforceLoadProviderSheetTask extends AsyncTask<FlexibusApp, Void, String> 
	{
		@Override
		protected String doInBackground(FlexibusApp... gs) 
		{
			List<Passenger>passengers;
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
					String result = gs[0].getDataHandler().localLogin();
					if (gs[0].LoggedIn())
					{
						Log.v(DEBUG_TAG, "TodaysBusServiceList doInBackground Logged In");
						busTrips = gs[0].getDataHandler().getTodaysBusTrips();
						if (busTrips != null)
						{
							Log.v(DEBUG_TAG, "Bus trips found");
							db.InitialiseDatabase(gs[0].getCurrentBusName());
							for (BusTrip trip: busTrips)
							{
								Log.v(DEBUG_TAG, "Inserting Bus trips");
								db.insertBusTrip(trip);
								passengers = gs[0].getDataHandler().getPassengerList(trip.salesforce_ID);
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
							return gs[0].getDataHandler().getLastError();
						}
					}
					else
					{
						return result;
					}
				}
				else
				{
					busTrips = gs[0].getDataHandler().getTodaysBusTrips();
				}
				return "";
			}
			catch (Exception e)
			{
				Log.v(DEBUG_TAG, "doInBackground in SalesforceLoadProviderSheetTask " + e.getMessage());
				return e.getMessage();
			}
		}

		protected void onPostExecute(String result) 
		{
	        if (m_progress != null)
	        {
	        	m_progress.dismiss();
				m_progress = null;
	        }
    		mDisableScreenRotation = false;
			if (!result.equals(""))
			{
				Toast.makeText(TodaysBusServiceList.this, result, Toast.LENGTH_SHORT).show();
				finish();
			}
			else
			{
				if (busTrips != null)
				{
					prepareListView();
				}
				else
				{
					final FlexibusApp gs = (FlexibusApp) getApplication();
					String errormsg = gs.getDataHandler().getLastError(); 
					if (errormsg == null || errormsg.equals(""))
					{
						errormsg = "No trips for this bus today";
					}
					Toast.makeText(getApplicationContext(), errormsg, Toast.LENGTH_SHORT).show();
					finish();
				}
			}
		}
	}
    public void prepareListView() 
	{
		if (busTrips != null)
		{
			final FlexibusApp gs = (FlexibusApp) getApplication();
			DBAdapter db = gs.getDatabase();
			db.open();

			ListView lv = getListView();
			addTitle (lv);
			lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
			setListAdapter(new BusTripAdapter(this, android.R.layout.simple_list_item_1, busTrips));
			
			lv.setOnItemClickListener(new OnItemClickListener() 
			{
			    public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			    {
			    	if (position == 0) // do nothing if the header is clicked on
					{
						return;
					}
			    	FlexibusApp gs = (FlexibusApp) getApplication();
//			    	DBAdapter db = gs.getDatabase();
			    	BusTrip selectedTrip = busTrips.get(position - CONSTANTS.LIST_HEADER_OFFSET);
			    	gs.setCurrentBusTripID(selectedTrip.salesforce_ID);
			    	
			    	launchPassengerList();

			    	return;
			    }
			  });

		}
	}

	public void launchPassengerList()
	{
		// could wipe previous picklist values here
		Intent i = new Intent(this, PassengerList.class);
		startActivity(i);
	}

	private void addTitle(ListView lv) 
	{
		header = new TextView(this);
		header.setText(((FlexibusApp) getApplication()).getCurrentBusName());
		header.setClickable(false);
		lv.addHeaderView(header);
	}
}
