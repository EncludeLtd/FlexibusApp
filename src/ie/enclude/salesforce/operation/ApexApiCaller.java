/**
 * Copyright (C) 2008 Dai Odahara.
 */

// client.sendAsync calls are more difficult than they need to be because success can result in error code 201
// when the request is RestRequest.getRequestForCreate

package ie.enclude.salesforce.operation;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.android.volley.VolleyError;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import android.util.Log;
import ie.enclude.flexibus.CONSTANTS;
import ie.enclude.flexibus.FlexibusApp;
import ie.enclude.flexibus.SalesforceResponseInterface;
import ie.enclude.flexibus.database.DBAdapter;
import ie.enclude.flexibus.util.BusTrip;
import ie.enclude.flexibus.util.Passenger;
import ie.enclude.flexibus.util.PassengerTrip;
import ie.enclude.salesforce.util.OAuthTokens;

import ie.enclude.salesforce.util.StaticInformation;
import com.salesforce.androidsdk.rest.RestResponse;

/**
 * This class works with Salesforce Apex API. 
 * @author Dai Odahara
 */
public class ApexApiCaller 
{
	public static final String DEBUG_TAG="FlexibusLogging";

	private static final String API_VERSION = "v20.0";
	
	private OAuthTokens accessTokens;
	private String m_lastErrorMsg="";

	/** Called when the activity is first created. */
	public ApexApiCaller() {}

	public String getLastError ()
	{
		return m_lastErrorMsg;
	}
	
	public String login() 
	{
		String errorMsg = "";
		try 
		{
			Log.v(DEBUG_TAG, "Start REST Login");
			String reqUrl = StaticInformation.API_SERVER_URL + "/services/oauth2/token";
	        
	        DefaultHttpClient client = new DefaultHttpClient();
	        HttpPost post = new HttpPost(reqUrl);
	        try 
	        {
	        	List<NameValuePair> params = new ArrayList<NameValuePair>(6);
		        	params.add(new BasicNameValuePair("client_id", StaticInformation.CONSUMER_KEY));
		        	params.add(new BasicNameValuePair("client_secret", StaticInformation.CONSUMER_SECRET));
		           	params.add(new BasicNameValuePair("username", StaticInformation.USER_ID));
		           	params.add(new BasicNameValuePair("password", StaticInformation.USER_PW)); // +StaticInformation.USER_TOKEN));
		            params.add(new BasicNameValuePair("redirect_uri", "sfdcsample:success"));
		        	params.add(new BasicNameValuePair("grant_type", "password"));
		        	post.setEntity(new UrlEncodedFormEntity(params));
	
		        	
					HttpResponse resp = client.execute(post);
					String responseBody = EntityUtils.toString(resp.getEntity());
					errorMsg = parseToken(responseBody);
					
			}
	        catch (ClientProtocolException e) 
	        {
				Log.v(DEBUG_TAG, "login " + e.getMessage());
				errorMsg = e.getMessage();
			}
	        catch (IOException e) 
	        {
				Log.v(DEBUG_TAG, "login " + e.getMessage());
				errorMsg = e.getMessage();
			}
			Log.v(DEBUG_TAG, "End Login");
		}
		catch (Exception e) 
		{
			Log.v(DEBUG_TAG, "login " + e.getMessage());
			errorMsg = e.getMessage();
		}
		return errorMsg;
	}

	 public String parseToken(String responseFromSalesforce) 
	 {
	    	OAuthTokens myTokens = new OAuthTokens(responseFromSalesforce);
			setAccessTokens(myTokens);
			return myTokens.ErrorMsg();
	}

   	
	    public OAuthTokens getAccessTokens() 
	    {
	    	return accessTokens;
	    }

