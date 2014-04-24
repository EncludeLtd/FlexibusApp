package ie.enclude.flexibus.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ie.enclude.flexibus.util.BusTrip;
import ie.enclude.flexibus.util.Passenger;
import ie.enclude.flexibus.util.PassengerTrip;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.text.format.DateUtils;

public class DBAdapter
{
	public static final String DEBUG_TAG="FlexibusLogging";

	public static final String KEY_ROWID = "_id";
    public static final String KEY_SALESFORCE_ID = "Salesforce_ID";
    public static final String KEY_DATE = "Date__c";
    public static final String KEY_START_TIME = "StartTime";
    public static final String KEY_END_TIME = "EndTime";
    public static final String KEY_BUS_TRIP_UNIQUE_ID = "Bus_Trip_Unique_ID__c";    
    public static final String KEY_BUS_TRIP = "Bus_Trip__c";
    public static final String KEY_PASSENGER = "Passenger__c";
    public static final String KEY_PASSENGER_CONTRIBUTION = "Passenger_Contribution__c";
    public static final String KEY_PASSENGER_NAME = "Name";
    public static final String KEY_PASSENGER_HOME_PHONE = "HomePhone";
    public static final String KEY_PASSENGER_MOBILE_PHONE = "MobilePhone";
    public static final String KEY_PASSENGER_STREET = "OtherStreet";
    public static final String KEY_PASSENGER_TOWN = "OtherCity";
    public static final String KEY_NOTE = "Note__c"; 
    public static final String KEY_PASSENGER_TRAVEL_PASS_NUMBER = "Free_Travel_Pass_Number__c";
    public static final String KEY_CURRENT_BUS = "CurrentBus";
    public static final String KEY_NUMBER_OF_COMPANIONS = "NumberOfCompanions";
    public static final String KEY_NUMBER_OF_FREE_PASSENGERS = "FreePassengers";
    public static final String KEY_NUMBER_OF_FARE_PAYING_PASSENGERS = "FarePayingPassengers";
    public static final String KEY_PASSENGER_TRIP_STATUS = "PassengerTripStatus";
    
    private static final String TAG = "DBAdapter";
 
    private static final String DATABASE_NAME = "flexibus";
    
    private static final String DATABASE_BUS_TRIP_TABLE = "Bus_Trip__c";
    private static final String DATABASE_PASSENGER_TRIP_TABLE = "Passenger_Trip__c";
    private static final String DATABASE_PASSENGER_TABLE = "Passenger";
    private static final String DATABASE_PROVIDER_SHEET_TABLE = "Provider_Sheet";
    
    private static final int DATABASE_VERSION = 2; // added passenger trip stats
 
    private static final String DATABASE__BUS_TRIP_TABLE_CREATE =
        "create table " + DATABASE_BUS_TRIP_TABLE 
        + "(" 
        		+ KEY_ROWID + " integer primary key autoincrement, "
        		+ KEY_SALESFORCE_ID + " text not null, "
        		+ KEY_START_TIME + " numeric not null, " 
        		+ KEY_END_TIME + " numeric not null, " 
        		+ KEY_BUS_TRIP_UNIQUE_ID + " text not null, "
        		+ KEY_NUMBER_OF_FREE_PASSENGERS + " integer default 0, "
        		+ KEY_NUMBER_OF_FARE_PAYING_PASSENGERS + " integer default 0"
        + ");";
 
    private static final String DATABASE_PASSENGER_TRIP_TABLE_CREATE =
            "create table " + DATABASE_PASSENGER_TRIP_TABLE 
            + "(" 
            		+ KEY_ROWID + " integer primary key autoincrement, "
            		+ KEY_BUS_TRIP + " text not null, " 
            		+ KEY_PASSENGER + " text not null, " 
            		+ KEY_PASSENGER_CONTRIBUTION + " integer default 0, "
            		+ KEY_NUMBER_OF_COMPANIONS + " integer default 0,"
            		+ KEY_PASSENGER_TRIP_STATUS + " Boolean default false"
            + ");";
    
