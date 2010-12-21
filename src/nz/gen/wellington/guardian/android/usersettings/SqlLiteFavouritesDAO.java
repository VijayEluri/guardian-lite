package nz.gen.wellington.guardian.android.usersettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.Section;
import nz.gen.wellington.guardian.android.model.Tag;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

// TODO null safe all db opens.
public class SqlLiteFavouritesDAO {
	
	private static final String TAG = "SqlLiteFavouritesDAO";
	private static final int MAX_FAVOURITE_TAGS = 20;
	private static final int MAX_FAVOURITE_ARTICLES = 5;

	private static final String DATABASE_NAME = "guardian-lite.db";
	private static final int DATABASE_VERSION = 2;
	
	private static final String TAG_TABLE = "favourites";
	private static final String SAVED_ARTICLES_TABLE = "saved_articles";
	
	private static final String SECTIONID = "sectionid";
	private static final String ARTICLEID = "articleid";
	private static final String NAME = "name";
	private static final String APIID = "apiid";
	private static final String TYPE = "type";
	
	private static final String NAME_ASC = "name asc";
	private static final String ARTICLEID_DESC = "articleid DESC";
	
	private static final String INSERT_FAVOURITE_TAG = "insert into " + TAG_TABLE + "(type, apiid, name, sectionid) values (?, ?, ?, ?)";
	private static final String INSERT_SAVED_ARTICLE = "insert into " + SAVED_ARTICLES_TABLE + "(articleid) values (?)";
	
	private OpenHelper openHelper;

	public SqlLiteFavouritesDAO(Context context) {
		openHelper = new OpenHelper(context);
	}
	
	public synchronized boolean hasFavourites() {	// TODO count query rather than select all
		SQLiteDatabase db = openHelper.getReadableDatabase();		
		Cursor cursor = db.query(TAG_TABLE, new String[] { TYPE, APIID, NAME,SECTIONID }, null, null, null, null, NAME_ASC);		
		int total = cursor.getCount();
		closeCursor(cursor);
		db.close();
		return total > 0;	
	}
	