	    public void setAccessTokens(OAuthTokens accessTokens) 
	    {
	    	this.accessTokens = accessTokens;
	    }
		
	
	public boolean getBuses(final LocalDataHandler dataHandler, final SalesforceResponseInterface sfrp) 
	{	
		if (FlexibusApp.client != null)
		{
			RestClient client = FlexibusApp.client;
			Log.v(DEBUG_TAG, "Start getBuses");
			String soqlQuery = "Select Id, Name, ODO_Reading__c From Bus__c where Vehicle_Status__c = 'In Service' order by Name";
			
			try
			{
				RestRequest restRequest = RestRequest.getRequestForQuery(API_VERSION, soqlQuery);

				client.sendAsync(restRequest, new AsyncRequestCallback() {
					@Override
					public void onSuccess(RestRequest request, RestResponse result) {
						Log.v(DEBUG_TAG, "getBuses onSuccess");
						try {
							JSONArray records = result.asJSONObject().getJSONArray("records");
							int count = records.length();
							dataHandler.setBusNames(new String[count]);
							dataHandler.setBusOdoReading(new String[count]);
							dataHandler.setBuses(new JSONObject[count]);
							for (int i = 0; i < records.length(); i++) {
								JSONObject record = (JSONObject) records.get(i);
								String accountName = record.getString("Name");
								String odoReading = record.getString("ODO_Reading__c");
								dataHandler.getBusNames()[i] = accountName;
								dataHandler.getBusOdoReading()[i] = odoReading;
								dataHandler.getBusesObjects()[i] = record;
							}	
							sfrp.responseReceived("");
						} catch (Exception e) {
							onError(e);
						}
					}
					
					@Override
					public void onError(Exception e) {
						Log.v(DEBUG_TAG, "getBuses " + e.getMessage());
						sfrp.responseReceived(e.getMessage());
				}
				});
				return true;
			} 
			catch(Exception e)
			{
				Log.v(DEBUG_TAG, "getBuses " + e.getMessage());
				return false;
			}
		}
		else
		{
			Log.v(DEBUG_TAG, "getBuses - not logged in");
			m_lastErrorMsg = "Not logged In";
			return false;
		}	
	}

	public String addOdometerReadingToSelectedBus (final LocalDataHandler dataHandler, final String odoReading, final SalesforceResponseInterface sfrp)
	{
		if (FlexibusApp.client != null)
		{
			RestClient client = FlexibusApp.client;
			
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("ODO_Reading__c", odoReading);
			data.put("Bus__c", dataHandler.getCurrentBusID());
			data.put("Reading_source__c", "Phone App");
			
			try 
			{
				RestRequest restRequest = RestRequest.getRequestForCreate(API_VERSION, "Odometer_Reading__c", data);
					     
				client.sendAsync(restRequest, new AsyncRequestCallback() {
					private void processSuccess ()
					{
						Log.v(DEBUG_TAG, "sendODOReading onSuccess");
						dataHandler.setCurrentBusOdoReading(odoReading);
						sfrp.responseReceived("Odometer reading accepted");
					}
					@Override
					public void onSuccess(RestRequest request, RestResponse result) {
						processSuccess ();
						}
										
					@Override
					public void onError(Exception e) {
						Log.v(DEBUG_TAG, "addOdometerReadingToSelectedBus onError " + ((VolleyError)e).networkResponse.statusCode);
						if (e instanceof IOException || ((VolleyError)e).networkResponse.statusCode == 201) 
						{
							processSuccess ();
						}
						else sfrp.responseReceived("Failed to add odometer reading " + e.getMessage());
						
				}
				});
				return "";
			}
			catch (UnsupportedEncodingException e) 
			{
				Log.v(DEBUG_TAG, "addOdometerReadingToSelectedBus " + e.getMessage());
				return e.toString();
			}
			catch (ClientProtocolException e) 
			{
				Log.v(DEBUG_TAG, "addOdometerReadingToSelectedBus " + e.getMessage());
				return e.toString();
			} 
			catch (IOException e) 
			{
				Log.v(DEBUG_TAG, "addOdometerReadingToSelectedBus " + e.getMessage());
				return e.toString();
			}	
		}
		else
		{
			return "Not logged In";
		}
	}
	public String getOneBus(final LocalDataHandler dataHandler, String busName, final SalesforceResponseInterface sfrp)
	{
		
		if (FlexibusApp.client != null)
		{
			RestClient client = FlexibusApp.client;
			Log.v(DEBUG_TAG, "Start getOneBus");
			String soqlQuery = "Select Id, Name, ODO_Reading__c From Bus__c where Name = '"+busName+"'";
			
			try
			{
			RestRequest restRequest = RestRequest.getRequestForQuery(API_VERSION, soqlQuery);

			client.sendAsync(restRequest, new AsyncRequestCallback() {
				@Override
				public void onSuccess(RestRequest request, RestResponse result) {
					Log.v(DEBUG_TAG, "getOneBus onSuccess");
					try {
						JSONArray records = result.asJSONObject().getJSONArray("records");
						JSONObject record = (JSONObject) records.get(0);
						dataHandler.setOneBusDetails(record);
						
						sfrp.responseReceived("");
					} catch (Exception e) {
						onError(e);
					}
				}
				
				@Override
				public void onError(Exception e) {
					Log.v(DEBUG_TAG, "getBuses " + e.getMessage());
					sfrp.responseReceived(e.getMessage());
			}
			});
			}
			catch (UnsupportedEncodingException e) 
			{
				Log.v(DEBUG_TAG, "addOdometerReadingToSelectedBus " + e.getMessage());
				return e.toString();
			}

		}
		return null;
	}

