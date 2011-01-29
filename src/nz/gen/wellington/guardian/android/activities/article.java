package nz.gen.wellington.guardian.android.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.gen.wellington.guardian.android.R;
import nz.gen.wellington.guardian.android.activities.ui.TagListPopulatingService;
import nz.gen.wellington.guardian.android.api.ImageDAO;
import nz.gen.wellington.guardian.android.api.ImageDownloadDecisionService;
import nz.gen.wellington.guardian.android.factories.ArticleSetFactory;
import nz.gen.wellington.guardian.android.factories.SingletonFactory;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.MediaElement;
import nz.gen.wellington.guardian.android.network.NetworkStatusService;
import nz.gen.wellington.guardian.android.usersettings.FavouriteSectionsAndTagsDAO;
import nz.gen.wellington.guardian.android.utils.ShareTextComposingService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

public class article extends MenuedActivity implements FontResizingActivity {
		
	private static final String TAG = "article";
	
	private static final String REMOVE_SAVED_ARTICLE = "Remove saved article";
	private static final String SAVE_ARTICLE = "Save article";
	
	private NetworkStatusService networkStatusService;
    private ImageDAO imageDAO;
    private ArticleSetFactory articleSetFactory;
    private FavouriteSectionsAndTagsDAO favouriteSectionsAndTagsDAO;
    private Article article;
       
	private MainImageUpdateHandler mainImageUpdateHandler;
	private GalleryImageUpdateHandler galleryImageUpdateHandler;
    private MainImageLoader mainImageLoader;

    private Map<String, Bitmap> images;
	private MenuItem saveArticleMenuItem;
	private String shareText;
	private TagListPopulatingService tagListPopulatingService;
	private ImageDownloadDecisionService imageDownloadDecisionService;
	private ImageAdapter imageAdapter;
	private GridView thumbnails;
        
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		imageDAO = SingletonFactory.getImageDao(this.getApplicationContext());
		articleSetFactory = SingletonFactory.getArticleSetFactory(this.getApplicationContext());
		networkStatusService = SingletonFactory.getNetworkStatusService(this.getApplicationContext());
		favouriteSectionsAndTagsDAO = SingletonFactory.getFavouriteSectionsAndTagsDAO(this.getApplicationContext());
		tagListPopulatingService = SingletonFactory.getTagListPopulator(this.getApplicationContext());
		imageDownloadDecisionService = SingletonFactory.getImageDownloadDecisionService(this.getApplicationContext());
		
