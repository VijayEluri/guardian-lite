package nz.gen.wellington.guardian.android.api;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.android.activities.ArticleCallback;
import nz.gen.wellington.guardian.android.api.caching.FileBasedArticleCache;
import nz.gen.wellington.guardian.android.api.caching.FileBasedSectionCache;
import nz.gen.wellington.guardian.android.api.caching.FileService;
import nz.gen.wellington.guardian.android.api.caching.InMemorySectionCache;
import nz.gen.wellington.guardian.android.dates.DateTimeHelper;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.ArticleBundle;
import nz.gen.wellington.guardian.android.model.ArticleSet;
import nz.gen.wellington.guardian.android.model.Section;
import nz.gen.wellington.guardian.android.model.TopStoriesArticleSet;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ArticleDAO {
		
	private static final String TAG = "ArticleDAO";
	
	InMemorySectionCache sectionCache;
	FileBasedArticleCache fileBasedArticleCache;
	FileBasedSectionCache fileBasedSectionCache;
	ArticleCallback articleCallback;
	ContentSource openPlatformApi;
	private Context context;
	
	public ArticleDAO(Context context) {
		this.sectionCache = CacheFactory.getSectionCache();
		this.fileBasedArticleCache = new FileBasedArticleCache(context);
		this.fileBasedSectionCache = new FileBasedSectionCache(context);
		openPlatformApi = ArticleDAOFactory.getOpenPlatformApi(context);
		this.context = context;
	}
	
	
	public List<Section> getSections() {
		 List<Section> sections = fileBasedSectionCache.getSections();
		 if (sections != null) {
			 return sections;
		 }
		 
		 sections = openPlatformApi.getSections();
		 if (sections != null) {
			sectionCache.addAll(sections);
			fileBasedSectionCache.putSections(sections);
		}
		return sections;
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
			ArticleBundle localCopy = getLocalBundle(articleSet);
			if (localCopy != null && localCopy.getChecksum() != null) {
				Log.i(TAG, "Checking for checksum sync - local article set has checksum: " + localCopy.getChecksum());
				final String remoteChecksum = openPlatformApi.getRemoteChecksum(articleSet, getPageSizePreference());
				boolean checksumsMatch = remoteChecksum != null && remoteChecksum.equals(localCopy.getChecksum());
				if (checksumsMatch) {
					Log.i(TAG, "Remote checksum matches local copy. Not refetching");
					return localCopy;

				} else {
					return fetchFromLive(articleSet);								
				}
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
		return openPlatformApi.getRemoteChecksum(articleSet, getPageSizePreference());
	}
	
		
	private ArticleBundle getLocalBundle(ArticleSet articleSet) {
		return fileBasedArticleCache.getArticleSetArticles(articleSet, articleCallback);
	}
	
		
	private ArticleBundle fetchFromLive(ArticleSet articleSet) {
		Log.i(TAG, "Fetching from live");
		List<Section> sections = this.getSections();
		if (sections != null) {
			ArticleBundle bundle = openPlatformApi.getArticles(articleSet, sections, articleCallback, getPageSizePreference());		
			if (bundle != null) {
				fileBasedArticleCache.putArticleSetArticles(articleSet, bundle);
				return bundle;				
			}
		}
		return null;
	}

	private int getPageSizePreference() {
		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(context);
		final String pageSizeString = prefs.getString("pageSize", "10");
		int pageSize = Integer.parseInt(pageSizeString);
		return pageSize;
	}

	
	public void evictSections() {
		sectionCache.clear();
		fileBasedSectionCache.clear();
	}
	
	public void clearExpiredCacheFiles(Context context) {
		FileService.clearExpiredCacheFiles(context);
	}
	
	public void evictAll() {
		fileBasedArticleCache.clear();
	}
	
	
	public void evictArticleSet(ArticleSet articleSet) {
		fileBasedArticleCache.clear(articleSet);
	}
	
	public void saveTopStories(List<Article> topStories) {
		fileBasedArticleCache.putArticleSetArticles(new TopStoriesArticleSet(), new ArticleBundle(topStories, null, null, DateTimeHelper.now(), null));
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


	public Map<String, Section> getSectionsMap() {
		Map<String, Section> sectionsMap = new HashMap<String, Section>();
		List<Section> sections = this.getSections();
		if (sections != null) {
			for (Section section : sections) {
				sectionsMap.put(section.getId(), section);
			}
		}
		return sectionsMap;
	}
	
}