    private static final String DATABASE_PASSENGER_TABLE_CREATE =
            "create table " + DATABASE_PASSENGER_TABLE 
            + "(" 
            		+ KEY_ROWID + " integer primary key autoincrement, "
            		+ KEY_SALESFORCE_ID + " text not null, "
            		+ KEY_BUS_TRIP + " text not null, "
            		+ KEY_PASSENGER_NAME + " text not null, " 
            		+ KEY_PASSENGER_HOME_PHONE + " text, " 
            		+ KEY_PASSENGER_MOBILE_PHONE + " text, "
            		+ KEY_PASSENGER_STREET + " text, " 
            		+ KEY_PASSENGER_TOWN + " text, " 
            		+ KEY_NOTE + " text, "
            		+ KEY_PASSENGER_TRAVEL_PASS_NUMBER + " text"
            + ");";
    
    private static final String DATABASE_PROVIDER_SHEET_TABLE_CREATE =
            "create table " + DATABASE_PROVIDER_SHEET_TABLE 
            + "(" 
            		+ KEY_ROWID + " integer primary key autoincrement, "
            		+ KEY_DATE + " numeric not null, "
            		+ KEY_CURRENT_BUS + " text not null" 
            + ");";
    
    private final Context context;  
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;
    private String currentBusTripID;
	private String currentBusTripTitle;
 
