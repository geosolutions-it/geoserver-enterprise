/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.turbojpeg;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.geotools.image.ImageWorker;
import org.geotools.test.TestData;

/**
 * Testing directly the {@link TurboJpegImageWorker}.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
public class TurboImageWorkerTest extends TestCase {

    static final String ERROR_LIB_MESSAGE = "The TurboJpeg native library hasn't been loaded: Skipping test";

    static boolean SKIP_TESTS = false;

    static final Logger LOGGER = Logger.getLogger(TurboImageWorkerTest.class.toString());

    static{
        SKIP_TESTS = !TurboJpegUtilities.isTurboJpegAvailable();
    }

    public void testErrors() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // test-data
        final File input = TestData.file(this, "testmergb.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        try {
            new TurboJpegImageWorker(ImageIO.read(input)).writeTurboJPEG(new FileOutputStream(
                    output), 1.5f);
            assertFalse("We should not be allowed to specify compression ratios > 1", true);
        } catch (Exception e) {
            // TODO: handle exception
        }

        try {
            new TurboJpegImageWorker(ImageIO.read(input)).writeTurboJPEG(new FileOutputStream(
                    output), -.5f);
            assertFalse("We should not be allowed to specify compression ratios > 1", true);
        } catch (Exception e) {
            // TODO: handle exception
        }

    }
 
    public void testWriter() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // test-data
        final File input = TestData.file(this, "testmergb.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        new TurboJpegImageWorker(ImageIO.read(input)).writeTurboJPEG(new FileOutputStream(output),.5f);
        assertTrue("Unable to create output file", output.exists() && output.isFile());

        new ImageWorker(output).getBufferedImage().flush();

    }
    
    public void testWriterBandSelect() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // test-data
        final File input = TestData.file(this, "testmergba.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        new TurboJpegImageWorker(ImageIO.read(input)).writeTurboJPEG(new FileOutputStream(output),.5f);
        assertTrue("Unable to create output file", output.exists() && output.isFile());

        new ImageWorker(output).getBufferedImage().flush();

    }
}
