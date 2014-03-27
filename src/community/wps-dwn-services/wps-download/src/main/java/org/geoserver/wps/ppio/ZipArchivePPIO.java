/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.data.util.IOUtils;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;

/**
 * Handles input and output of feature collections as zipped files.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class ZipArchivePPIO extends BinaryPPIO {

    private final static Logger LOGGER = Logging.getLogger(ZipArchivePPIO.class);

    /** The catalog. */
    private Catalog catalog;

    private int compressionLevel = ZipOutputStream.STORED;

    /**
     * Instantiates a new zip archive ppio.
     * 
     * @param geoServer the geo server
     * @param resources the resources
     */
    public ZipArchivePPIO(GeoServer geoServer, int compressionLevel) {
        super(File.class, File.class, "application/zip");
        Utilities.ensureNonNull("geoserver", geoServer);
        if (compressionLevel < ZipOutputStream.STORED
                || compressionLevel > ZipOutputStream.DEFLATED) {
            throw new IllegalArgumentException("Invalid Compression Level: " + compressionLevel);
        }
        this.catalog = geoServer.getCatalog();
        this.compressionLevel = compressionLevel;
    }

    /**
     * Encode.
     * 
     * @param output the output
     * @param os the os
     * @throws Exception the exception
     */
    @Override
    public void encode(final Object output, OutputStream os) throws Exception {
        // avoid double zipping
        if (output instanceof File && isZpFile((File) output)) {
            FileUtils.copyFile((File) output, os);
            return;
        }

        ZipOutputStream zipout = new ZipOutputStream(os);
        zipout.setLevel(compressionLevel);

        // directory
        if (output instanceof File) {
            final File file = ((File) output);
            if (file.isDirectory()) {
                IOUtils.zipDirectory(file, zipout, FileFilterUtils.trueFileFilter());
            } else {
                // check if is a zip file already
                zipFile(file, zipout);
            }
        } else {
            // list of files
            if (output instanceof Collection) {
                // create temp dir
                final Collection collection = (Collection) output;
                for (Object obj : collection) {
                    if (obj instanceof File) {
                        // convert to file and add to zip
                        final File file = ((File) obj);
                        if (file.isDirectory()) {
                            IOUtils.zipDirectory(file, zipout, FileFilterUtils.trueFileFilter());
                        } else {
                            // check if is a zip file already
                            zipFile(file, zipout);
                        }
                    } else {
                        LOGGER.info("Skipping object -->" + obj.toString());
                    }
                }
            } else {
                // error
                throw new IllegalArgumentException("Unable to zip provided output. Output-->"
                        + output != null ? output.getClass().getCanonicalName() : "null");
            }
        }
        zipout.finish();
    }

    /**
     * Decode.
     * 
     * @param input the input
     * @return the object
     * @throws Exception the exception TODO review
     */
    @Override
    public Object decode(InputStream input) throws Exception {
        // create the temp directory and register it as a temporary resource
        final File directory = catalog.getResourceLoader().findOrCreateDirectory("downloads");

        // unzip to the temporary directory
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(input);
            ZipEntry entry = null;

            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(directory, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdir();
                } else {
                    int count;
                    byte data[] = new byte[4096];
                    // write the files to the disk
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file);
                        while ((count = zis.read(data)) != -1) {
                            fos.write(data, 0, count);
                        }
                        fos.flush();
                    } finally {
                        if (fos != null) {
                            fos.close();
                        }
                    }
                }
                zis.closeEntry();
            }
        } finally {
            if (zis != null) {
                zis.close();
            }
        }

        return directory;
    }

    /**
     * Gets the file extension.
     * 
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return "zip";
    }

    /**
     * This method zip the provided file to the provided {@link ZipOutputStream}.
     * 
     * <p>
     * It throws {@link IllegalArgumentException} in case the provided file does not exists or is not a readable file.
     * 
     * @param file the {@link File} to zip
     * @param zipout the {@link ZipOutputStream} to write to
     * @throws IOException in case something bad happen
     */
    public static void zipFile(File file, ZipOutputStream zipout) throws IOException {
        // copy file by reading 4k at a time (faster than buffered reading)
        byte[] buffer = new byte[4096];
        zipFileInternal(file, zipout, buffer);

    }

    /**
     * This method tells us if the provided {@link File} is a Zip File.
     * 
     * <p>
     * It throws {@link IllegalArgumentException} in case the provided file does not exists or is not a readable file.
     * 
     * @param file the {@link File} to check for zip
     * @throws IOException in case something bad happen
     */
    public static boolean isZpFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new IllegalArgumentException(
                    "Provided File is not valid and/or reqadable! --> File:" + file != null ? file
                            .getAbsolutePath() : "null");
        }

        if (file.isDirectory()) {
            return false;
        }
        if (file.length() < 4) {
            return false;
        }
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(file));

            int test = in.readInt();
            return test == 0x504b0304;
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
            }
            return false;
        } finally {
            if (in != null) {
                org.apache.commons.io.IOUtils.closeQuietly(in);
            }
        }

    }

    /**
     * This method zip the provided file to the provided {@link ZipOutputStream}.
     * 
     * <p>
     * It throws {@link IllegalArgumentException} in case the provided file does not exists or is not a readable file.
     * 
     * @param file the {@link File} to zip
     * @param zipout the {@link ZipOutputStream} to write to
     * @param buffer the buffer to use for reading/writing
     * @throws IOException in case something bad happen
     */
    private static void zipFileInternal(File file, ZipOutputStream zipout, byte[] buffer)
            throws IOException {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new IllegalArgumentException(
                    "Provided File is not valid and/or reqadable! --> File:" + file != null ? file
                            .getAbsolutePath() : "null");
        }

        final ZipEntry entry = new ZipEntry(FilenameUtils.getName(file.getAbsolutePath()));
        zipout.putNextEntry(entry);

        // copy over the file
        InputStream in = null;
        try {
            int c;
            in = new FileInputStream(file);
            while (-1 != (c = in.read(buffer))) {
                zipout.write(buffer, 0, c);
            }
            zipout.closeEntry();
        } finally {
            // close the input stream
            if (in != null) {
                org.apache.commons.io.IOUtils.closeQuietly(in);
            }
        }
        zipout.flush();
    }
}
