package nz.gen.wellington.guardian.android.activities.ui;

import java.util.ArrayList;
import java.util.List;

import nz.gen.wellington.guardian.android.api.ArticleDAOFactory;
import nz.gen.wellington.guardian.android.api.ImageDAO;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.ImageDecoratedArticle;
import android.content.Context;
import android.graphics.Bitmap;

public class ArticleImageDecorator {

	public static List<ImageDecoratedArticle> decorateNewsitemsWithThumbnails(List<Article> newsitems, Context context) {
		List<ImageDecoratedArticle> decorated = new ArrayList<ImageDecoratedArticle>();
		ImageDAO imageDAO = ArticleDAOFactory.getImageDao(context);
		for (Article article : newsitems) {
			applyThumbnailIfAvailableLocally(decorated, imageDAO, article);
		}
		return decorated;
	}

	
	private static void applyThumbnailIfAvailableLocally(
			List<ImageDecoratedArticle> decorated, ImageDAO imageDAO,
			Article article) {
		Bitmap image = null;		
		if (article.getThumbnailUrl() != null && imageDAO.isAvailableLocally(article.getThumbnailUrl())) {
			image = imageDAO.getImage(article.getThumbnailUrl());
		}
		decorated.add(new ImageDecoratedArticle(article, image));
	}
	
}
