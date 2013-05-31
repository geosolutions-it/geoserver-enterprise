package org.geoserver.importer;

import java.io.File;
import java.io.Serializable;

/**
 * Configurees the coverage importer in terms of inputs, outputs and raster
 * processing steps
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CoverageImportConfiguration implements Serializable {

	private static final long serialVersionUID = -2602690137275636513L;

	private File imageFile;

	private File outputDirectory;

	private String workspace;

	private boolean copy;

	private TilingConfiguration tiling = new TilingConfiguration();

	private OverviewConfiguration overview = new OverviewConfiguration();

	private CompressionConfiguration compression = new CompressionConfiguration();

	public File getImageFile() {
		return imageFile;
	}

	public void setImageFile(File imageFile) {
		this.imageFile = imageFile;
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String wsName) {
		this.workspace = wsName;
	}

	public boolean isCopy() {
		return copy;
	}

	public void setCopy(boolean copy) {
		this.copy = copy;
	}

	public TilingConfiguration getTiling() {
		return tiling;
	}

	public void setTiling(TilingConfiguration tiling) {
		this.tiling = tiling;
	}

	public OverviewConfiguration getOverview() {
		return overview;
	}

	public void setOverview(OverviewConfiguration overview) {
		this.overview = overview;
	}

	public CompressionConfiguration getCompression() {
		return compression;
	}

	public void setCompression(CompressionConfiguration compression) {
		this.compression = compression;
	}

	@Override
	public String toString() {
		return "CoverageImportConfiguration [imageFile=" + imageFile
				+ ", outputDirectory=" + outputDirectory + ", wsName="
				+ workspace + ", copy=" + copy + ", tiling=" + tiling
				+ ", overview=" + overview + ", compression=" + compression
				+ "]";
	}

}
