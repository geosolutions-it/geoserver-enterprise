/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geotools.renderer.style.FontCache;
import org.geotools.renderer.style.ImageGraphicFactory;
import org.geotools.renderer.style.SVGGraphicFactory;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Drops imaging caches
 * 
 * @author Andrea Aime - OpenGeo
 */
public class WMSLifecycleHandler implements GeoServerLifecycleHandler, ApplicationListener {

    static final Logger LOGGER = Logging.getLogger(WMSLifecycleHandler.class);

    GeoServerDataDirectory data;
    WMS wmsConfig;

    public WMSLifecycleHandler(GeoServerDataDirectory data, WMS wmsConfig) {
        this.data = data;
        this.wmsConfig = wmsConfig;
    }

    public void onDispose() {
        // dispose the WMS Animator Executor Service
        shutdownAnimatorExecutorService();
    }

    public void onReload() {
        // clear the caches for good measure
        onReset();
    }

    public void onReset() {
        // kill the image caches
        ImageGraphicFactory.resetCache();
        SVGGraphicFactory.resetCache();

        // reloads the font cache
        reloadFontCache();
        
        // reset WMS Animator Executor Service
        resetAnimatorExecutorService();
    }

    /**
     * Shutting down pending tasks and resetting the executor service
     * timeout.
     */
    private void resetAnimatorExecutorService() {
        shutdownAnimatorExecutorService();

        Long framesTimeout = this.wmsConfig.getMaxAnimatorRenderingTime() != null ? 
                this.wmsConfig.getMaxAnimatorRenderingTime() : Long.MAX_VALUE;
        ExecutorService animatorExecutorService = 
            new ThreadPoolExecutor(4, 20, framesTimeout, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

        this.wmsConfig.setAnimatorExecutorService(animatorExecutorService);
    }

    /**
     * Suddenly shuts down the Animator Executor Service
     */
    private void shutdownAnimatorExecutorService() {
        final ExecutorService animatorExecutorService = this.wmsConfig.getAnimatorExecutorService();
        if (animatorExecutorService != null &&
                !animatorExecutorService.isShutdown()) {
            animatorExecutorService.shutdownNow();
        }
    }

    void reloadFontCache() {
        List<Font> fonts = loadFontsFromDataDirectory();
        final FontCache cache = FontCache.getDefaultInstance();
        cache.resetCache();
        for (Font font : fonts) {
            cache.registerFont(font);
        }
    }

    List<Font> loadFontsFromDataDirectory() {
        List<Font> result = new ArrayList<Font>();
        try {
            final File findStyleDir = data.findStyleDir();
            if(findStyleDir==null||!findStyleDir.exists()){
                return Collections.emptyList();
            }
            Collection<File> files = FileUtils.listFiles(findStyleDir, new String[] { "ttf",
                    "TTF" }, true);
            for (File file : files) {
                try {
                    final Font font = Font.createFont(Font.TRUETYPE_FONT, file);
                    result.add(font);
                    LOGGER.log(Level.INFO,
                            "Loaded font file " + file + ", loaded font '" + font.getName()
                                    + "' in family '" + font.getFamily() + "'");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load font file " + file, e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to scan style directory for fonts", e);
        }

        return result;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            reloadFontCache();
            
            // reset WMS Animator Executor Service
            resetAnimatorExecutorService();
        }
    }

}