	@SuppressWarnings("unused")
	private String ProcessHttpCall(HttpUriRequest request) throws ClientProtocolException, IOException, JSONException 
	{
		DefaultHttpClient client = new DefaultHttpClient(); 
		HttpResponse response = client.execute(request);
		String result = EntityUtils.toString(response.getEntity());
		
		int resultCode = response.getStatusLine().getStatusCode();
		if (resultCode == 400 || resultCode == 401)
		{
			HandleRESTError (response.getStatusLine().getStatusCode(), result);
			if (resultCode == 401) // try again
			{
				response = client.execute(request);
				result = EntityUtils.toString(response.getEntity());
				resultCode = response.getStatusLine().getStatusCode();
			}
		}


		if (resultCode == 200 || resultCode == 201)
		{
			return result;
		}
		else
		{
			return "";
		}
	}

	private String HandleRESTError(int resultCode, String result) throws JSONException 
	{
		if (resultCode == 401)
		{
			// attempt to login
			login();
		}
		
		JSONArray value = (JSONArray)new JSONTokener(result).nextValue();
		JSONObject object = (JSONObject)value.get(0);
		String errorCode = object.getString("errorCode");
		if (errorCode != null)
		{
			return object.getString("message");
		}
		else
		{
			return "";
		}
	}
	
	public List<String> getFieldList (final LocalDataHandler dataHandler, String objectName) 
	{
		if (FlexibusApp.client != null)
		{
			RestClient client = FlexibusApp.client;
			try 
			{
				RestRequest restRequest = RestRequest.getRequestForDescribe(API_VERSION, objectName);

				client.sendAsync(restRequest, new AsyncRequestCallback() {
				
				@Override
				public void onSuccess(RestRequest request, RestResponse result)
				{
					try
					{
						JSONArray records = result.asJSONObject().getJSONArray("fields");
						List<String> labels = new ArrayList<String>(records.length());	
						for (int i=0;i<records.length();i++) 
						{
							JSONObject record = (JSONObject) records.get(i);
							String name = record.getString("name");
							String type = record.getString("type");
							if (name.endsWith("__c"))
							{
								if (type.equals("picklist"))
								{
									String label = record.getString("label");
									labels.add(label);
									dataHandler.setDriversStartupCheckListName(label, name);
									
									JSONArray pickList = record.getJSONArray("picklistValues");
									List<String> pickListValues = new ArrayList<String>(pickList.length());
									for (int j=0; j<pickList.length();j++)
									{
										JSONObject pickListItem = (JSONObject) pickList.get(j);
										pickListValues.add(pickListItem.getString("value"));
									}
									if (pickList.length() > 0)
									{
										dataHandler.addPickListToDriversStartupCheckList(label, pickListValues);
									}
								}
							}
						}

						if (records.length() > 0)
						{
							m_lastErrorMsg = "";
							Collections.sort(labels, String.CASE_INSENSITIVE_ORDER);
							dataHandler.setStartupCheckLabels (labels);
						}
						else
						{
							m_lastErrorMsg = "No items in this checklist";
						}
					}
					catch (JSONException e) 
					{
						Log.v(DEBUG_TAG, "getFieldList " + e.getMessage());
					} 
					catch (IOException e) 
					{
						Log.v(DEBUG_TAG, "getFieldList " + e.getMessage());
					}	
				}
				
					@Override
					public void onError(Exception e) {
						m_lastErrorMsg = "Failed to process Http call";
					}
				});
				m_lastErrorMsg = "";
			}
			catch (Exception e) 
			{
				Log.v(DEBUG_TAG, "getFieldList " + e.getMessage());
				m_lastErrorMsg = e.toString();
			} 
		}
		else
		{
			Log.v(DEBUG_TAG, "getFieldList not logged in");
			m_lastErrorMsg = "Not logged In";
		}	
		return null;
	}

