/**
 *
 */
package org.geoserver.web.importer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.web.GeoServerSecuredPage;


/**
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 *
 *
 */
public class ImporterSecuredPage extends GeoServerSecuredPage
{

    /**
     * Default constructor
     */
    public ImporterSecuredPage()
    {
    }

    /**
     * Returns a default output data directory
     *
     * @throws IOException
     */
    protected String buildOutputDirectory(final String directory)
    {
        String outDirectory = "";
        try
        {
            GeoServerDataDirectory dd = getGeoServerApplication().getBeanOfType(
                    GeoServerDataDirectory.class);
            outDirectory = dd.findDataDir(directory).getAbsoluteFile().getAbsolutePath();
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "Error while setting up output directory", e);
        }

        return outDirectory;
    }

    /**
     * Checks the existence of a directory corresponding to the value of validatable
     */
    protected class OutDirectoryValidator extends AbstractValidator
    {

        @Override
        protected void onValidate(IValidatable validatable)
        {

            String directory = (String) validatable.getValue();
            try
            {
                File file = new File(directory);
                if ((!file.exists() && !file.mkdirs()) || !file.isDirectory())
                {
                    error(validatable, "ImporterSecuredPage.invalidPath");
                }
                if (!file.canRead() || !file.canWrite())
                {
                    error(validatable, "ImporterSecuredPage.notEnoughPermission");
                }
            }
            catch (Exception e)
            {
                if (LOGGER.isLoggable(Level.WARNING))
                {
                    LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
                error(validatable, "ImporterSecuredPage.noData");
            }
        }
    }

    /**
     * Checks the existence of a directory corresponding to the value of validatable and the
     * presence of GeoTIFFs in there
     */
    protected class DirectoryValidator extends AbstractValidator
    {

        @Override
        protected void onValidate(IValidatable validatable)
        {

            String directory = (String) validatable.getValue();
            try
            {
                File file = new File(directory);
                if (!file.canRead())
                {
                    error(validatable, "ImporterSecuredPage.invalidPath");
                }

                if (file.isDirectory() && (file.listFiles((FilenameFilter) GeoTIFFPage.INTERNAL_FILTER).length == 0))
                {
                    error(validatable, "ImporterSecuredPage.noData");
                }

            }
            catch (Exception e)
            {
                if (LOGGER.isLoggable(Level.WARNING))
                {
                    LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
                error(validatable, "ImporterSecuredPage.noData");
            }
        }
    }

    protected final class ProjectValidator extends AbstractValidator
    {

        @Override
        protected void onValidate(IValidatable validatable)
        {
            String project = (String) validatable.getValue();

            // new workspace? if so, good
            WorkspaceInfo ws = getCatalog().getWorkspaceByName(project);
            if (ws == null)
            {
                return;
            }

            // new store too?
            StoreInfo store = getCatalog().getStoreByName(ws, project, StoreInfo.class);
            if (store != null)
            {
                error(validatable, "GeoTIFFPage.duplicateStore", Collections.singletonMap(
                        "project", project));
            }
        }

    }

}
