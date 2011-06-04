/*	Guardian Lite - an Android reader for the Guardian newspaper.
 *	Copyright (C) 2011  Eel Pie Consulting Limited
 *
 *	This program is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.	*/

package nz.gen.wellington.guardian.android.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.util.Log;

public class DateTimeHelper {
	
	private static final String TAG = "DateTimeHelper";
	
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private static final String ZULU_DATA_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	private static final String ZULU_TIME_ZONE = "Zulu";
	
	public static Date parseDate(String dateString) {		
		// The Guardian Content API does something slightly interesting with daylight savings and GMT times.
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
		if (dateString.endsWith("Z")) {
			dateFormat = new SimpleDateFormat(ZULU_DATA_TIME_FORMAT);
			dateFormat.setTimeZone(java.util.TimeZone.getTimeZone(ZULU_TIME_ZONE));
		}	
		 
		try {
			return dateFormat.parse(dateString);
		} catch (ParseException e) {
			Log.e(TAG, "Failed to parse date '" + dateString +  "': " + e.getMessage());
		}
		return null;
	}

	public static Date now() {
	    Calendar cal = Calendar.getInstance();
	    return cal.getTime();
	}

	public static Date yesterday() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, -24);
		return cal.getTime();
	}		

	public static String format(Date date, String format) {
		SimpleDateFormat df = new SimpleDateFormat(format);
		return df.format(date);
	}
	
	public static String calculateTimeTaken(Date startTime, Date now) {
		int seconds = durationInSecords(startTime, now);
		if (seconds < 60) {
			return Long.toString(seconds) + " seconds";
		}
		
		StringBuilder output = new StringBuilder();
		int minutes = (seconds / 60);
		int remainer = (seconds % 60);
		output.append(minutes + " " + Plurals.getPrural("minute", minutes));
		if (remainer > 0) {
			output.append(" " + remainer + " " + Plurals.getPrural("second", remainer));
		}
		return output.toString();
	}
		
	public static boolean isMoreThanHoursOld(Date then, int hours) {
		int seconds = durationInSecords(then, now());
		return (seconds > (hours * 3600));
	}
	
	private static int durationInSecords(Date startTime, Date now) {
		long mills = now.getTime() - startTime.getTime();
		int seconds = new Long(mills / 1000).intValue();
		return seconds;
	}
	
}