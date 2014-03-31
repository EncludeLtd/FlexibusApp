package ie.enclude.flexibus.util;

public class PassengerTrip 
{
    public String busTripID;
    public String passengerID;
    public Integer passengerStatus;
    public Integer passengerContribution;  
	public Integer actualCompanions;
	public boolean passengerOnBoard;

    public PassengerTrip (String tripID, String passID, Integer contribution, Integer companions, int onBoard)
    {
    	busTripID = tripID;
    	passengerID = passID;
    	passengerStatus = 0;
    	passengerContribution = contribution;
    	actualCompanions = companions;
    	passengerOnBoard = onBoard == 1;
    }
}
