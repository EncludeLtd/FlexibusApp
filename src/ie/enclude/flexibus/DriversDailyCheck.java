package ie.enclude.flexibus;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class DriversDailyCheck  extends ListActivity 
{
	List<String>checkItems;
	List<String>unCheckedItems;
	boolean[] checkListState;
	int uncheckedCount=0;
	ProgressDialog m_progress;
	
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) 
    {
    	super.onCreate(icicle);
    	if (icicle != null)
    	{
    		checkListState = icicle.getBooleanArray(CONSTANTS.CHECKLISTSTATE_KEY);
    	}
		m_progress = ProgressDialog.show(this, "Loading...", "Loading startup checklist from Salesforce");
		FlexibusApp gs = (FlexibusApp) getApplication();
		new SalesforceLoadDailyCheckListTask().execute(gs);
    }
 
    @Override
    protected void onSaveInstanceState(Bundle icicle)
    {
		int totalItems = getListView().getCount();
		if (icicle != null && totalItems > 0)
		{
			SparseBooleanArray allItems = getListView().getCheckedItemPositions();
			boolean [] checkListItems = new boolean[totalItems];
			for (int i=0; i<totalItems; i++)
			{
				checkListItems[i] = allItems.get(i);
			}
			icicle.putBooleanArray(CONSTANTS.CHECKLISTSTATE_KEY, checkListItems);
		}
    }
    
	public class SalesforceLoadDailyCheckListTask extends AsyncTask<FlexibusApp, Void, String> 
	{
		@Override
		protected String doInBackground(FlexibusApp... gs) 
		{
			gs[0].getDataHandler().localLogin();
			checkItems = gs[0].getDataHandler().getFieldList("Bus_Startup_CheckList__c");
			if (checkItems != null)
			{
				return "";
			}
			else
			{
				return gs[0].getDataHandler().getLastError();
			}
		}

		@Override
		public void onPostExecute (String result)
		{
	        if (m_progress != null)
	        {
	        	m_progress.dismiss();
				m_progress = null;
	        }
			if (!result.equals(""))
			{
				Toast.makeText(DriversDailyCheck.this, result, Toast.LENGTH_SHORT).show();
				finish();
			}
			else
			{
				final FlexibusApp gs = (FlexibusApp) getApplication();
				checkItems = gs.getDataHandler().getFieldList("Bus_Startup_CheckList__c");
				if (checkItems != null)
				{
					prepareListView();
				}
				else
				{
					Toast.makeText(getApplicationContext(), gs.getDataHandler().getLastError(), Toast.LENGTH_SHORT).show();
					finish();
				}
			}
		}
	}
    
    @Override
    public void onBackPressed()
	{
		unCheckedItems = new ArrayList<String>(20);
		int totalItems = getListView().getCount();
		uncheckedCount = 0;
		final FlexibusApp gs = (FlexibusApp) getApplication();
		gs.getDataHandler().wipePreviousStartupListAnswers();
		SparseBooleanArray allItems = getListView().getCheckedItemPositions();
		for (int i=0; i<totalItems; i++)
		{
			if (!allItems.get(i))
			{
				uncheckedCount++;
				unCheckedItems.add(checkItems.get(i));
				
				final String[] items = gs.getDataHandler().getPickList(checkItems.get(i));
				final String alertTitle = checkItems.get(i);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			builder.setTitle(alertTitle)
    			.setItems(items, new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int item) 
    				{
    					Toast.makeText(getApplicationContext(), alertTitle + ": " + items[item], Toast.LENGTH_SHORT).show();
    					gs.getDataHandler().setPickListResult(alertTitle, items[item]);
    					if (isLastFaultItem())
    					{
    						setResult(CONSTANTS.AT_LEAST_ONE_FAULT);
    						finish();
    					}
 				}
    			});
    			AlertDialog chooseFault = builder.create();
				chooseFault.show();
			}
		}
		if (uncheckedCount == 0)
		{
			setResult(CONSTANTS.NO_FAULTS);
			finish();
		}
	}

	public void prepareListView() 
	{
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, checkItems));
		
		if (checkListState != null)
		{
			int totalItems = getListView().getCount();
			if (totalItems > 0)
			{
				for (int i=0; i<totalItems; i++)
				{
					getListView().setItemChecked(i, checkListState[i]);
				}
			}
		}
	}

	protected boolean isLastFaultItem() 
	{
		return --uncheckedCount <= 0;
	}
    
	
}
