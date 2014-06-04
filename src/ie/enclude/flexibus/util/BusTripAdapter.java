package ie.enclude.flexibus.util;

import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;

public class BusTripAdapter extends ArrayAdapter <BusTrip> 
{
	List<BusTrip>busTrips;

    public BusTripAdapter(Context context, int textViewResourceId, List<BusTrip> trips) {
            super(context, textViewResourceId, trips);
            this.busTrips = trips;
    }

}
