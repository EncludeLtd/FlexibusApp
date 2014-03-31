package ie.enclude.salesforce.operation;

import java.util.List;

import ie.enclude.flexibus.FlexibusActivity;
import ie.enclude.flexibus.FlexibusApp;
import ie.enclude.flexibus.SalesforceResponseInterface;
import ie.enclude.flexibus.database.DBAdapter;
import ie.enclude.flexibus.util.BusTrip;
import ie.enclude.flexibus.util.Passenger;

public class DataHandleFactory {
	private LocalDataHandler ldh;
	private Boolean bLoggedIn=false;
	
	public DataHandleFactory(FlexibusActivity mainActivity) {
		ldh = new LocalDataHandler(mainActivity);
	}
	
	/** initial login process 
	 * @param context */
	public String localLogin() 
	{
		return "Logged In";
/*		
		String result = "";
		if (!bLoggedIn)
		{
			String errorMsg = ldh.loginWithApi();
			if (errorMsg.length() == 0)
			{
				bLoggedIn = true;
				result = "Logged In";
			}
			else
			{
				bLoggedIn = false;
				result = "Failed to login, please try again later " + errorMsg;
			}
		}
		else
		{
			// TODO: test access token
		}
		return result;
*/
	}
	
	public boolean getBuses(SalesforceResponseInterface sfrp) 
	{
		return ldh.getBuses(sfrp);
	}

	public String addOdometerReadingToSelectedBus (String odoReading, SalesforceResponseInterface sfrp)
	{
		return ldh.addOdometerReadingToSelectedBus(odoReading, sfrp);
	}
	
	public String[] getBusNames() {
		return ldh.getBusNames();
	}

	public boolean IsLoggedIn() 
	{
		return bLoggedIn;
	}

	public void setSelectedBus(CharSequence busName) 
	{
		ldh.setSelectedBus (busName);
	}

	public String getCurrentBusName() {
		return ldh.getCurrentBusName();
	}

	public String getCurrentBusOdoReading() {
		return ldh.getCurrentBusOdoReading();
	}

	public boolean initialiseSelectedBus(String busName, String busOdoReading, SalesforceResponseInterface sfrp) 
	{
		return ldh.initialiseSelectedBus(busName, busOdoReading, sfrp);
	}

	public String getLastError() 
	{
		return ldh.getLastError();
	}

	public List<String> getFieldList(String objectName) {
		return ldh.getFieldList(objectName);
	}

	public String[] getPickList(String pickListtName) {
		return ldh.getPickList(pickListtName);
	}

	public void addFaultReportTextToCheckList(String faultReport) {
		ldh.addFaultReportTextToCheckList(faultReport);
	}

	public String sendStartupCheckListToSalesforce() {
		return ldh.sendStartupCheckListToSalesforce();
	}

	public void setPickListResult(String alertTitle, String itemPicked) {
		ldh.setPickListResult(alertTitle, itemPicked);
		
	}

	public void wipePreviousStartupListAnswers() {
		ldh.wipePreviousStartupListAnswers();
		
	}

	public String recordFuelPurchased(FlexibusApp gs) {
		return ldh.recordFuelPurchased(gs);
	}

	public List<BusTrip> getTodaysBusTrips() {
		return ldh.getTodaysBusTrips();
	}

	public List<Passenger> getPassengerList(String busTripID) {
		return ldh.getPassengerList(busTripID);
	}

	public String sendTripReports(DBAdapter database) 
	{
		return ldh.sendTripReports(database);
	}
}