	public synchronized long insertFavouriteTag(String type, String apiid, String name, String sectionid) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		SQLiteStatement insertStmt = db.compileStatement(INSERT_FAVOURITE_TAG);
		insertStmt.bindString(1, type);
		insertStmt.bindString(2, apiid);
		insertStmt.bindString(3, name);
		insertStmt.bindString(4, sectionid);
		long result = insertStmt.executeInsert();
		db.close();
		return result;
	}
	
	
	private synchronized long insertSavedArticle(String articleId) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		SQLiteStatement insertStmt = db.compileStatement(INSERT_SAVED_ARTICLE);
		insertStmt.bindString(1, articleId);		
		long result = insertStmt.executeInsert();
		db.close();
		return result;
		
	}
	
		
	public synchronized void deleteAll() {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.delete(TAG_TABLE, null, null);
		db.close();
	}
	
	
	public synchronized boolean isFavourite(Tag tag) {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor cursor = db.query(TAG_TABLE, new String[] { APIID }, " type = 'tag' and apiid = ? ", new String[] { tag.getId() }, null, null, NAME_ASC);
		final boolean isFavourite = cursor.getCount() > 0;
		closeCursor(cursor);
		final boolean result = isFavourite;	
		db.close();
		return result;
	}
	

	public synchronized boolean isFavourite(Section section) {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor cursor = db.query(TAG_TABLE, new String[] { APIID }, " type = 'section' and apiid = ? ", new String[] { section.getId() }, null, null, NAME_ASC);
		final boolean isFavourite = cursor.getCount() > 0;
		closeCursor(cursor);		
		db.close();
		return isFavourite;	
	}
	
	public boolean isSavedArticle(Article article) {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor cursor = db.query(SAVED_ARTICLES_TABLE, new String[] { ARTICLEID }, " articleid = ? ", new String[] { article.getId() }, null, null, ARTICLEID_DESC);
		final boolean isSaved = cursor.getCount() > 0;
		closeCursor(cursor);
		db.close();
		return isSaved;
	}
	
	public synchronized void removeSection(Section section) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.delete(TAG_TABLE, " apiid = ? ", new String[] { section.getId() });
		db.close();
	}
	
	public synchronized void removeTag(Tag tag) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.delete(TAG_TABLE, " apiid = ? ", new String[] { tag.getId() });
		db.close();
	}
	
	
	public synchronized boolean removeSavedArticle(Article article) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		final int deleted = db.delete(SAVED_ARTICLES_TABLE, " articleid = ? ", new String[] { article.getId() });
		db.close();
		return deleted > 0;
	}
	
	
	public void removeAllSavedArticles() {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.execSQL("DELETE FROM " + SAVED_ARTICLES_TABLE);
		db.close();
	}
	
	
	public synchronized List<Tag> getFavouriteTags(Map<String, Section> sectionsMap) {
		List<Tag> favouriteTags = new ArrayList<Tag>();
		SQLiteDatabase db = openHelper.getReadableDatabase();
		if (db != null && db.isOpen()) {
			Cursor cursor = db.query(TAG_TABLE, new String[] {TYPE, APIID, NAME, SECTIONID}, null, null, null, null, NAME_ASC);		
			if (cursor.moveToFirst()) {
				do {
					final String type = cursor.getString(0);
					final String id = cursor.getString(1);
					final String name = cursor.getString(2);
					final String sectionId = cursor.getString(3);
					if (type.equals("tag")) {
						favouriteTags.add(new Tag(name, id, sectionsMap.get(sectionId)));
					}
					
				} while (cursor.moveToNext());
			}
			closeCursor(cursor);
			db.close();
		} else {
			Log.i(TAG, "Could not open readable database connection");
		}
		return favouriteTags;
	}


	public synchronized List<Section> getFavouriteSections(Map<String, Section> sectionsMap) {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor cursor = db.query(TAG_TABLE, new String[] {TYPE, APIID, NAME, SECTIONID}, null, null, null, null, NAME_ASC);
		
		List<Section> favouriteSections = new ArrayList<Section>();
		if (cursor.moveToFirst()) {
			do {
				final String type = cursor.getString(0);
				final String sectionId = cursor.getString(3);
				if (type.equals("section")) {
					Section section = sectionsMap.get(sectionId);
					if (section != null) {
						favouriteSections.add(section);
					}
				}
				
			} while (cursor.moveToNext());
		}
		closeCursor(cursor);
		db.close();
		return favouriteSections;
	}
	
	
	public List<String> getSavedArticleIds() {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor cursor = db.query(SAVED_ARTICLES_TABLE, new String[] {ARTICLEID}, null, null, null, null, ARTICLEID_DESC);
		
		List<String> savedArticleIds = new ArrayList<String>();
		if (cursor.moveToFirst()) {
			do {
				final String articleId = cursor.getString(0);
				savedArticleIds.add(articleId);
			} while (cursor.moveToNext());
		}
		closeCursor(cursor);
		db.close();
		return savedArticleIds;
	}
	
	
	public synchronized boolean addTag(Tag keyword) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		boolean result = false;
		if (haveRoom(db)) {
			this.insertFavouriteTag("tag", keyword.getId(), keyword.getName(), (keyword.getSection() != null) ? keyword.getSection().getId(): "global");
			result = true;
		}
		db.close();
		return result;		
	}
	
	
	public synchronized boolean addSection(Section section) {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		boolean result = false;
		if (this.haveRoom(db)) {
			this.insertFavouriteTag("section", section.getId(), section.getName(), section.getId());
			result = true;
		}
		db.close();
		return result;
	}
	
	public boolean addSavedArticle(Article article) {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		boolean result = false;
		if (this.haveRoomForSavedArticle(db)) {
			this.insertSavedArticle(article.getId());
			result = true;
		}
		db.close();
		return result;
	}
	
	private boolean haveRoomForSavedArticle(SQLiteDatabase db) {
		Cursor cursor = db.query(SAVED_ARTICLES_TABLE, new String[] {ARTICLEID}, null, null, null, null, ARTICLEID_DESC);		
		int total = cursor.getCount();
		closeCursor(cursor);
		return total < MAX_FAVOURITE_ARTICLES;	
	}

	private boolean haveRoom(SQLiteDatabase db) {
		Cursor cursor = db.query(TAG_TABLE, new String[] {TYPE, APIID, NAME,SECTIONID}, null, null, null, null, NAME_ASC);		
		int total = cursor.getCount();
		closeCursor(cursor);
		return total < MAX_FAVOURITE_TAGS;	
	}
	
	private void closeCursor(Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}	
		
	private static class OpenHelper extends SQLiteOpenHelper {
		
		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
			
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TAG_TABLE + "(id INTEGER PRIMARY KEY, type, apiid, name, sectionid TEXT)");
			db.execSQL("CREATE TABLE " + SAVED_ARTICLES_TABLE + "(id INTEGER PRIMARY KEY, articleid TEXT)");
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO can this be made into a loseless upgrade
			db.execSQL("DROP TABLE IF EXISTS " + SAVED_ARTICLES_TABLE);			
			db.execSQL("DROP TABLE IF EXISTS " + TAG_TABLE);
			onCreate(db);
		}
	}
	
}
