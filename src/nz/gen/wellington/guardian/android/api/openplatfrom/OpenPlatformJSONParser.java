package nz.gen.wellington.guardian.android.api.openplatfrom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nz.gen.wellington.guardian.android.activities.ArticleCallback;
import nz.gen.wellington.guardian.android.api.ArticleBodyCleaner;
import nz.gen.wellington.guardian.android.model.Article;
import nz.gen.wellington.guardian.android.model.Section;
import nz.gen.wellington.guardian.android.model.SectionColourMap;
import nz.gen.wellington.guardian.android.model.Tag;

import org.apache.commons.lang.StringEscapeUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.Log;

public class OpenPlatformJSONParser {
			
	private static final String TAG = "OpenPlatformJSONParser";
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	
	public static final String ARTICLE_AVAILABLE = "nz.gen.wellington.guardian.android.api.ARTICLE_AVAILABLE";

	private boolean running;
	ResultsHandler hb;
	
	public OpenPlatformJSONParser(Context context) {
		running = true;
	}


	public List<Article> parseArticlesXml(InputStream inputStream, List<Section> sections, ArticleCallback articleCallback) {
		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			hb = new ResultsHandler(articleCallback, sections);
			saxParser.parse(inputStream, hb);
			inputStream.close();
			return hb.getArticles();

		} catch (SAXException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} catch (ParserConfigurationException e) {
			Log.e(TAG, e.getMessage());
		}
		return null;
	}
	
	public List<Tag> getRefinements() {
		if (hb != null) {
			return hb.getRefinements();
		}
		return new ArrayList<Tag>();
	}

	

	public String getChecksum() {
		if (hb != null) {
			return hb.getChecksum();
		}
		return null;
	}
	

	 class ResultsHandler extends HandlerBase {
         
		 List<Article> articles;
		 List<Tag> refinements;
         Article article;
         String checksum;
         
         StringBuilder sb = new StringBuilder();
         String currentField;
         String currentRefinementGroupType;
         ArticleCallback articleCallback;
         private List<Section> sections;
         
         public ResultsHandler(ArticleCallback articleCallback, List<Section> sections) {
        	 this.articleCallback =articleCallback;
        	 this.sections = sections;
         }
                  
         public List<Tag> getRefinements() {
			return refinements;
         }
         
         public String getChecksum() {        	 
        	 return checksum;
         }
         

		private Section getSectionById(String sectionId) {
 			for (Section section : sections) {
 				if (section.getId().equals(sectionId)) {
 					return section;
 				}
 			}
 			return null;			
 		}

		public List<Article> getArticles() {
        	 return articles;
		}
		
		
		@Override
		public void startDocument() throws SAXException {
			super.startDocument();
			articles = new LinkedList<Article>();
			refinements = new LinkedList<Tag>();
		}

         @Override
         public void startElement(String name, AttributeList attributes) throws SAXException {        	 
        	 super.startElement(name, attributes);
        	 if (!running) {
        		 Log.i(TAG, "Parser stopping");
        		 throw new SAXException("Parse has been stopped");        		 
        	 }
        	 
        	 if (name.equals("content")) {
        		 article = new Article();
        		 sb = new StringBuilder();
        		 	 
        		 article.setId(attributes.getValue("id"));
        		 
        		 final String sectionId =  attributes.getValue("section-id");
        		 Section section = getSectionById(sectionId);
        		 article.setSection(section);
        		 
        		 
        		 final String dateString = attributes.getValue("web-publication-date");
        		 try {
        			 DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_TIME_FORMAT);
        			 article.setPubDate(fmt.parseDateTime(dateString));
        		 } catch (Exception e) {
        				Log.e(TAG, "Failed to parse date '" + dateString +  "': " + e.getMessage());
        		 }
        	 }
                 
        	 if (name.equals("field")) {                             
        		 String fieldname = attributes.getValue("name");
        		 if (!fieldname.equals(currentField)) {
        			 currentField = fieldname;
        		 }
        	 }
        	 
        	 if (name.equals("results")) {
        		 checksum = attributes.getValue("checksum");
        	 }
        	 
        	 if (name.equals("tag")) {
        		 
        		 if (attributes.getValue("type").equals("keyword")) {
        			 Section tagSection = getSectionById(attributes.getValue("section-id"));        		 
        			 Tag tag = new Tag(attributes.getValue("web-title"),
        				 attributes.getValue("id"), tagSection);        		 
        			 article.addKeyword(tag);
        		 }
        		 
        		 if (attributes.getValue("type").equals("contributor")) {
        			 Tag tag = new Tag(attributes.getValue("web-title"),
            				 attributes.getValue("id"), null);        
        			 article.addAuthor(tag);
        		 }
        	 }
        	 
        	 if (name.equals("refinement-group")) {
        		 currentRefinementGroupType = attributes.getValue("type");
        	 }
        	 
        	 if (name.equals("refinement")) {
        		 if (currentRefinementGroupType != null && currentRefinementGroupType.equals("keyword")) {
        			 final String tagId = attributes.getValue("id");
        			 final String sectionId = tagId.split("/")[0];

        			 Section section = getSectionById(sectionId);
        			 Log.i(TAG, "Found refinement keyword: " + tagId);
        			 refinements.add(new Tag(attributes.getValue("display-name"), tagId, section));        			 
        		 }        		 
        	 }
        	         	 
        	 if (name.equals("asset")) {
        		 if (article.getMainImageUrl() == null && attributes.getValue("type").equals("picture")) {
        			 article.setMainImageUrl(attributes.getValue("file"));        			 
        		 }
        	 }
         }

	
		@Override
		public void endElement(String name) throws SAXException {
			super.endElement(name);

			if (currentField != null) {
				
				if (currentField.equals("headline")) {
					article.setTitle(ArticleBodyCleaner.stripHtml(sb.toString()));
				}
				
				if (currentField.equals("byline")) {
					article.setByline(ArticleBodyCleaner.stripHtml(sb.toString()));
				}
				
				if (currentField.equals("standfirst")) {
					article.setStandfirst(ArticleBodyCleaner.stripHtml(sb.toString()));
				}
				
				if (currentField.equals("thumbnail")) {
					article.setThumbnailUrl(ArticleBodyCleaner.stripHtml(sb.toString()));
				}
				
				if (currentField.equals("body")) {
					article.setDescription(ArticleBodyCleaner.stripHtml(sb.toString()));
				}
				
				if (currentField.equals("caption")) {
					article.setCaption(ArticleBodyCleaner.stripHtml(sb.toString()));
				}
				
				currentField = null;
				sb = new StringBuilder();
			}

			if (name.equals("content")) {
				boolean isArticleValid = article.getSection() != null;
				if (isArticleValid) {
					articles.add(article);
					if (articleCallback !=  null) {
						articleCallback.articleReady(article);
					}
				} else {
					Log.w(TAG, "Invalid article: " + article.getId());
				}
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			if (currentField != null) {
				for (int i = start; i < start + length; i++) {
					sb.append(ch[i]);
				}
			}
		}
		
	 }


	public List<Section> parseSectionsJSON(InputStream input) {
		try {			
			StringBuilder content = new StringBuilder();
			BufferedReader in = new BufferedReader(new InputStreamReader(input));
			String str;
			while ((str = in.readLine()) != null) {
				content.append(str);
				content.append("\n");
			}
			in.close();
			
			JSONObject json = new JSONObject(content.toString());
			if (!isResponseOk(json)) {
				return null;				
			}
			JSONObject response = json.getJSONObject("response");
			JSONArray results = response.getJSONArray("results");
				
			List<Section> sections = new LinkedList<Section>();
			for (int i=0; i < results.length(); i++) {		
				JSONObject section = results.getJSONObject(i);				
				 final String sectionName = StringEscapeUtils.unescapeHtml(section.getString("webTitle"));
				 final String id = section.getString("id");
				 sections.add(new Section(id, sectionName, SectionColourMap.getColourForSection(id)));
			}
			
			return sections;			
			
		} catch (JSONException e) {
			Log.e(TAG, "JSONException while parsing articles: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "IOException while parsing articles: " + e.getMessage());
		}
		return null;
	}
	
	
	public boolean isResponseOk(JSONObject json) {
		try {
			JSONObject response = json.getJSONObject("response");
			String status = response.getString("status");
			return status != null && status.equals("ok");
		} catch (JSONException e) {
			return false;
		}
	}
	
	public String getUserTier(String jsonString) {
		try {
			JSONObject json = new JSONObject(jsonString); // TODO wasteful duplicate parsing
			if (!isResponseOk(json)) {
				return null;				
			}
			JSONObject response = json.getJSONObject("response");
			if (response.has("userTier")) {
				return response.getString("userTier");
			}
			return null;
		} catch (JSONException e) {
			return null;
		}		
	}


	public void stop() {
		running = false;		
	}
	
}
