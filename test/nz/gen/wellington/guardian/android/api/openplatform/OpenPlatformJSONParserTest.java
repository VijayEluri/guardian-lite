package nz.gen.wellington.guardian.android.api.openplatform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import junit.framework.TestCase;

import nz.gen.wellington.guardian.android.api.openplatfrom.OpenPlatformJSONParser;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.Section;

import org.json.JSONObject;
import org.junit.Test;

public class OpenPlatformJSONParserTest extends TestCase {

	OpenPlatformJSONParser parser = new OpenPlatformJSONParser();
	
	@Test
	public void testCanParseSectionContentResults() throws Exception {
		final String jsonString = loadContent("open-platform/science.json").toString();		
		List<Article> articles = parser.parseArticlesJSON(jsonString);		
		assertEquals(10, articles.size());
		
		Article first = articles.get(0);
		assertEquals("science/2010/may/24/women-domestic-violence", first.getId());
		assertEquals("Why do so many women put up with domestic violence?", first.getTitle());
		assertEquals("<strong>Carole Jahme</strong> shines the cold light of evolutionary psychology on readers' problems. This week: domestic violence", first.getStandfirst());
		assertEquals("http://static.guim.co.uk/sys-images/Society/Pix/pictures/2008/10/28/domesticviolencetrail.jpg", first.getThumbnailUrl());
		assertEquals(1, first.getAuthors().size());
	}
		
	
	public void testCanParseSingleItemForMediaElement() throws Exception {
		final String jsonString = loadContent("open-platform/article_with_media_elements.json").toString();
		
		String mainPictureUrl = parser.parseArticleJSONForMainPictureUrl(jsonString);	
		assertEquals("http://static.guim.co.uk/sys-images/Guardian/Pix/pictures/2010/6/1/1275408861105/Charles-Dickens-005.jpg", mainPictureUrl);
	}
	
	
	@Test
	public void testCanParseSectionsJSON() throws Exception {
		final String jsonString = loadContent("open-platform/sections.json").toString();		
		List<Section> sections = parser.parseSectionsJSON(jsonString);
		assertEquals(38, sections.size());
	}
	
	@Test
	public void testCanSeeValidStatus() throws Exception {
		final String jsonString = loadContent("open-platform/sections.json").toString();
		JSONObject json  = new JSONObject(jsonString);
		assertTrue(parser.isResponseOk(json));		
	}
	
	private StringBuffer loadContent(String filename) throws IOException {
		StringBuffer content = new StringBuffer();
		File contentFile = new File(ClassLoader.getSystemClassLoader().getResource(filename).getFile());
		Reader freader = new FileReader(contentFile);
		BufferedReader in = new BufferedReader(freader);
		String str;
		while ((str = in.readLine()) != null) {
			content.append(str);
			content.append("\n");
		}
		in.close();
		freader.close();
		return content;
	}
	
}
