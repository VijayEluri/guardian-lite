package nz.gen.wellington.guardian.android.usersettings;

import nz.gen.wellington.guardian.android.model.BlackOnWhiteColourScheme;
import nz.gen.wellington.guardian.android.model.ColourScheme;
import nz.gen.wellington.guardian.android.model.WhiteOnBlackColourScheme;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesDAO {
	
	private static final String GUARDIAN_LITE_PROXY_API_PREFIX = "http://2.guardian-lite.appspot.com";
	private static final String CONTENT_API_URL = "http://content.guardianapis.com";

	private SharedPreferences prefs;

	public PreferencesDAO(Context context) {
		prefs =  PreferenceManager.getDefaultSharedPreferences(context);
	}

	public int getPageSizePreference() {
		final String pageSizeString = prefs.getString("pageSize", "10");
		return Integer.parseInt(pageSizeString);
	}
	

	public String getTrailPicturesPreference() {
		return (String) prefs.getString("trailImagesOption", "ALWAYS");
	}

	public String getLargePicturesPreference() {
		return (String) prefs.getString("largeImagesOption", "WIFI_ONLY");
	}
	
	public String getSyncPreference() {
		return (String) prefs.getString("syncType", "NEVER");
	}

	public int getBaseFontSize() {
		final String baseSizeString = prefs.getString("baseFontSize", "7");
		return Integer.parseInt(baseSizeString);
	}
	
	public boolean useContentApi() {
		return prefs.getBoolean("useContentApi", false);		
	}
	
	public boolean showDateRefinements() {
		return prefs.getBoolean("showDateDefinements", false);
	}
	
	public String getPreferedApiHost() {
		if (useContentApi()) {
			return CONTENT_API_URL;
		}
		return GUARDIAN_LITE_PROXY_API_PREFIX;
	}

	public String getApiKey() {
		return (String) prefs.getString("contentApiKey", null);
	}
	
	public ColourScheme getColourScheme() {
		final String colourSchemePreferences = (String) prefs.getString("colourScheme", "WHITE_ON_BLACK");
		if (colourSchemePreferences.equals("BLACK_ON_WHITE")) {
			return new BlackOnWhiteColourScheme();
		}
		return new WhiteOnBlackColourScheme();
	}
	
}
