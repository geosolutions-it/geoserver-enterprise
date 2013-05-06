package org.geoserver.importer;

public class TilingConfiguration {

	private boolean enabled;

	private int tileWidth;

	private int tileHeight;

	private boolean retainNativeTiles = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean tile) {
		this.enabled = tile;
	}

	public int getTileWidth() {
		return tileWidth;
	}

	public void setTileWidth(int tilewidth) {
		this.tileWidth = tilewidth;
	}

	public int getTileHeight() {
		return tileHeight;
	}

	public void setTileHeight(int tileheight) {
		this.tileHeight = tileheight;
	}

	public boolean isRetainNativeTiles() {
		return retainNativeTiles;
	}

	public void setRetainNativeTiles(boolean retainTile) {
		this.retainNativeTiles = retainTile;
	}
	
	@Override
	public String toString() {
		return "TilingConfiguration [enabled=" + enabled + ", tileWidth="
				+ tileWidth + ", tileHeight=" + tileHeight
				+ ", retainNativeTiles=" + retainNativeTiles + "]";
	}

}