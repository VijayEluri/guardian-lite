package nz.gen.wellington.guardian.android.api.caching;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;

import android.content.Context;
import android.util.Log;

public class FileService {
	
	private static final String TAG = "FileService";
	
	
	public static FileOutputStream getFileOutputStream(Context context, String url) throws FileNotFoundException {
		final String filepath = FileService.getLocalFilename(url);
		File file = new File(getCacheDir(context) + "/" + filepath);
		Log.i(TAG, "Opening output stream to: " + file.getAbsolutePath());
		return new FileOutputStream(file);
	}
		
	public static FileInputStream getFileInputStream(Context context, String url) throws FileNotFoundException {
		final String filepath = FileService.getLocalFilename(url);
		File file = new File(getCacheDir(context) + "/" + filepath);
		Log.i(TAG, "Opening input stream to: " + file.getAbsolutePath());
		return new FileInputStream(file);
	}

	public static boolean isLocallyCached(Context context, String apiUrl) {
		File localFile = new File(getCacheDir(context), getLocalFilename(apiUrl));
		Log.i(TAG, "Checking for local cache file at: " + localFile.getAbsolutePath());
		return localFile.exists() && localFile.canRead();
	}
	
	public static String getLocalFilename(String url) {
		return url.replaceAll("/", "").replaceAll(":", "");
	}

	public static void clear(Context context, String apiUrl) {
		File localFile = new File(getCacheDir(context), getLocalFilename(apiUrl));
		Log.i(TAG, "Clearing: " + localFile.getAbsolutePath());
		localFile.delete();
	}

	
	public static void clearAll(Context context) {		
		Log.i(TAG, "Clearing all cache files");				
		FileFilter allFilesFilter = new FileFilter() {				
			@Override
			public boolean accept(File file) {
				return true;
			}
		};
		deleteFiles(context, allFilesFilter);
	}
	
	
	public static void clearAllArticleSets(Context context) {		
		Log.i(TAG, "Clearing all article set cache files");
				
		FileFilter jsonFilesFilter = new FileFilter() {				
			@Override
			public boolean accept(File file) {
				return file.getPath().endsWith("json") && !file.getPath().endsWith("sections.json");
			}
		};		
		deleteFiles(context, jsonFilesFilter);
	}

	
	private static void deleteFiles(Context context, FileFilter jsonFilesFilter) {		
		File cacheDir = getCacheDir(context);
		if (cacheDir == null) {
			Log.i(TAG, "No cache folder found");
			return;
		}
		Log.i(TAG, "Cache dir path is: " + cacheDir.getPath());
		Log.i(TAG, "Cache dir absolute path is: " + cacheDir.getAbsolutePath());
				
		
		File[] listFiles = cacheDir.listFiles(jsonFilesFilter);
		Log.i(TAG, "Cache dir file count: " + listFiles.length);
		for (int i = 0; i < listFiles.length; i++) {
			File cacheFile = listFiles[i];
			Log.i(TAG, "Found cache file: " + cacheFile.getAbsolutePath());
			if (cacheFile.delete()) {
				Log.i(TAG, "Deleted cache file: " + cacheFile.getAbsolutePath());				
			}		
		}
	}
	

	// TODO make a preference - only use external if installed - external is the SD card right?
	private static File getCacheDir(Context context) {
		return context.getCacheDir();
	}
	
}
