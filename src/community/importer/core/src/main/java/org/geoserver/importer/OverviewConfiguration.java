package org.geoserver.importer;

import java.io.Serializable;

public class OverviewConfiguration implements Serializable {
	private static final long serialVersionUID = 6313545296794972682L;

	private boolean enabled;

	private int downsampleStep;

	private String subsampleAlgorithm;

	private int numOverviews;

	private boolean retainOverviews = false;

	private boolean externalOverviews = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean overview) {
		this.enabled = overview;
	}

	public int getDownsampleStep() {
		return downsampleStep;
	}

	public void setDownsampleStep(int downsamplestep) {
		this.downsampleStep = downsamplestep;
	}

	public String getSubsampleAlgorithm() {
		return subsampleAlgorithm;
	}

	public void setSubsampleAlgorithm(String subsamplealgorithm) {
		this.subsampleAlgorithm = subsamplealgorithm;
	}

	public int getNumOverviews() {
		return numOverviews;
	}

	public void setNumOverviews(int numOverviews) {
		this.numOverviews = numOverviews;
	}

	public boolean isRetainOverviews() {
		return retainOverviews;
	}

	public void setRetainOverviews(boolean retainOverviews) {
		this.retainOverviews = retainOverviews;
	}

	public boolean isExternalOverviews() {
		return externalOverviews;
	}

	public void setExternalOverviews(boolean extoverview) {
		this.externalOverviews = extoverview;
	}

	@Override
	public String toString() {
		return "OverviewConfiguration [enabled=" + enabled
				+ ", downsampleStep=" + downsampleStep
				+ ", subsampleAlgorithm=" + subsampleAlgorithm
				+ ", numOverviews=" + numOverviews + ", retainOverviews="
				+ retainOverviews + ", externalOverviews=" + externalOverviews
				+ "]";
	}

}