	public String sendStartupCheckListToSalesforce(LocalDataHandler dataHandler,
			Map<String, List<String>> listofPickLists,
			List<String> startupCheckLabels, String startupCheckListFaultReport, final SalesforceResponseInterface sfrp) 
	{
		if (FlexibusApp.client != null)
		{
			RestClient client = FlexibusApp.client;
			Map<String, Object> data = new HashMap<String, Object>();
			
			try 
			{
				List<String> labels = dataHandler.getFieldList("Bus_Startup_CheckList__c");
				for (int i=0; i<labels.size(); i++)
				{
					String fieldlabel = labels.get(i);
					String fieldname = dataHandler.getStartupCheckListFieldName(fieldlabel);
					String itemPicked = dataHandler.getPickListResults(fieldlabel);
					if (itemPicked != null)
					{
						data.put(fieldname, itemPicked);
					}
				}
				if (startupCheckListFaultReport.length() > 0)
				{
					if (startupCheckListFaultReport.length() > CONSTANTS.MAX_NOTE_SIZE)
					{
						data.put("Note__c", startupCheckListFaultReport.substring(0, CONSTANTS.MAX_NOTE_SIZE-1));
					}
					else
					{
						data.put("Note__c", startupCheckListFaultReport);
					}
				}
				
				data.put("Bus_Checked__c", dataHandler.getCurrentBusID());

				RestRequest restRequest = RestRequest.getRequestForCreate(API_VERSION, "Bus_Startup_CheckList__c", data);
				     
				client.sendAsync(restRequest, new AsyncRequestCallback() {
					private void processSuccess ()
					{
						Log.v(DEBUG_TAG, "sendStartupCheckListToSalesforce onSuccess");
						sfrp.responseReceived("Startup checklist accepted");
					}
					@Override
					public void onSuccess(RestRequest request, RestResponse result) {
						processSuccess ();
						}
										
					@Override
					public void onError(Exception e) {
						Log.v(DEBUG_TAG, "sendStartupCheckListToSalesforce onError " + ((VolleyError)e).networkResponse.statusCode);
						if (e instanceof IOException || ((VolleyError)e).networkResponse.statusCode == 201) 
						{
							processSuccess ();
						}
						else sfrp.responseReceived("Failed to add startup checklist " + e.getMessage());
					}
				});
				return "";
			}
			catch (UnsupportedEncodingException e) 
			{
				Log.v(DEBUG_TAG, "sendStartupCheckListToSalesforce " + e.getMessage());
				return e.toString();
			}
			catch (ClientProtocolException e) 
			{
				Log.v(DEBUG_TAG, "sendStartupCheckListToSalesforce " + e.getMessage());
				return e.toString();
			} 
			catch (IOException e) 
			{
				Log.v(DEBUG_TAG, "sendStartupCheckListToSalesforce " + e.getMessage());
				return e.toString();
			}	
		}
		else
		{
			Log.v(DEBUG_TAG, "sendStartupCheckListToSalesforce not logged in");
			return "Not logged In";
		}
	}

