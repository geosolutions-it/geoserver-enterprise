package org.geoserver.importer;

import java.io.Serializable;

/**
 * 
 * @author aaime
 *
 */
public class CompressionConfiguration implements Serializable {

	private static final long serialVersionUID = 3988735102665141160L;

	private String type;

	private double ratio;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getRatio() {
		return ratio;
	}

	public void setRatio(double ratio) {
		this.ratio = ratio;
	}

	public boolean isEnabled() {
		return this.type != null && !this.type.equalsIgnoreCase("NONE");
	}

	@Override
	public String toString() {
		return "Compression [type=" + type + ", ratio=" + ratio + "]";
	}

}
