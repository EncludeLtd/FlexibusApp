package ie.enclude.flexibus.util;

public class Passenger
{
	public String salesforce_ID;
    public String name;
    public String homePhone;
    public String mobilePhone;
    public String street;
    public String town;
    public String note;
    public String freeTravelPassNumber;
	public String busTrip_ID;
	private int actualCompanions;
	private boolean passengerOnBoard;

    public Passenger (String id, String busTripId, String passengerName, String phone1, String phone2, String street1, String town1, String passengerNote, String ftp, int companions)
    {
    	salesforce_ID = id;
    	busTrip_ID = busTripId;
    	name = passengerName;
    	homePhone = phone1;
    	mobilePhone = phone2;
    	street = street1;
    	town = town1;
    	note = passengerNote;
    	freeTravelPassNumber = ftp;
    	actualCompanions = companions;
    	passengerOnBoard = false;
    }
    
    public String getAddress ()
    {
    	if (street.equals("null") && town.equals("null")) return "null";
    	if (street.equals("null")) return town;
    	if (town.equals("null")) return street;
    	return street + ", " + town;
    }
    
    public boolean hasFreeTravel ()
    {
    	if (freeTravelPassNumber.length() > 0 && !freeTravelPassNumber.equals("null"))
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    
    public void addCompanion ()
    {
    	actualCompanions++;
    }
    
    public int getNumberOfCompanions ()
    {
    	return actualCompanions;
    }
    
    @Override
    public String toString()
    {
    	if (actualCompanions > 0)
    	{
    		return name + " +" + actualCompanions;
    	}
    	else
    	{
    		return name;
    	}
    }

	public void clearCompanions() 
	{
		actualCompanions = 0;		
	}

	public void flagAsOnBoard(boolean passengerOnBoard) 
	{
		this.passengerOnBoard = passengerOnBoard;
	}

	public boolean isPassengerOnBoard() 
	{
		return passengerOnBoard;
	}
	
	public void notOnBoard()
	{
		passengerOnBoard = false;
	}
}
