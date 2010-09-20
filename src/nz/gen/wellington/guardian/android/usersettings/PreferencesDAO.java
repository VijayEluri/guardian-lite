package nz.gen.wellington.guardian.android.usersettings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesDAO {

	private SharedPreferences prefs;

	public PreferencesDAO(Context context) {
		prefs =  PreferenceManager.getDefaultSharedPreferences(context);
	}

	public int getPageSizePreference() {
		final String pageSizeString = prefs.getString("pageSize", "10");
		int pageSize = Integer.parseInt(pageSizeString);
		return pageSize;
	}

	public String getLargePicturesPreference() {
		return (String) prefs.getString("largeImagesOption", "WIFI_ONLY");
	}
	
	public String getSyncPreference() {
		return (String) prefs.getString("syncType", "NEVER");
	}
	
}
