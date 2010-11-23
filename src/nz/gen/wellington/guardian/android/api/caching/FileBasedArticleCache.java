package nz.gen.wellington.guardian.android.api.caching;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import nz.gen.wellington.guardian.android.activities.ArticleCallback;
import nz.gen.wellington.guardian.android.api.openplatfrom.ContentApiUrlService;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.ArticleBundle;
import nz.gen.wellington.guardian.android.model.ArticleSet;
import android.content.Context;
import android.util.Log;

public class FileBasedArticleCache {
	
	private static final String TAG = "FileBasedArticleCache";

	private Context context;
	
	public FileBasedArticleCache(Context context) {
		this.context = context;
	}
	
	
	 public void putArticleSetArticles(ArticleSet articleSet, ArticleBundle bundle) {
		 Log.i(TAG, "Writing to disk '" + articleSet.getName() + "' with checksum: " + bundle.getChecksum());
		 try {
			 FileOutputStream fos = FileService.getFileOutputStream(context, getApiUrlFor(articleSet));
			 ObjectOutputStream out = new ObjectOutputStream(fos);
			 out.writeObject(bundle);
			 out.close();
		 } catch (IOException ex) {
			 Log.e(TAG, "IO Exception while writing article set: " + articleSet.getName() + ex.getMessage());
		 }
	 }
	 
	 
	 public void touchArticleSet(ArticleSet articleSet, Date modTime) {
		 FileService.touchFile(context, getApiUrlFor(articleSet), modTime);
	 }
	 
	 
	 public boolean isLocallyCached(ArticleSet articleSet) {		 
		 String localFileKeyForArticleSet = getApiUrlFor(articleSet);
		 boolean locallyCached = FileService.isLocallyCached(context, localFileKeyForArticleSet);
		 if (locallyCached) {
			 Log.i(TAG, "Article set '" + articleSet.getName() + "' is locally cached");
		 }
		return locallyCached;
	 }
	 
	 public ArticleBundle getArticleSetArticles(ArticleSet articleSet, ArticleCallback articleCallback) {
		if (!FileService.isLocallyCached(context, getApiUrlFor(articleSet))) {
			return null;
		}
		
		Log.i(TAG, "Reading from disk: " + articleSet.getName());
		try {
			final String localFileKey = getApiUrlFor(articleSet);
			FileInputStream fis = FileService.getFileInputStream(context, localFileKey);
			ObjectInputStream in = new ObjectInputStream(fis);
			ArticleBundle loaded = (ArticleBundle) in.readObject();
			in.close();
			
			Log.i(TAG, "Finished reading from disk: " + articleSet.getName());
			if (loaded != null) {
								
				if (articleCallback != null) {
					for (Article article : loaded.getArticles()) {
						articleCallback.articleReady(article);
					}
				}
					
				Log.i(TAG, "Loaded " + loaded.getArticles().size() + " articles");
				Log.i(TAG, "Content checksum is: " + loaded.getChecksum());
				return loaded;

			} else {
				Log.w(TAG, "Article bundle was null after read attempt");
			}
			return null;

		} catch (IOException ex) {
			Log.e(TAG, "IO Exception while reading article set: " + articleSet.getName() + ex.getMessage());
		} catch (ClassNotFoundException ex) {
			Log.e(TAG, "Exception while reading article set: " + articleSet.getName() + ex.getMessage());
		}
		return null;
	}
	
	
	public void clear() {
		FileService.clearAll(context);
	}
	
		
	public void clear(ArticleSet articleSet) {
		Log.i(TAG, "Clearing article set: " + articleSet.getName());
		if (FileService.isLocallyCached(context, getApiUrlFor(articleSet))) {
			FileService.clear(context, getApiUrlFor(articleSet));
		} else {
			Log.i(TAG, "No local copy to clear:" + articleSet.getName());
		}
	}


	public Date getModificationTime(ArticleSet articleSet) {
		return FileService.getModificationTime(context, getApiUrlFor(articleSet));
	}
	
	
	
	private String getApiUrlFor(ArticleSet articleSet) {
		ContentApiUrlService contentApiUrlService = new ContentApiUrlService(context);
		return contentApiUrlService.getContentApiUrlForArticleSet(articleSet);
	}
}
