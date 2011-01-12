package nz.gen.wellington.guardian.android.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class FavouriteTagsArticleSet extends AbstractArticleSet implements Serializable, ArticleSet {

	private static final long serialVersionUID = 1L;
	
	private List<Section> sections;
	private List<Tag> tags;
	
	private String[] permittedRefinements = {};
	
	public FavouriteTagsArticleSet(List<Section> sections, List<Tag> tags, int pageSize) {
		super(pageSize);
		this.sections = sections;
		this.tags = tags;
	}
	
	@Override
	public String getName() {
		return "Favourites";
	}
	
	@Override
	public List<String> getPermittedRefinements() {
		return Arrays.asList(permittedRefinements);
	}
	
	@Override
	public boolean isEmpty() {
		return sections.isEmpty() && tags.isEmpty();
	}

	public List<Section> getSections() {
		return sections;
	}

	public List<Tag> getTags() {
		return tags;
	}
	
	@Override
	public boolean isFeatureTrailAllowed() {
		return false;
	}
	
}