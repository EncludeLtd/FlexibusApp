/**
 * Copyright (C) 2008 Dai Odahara.
 */

package ie.enclude.salesforce.util;

import android.view.ViewGroup;

/**
 * This class is salesforce utility class. Analyze xml from salesforce query, describe, etc.
 * @author Dai Odahara
 *
 */
public class StaticInformation {
	public static final int RECORD_ID_LENGTH = 18;
	public static final int SOBJECT_PREFIX_SIZE = 3;
	public static final int ONLINE_SIZE = 6;
	

    public static String DOMAIN = "";
	public static String USER_ID= "Flexibus2@enclude.ie";
	public static String USER_NAME= "";
	
	public static String USER_PW = "e3n6c3l9u7";
	public static String USER_TOKEN = "aP6se7q8UPZxSsqTwrKsewpC";
	
	public static String SESSION_ID = "";
	
	public static String USER_ID_18DIGITS;
	public static String API_SERVER_URL = "https://login.salesforce.com";
	public static String CONSUMER_KEY = "3MVG9WtWSKUDG.x6pEndQgx6g.2Pi92T4WlI_wHGasdKKrRh9I9PajhMkq_gpYPFZuunfRo0C5LmmoYlsK0wN";
	public static String CONSUMER_SECRET = "6623758498843038948";
	
	public static String API_META_DATA_SERVER_URL;
	
	public static boolean isActive = false;
	public static boolean isDandV = false;
	
	public static boolean isDemo = false;
	
	public static final String SOAP_ACTION = "\"\"";
	public static final String LOGIN_SERVER_URL = "https://www.salesforce.com/services/Soap/c/14.0";
	public static final String NAMESPACE = "urn:enterprise.soap.sforce.com";	
	public static final String SOBJECT_PACKAGE_NAME = "com.android.salesforce.sobject.";
	public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
	
    public static ViewGroup MainContainer;
    
    public static String[] DOWNLOAD_SOBJECTS = { "Event", "Task", "Lead", "Account", "Contact", 
    		"Opportunity", "Case"};
    
    public static String[] DOWNLOAD_SOBJECTS_WITH_DEMO = { "Event", "Task", "Lead", "Account", "Contact", 
		"Opportunity", "Case" /*, "ChartViewer"*/ , "Dashboard", "Visualforce"};

    public static final String METADATA_NAMESPACE = "http://soap.sforce.com/2006/04/metadata";
	
    public static final String MASTER_RECORD_TYPE_ID = "012000000000000AAA";
    
    public static String vUrl = "";
    public static String dUrl = "";
    
 
}