    public DBAdapter(Context ctx) 
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
        currentBusTripID = null; 
    }
    
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
 
        @Override
        public void onCreate(SQLiteDatabase db) 
        {
            db.execSQL(DATABASE__BUS_TRIP_TABLE_CREATE);
            db.execSQL(DATABASE_PASSENGER_TRIP_TABLE_CREATE);
            db.execSQL(DATABASE_PASSENGER_TABLE_CREATE);
            db.execSQL(DATABASE_PROVIDER_SHEET_TABLE_CREATE);
        }
 
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
                              int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion 
                  + " to "
                  + newVersion + ", which will destroy all old data");
            WipeDatabase(db);
            onCreate(db);
        }

		private void WipeDatabase(SQLiteDatabase db) 
		{
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_BUS_TRIP_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_PASSENGER_TRIP_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_PASSENGER_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_PROVIDER_SHEET_TABLE);
		}

		public void InitialiseDatabase(SQLiteDatabase db) 
		{
			WipeDatabase(db);
			onCreate(db);			
		}
    }
    
    public DBAdapter open() throws SQLException 
    {
        db = DBHelper.getWritableDatabase();
        return this;
    }
 
    public void close() 
    {
        DBHelper.close();
    }
    
    public long insertBusTrip(BusTrip thisTrip) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_SALESFORCE_ID, thisTrip.salesforce_ID);
        initialValues.put(KEY_START_TIME, thisTrip.GetStartTime());
        initialValues.put(KEY_END_TIME, thisTrip.GetEndTime());
        initialValues.put(KEY_BUS_TRIP_UNIQUE_ID, thisTrip.uniqueID);
        return db.insert(DATABASE_BUS_TRIP_TABLE, null, initialValues);
    }
    
    public long insertPassengerTrip(String busTrip, String passenger, int contribution, Boolean status) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_BUS_TRIP, busTrip);
        initialValues.put(KEY_PASSENGER, passenger);
        initialValues.put(KEY_PASSENGER_CONTRIBUTION, contribution);
        initialValues.put(KEY_PASSENGER_TRIP_STATUS, status);
        return db.insert(DATABASE_PASSENGER_TRIP_TABLE, null, initialValues);
    }
    
    public long insertPassenger(Passenger thisPassenger) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_SALESFORCE_ID, thisPassenger.salesforce_ID);
        initialValues.put(KEY_BUS_TRIP, thisPassenger.busTrip_ID);
        initialValues.put(KEY_PASSENGER_NAME, thisPassenger.name);
        initialValues.put(KEY_PASSENGER_HOME_PHONE, thisPassenger.homePhone);
        initialValues.put(KEY_PASSENGER_MOBILE_PHONE, thisPassenger.mobilePhone);
        initialValues.put(KEY_PASSENGER_STREET, thisPassenger.street);
        initialValues.put(KEY_PASSENGER_TOWN, thisPassenger.town);
        initialValues.put(KEY_NOTE, thisPassenger.note);
        initialValues.put(KEY_PASSENGER_TRAVEL_PASS_NUMBER, thisPassenger.freeTravelPassNumber);
        return db.insert(DATABASE_PASSENGER_TABLE, null, initialValues);
    }

    public List<Passenger> getAllPassengers(String busTripID) 
    {
    	try
    	{
    		String sql = "select distinct " +
    				KEY_SALESFORCE_ID + ", " +
    				DATABASE_PASSENGER_TABLE + "." + KEY_BUS_TRIP + ", " + 
    				KEY_PASSENGER_NAME + ", " + 
    				KEY_PASSENGER_HOME_PHONE + ", " +
    				KEY_PASSENGER_MOBILE_PHONE + ", " +
    				KEY_PASSENGER_STREET + ", " +
    				KEY_PASSENGER_TOWN + ", " +
    				KEY_NOTE + ", " +
    				KEY_PASSENGER_TRAVEL_PASS_NUMBER + ", " +
    				DATABASE_PASSENGER_TRIP_TABLE + "." + KEY_NUMBER_OF_COMPANIONS + 
    				" from " + DATABASE_PASSENGER_TABLE + 
    				" LEFT OUTER JOIN " + DATABASE_PASSENGER_TRIP_TABLE + " ON " + DATABASE_PASSENGER_TRIP_TABLE + "." + KEY_PASSENGER + " = " + DATABASE_PASSENGER_TABLE + "." + KEY_SALESFORCE_ID +
    				" and " + DATABASE_PASSENGER_TRIP_TABLE + "." + KEY_BUS_TRIP + " = '" + busTripID + "'" +
    				" where " + DATABASE_PASSENGER_TABLE + "." + KEY_BUS_TRIP + " = '" + busTripID + "'";
            		
    		Cursor passengers = db.rawQuery(sql, null);
    	
	        if (passengers.moveToFirst())
			{
				List<Passenger> pass = new ArrayList<Passenger>(passengers.getCount());
				do
				{
					Log.v(DEBUG_TAG, "In getAllPassengers: " + passengers.getString(0) + " " + passengers.getString(1) + " " + passengers.getString(2) + " " + passengers.getString(3));
					pass.add(new Passenger (passengers.getString(0), passengers.getString(1), passengers.getString(2), passengers.getString(3), passengers.getString(4), passengers.getString(5), passengers.getString(6), passengers.getString(7), passengers.getString(8), passengers.getInt(9)));
					insertPassengerTrip(passengers.getString(1), passengers.getString(0), 0, false);
				} while (passengers.moveToNext());
				passengers.close();
				return pass;
			}
			else
			{
				passengers.close();
				return null;
			}
    	}
    	catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "getAllPassengers " + e.getMessage());
			return null;
		}
    }

	public boolean ContainsTodaysTrips(String currentBusName) 
	{
		if (db == null) open (); 
		try
		{
			Cursor dbDate = db.query(DATABASE_PROVIDER_SHEET_TABLE, new String[] {KEY_DATE},
					KEY_CURRENT_BUS + " = '" + currentBusName + "'",
					null, null, null, null);
			if (dbDate.moveToFirst())
			{
				long providerDate = dbDate.getLong(0);
				dbDate.close();
				return DateUtils.isToday(providerDate);
			}
			else
			{
				dbDate.close();
				return false;
			}
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "ContainsTodaysTrips " + e.getMessage());
			return false;
		}
	}

	public void InitialiseDatabase(String currentBus) 
	{
		try
		{
			DBHelper.InitialiseDatabase(db);
			
			Date today = new Date();
			ContentValues initialValues = new ContentValues();
	        initialValues.put(KEY_DATE, today.getTime());
	        initialValues.put(KEY_CURRENT_BUS, currentBus);
	        db.insert(DATABASE_PROVIDER_SHEET_TABLE, null, initialValues);
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "InitialiseDatabase " + e.getMessage());
		}
}

	public List<Passenger> getTodaysPassengers(String busTripID) 
	{
		try
		{
			String whereClause = KEY_SALESFORCE_ID + " = '" + busTripID + "'";
			Cursor currentTrip = db.query(DATABASE_BUS_TRIP_TABLE, new String[] {KEY_SALESFORCE_ID, KEY_BUS_TRIP_UNIQUE_ID}, 
					whereClause, null, null, null, KEY_START_TIME);
			if (currentTrip.moveToFirst())
			{
				currentBusTripID = currentTrip.getString(0);
				currentBusTripTitle = currentTrip.getString(1);
				currentTrip.close();
				return getAllPassengers(currentBusTripID);
			}
			else
			{
				currentTrip.close();
				return null;
			}
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "getTodaysPassengers " + e.getMessage());
			return null;
		}
	}
	
	public boolean checkBusTripIDIsCurrent (String idToCheck)
	{
		return true; // not required any more
	/*
		try
		{
			Date now = new Date();
			String whereClause = KEY_END_TIME + " > '" + now.getTime() + "'";
			Cursor currentTrip = db.query(DATABASE_BUS_TRIP_TABLE, new String[] {KEY_SALESFORCE_ID}, 
					whereClause, null, null, null, KEY_START_TIME);
			if (currentTrip.moveToFirst())
			{
				boolean result = currentTrip.getString(0).equals(idToCheck);
				currentTrip.close();
				return result;
			}
			else
			{
				currentTrip.close();
				return false;
			}
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "checkBusTripIDIsCurrent " + e.getMessage());
			return false;
		}
		*/
	}
	
	public String getCurrentBusTrip()
	{
		return currentBusTripID;
	}

	public String getCurrentBusTripTitle()
	{
		return currentBusTripTitle;
	}

	public void registerPassengerStatus(Passenger passenger, int contribution, Boolean status) 
	{
		// note not saving the contribution
		passenger.flagAsOnBoard (status);
		UpdatePassengerOnTrip (passenger);
		/* don't insert twice
		if (isPassengerOnBoard (passenger))
		{
			RemovePassengerFromTrip (passenger);
		}
		insertPassengerTrip (getCurrentBusTrip(), passenger.salesforce_ID, contribution, status);*/
	}

	public boolean isPassengerOnBoard(Passenger passenger) 
	{
		try
		{
			String whereclause = KEY_BUS_TRIP + " = '" + getCurrentBusTrip() + "' AND " + KEY_PASSENGER + " = '" + passenger.salesforce_ID + "'";
			//	whereclause += " AND " + KEY_PASSENGER_TRIP_STATUS + " = 'true'";
			Cursor anypassenger = db.query(DATABASE_PASSENGER_TRIP_TABLE, new String[]{KEY_ROWID,KEY_PASSENGER_TRIP_STATUS},
					whereclause, 
					null, null, null, null);
			Log.v(DEBUG_TAG, "isPassengerOnBoard where clause: " + whereclause);
			if (anypassenger.moveToFirst())
			{
				Log.v(DEBUG_TAG, "Column Index " + anypassenger.getColumnIndex(KEY_PASSENGER_TRIP_STATUS));
				Log.v(DEBUG_TAG, "Value: " + anypassenger.getInt(anypassenger.getColumnIndex(KEY_PASSENGER_TRIP_STATUS)));
				int returnValue = anypassenger.getInt(anypassenger.getColumnIndex(KEY_PASSENGER_TRIP_STATUS));
				anypassenger.close();
				if (returnValue == 1) 
				{
					Log.v(DEBUG_TAG, "Passenger on board");
					return true;
				}
				else 
				{
					Log.v(DEBUG_TAG, "Passenger NOT on board");
					return false;
				}
			}
			else
			{
				Log.v(DEBUG_TAG, "Passenger NOT on board - record not found");
				anypassenger.close();
				return false;
			}
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "isPassengerOnBoard " + e.getMessage());
			return false;
		}
	}

	public void RemovePassengerFromTrip(Passenger passenger) 
	{
		String whereclause = KEY_BUS_TRIP + " = '" + getCurrentBusTrip() + "' AND " + KEY_PASSENGER + " = '" + passenger.salesforce_ID + "'";
		db.delete(DATABASE_PASSENGER_TRIP_TABLE, whereclause, null);		
		passenger.clearCompanions();
		passenger.notOnBoard();
	}
	
	public boolean saveNumberOfPassengersOnTrip (int free, int farePaying)
	{
		try
		{
			String whereclause = KEY_SALESFORCE_ID + " = '" + getCurrentBusTrip() + "'";
			ContentValues updateValues = new ContentValues();
			updateValues.put(KEY_NUMBER_OF_FREE_PASSENGERS, free);
			updateValues.put(KEY_NUMBER_OF_FARE_PAYING_PASSENGERS, farePaying);
	 		int rows = db.update(DATABASE_BUS_TRIP_TABLE, updateValues, whereclause, null);
	 		return rows == 1;
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "saveNumberOfPassengersOnTrip " + e.getMessage());
			return false;
		}
	}
	
	public String getListOfBusTripsInPassengerTripTable ()
	{
		try
		{
			Cursor busTrips = db.query(true, DATABASE_PASSENGER_TRIP_TABLE, new String[]{KEY_BUS_TRIP}, null, null, null, null, null, null);
			if (busTrips.moveToFirst())
			{
				String busTripIDs = "";
				do
				{
					if (!busTripIDs.equals(""))
					{
						busTripIDs = busTripIDs.concat(",");
					}
						
					busTripIDs = busTripIDs + "'" + busTrips.getString(0) + "'";
				} while (busTrips.moveToNext());
				busTrips.close();
				return busTripIDs;
			}
			else
			{
				busTrips.close();
				return null;
			}
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "getListOfBusTripsInPassengerTripTable " + e.getMessage());
			return null;
		}
	}

	public PassengerTrip findPassengerTrip(String busTripID, String passengerID) 
	{
		try
		{
			String whereclause = KEY_BUS_TRIP + " = '" + busTripID + "' AND " + KEY_PASSENGER + " = '" + passengerID + "'";
			Log.v(DEBUG_TAG, "findPassengerTrip where " + whereclause);

			Cursor anypassenger = db.query(DATABASE_PASSENGER_TRIP_TABLE, new String[]{KEY_PASSENGER_CONTRIBUTION, KEY_NUMBER_OF_COMPANIONS, KEY_PASSENGER_TRIP_STATUS},
					whereclause, 
					null, null, null, null);
			if (anypassenger.moveToFirst())
			{
				PassengerTrip passTrip = new PassengerTrip (busTripID, passengerID, anypassenger.getInt(0), anypassenger.getInt(1), anypassenger.getInt(2));
				anypassenger.close();
				return passTrip;
			}
			else
			{
				anypassenger.close();
				return null;
			}
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "findPassengerTrip " + e.getMessage());
			return null;
		}
	}

	public void UpdatePassengerOnTrip(Passenger passenger) 
	{
		try
		{
			String whereclause = KEY_BUS_TRIP + " = '" + getCurrentBusTrip() + "' AND " + KEY_PASSENGER + " = '" + passenger.salesforce_ID + "'";
			ContentValues updateValues = new ContentValues();
			updateValues.put(KEY_NUMBER_OF_COMPANIONS, passenger.getNumberOfCompanions());
			updateValues.put(KEY_PASSENGER_TRIP_STATUS, passenger.isPassengerOnBoard());
	 		db.update(DATABASE_PASSENGER_TRIP_TABLE, updateValues, whereclause, null);
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "UpdatePassengerOnTrip " + e.getMessage());
		}
	}

	public String[] getPassengerNumbers(String tripID) 
	{
		String whereClause = KEY_SALESFORCE_ID + " = '" + tripID + "'";
		Log.v(DEBUG_TAG, "getPassengerNumbers " + whereClause);
		try
		{
			Cursor currentTrip = db.query(DATABASE_BUS_TRIP_TABLE, new String[] {KEY_NUMBER_OF_FREE_PASSENGERS, KEY_NUMBER_OF_FARE_PAYING_PASSENGERS}, 
					whereClause, null, null, null, null);
			if (currentTrip.moveToFirst())
			{
				int free = currentTrip.getInt(0);
				int fare = currentTrip.getInt(1);
				currentTrip.close();
				return new String[] {Integer.toString(free), Integer.toString(fare)};
			}
			else
			{
				currentTrip.close();
				return null;
			}
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "getPassengerNumbers " + e.getMessage());
			return null;
		}
	}

	public List<BusTrip> getTodaysTrips(String currentBusName) 
	{
		// TODO - this ignores the currentBusName
		try
		{
			String whereClause = null; // KEY_SALESFORCE_ID + " = '" + busTripID + "'";
			Cursor currentTrip = db.query(DATABASE_BUS_TRIP_TABLE, new String[] {KEY_SALESFORCE_ID, KEY_START_TIME, KEY_END_TIME, KEY_BUS_TRIP_UNIQUE_ID}, 
					whereClause, null, null, null, KEY_START_TIME);
			
			if (currentTrip.moveToFirst())
			{
				List<BusTrip> todaysTrips = new ArrayList<BusTrip>(currentTrip.getCount());
				do
				{
					todaysTrips.add(new BusTrip (currentTrip.getString(0), currentTrip.getString(1), currentTrip.getString(2), currentTrip.getString(3), ""));
				} while (currentTrip.moveToNext());
				currentTrip.close();

				return todaysTrips;
			}
			else
			{
				currentTrip.close();
				return null;
			}
		}
		catch (SQLException e)
		{
			Log.v(DEBUG_TAG, "getTodaysPassengers " + e.getMessage());
			return null;
		}
	}
}

