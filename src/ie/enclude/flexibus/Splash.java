package ie.enclude.flexibus;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import ie.enclude.flexibus.R;
     
public class Splash extends Activity 
{
	private static final int VISIBLE = 0;
	private final int SPLASH_DISPLAY_LENGTH = 3000;
    private TextView m_versionText;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) 
    {
    	super.onCreate(icicle);
        setContentView(R.layout.splashscreen);
        m_versionText = (TextView) findViewById(R.string.version);
        m_versionText.setText (getVersionInfo());
        if (CONSTANTS.TESTING)
        {
        	m_versionText.append("t");
        }
    
        new Handler().postDelayed(new Runnable()
        {
        	public void run()
        	{
                TextView whoby = (TextView) findViewById(R.string.appby);
                whoby.setVisibility(VISIBLE);
        	}
        	
        }, SPLASH_DISPLAY_LENGTH/3);
        
        new Handler().postDelayed(new Runnable()
        {
        	public void run()
        	{
                ImageView logo = (ImageView) findViewById(R.drawable.encludelogo);
                logo.setVisibility(VISIBLE);
        	}
        	
        }, SPLASH_DISPLAY_LENGTH*2/3);
        
       /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
     
        new Handler().postDelayed(new Runnable()
        {
            public void run() 
        	{
        		/* Create an Intent that will start the HelloAndroid-Activity. */
                Intent mainIntent = new Intent(Splash.this,FlexibusActivity.class);
                Splash.this.startActivity(mainIntent);
                Splash.this.finish();
            }
         }, SPLASH_DISPLAY_LENGTH);
    }
    
    public String getVersionInfo() {
        String strVersion = "Version: ";

        PackageInfo packageInfo;
        try {
            packageInfo = getApplicationContext().getPackageManager().getPackageInfo(
                    getApplicationContext().getPackageName(), 0);
            strVersion += packageInfo.versionName;
        } catch (NameNotFoundException e) {
            strVersion += "Unknown";
        }

        return strVersion;
    }
}
