package nz.gen.wellington.guardian.android.activities;

import java.util.Arrays;
import java.util.List;

import nz.gen.wellington.guardian.android.factories.ArticleSetFactory;
import nz.gen.wellington.guardian.android.factories.SingletonFactory;
import nz.gen.wellington.guardian.android.model.ArticleSet;
import nz.gen.wellington.guardian.android.usersettings.FavouriteSectionsAndTagsDAO;
import nz.gen.wellington.guardian.android.usersettings.PreferencesDAO;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class savedArticles extends ArticleListActivity implements FontResizingActivity {
		
	private PreferencesDAO preferencesDAO;
	private ArticleSetFactory articleSetFactory;
	private FavouriteSectionsAndTagsDAO favouriteSectionsAndTagsDAO;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		preferencesDAO = SingletonFactory.getPreferencesDAO(this.getApplicationContext());
		articleSetFactory = SingletonFactory.getArticleSetFactory(this.getApplicationContext());
		favouriteSectionsAndTagsDAO = SingletonFactory.getFavouriteSectionsAndTagsDAO(this.getApplicationContext());
		
    	setHeading("Saved articles");
    	final int baseSize = preferencesDAO.getBaseFontSize();
		setFontSize(baseSize);
	}
	
	protected ArticleSet getArticleSet() {
		return articleSetFactory.getSavedArticlesArticleSet(favouriteSectionsAndTagsDAO.getSavedArticleIds());
	}
	
	protected List<String> getPermittedRefinements() {
		return Arrays.asList(permittedRefinements);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		final int baseSize = preferencesDAO.getBaseFontSize();
		setFontSize(baseSize);
	}

	@Override
	protected String getRefinementDescription(String refinementType) {
		return null;
	}
	
	
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 1, 0, "Home");
		menu.add(0, 2, 0, "Refresh");
		menu.add(0, 3, 0, "Remove all");
	    return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			switchToMain();
			return true;
		case 2:
			refresh();
			return true;
		case 3:
			removeAll();
			return true;
		}
		return false;
	}

	private void removeAll() {
		favouriteSectionsAndTagsDAO.removeAllSavedArticles();
		refresh();
	}
	
}