/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.RenderListener;
import org.opengis.feature.simple.SimpleFeature;

import com.lowagie.text.pdf.ByteBuffer;
import com.lowagie.text.pdf.PdfGraphics2D;

/**
 * Attaches itself to the renderer and ensures no more than maxSize bytes are used to
 * store the PDF in memory, and stops the renderer in case that happens.
 * @author Andrea Aime - OpenGeo
 */
public class PDFMaxSizeEnforcer {

    long maxSize;
    
    ByteBuffer pdfBytes;

    /**
     * Builds a new max errors enforcer. If maxErrors is not positive the enforcer will do nothing
     * 
     * @param renderer
     * @param maxErrors
     */
    public PDFMaxSizeEnforcer(final GTRenderer renderer, final PdfGraphics2D graphics, final int maxSize) {
        this.maxSize = maxSize;
        this.pdfBytes = graphics.getContent().getInternalBuffer();

        if (maxSize > 0) {
            renderer.addRenderListener(new RenderListener() {

                public void featureRenderer(SimpleFeature feature) {
                    if(pdfBytes.size() >  maxSize) {
                        renderer.stopRendering();
                    }
                }

                public void errorOccurred(Exception e) {
                }
            });
        }
    }

    /**
     * True if the memory used by the PDF buffer exceeds the max memory settings
     * @return
     */
    public boolean exceedsMaxSize() {
        return maxSize > 0 && pdfBytes.size() >  maxSize;
    }
    
    /**
     * Returns the amount of memory currently used by the 
     * @return
     */
    public long memoryUsed() {
        return pdfBytes.size();
    }

}
