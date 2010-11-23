package nz.gen.wellington.guardian.android.api;

import java.util.Date;
import java.util.List;

import nz.gen.wellington.guardian.android.activities.ArticleCallback;
import nz.gen.wellington.guardian.android.api.caching.FileBasedArticleCache;
import nz.gen.wellington.guardian.android.dates.DateTimeHelper;
import nz.gen.wellington.guardian.android.model.ArticleBundle;
import nz.gen.wellington.guardian.android.model.ArticleSet;
import nz.gen.wellington.guardian.android.model.Section;
import nz.gen.wellington.guardian.android.usersettings.PreferencesDAO;
import android.content.Context;
import android.util.Log;

public class ArticleDAO {
		
	private static final String TAG = "ArticleDAO";
	
	private FileBasedArticleCache fileBasedArticleCache;
	private ArticleCallback articleCallback;
	private ContentSource openPlatformApi;
	private SectionDAO sectionsDAO;
	private PreferencesDAO preferencesDAO;
	
	
	public ArticleDAO(Context context) {
		fileBasedArticleCache = new FileBasedArticleCache(context);		
		openPlatformApi = ArticleDAOFactory.getOpenPlatformApi(context);
		sectionsDAO = ArticleDAOFactory.getSectionDAO(context);
		preferencesDAO = ArticleDAOFactory.getPreferencesDAO(context);
	}
	
	
	public ArticleBundle getArticleSetArticles(ArticleSet articleSet, ContentFetchType fetchType) {
		Log.i(TAG, "Retrieving articles for article set: " + articleSet.getName() + " (" + fetchType.name() + ")");
		
		if (fetchType.equals(ContentFetchType.LOCAL_ONLY)) {
			return getLocalBundle(articleSet);
		}
		
		if (fetchType.equals(ContentFetchType.UNCACHED)) {
			return fetchFromLive(articleSet);			
		}
		
		if (fetchType.equals(ContentFetchType.CHECKSUM)) {
			ArticleBundle localCopy = fileBasedArticleCache.getArticleSetArticles(articleSet, null);
			if (localCopy != null && localCopy.getChecksum() != null) {
				
				Log.i(TAG, "Checking for checksum sync - local article set has checksum: " + localCopy.getChecksum());
				final String remoteChecksum = openPlatformApi.getRemoteChecksum(articleSet, preferencesDAO.getPageSizePreference());
				Log.i(TAG, "Remote checksum is: " + remoteChecksum);
				boolean checksumsMatch = remoteChecksum != null && remoteChecksum.equals(localCopy.getChecksum());
				if (checksumsMatch) {
					Log.i(TAG, "Remote checksum matches local copy. Not refetching");
					fileBasedArticleCache.touchArticleSet(articleSet, DateTimeHelper.now());
					return getLocalBundle(articleSet);		// TODO duplicate read, for the proposes of triggering the call back only is abit rubbish. Should be able to reset on localcopy bundle
					
				} else {
					return fetchFromLive(articleSet);								
				}
				
			} else {
				// TODO Content api sourced article sets do not have a checksum at this point.
				Log.i(TAG, "No checksumed local copy available - fetching from live");
				return fetchFromLive(articleSet);
			}
		}
		
		if (fetchType.equals(ContentFetchType.NORMAL)) {
			ArticleBundle localCopy = getLocalBundle(articleSet);
			if (localCopy != null) {
				return localCopy;
			} else {
				return fetchFromLive(articleSet);
			}			
		}
		
		return null;
	}

		
	public String getArticleSetRemoteChecksum(ArticleSet articleSet) {	
		return openPlatformApi.getRemoteChecksum(articleSet, preferencesDAO.getPageSizePreference());
	}
	
		
	private ArticleBundle getLocalBundle(ArticleSet articleSet) {
		return fileBasedArticleCache.getArticleSetArticles(articleSet, articleCallback);
	}
	
		
	private ArticleBundle fetchFromLive(ArticleSet articleSet) {
		Log.i(TAG, "Fetching from live");
		List<Section> sections = sectionsDAO.getSections();
		if (sections != null) {
			ArticleBundle bundle = openPlatformApi.getArticles(articleSet, sections, articleCallback, preferencesDAO.getPageSizePreference());		
			if (bundle != null) {
				fileBasedArticleCache.putArticleSetArticles(articleSet, bundle);
				return bundle;				
			}
		}
		return null;
	}

	public void clearExpiredCacheFiles(Context context) {
		Log.i(TAG, "Purging expired content");
		fileBasedArticleCache.clearExpiredFiles(context);
	}
	
	public void evictArticleSet(ArticleSet articleSet) {
		fileBasedArticleCache.clear(articleSet);
	}
	
	
	public Date getModificationTime(ArticleSet articleSet) {
		return fileBasedArticleCache.getModificationTime(articleSet);
	}


	public void stopLoading() {
		openPlatformApi.stopLoading();
	}


	public void setArticleReadyCallback(ArticleCallback articleCallback) {
		this.articleCallback = articleCallback;		
	}
	
}
