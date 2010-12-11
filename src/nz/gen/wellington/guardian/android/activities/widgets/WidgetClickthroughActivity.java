package nz.gen.wellington.guardian.android.activities.widgets;

import nz.gen.wellington.guardian.android.activities.article;
import nz.gen.wellington.guardian.android.api.ArticleDAO;
import nz.gen.wellington.guardian.android.api.ContentFetchType;
import nz.gen.wellington.guardian.android.factories.SingletonFactory;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.ArticleBundle;
import nz.gen.wellington.guardian.android.model.ArticleSet;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public abstract class WidgetClickthroughActivity extends Activity {

	private static final String TAG = "WidgetClickthroughActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	protected void onResume() {
		super.onResume();		
		Intent intent = new Intent(this, getDefaultActivity());
		
		Uri dataUri = this.getIntent().getData();
		if (dataUri != null) {
			final String articleId = extractArticleIdFromUri(dataUri);
			
			Log.d(TAG, "Requested article id was: " + articleId);
			if (articleId != null) {
				final Article article = getArticleById(articleId);
				
				if (article != null) {
					intent = new Intent(this, article.class);
					intent.putExtra("article", article);
	
				} else {
					Log.d(TAG, "Failed to find article: " + articleId);
				}
			}
			
		} else {
			Log.w(TAG, "No data uri was found on intent");
		}
		this.startActivity(intent);
	}

	private String extractArticleIdFromUri(Uri dataUri) {
		String path = dataUri.getPath();
		Log.d(TAG, "Path is: " + path);
		if (path.startsWith("/id/")) {
			return path.replaceFirst("/id/", "");	
		}
		return null;
	}

	protected abstract Class<? extends Activity> getDefaultActivity();
	
	protected abstract ArticleSet getArticleSet();
		
	private Article getArticleById(final String articleId) {
		ArticleSet articleSet = getArticleSet();
		ArticleDAO articleDAO = SingletonFactory.getDao(this.getApplicationContext());
		ArticleBundle bundle = articleDAO.getArticleSetArticles(articleSet, ContentFetchType.LOCAL_ONLY);
		if (bundle != null) {
			for (Article article : bundle.getArticles()) {
				if (article.getId().equals(articleId)) {
					return article;
				}
			}
		}
		return null;
	}
	
}
