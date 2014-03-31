/* Simple 'DAO' wrapper class that is used to store all the OAuth values returned by Salesforce via the redirect URI callback
 */
package ie.enclude.salesforce.util;

import java.util.Calendar;

import org.apache.http.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class OAuthTokens 
{
   	private String _access_token;
   	private String _instance_url;
   	private String _id;
   	private Calendar _issued_at;
   	private String _signature;
   	private String _errorMsg="";
   	
   	public OAuthTokens (String toTokenise)
   	{
		try 
		{
			JSONObject object = (JSONObject) new JSONTokener(toTokenise).nextValue();
			JSONArray names = object.names();
			JSONArray values = object.toJSONArray(names);
	    	for(int i=0; i<values.length(); i++)
	    	{
	    		if (names.getString(i).equals("error"))
	    		{
	    			set_error (object.getString("error"), object.getString("error_description"));
	    		}
	    		else if (names.getString(i).equals("access_token"))
	    		{
	    			set_access_token(object.getString("access_token"));
	    		}
	    		else if (names.getString(i).equals("instance_url"))
	    		{
	    			set_instance_url(object.getString("instance_url"));
	    		}
	    		else if (names.getString(i).equals("id"))
	    		{
	    			set_id(object.getString("id"));
	    		}
	    		else if (names.getString(i).equals("issued_at"))
	    		{
	    			set_issued_at(Long.valueOf(object.getString("issued_at")));
	    		}
	    		else if (names.getString(i).equals("signature"))
	    		{
	    			set_signature(object.getString("signature"));
	    		}
	    	}
		} 
		catch (ParseException e) 
		{
			set_error ("Parse Exception", e.getMessage());
		} 
		catch (JSONException e) 
		{
			set_error ("JSON Exception", e.getMessage());
		} 

   	}
	private void set_error(String error, String errorDescription)
	{
		if (error != "")
		{
			_errorMsg = error + ": " + errorDescription;
		}
	}
	
	public String ErrorMsg ()
	{
		return _errorMsg;
	}
	
			public String get_access_token() {
				return _access_token;
			}
			public void set_access_token(String _access_token) {
				this._access_token = _access_token;
			}
			public String get_instance_url() {
				return _instance_url;
			}
			public void set_instance_url(String _instance_url) {
				this._instance_url = _instance_url;
			}
			public String get_id() {
				return _id;
			}
			public void set_id(String _id) {
				this._id = _id;
			}
			public Calendar get_issued_at() {
				return _issued_at;
			}
			public void set_issued_at(Long issued_at) {
				this._issued_at = Calendar.getInstance();
				this._issued_at.setTimeInMillis(issued_at);
			}
			public String get_signature() {
				return _signature;
			}
			public void set_signature(String _signature) {
				this._signature = _signature;
			}
	      
}