		images = new HashMap<String, Bitmap>();
    	mainImageUpdateHandler = new MainImageUpdateHandler();
    	galleryImageUpdateHandler = new GalleryImageUpdateHandler();

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.article);
		
		this.article = (Article) this.getIntent().getExtras().get("article");		
		if (article != null) {
			populateContent(article, colourScheme.getBodytext(), colourScheme.getHeadline());			
		} else {
        	Toast.makeText(this, "Could not load article", Toast.LENGTH_SHORT).show();
		}		
	}
	

	@Override
	protected void onResume() {
		super.onResume();
		//setFontSize();	
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		images.clear();
	}

	
	private void populateContent(Article article, int bodytextColour, int headlineColour) {
		if (article.isGallery()) {
			setContentView(R.layout.gallery);
		}
		
		if (article.getSection() != null) {
			setHeading(article.getSection().getName());
			setHeadingColour(article.getSection().getColour());
		}
		
		TextView headline = (TextView) findViewById(R.id.Headline);
		TextView pubDate = (TextView) findViewById(R.id.PubDate);
		TextView byline = (TextView) findViewById(R.id.Byline);
		TextView standfirst = (TextView) findViewById(R.id.Standfirst);
		
		headline.setTextColor(headlineColour);
		pubDate.setTextColor(bodytextColour);        
		byline.setTextColor(bodytextColour);
		standfirst.setTextColor(bodytextColour);
				
		headline.setText(article.getTitle());
		if (article.getPubDate() != null) {
			pubDate.setText(article.getPubDateString());
		}
		
		if (article.getByline() != null && !article.getByline().trim().equals("")) {
			byline.setText(article.getByline());
		} else {
			byline.setVisibility(View.GONE);
		}
		
		if (article.getStandfirst() != null && !article.getStandfirst().trim().equals("")) { 	
			standfirst.setText(article.getStandfirst());
		} else {
			standfirst.setVisibility(View.GONE);
		}
		
	    if (article.isGallery()) {        	
        	populateGalleryView(article);        	
        } else {
        	populateArticleView(article, bodytextColour, headlineColour);
        }
	    
		final boolean isTagged = !article.getAuthors().isEmpty()
		|| !article.getKeywords().isEmpty();
		if (isTagged) {
			final boolean connectionAvailable = networkStatusService
			.isConnectionAvailable();
			populateTags(article, connectionAvailable);
		}
	}


	private void populateArticleView(Article article, int bodytextColour, int headlineColour) {
		TextView description = (TextView) findViewById(R.id.Description);
		        
		setFontSize();

        description.setTextColor(bodytextColour);        
		description.setVisibility(View.VISIBLE);
		if (article.isRedistributionAllowed()) {
			description.setText(article.getDescription());
		} else {
			description.setText("Redistribution rights for this article are not available. "
					+ "The full content cannot be downloaded but you should still be able to use the open in browser option to view the original article.");
		}

		final String mainImageUrl = article.getMainImageUrl();
		if (mainImageUrl != null && (imageDAO.isAvailableLocally(mainImageUrl) || imageDownloadDecisionService.isOkToDownloadMainImages())) {
			mainImageLoader = new MainImageLoader(imageDAO, article.getMainImageUrl());
			Thread loader = new Thread(mainImageLoader);
			loader.start();
		}

	}

	private void populateGalleryView(Article article) {
		thumbnails = (GridView) findViewById(R.id.GalleryThumbnails);
		imageAdapter = new ImageAdapter();
		thumbnails.setAdapter(imageAdapter);
		
		setFontSize();
		
		if (!article.getMediaElements().isEmpty()) {			
			GalleryImageLoader galleryImageLoader = new GalleryImageLoader(imageDAO, article.getMediaElements());
			Thread loader = new Thread(galleryImageLoader);
			loader.start();
		}
	}


	public void setFontSize() {
		super.setFontSize();
		
		TextView headline = (TextView) findViewById(R.id.Headline);
		TextView caption = (TextView) findViewById(R.id.Caption);
		TextView pubDate = (TextView) findViewById(R.id.PubDate);
		TextView byline = (TextView) findViewById(R.id.Byline);
		TextView standfirst = (TextView) findViewById(R.id.Standfirst);
		TextView description = (TextView) findViewById(R.id.Description);
		
		headline.setTextSize(TypedValue.COMPLEX_UNIT_PT, baseFontSize + 1);
		pubDate.setTextSize(TypedValue.COMPLEX_UNIT_PT, baseFontSize - 2);
		caption.setTextSize(TypedValue.COMPLEX_UNIT_PT, baseFontSize -2);
		byline.setTextSize(TypedValue.COMPLEX_UNIT_PT, baseFontSize);
		pubDate.setTextSize(TypedValue.COMPLEX_UNIT_PT, baseFontSize - 2);
        standfirst.setTextSize(TypedValue.COMPLEX_UNIT_PT, baseFontSize);
        
        if (description != null) {
        	description.setTextSize(TypedValue.COMPLEX_UNIT_PT, baseFontSize);
        }
        
		caption.setTextColor(colourScheme.getBodytext());
		
		TextView tagLabel =  (TextView) findViewById(R.id.TagLabel);
		if (tagLabel != null) {
			tagLabel.setTextColor(colourScheme.getBodytext());
		}
	}
	
	private void populateTags(Article article, final boolean connectionAvailable) {
		LayoutInflater inflater = LayoutInflater.from(this);
		findViewById(R.id.TagLabel).setVisibility(View.VISIBLE);
		
		tagListPopulatingService.populateTags(inflater, connectionAvailable, (LinearLayout) findViewById(R.id.AuthorList), articleSetFactory.getArticleSetsForTags(article.getAuthors()), colourScheme);
		tagListPopulatingService.populateTags(inflater, connectionAvailable, (LinearLayout) findViewById(R.id.TagList), articleSetFactory.getArticleSetsForTags(article.getKeywords()), colourScheme);
	}

	private void populateMainImage(String mainImageUrl) {
		if (article != null && article.getMainImageUrl() != null && article.getMainImageUrl().equals(mainImageUrl)) {		
			if (images.containsKey(mainImageUrl)) {		
				Bitmap bitmap = images.get(mainImageUrl);
				if (bitmap != null) {
					populateMainImage(bitmap);
				}
			}
		}
	}

	private void populateMainImage(Bitmap bitmap) {
		ImageView imageView = (ImageView) findViewById(R.id.ArticleImage);
		imageView.setImageBitmap(bitmap);			
		imageView.setVisibility(View.VISIBLE);
		final boolean isImageLandScaped = bitmap.getWidth() > bitmap.getHeight();
		if (isImageLandScaped) {
			imageView.setScaleType(ScaleType.FIT_XY);
		}
		populateCaption(article.getCaption());
	}

	private void populateCaption(String caption) {
		if (caption != null && !caption.trim().equals("")) {
			TextView captionView = (TextView) findViewById(R.id.Caption);
			captionView.setVisibility(View.VISIBLE);
			captionView.setText(caption);
		}
	}
	
	
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MenuedActivity.HOME, 0, "Home");
		
		if (article != null && article.getId() != null) {
			if (favouriteSectionsAndTagsDAO.isSavedArticle(article)) {
				saveArticleMenuItem = menu.add(0, MenuedActivity.SAVE_REMOVE_ARTICLE, 0, REMOVE_SAVED_ARTICLE);				
			} else {
				saveArticleMenuItem = menu.add(0, MenuedActivity.SAVE_REMOVE_ARTICLE, 0, SAVE_ARTICLE);
			}
		}
		
	    MenuItem showInBrowserMenuOption = menu.add(0, MenuedActivity.BROWSER, 0, "Open in browser");
		if (article != null && article.getWebUrl() != null) {
			showInBrowserMenuOption.setEnabled(true);
		} else {
			showInBrowserMenuOption.setEnabled(false);
		}
	    
	    MenuItem shareMenuOption = menu.add(0, MenuedActivity.SHARE, 0, "Share");
		shareText = ShareTextComposingService.composeShareText(article);
		if (article != null && shareText != null) {
			shareMenuOption.setEnabled(true);
		} else {
			shareMenuOption.setEnabled(false);
		}
	    return true;
	}
	
	
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!super.onOptionsItemSelected(item)) {
			switch (item.getItemId()) {
			case MenuedActivity.SAVE_REMOVE_ARTICLE:
				processSavedArticle(article);
				return true;
			case MenuedActivity.BROWSER:
				showArticleInBrowser(article);
				return true;
			case MenuedActivity.SHARE:
				shareArticle(article);
				return true;
			}
		}
		return false;
	}
	

	private void processSavedArticle(Article article) {
		if (!favouriteSectionsAndTagsDAO.isSavedArticle(article)) {
			if (favouriteSectionsAndTagsDAO.addSavedArticle(article)) {
				saveArticleMenuItem.setTitle(REMOVE_SAVED_ARTICLE);
			} else {
				Toast.makeText(this, "Saved articles list is full", Toast.LENGTH_LONG).show();
			}			
		} else {
			if (favouriteSectionsAndTagsDAO.removeSavedArticle(article)) {
				saveArticleMenuItem.setTitle(SAVE_ARTICLE);
			} else {
				Toast.makeText(this, "Saved articles list is full", Toast.LENGTH_LONG).show();
			}
		}
	}

	
	private void shareArticle(Article article) {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, "guardian.co.uk article");
		shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
		startActivity(Intent.createChooser(shareIntent, "Share"));
	}
	
	
	private void showArticleInBrowser(Article article) {
		Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(article.getWebUrl()));
		startActivity(browserIntent);		
	}
	
	
	class MainImageLoader implements Runnable {		

		private ImageDAO imageDAO;
		private String mainImageUrl;
		
		public MainImageLoader(ImageDAO imageDAO, String mainImageUrl) {
			this.imageDAO = imageDAO;
			this.mainImageUrl = mainImageUrl;
		}
		
		@Override
		public void run() {
			Bitmap image = imageDAO.getImage(mainImageUrl);
			if (image != null) {
				images.put(mainImageUrl, image);
				sendMainImageAvailableMessage(mainImageUrl);
			}
			return;
		}

		private void sendMainImageAvailableMessage(String mainImageUrl) {
			Message msg = new Message();
			msg.what = MainImageUpdateHandler.MAIN_IMAGE_AVAILABLE;
			msg.getData().putString("mainImageUrl", mainImageUrl);
			mainImageUpdateHandler.sendMessage(msg);
		}		
	}
	
	class GalleryImageLoader implements Runnable {

		private static final String TAG = "GalleryImageLoader"
			;
		private ImageDAO imageDAO;
		private List<MediaElement> mediaElements;
		
		public GalleryImageLoader(ImageDAO imageDAO, List<MediaElement> mediaElements) {
			this.imageDAO = imageDAO;
			this.mediaElements = mediaElements;
		}
		
		@Override
		public void run() {
			Log.i(TAG, "Running gallery image loader with " + mediaElements + " media elements");
			for (MediaElement mediaElement : mediaElements) {
				
				if (mediaElement != null && mediaElement.isPicture() && mediaElement.getThumbnail() != null) {
					final String imageUrl = mediaElement.getThumbnail();
					Bitmap image = imageDAO.getImage(imageUrl);
					if (image != null) {
						images.put(imageUrl, image);
						sendGalleryImageAvailableMessage(imageUrl);
					}					
				}
			}
		}	

		private void sendGalleryImageAvailableMessage(String imageUrl) {
			Message msg = new Message();
			msg.what = GalleryImageUpdateHandler.GALLERY_IMAGE_AVAILABLE;
			msg.getData().putString("imageUrl", imageUrl);
			galleryImageUpdateHandler.sendMessage(msg);
		}
		
	}
	
	
	class GalleryImageUpdateHandler extends Handler {
		
		public static final int GALLERY_IMAGE_AVAILABLE = 1;

		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			switch (msg.what) {	   
			    case GALLERY_IMAGE_AVAILABLE:
			    final String mainImageUrl = msg.getData().getString("imageUrl");
			    populateGalleryImage(mainImageUrl);
			}
		}
	}
	
	
	class MainImageUpdateHandler extends Handler {
		
		private static final int MAIN_IMAGE_AVAILABLE = 1;

		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			switch (msg.what) {	   
			    case MAIN_IMAGE_AVAILABLE:
			    final String mainImageUrl = msg.getData().getString("mainImageUrl");
			    populateMainImage(mainImageUrl);
			}
		}
	}


	public void populateGalleryImage(String imageUrl) {
		ImageView imageView = new ImageView(this.getApplicationContext());
		Bitmap image = images.get(imageUrl);
		imageView.setImageBitmap(image);
		imageView.setPadding(5, 5, 5, 5);
		//imageView.setLayoutParams(new GridView.LayoutParams(50, 50));

		
		Log.i(TAG, "Adding view to gridview");
		imageAdapter.add(imageView);
		thumbnails.invalidateViews();
		
		// TODO
		//TextView caption = new TextView(this.getApplicationContext());
		//caption.setText(mediaElement.getCaption());
		//authorList.addView(caption);		
	}
	
	
	
	
	public class ImageAdapter extends BaseAdapter {

		private List<View> views;

		public ImageAdapter() {
			views = new ArrayList<View>();
		}

		public int getCount() {
			return views.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {  
				return views.get(position);
			} else {
				convertView = views.get(position);
				return convertView;
			}
		}

		public void add(View view) {
			views.add(view);
		}
	}
			
}
