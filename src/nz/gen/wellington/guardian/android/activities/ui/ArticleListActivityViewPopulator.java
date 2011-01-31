package nz.gen.wellington.guardian.android.activities.ui;

import java.util.List;

import nz.gen.wellington.guardian.android.R;
import nz.gen.wellington.guardian.android.api.ImageDAO;
import nz.gen.wellington.guardian.android.factories.SingletonFactory;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.ArticleSet;
import nz.gen.wellington.guardian.android.model.ColourScheme;
import nz.gen.wellington.guardian.android.model.Section;
import nz.gen.wellington.guardian.android.model.SectionArticleSet;
import nz.gen.wellington.guardian.android.model.SectionColourMap;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ArticleListActivityViewPopulator {
	
	private Context context;
	private ImageDAO imageDAO;
	private TagListPopulatingService tagListPopulatingService;
	private ImageStretchingService imageStretchingService;

	public ArticleListActivityViewPopulator(Context context) {
		this.context = context;
		this.imageDAO = SingletonFactory.getImageDao(context);
		this.tagListPopulatingService = SingletonFactory.getTagListPopulator(context);
		imageStretchingService = new ImageStretchingService();
	}
			
	public View populateArticleListView(Article article, ColourScheme colourScheme, float baseFontSize, String trailImageUrl, boolean shouldUseFeatureTrail, boolean first, LayoutInflater mInflater, boolean isTrailImageAvailableLocally) {
		View view = chooseTrailView(mInflater, shouldUseFeatureTrail, first);
		
		TextView titleText = (TextView) view.findViewById(R.id.Headline);
		TextView pubDateText = (TextView) view.findViewById(R.id.Pubdate);
		TextView standfirst = (TextView) view.findViewById(R.id.Standfirst);
		TextView caption = (TextView) view.findViewById(R.id.Caption);
		
		titleText.setTextColor(colourScheme.getHeadline());
		pubDateText.setTextColor(colourScheme.getBodytext());			
		standfirst.setTextColor(colourScheme.getBodytext());
		
		titleText.setTextSize(TypedValue.COMPLEX_UNIT_PT, baseFontSize);
		pubDateText.setTextSize(TypedValue.COMPLEX_UNIT_PT, baseFontSize -2);
		standfirst.setTextSize(TypedValue.COMPLEX_UNIT_PT, new Float(baseFontSize - 0.75));

		if (caption != null) {
			caption.setTextColor(colourScheme.getBodytext());
		}
		titleText.setText(article.getTitle());			
		if (article.getPubDate() != null) {
			pubDateText.setText(article.getPubDateString());
		}
		
		if (article.getStandfirst() != null) {
			standfirst.setText(article.getStandfirst());
		}
					
		if (caption != null && article.getCaption() != null) {
			caption.setText(article.getCaption());
			caption.setVisibility(View.VISIBLE);
		}
		
		if (trailImageUrl != null && isTrailImageAvailableLocally) {
			boolean isMainImage = trailImageUrl != null && trailImageUrl.equals(article.getMainImageUrl());
			populateTrailImage(trailImageUrl, view, isMainImage);
		}
		
		view.setOnClickListener(new ContentClicker(article));
		return view;
	}
	
	
	
	private View chooseTrailView(LayoutInflater mInflater, boolean shouldUseFeatureTrail, boolean hideDivider) {
		View view;
		if (shouldUseFeatureTrail) {
			view = mInflater.inflate(R.layout.featurelist, null);
		} else {
			view = mInflater.inflate(R.layout.list, null);
		}
		if (hideDivider) {
			View divider = view.findViewById(R.id.Divider);
			divider.setVisibility(View.GONE);
		}
		return view;
	}
	
	

	public boolean populateTagDescription(LinearLayout mainpane, String descripton, int fontSize, ColourScheme colourScheme) {
		// TODO move to the layout file
		TextView descriptionView = new TextView(context);
		descriptionView.setId(R.id.Description);
		descriptionView.setText(descripton);
		descriptionView.setPadding(2, 3, 2, 15);
		mainpane.addView(descriptionView, 0);
		descriptionView.setTextSize(TypedValue.COMPLEX_UNIT_PT, fontSize);	// TODO duplicated setting code
		descriptionView.setLineSpacing(new Float(0), new Float(1.1));
		
		descriptionView.setTextColor(colourScheme.getBodytext());
		descriptionView.setPadding(2, 3, 2, 3);	
		return true;
	}
	
	
	public void populateTrailImage(final String url, View trailView, boolean isMainImage) {
		if (imageDAO.isAvailableLocally(url)) {
			ImageView trialImage = (ImageView) trailView.findViewById(R.id.TrailImage);			
			Bitmap image = imageDAO.getImage(url);
			if (image != null) {
				if (isMainImage) {
					trialImage.setImageBitmap(imageStretchingService.stretchImageToFillView(image, trailView));
				}
				trialImage.setImageBitmap(image);
				trialImage.setVisibility(View.VISIBLE);
			}
		}
	}
	
	
	public void populateRefinementType(LinearLayout mainpane, LayoutInflater inflater, String description, List<ArticleSet> refinementArticleSets, ColourScheme colourScheme) {
		View refinementsHeadingView = inflater.inflate(R.layout.refinements, null);			
		TextView descriptionView = (TextView) refinementsHeadingView.findViewById(R.id.RefinementsDescription);
		descriptionView.setText(description);
		descriptionView.setTextColor(colourScheme.getBodytext());
		descriptionView.setPadding(2, 3, 2, 3);
		mainpane.addView(refinementsHeadingView);
		
		// TODO move to a layout
		LinearLayout tagGroup = new LinearLayout(context);
		tagGroup.setOrientation(LinearLayout.VERTICAL);
		tagGroup.setPadding(2, 0, 2, 0);
		
		tagListPopulatingService.populateTags(inflater, true, tagGroup, refinementArticleSets, colourScheme);
		mainpane.addView(tagGroup);
	}
	
	
	
	public void addSeperator(LayoutInflater mInflater, LinearLayout mainpane, ArticleSet articleSetForSection, boolean contentIsAvailable, ColourScheme colourScheme, int fontSize) {
		View seperator = mInflater.inflate(R.layout.seperator, null);
			
		final Section section = ((SectionArticleSet) articleSetForSection).getSection();
		seperator.setBackgroundColor(Color.parseColor(SectionColourMap.getColourForSection(section.getId())));		

		TextView heading = (TextView) seperator.findViewById(R.id.TagName);
		heading.setText(section.getName());
		heading.setTextSize(TypedValue.COMPLEX_UNIT_PT, fontSize);
			
		ClickerPopulatingService.populateTagClicker(articleSetForSection, seperator, contentIsAvailable, colourScheme.getAvailableTagOnSeperator(), colourScheme.getUnavailableTagOnSeperator());
		mainpane.addView(seperator);		
	}
	
}
