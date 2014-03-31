package ie.enclude.flexibus;

import java.io.UnsupportedEncodingException;

import org.json.JSONArray;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;

import ie.enclude.flexibus.R;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class BusListView extends ListActivity implements SalesforceResponseInterface
{
	ProgressDialog m_progress;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		Log.v("FlexibusLogging", "BusListView: onCreate");
		m_progress = ProgressDialog.show(this, "Loading...", "Loading bus list from Salesforce");
		FlexibusApp gs = (FlexibusApp) getApplication();
		gs.getDataHandler().getBuses(this);
	}
	
	@Override
	public void responseReceived(String result) 
	{
			Log.v("FlexibusLogging", "BusListView: onPostExecute " + result);
	        if (m_progress != null)
	        {
	        	m_progress.dismiss();
				m_progress = null;
	        }
			if (!result.equals(""))
			{
				Toast.makeText(BusListView.this, result, Toast.LENGTH_SHORT).show();
				finish();
			}
			else
			{
				final FlexibusApp gs = (FlexibusApp) getApplication();
				String[] busnames = gs.getDataHandler().getBusNames();
				if (busnames != null)
				{
					if (busnames.length > 0)
					{
						setListAdapter(new ArrayAdapter<String>(BusListView.this, R.layout.list_item, busnames));
					}
			
					 ListView lv = getListView();
					  lv.setTextFilterEnabled(true);
			
					  lv.setOnItemClickListener(new OnItemClickListener() 
					  {
					    public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
					    {
					    	String oldBusName = gs.getDataHandler().getCurrentBusName();
					    	gs.getDataHandler().setSelectedBus(((TextView) view).getText());
					    	if (!oldBusName.equals(gs.getDataHandler().getCurrentBusName()))
					    	{
					    		gs.clearPassengerList();
					    	}
					      // When clicked, show a toast with the TextView text
					      Toast.makeText(BusListView.this, ((TextView) view).getText(), Toast.LENGTH_SHORT).show();
					      finish();
					    }
					  });
				}
			}
	

	}

}

