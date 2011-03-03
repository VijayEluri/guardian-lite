/*	Guardian Lite - an Android reader for the Guardian newspaper.
 *	Copyright (C) 2011  Eel Pie Consulting Limited
 *
 *	This program is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.	*/

package nz.gen.wellington.guardian.android.model;

import java.io.Serializable;
import java.util.List;

import nz.gen.wellington.guardian.model.Section;

public interface ArticleSet extends Serializable {
	
	public String getName();	
	public String getShortName();
	public List<String> getPermittedRefinements();	
	public int getPageSize();
	public boolean getShowMedia();
	public void setShowMedia(boolean showMedia);	
	public String getSourceUrl();
	void setSourceUrl(String urlForArticleSet);
	boolean isEmpty();
	boolean isFeatureTrailAllowed();
	public Section getSection();
	public int getCount();
	
}