	public String recordFuelPurchased(LocalDataHandler dataHandler, FlexibusApp gs, final SalesforceResponseInterface sfrp) 
	{
		if (FlexibusApp.client != null)
		{
			RestClient client = FlexibusApp.client;
			
			try 
			{
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("Mileage__c", gs.getSavedBusOdoReading());
				data.put("Fuel_Litres__c", gs.getSavedFuelPurchased());
				data.put("Bus__c", dataHandler.getCurrentBusID());
					     
				RestRequest restRequest = RestRequest.getRequestForCreate(API_VERSION, "Fuel_Purchase__c", data);

				client.sendAsync(restRequest, new AsyncRequestCallback() {
					private void processSuccess ()
					{
						Log.v(DEBUG_TAG, "recordFuelPurchased onSuccess");
						sfrp.responseReceived("Fuel purchased record accepted");
					}
					@Override
					public void onSuccess(RestRequest request, RestResponse result) {
						processSuccess ();
						}
										
					@Override
					public void onError(Exception e) {
						Log.v(DEBUG_TAG, "recordFuelPurchased onError " + ((VolleyError)e).networkResponse.statusCode);
						if (e instanceof IOException || ((VolleyError)e).networkResponse.statusCode == 201) 
						{
							processSuccess ();
						}
						else sfrp.responseReceived("Failed to add fuel purchased record " + e.getMessage());
					}
				});
			}
			catch (UnsupportedEncodingException e) 
			{
				Log.v(DEBUG_TAG, "recordFuelPurchased " + e.getMessage());
				return e.toString();
			}
			catch (ClientProtocolException e) 
			{
				Log.v(DEBUG_TAG, "recordFuelPurchased " + e.getMessage());
				return e.toString();
			} 
			catch (IOException e) 
			{
				Log.v(DEBUG_TAG, "recordFuelPurchased " + e.getMessage());
				return e.toString();
			}	
		}
		else
		{
			Log.v(DEBUG_TAG, "recordFuelPurchased not logged in");
			return "Not logged In";
		}
		return "";
	}

	public List<BusTrip> getTodaysBusTrips(final LocalDataHandler dataHandler) 
	{
		m_lastErrorMsg = "";
		if (FlexibusApp.client != null)
		{
			RestClient client = FlexibusApp.client;
			Log.v(DEBUG_TAG, "Start getTodaysBusTrips");
			String currentBus = dataHandler.getCurrentBusID();
			String soqlQuery = "Select Id, Bus_Trip_Unique_ID__c, Estimated_Start_Time__c, Estimated_End_Time__c, Driver_name__c From Bus_Trip__c "
					+ "where Actual_Bus__c = '" + currentBus + "' and Date__c = TODAY order by Estimated_Start_Time__c";
			
			try
			{
				RestRequest restRequest = RestRequest.getRequestForQuery(API_VERSION, soqlQuery);
	
				RestResponse result = client.sendSync(restRequest);
				try 
				{
					JSONArray records = result.asJSONObject().getJSONArray("records");
					int count = records.length();
								
					DBAdapter db = FlexibusApp.db;
					db.open();
					db.InitialiseDatabase(dataHandler.getCurrentBusName());

					List<BusTrip> trips = new ArrayList<BusTrip>(count);
								
					for (int i=0;i<count;i++) {
						JSONObject record = (JSONObject) records.get(i);
						String salesforce_id = record.getString("Id");
						String uniqueID = record.getString("Bus_Trip_Unique_ID__c");
						String startTime = record.getString("Estimated_Start_Time__c");
						String endTime = record.getString("Estimated_End_Time__c");
						String driverName = record.getString("Driver_name__c");
						BusTrip oneTrip = new BusTrip (salesforce_id, startTime, endTime, uniqueID, driverName);
						trips.add (oneTrip); // TODO - this is overkill either use the database or the variables
						db.insertBusTrip(oneTrip);
					}
					m_lastErrorMsg = "";
					dataHandler.setTodaysTrips (trips);
							
					if (getPassengers (trips))
					{
						return trips;
					}
					else
					{
						return null;
					}
				}
				catch (IOException e) 
				{
					Log.v(DEBUG_TAG, "getTodaysBusTrips " + e.getMessage());
					m_lastErrorMsg = e.toString();
				} 
				catch (JSONException e) 
				{
					Log.v(DEBUG_TAG, "getTodaysBusTrips " + e.getMessage());
					m_lastErrorMsg = e.toString();
				}
			}
			catch (UnsupportedEncodingException e) 
			{
				Log.v(DEBUG_TAG, "getTodaysBusTrips " + e.getMessage());
				return null;
			} catch (IOException e1) {
				Log.v(DEBUG_TAG, "getTodaysBusTrips " + e1.getMessage());
				m_lastErrorMsg = e1.toString();
			}
			return null;
		}
		return null;
	}

