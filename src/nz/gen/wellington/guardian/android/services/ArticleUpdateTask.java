package nz.gen.wellington.guardian.android.services;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import nz.gen.wellington.guardian.android.api.ArticleDAO;
import nz.gen.wellington.guardian.android.api.ArticleDAOFactory;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.ContentUpdateReport;
import nz.gen.wellington.guardian.android.network.NetworkStatusService;

public abstract class ArticleUpdateTask {

	private static final String TAG = "ArticleUpdateTask";
	
	protected Context context;
	protected ContentUpdateReport report;
	protected ArticleDAO articleDAO;
	protected boolean running = true;

	final public void setReport(ContentUpdateReport report) {
		this.report = report;
	}
	

	final public void stop() {
		Log.i(TAG, "Stopping");
		articleDAO.stopLoading();
		running = false;
	}
	
	
	final protected void processArticles(List<Article> articles) {
		SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(context);
		final boolean largeImagesPreference = (Boolean) prefs.getBoolean("largeImages", true);
		NetworkStatusService networkStatusService = new NetworkStatusService(context);
		final boolean largeImages = largeImagesPreference || networkStatusService.isWifiConnection();
		
		if (articles != null) {
			for (Article article : articles) {
				if (article.getThumbnailUrl() != null) {
					String description = article.getTitle() + " - trail image";					
					queueImageDownloadIsNotAvailableLocally(article.getThumbnailUrl(), description);
				}
				if (largeImages && article.getMainImageUrl() != null) {
					String description = article.getTitle() + " - main image";
					if (article.getCaption() != null) {
						description = article.getCaption();
					}
					queueImageDownloadIsNotAvailableLocally(article.getMainImageUrl(), description);
				}
				report.setArticleCount(report.getArticleCount()+1);
			}
		}
	}
	
	
	final protected void queueImageDownloadIsNotAvailableLocally(String imageUrl, String description) {
		if (imageUrl != null && running) {
			if (!ArticleDAOFactory.getImageDao(context).isAvailableLocally(imageUrl)) {
				ArticleDAOFactory.getTaskQueue(context).addImageTask(new ImageFetchTask(imageUrl, context, description));
			}
		}
	}
	
}
