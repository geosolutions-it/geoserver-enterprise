/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.geoserver.wms.WMSMapContent;
import org.geotools.util.SoftValueHashMap;
import org.vfny.geoserver.global.GeoserverDataDirectory;

public class WatermarkDecoration implements MapDecoration {
    /** A logger for this class. */
    private static final Logger LOGGER = Logger.getLogger("org.geoserver.wms.responses");

    public static final Color TRANSPARENT = new Color(255, 255, 255, 0);

    private static final int TRANSPARENT_CODE = (255 << 16) | (255 << 8) | 255;

    private String imageURL;

    private float opacity = 1.0f;

    /**
     * Transient cache to avoid reloading the same file over and over
     */
    private static final Map<URL, LogoCacheEntry> logoCache =
        new SoftValueHashMap<URL, LogoCacheEntry>();

    public void loadOptions(Map<String, String> options){
        this.imageURL = options.get("url");

        if (options.containsKey("opacity")) {
            try {
                opacity = Float.valueOf(options.get("opacity")) / 100f;
                opacity = Math.max(Math.min(opacity, 1f), 0f);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Invalid opacity value: " + options.get("opacity"), e);
            }
        }
    }

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent){
        try{
            BufferedImage logo = getLogo(mapContent);
            return new Dimension(logo.getWidth(), logo.getHeight());
        } catch (Exception e) {
            return new Dimension(20, 20);
        }
    }

    /**
     * Print the WaterMarks into the graphic2D.
     * 
     * @param g2D
     * @param paintArea
     * @throws IOException
     * @throws ClassCastException
     * @throws MalformedURLException
     */
    public void paint(Graphics2D g2D, Rectangle paintArea, WMSMapContent mapContent) 
    throws MalformedURLException, ClassCastException, IOException {
        BufferedImage logo = getLogo(mapContent);

        if (logo != null) {
            Composite oldComposite = g2D.getComposite();
            g2D.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity)
            );

            AffineTransform tx = 
                AffineTransform.getTranslateInstance(paintArea.getX(), paintArea.getY());

            tx.scale(
                paintArea.getWidth() / logo.getWidth(),
                paintArea.getHeight() / logo.getHeight()
            );

            g2D.drawImage(logo, tx, null);

            g2D.setComposite(oldComposite);
        }
    }

    protected BufferedImage getLogo(WMSMapContent mapContent) throws IOException {
        BufferedImage logo = null;

        // fully resolve the url (consider data dir)
        URL url = null;

        try {
            url = new URL(GeoserverDataDirectory.getGeoserverDataDirectory().toURL(), imageURL);

            if (url.getProtocol() == null || url.getProtocol().equals("file")) {
                File file = GeoserverDataDirectory.findDataFile(imageURL);
                if (file.exists())
                    url = file.toURL();
            }
        } catch (MalformedURLException e) {
            url = null;
        }

        if (url == null)
            return null;

        LogoCacheEntry entry = logoCache.get(url);
        if (entry == null || entry.isExpired()) {
            logo = ImageIO.read(url);
            if (url.getProtocol().equals("file")) {
                entry = new LogoCacheEntry(logo, new File(url.getFile()));
                logoCache.put(url, entry);
            }
        } else {
            logo = entry.getLogo();
        }

        return logo;
    }

    /**
     * Contains an already loaded logo and the tools to check it's up to date
     * compared to the file system
     * 
     * @author Andrea Aime - TOPP
     * 
     */
    private static class LogoCacheEntry {
        private BufferedImage logo;

        private long lastModified;

        private File file;

        public LogoCacheEntry(BufferedImage logo, File file) {
            this.logo = logo;
            this.file = file;
            lastModified = file.lastModified();
        }

        public boolean isExpired() {
            return file.lastModified() > lastModified;
        }

        public BufferedImage getLogo() {
            return logo;
        }
    }
}