	public Boolean getPassengers (List<BusTrip> trips)
	{
		RestClient client = FlexibusApp.client;
		
		for (BusTrip oneTrip: trips)
		{
			String busTripID = oneTrip.salesforce_ID;
		
			String soqlQuery = "Select Id, Name, HomePhone, MobilePhone, OtherStreet, OtherCity, Note__c, Free_Travel_Pass_Number__c from Contact "
				+ "where id in (select Passenger__c from Passenger_Trip__c where Bus_Trip__c = '" + busTripID + "')";
		
			try
			{
				RestRequest restRequest = RestRequest.getRequestForQuery(API_VERSION, soqlQuery);
				RestResponse result = client.sendSync(restRequest);
				try
				{
					JSONArray records = result.asJSONObject().getJSONArray("records");
					int count = records.length();
					
					DBAdapter db = FlexibusApp.db;
					for (int i=0;i<count;i++) 
					{
						JSONObject record = (JSONObject) records.get(i);
						String salesforce_id = record.getString("Id");
						String name = record.getString("Name");
						String phone1 = record.getString("HomePhone");
						String phone2 = record.getString("MobilePhone");
						String street = record.getString("OtherStreet");
						String town = record.getString("OtherCity");
						String note = record.getString ("Note__c");
						String ftp = record.getString("Free_Travel_Pass_Number__c");
						db.insertPassenger(new Passenger (salesforce_id, busTripID, name, phone1, phone2, street, town, note, ftp, 0));
					}
				}
				catch (IOException e) 
				{
					Log.v(DEBUG_TAG, "getPassengers " + e.getMessage());
					m_lastErrorMsg = e.toString();
					return false;
				} 
				catch (JSONException e) 
				{
					Log.v(DEBUG_TAG, "getPassengers " + e.getMessage());
					m_lastErrorMsg = e.toString();
					return false;
				}
			}
			catch (UnsupportedEncodingException e) 
			{
				Log.v(DEBUG_TAG, "getPassengers " + e.getMessage());
				m_lastErrorMsg = e.toString();
				return false;
			} 
			catch (IOException e1) 
			{
				Log.v(DEBUG_TAG, "getPassengers " + e1.getMessage());
				m_lastErrorMsg = e1.toString();
				return false;
			}
		}
		return true;
	}
	
	public List<Passenger> getPassengerList(LocalDataHandler dataHandler, String busTripID) 
	{
		DBAdapter db = FlexibusApp.db;
		return db.getAllPassengers (busTripID);
	}

	// these are only retrieved when the driver wants to send the trip reports
	public JSONArray getPassengerTripRecords(String busTripIDs)
	{
		
		if (FlexibusApp.client != null)
		{
			RestClient client = FlexibusApp.client;

			Log.v(DEBUG_TAG, "Start getPassengerTripRecords");
			String soqlQuery = "Select Id, Bus_Trip__c, Passenger__c From Passenger_Trip__c where Bus_Trip__c in ("+busTripIDs+")";
			
			try
			{
				RestRequest restRequest = RestRequest.getRequestForQuery(API_VERSION, soqlQuery);

				RestResponse result = client.sendSync(restRequest);

				return result.asJSONObject().getJSONArray("records");
			}
			catch (IOException e) 
			{
				Log.v(DEBUG_TAG, "getPassengerTripRecords " + e.getMessage());
			} 
			catch (JSONException e) 
			{
				Log.v(DEBUG_TAG, "getPassengerTripRecords " + e.getMessage());
			}
		}
		return null;
	}

