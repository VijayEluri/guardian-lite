package nz.gen.wellington.guardian.android.model;

import android.graphics.Color;

public class WhiteOnBlackColourScheme extends ColourScheme {
	
	@Override
	public Integer getBackground() {
		return Color.BLACK;
	}

	@Override
	public Integer getBodytext() {
		return Color.LTGRAY;
	}

	@Override
	public Integer getHeadline() {
		return Color.WHITE;
	}
	
	@Override
	public Integer getAvailableTag() {
		return Color.WHITE;
	}

	@Override
	public Integer getUnavailableTag() {
		return Color.DKGRAY;
	}
	
	@Override
	public Integer getStatus() {
		return Color.LTGRAY;
	}
	
}
