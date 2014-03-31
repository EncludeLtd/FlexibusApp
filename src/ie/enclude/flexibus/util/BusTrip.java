package ie.enclude.flexibus.util;

import java.util.Date;

public class BusTrip 
{
	public String salesforce_ID;
    public String uniqueID;
    public Date startTime;
    public Date endTime;
    String driversName;
    
    public BusTrip (String id, String startTime2, String endTime2, String stringID, String driver)
    {
    	
    	salesforce_ID = id;
    	startTime = ConvertStringToDate (startTime2);
    	endTime = ConvertStringToDate (endTime2);
    	uniqueID = stringID;
    	driversName = driver;
    }

    private Date ConvertStringToDate (String strTime)
    {
    	Date today = new Date();
    	if (strTime != null)
    	{
	    	String[] hoursmins = strTime.split(":");
	    	if (hoursmins.length == 2)
	    	{
	    		return new Date (today.getYear(), today.getMonth(), today.getDate(), Integer.parseInt(hoursmins[0]), Integer.parseInt(hoursmins[1]));
	    	}
	    	else
	    	{
	    		return new Date ();
	    	}
    	}
    	else
    	{
    		return new Date ();
    	}
    }
    
	public long GetStartTime() 
	{
		return startTime.getTime();
	}

	public long GetEndTime() 
	{
		return endTime.getTime();
	}
	
	@Override
    public String toString()
    {
		if (driversName != "null" && driversName != "")
			return uniqueID + " " + driversName;
		else return uniqueID;
    }
}