	public String sendTripReports(LocalDataHandler dataHandler,  DBAdapter database) 
	{
		String result = "No trips to send";
 		Log.v(DEBUG_TAG, "Start sendTripReports");
 
		String busTripIDs = database.getListOfBusTripsInPassengerTripTable();
		if (busTripIDs != null)
		{
			// first save the bus trips, then save the passenger trips
			String busTripsResult = sendBusTripReports (busTripIDs, database);
			
			if (busTripsResult != null)
			{
				return busTripsResult;
			}
			
			JSONArray passengerTrips = getPassengerTripRecords (busTripIDs);
			if (passengerTrips != null)
			{
				for (int i=0; i<passengerTrips.length(); i++)
				{
					JSONObject passengerTrip;
					try
					{
						passengerTrip = (JSONObject) passengerTrips.get(i);
						String busTripID = passengerTrip.getString("Bus_Trip__c");
						String passengerID = passengerTrip.getString("Passenger__c");
						PassengerTrip pass = database.findPassengerTrip (busTripID, passengerID);
						if (pass != null)
						{
							Log.v(DEBUG_TAG, "sendTripReports Saving Trip Report for " + passengerTrip.getString("Id"));
							sendOnePassengerTripReport (passengerTrip.getString("Id"), busTripID, passengerID, pass.passengerContribution, pass.actualCompanions, pass.passengerOnBoard);
						}
					}
					catch (JSONException e)
					{
					Log.v(DEBUG_TAG, "sendTripReports Exception" + e.getMessage());
					return e.getMessage();
					}
				}
			}
			else
			{
				result = "No individual passengers found";
			}
			result = "Trip reports sent";
		}
		Log.v(DEBUG_TAG, "End sendTripReports2");
		return result;
	}
	
	private String sendBusTripReports(String busTripIDs, DBAdapter database) 
	{
		String[]busTrips = busTripIDs.split(",");
		Log.v(DEBUG_TAG, "Start saveBusTripRecordsUsingSoap");
		
		for (String tripID: busTrips)
		{
			tripID = tripID.replaceAll("'", "");
			
			String[] passengerNumbers = database.getPassengerNumbers(tripID);
			if (passengerNumbers != null)
			{
				if (FlexibusApp.client != null)
				{
					RestClient client = FlexibusApp.client;
					
					Map<String, Object> data = new HashMap<String, Object>();
					data.put("Free_Travel_Pass_Passengers__c", passengerNumbers[0]);
					data.put("Fare_Paying_Passengers__c", passengerNumbers[1]);
					
					try 
					{
						RestRequest restRequest = RestRequest.getRequestForUpdate(API_VERSION, "Bus_Trip__c", tripID, data);
						client.sendSync(restRequest);
					}
					catch (IOException e)
					{
						Log.v(DEBUG_TAG, "saveBusTripReports " + e.getMessage());
						return e.getMessage();
					}
				}
			}
		}
		Log.v(DEBUG_TAG, "End saveBusTripRecordsUsingSoap");
		return null;
	}
	


	private String sendOnePassengerTripReport(String passengerTripID, String busTripID, String passengerID, int contribution, int companions, boolean status) 
	{
		Log.v(DEBUG_TAG, "Start saveTripReport for ID " + passengerTripID + " Status flag is " + status);
		if (FlexibusApp.client != null)
		{
			RestClient client = FlexibusApp.client;
			
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("Contribution__c", Integer.toString(contribution));
			data.put("Passenger_Trip_Status__c", status?"Travelled":"No Show");
			
			try 
			{
				RestRequest restRequest = RestRequest.getRequestForUpdate(API_VERSION, "Passenger_Trip__c", passengerTripID, data);
					     
				client.sendSync(restRequest);
			}
			catch (IOException e)
			{
				Log.v(DEBUG_TAG, "saveBusTripReports " + e.getMessage());
				return e.getMessage();
			}
		}
		return null;
	}

}