package nz.gen.wellington.guardian.android.api.openplatfrom;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.android.activities.ArticleCallback;
import nz.gen.wellington.guardian.android.api.ArticleDAOFactory;
import nz.gen.wellington.guardian.android.api.ContentSource;
import nz.gen.wellington.guardian.android.dates.DateTimeHelper;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.ArticleBundle;
import nz.gen.wellington.guardian.android.model.ArticleSet;
import nz.gen.wellington.guardian.android.model.Section;
import nz.gen.wellington.guardian.android.model.Tag;
import nz.gen.wellington.guardian.android.network.HttpFetcher;
import nz.gen.wellington.guardian.android.usersettings.PreferencesDAO;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ContentApiStyleApi implements ContentSource {
		
	private static final String TAG = "ContentApiStyleApi";
	
	private ContentApiStyleXmlParser contentXmlParser;
	private ContentApiStyleJSONParser contentJsonParser;
	PreferencesDAO preferencesDAO;
	private HttpFetcher httpFetcher;
	private ContentApiUrlService articleSetContentApiUrlService;

	private Context context;

	
	public ContentApiStyleApi(Context context) {
		this.context = context;
		httpFetcher = new HttpFetcher(context);
		contentXmlParser = new ContentApiStyleXmlParser(context);
		contentJsonParser = new ContentApiStyleJSONParser();		
		preferencesDAO = ArticleDAOFactory.getPreferencesDAO(context);
	}

	
	@Override
	public ArticleBundle getArticles(ArticleSet articleSet, List<Section> sections, ArticleCallback articleCallback, int pageSize) {
		Log.i(TAG, "Fetching articles for: " + articleSet.getName());
		
		final String contentApiUrl = articleSetContentApiUrlService.getContentApiUrlForArticleSet(articleSet, pageSize);
		
		announceDownloadStarted(articleSet.getName() + " article set");
		InputStream input = getHttpInputStream(contentApiUrl);
		if (input != null) {
			List<Article> articles = contentXmlParser.parseArticlesXml(input, sections, articleCallback);
			if (articles != null && !articles.isEmpty()) {
				return new ArticleBundle(articles, contentXmlParser.getRefinements(), contentXmlParser.getChecksum(), DateTimeHelper.now(), contentXmlParser.getDescription());
			}
		}
		return null;
	}
	
	
	@Override
	public String getRemoteChecksum(ArticleSet articleSet, int pageSize) {		
		Log.i(TAG, "Fetching article set checksum for article set: " + articleSet.getName());
		
		String contentApiUrl = articleSetContentApiUrlService.getContentApiUrlForArticleSetChecksum(articleSet, pageSize);
		
		announceDownloadStarted(articleSet.getName() + " article set checksum");		
		InputStream input = getHttpInputStream(contentApiUrl);		
		if (input != null) {
			contentXmlParser.parseArticlesXml(input, null, null);
			return contentXmlParser.getChecksum();			
		}
		return null;
	}


	@Override
	public List<Section> getSections() {
		Log.i(TAG, "Fetching section list from live api");
		String contentApiUrl = articleSetContentApiUrlService.getSectionsQueryUrl();
		InputStream input = getHttpInputStream(contentApiUrl);
		if (input != null) {
			return contentJsonParser.parseSectionsJSON(input);
		}
		return null;
	}
	
	
	@Override
	public List<Tag> searchTags(String searchTerm, Map<String, Section> sections) {
		Log.i(TAG, "Fetching tag list from live api: " + searchTerm);
		announceDownloadStarted("tag results");
		InputStream input = getHttpInputStream(articleSetContentApiUrlService.getTagSearchQueryUrl(searchTerm));
		if (input != null) {
			return contentJsonParser.parseTagsJSON(input, sections);
		}
		return null;
	}


	@Override
	public void stopLoading() {
		contentXmlParser.stop();
		httpFetcher.stopLoading();
	}
	
	private void announceDownloadStarted(String downloadName) {
		Intent intent = new Intent(HttpFetcher.DOWNLOAD_PROGRESS);
		intent.putExtra("type", HttpFetcher.DOWNLOAD_STARTED);
		intent.putExtra("url", downloadName);
		context.sendBroadcast(intent);
	}
	
	private InputStream getHttpInputStream(String url) {
		return httpFetcher.httpFetch(url);		
	}
		
}
