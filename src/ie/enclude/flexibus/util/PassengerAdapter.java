package ie.enclude.flexibus.util;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

public class PassengerAdapter extends ArrayAdapter <Passenger> 
{
	List<Passenger>passengers;
	private final Context context;

    public PassengerAdapter(Context context, int textViewResourceId, List<Passenger> passengers) 
    {
    	super(context, textViewResourceId, passengers);
    	this.context = context;
        this.passengers = passengers;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) 
    {
    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View rowView = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
    	
        Passenger pass = passengers.get(position);
        if (pass != null) 
        {
        	CheckedTextView tv = (CheckedTextView) rowView;
        	tv.setChecked(pass.isPassengerOnBoard());
        	tv.setText(pass.name);
        }
        return rowView;
    }
/*
            if (v == null) 
            {
            	v = super.getView(position, v, parent);
            }
            Passenger pass = passengers.get(position);
            if (pass != null) 
            {
            	CheckedTextView tv = (CheckedTextView) v;
            	tv.setChecked(pass.isPassengerOnBoard());
            	return tv;
            }
            return v;
    }
*/
}
