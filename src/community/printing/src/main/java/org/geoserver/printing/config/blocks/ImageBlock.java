/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.geoserver.printing.config.blocks;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.wicket.util.file.File;
import org.mapfish.print.ChunkDrawer;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.config.layout.Block;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;

/**
 * Configuration and logic to add an !image block.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Imageblock
 */
public class ImageBlock extends Block {
    private String url = null;
    private double maxWidth = 0.0;
    private double maxHeight = 0.0;
    private String rotation = "0";

    public void render(PJsonObject params, PdfElement target, RenderingContext context) throws DocumentException {
        final URI url;
        try {
            final String urlTxt = PDFUtils.evalString(context, params, this.url);
            if (urlTxt.startsWith("file:/") && !urlTxt.startsWith("file:///")) {
            	url = new File(urlTxt.substring("file:/".length())).toURI();
            	
            	if (!url.getPath().endsWith(".svg")) {
            		final Image image = getImage(context, url, (float) maxWidth, (float) maxHeight);
            		
            		if (getRotationRadian(context, params) != 0.0F) {
                        image.setRotation(getRotationRadian(context, params));
                    }
            		
            		target.add(new Chunk(image, 0f, 0f, true));
            		return;
            	}
            } else {
            	url = new URI(urlTxt);
            }
        } catch (URISyntaxException e) {
            throw new InvalidValueException("url", this.url, e);
        } catch (MalformedURLException e) {
            throw new InvalidValueException("url", this.url, e);
		} catch (IOException e) {
            throw new InvalidValueException("url", this.url, e);
		}
        if (url.getPath().endsWith(".svg")) {
            drawSVG(context, params, target, url);
        } else {
            target.add(PDFUtils.createImageChunk(context, maxWidth, maxHeight, url, getRotationRadian(context, params)));
        }
    }

    private float getRotationRadian(RenderingContext context, PJsonObject params) {
        return (float) (Float.parseFloat(PDFUtils.evalString(context, params, this.rotation)) * Math.PI / 180.0F);
    }

    private void drawSVG(RenderingContext context, PJsonObject params, PdfElement paragraph, URI url) throws DocumentException {
        final TranscoderInput ti = new TranscoderInput(url.toString());
        final PrintTranscoder pt = new PrintTranscoder();
        pt.addTranscodingHint(PrintTranscoder.KEY_SCALE_TO_PAGE, Boolean.TRUE);
        pt.transcode(ti, null);

        final Paper paper = new Paper();
        paper.setSize(maxWidth, maxHeight);
        paper.setImageableArea(0, 0, maxWidth, maxHeight);
        final float rotation = getRotationRadian(context, params);

        final PageFormat pf = new PageFormat();
        pf.setPaper(paper);

        final SvgDrawer drawer = new SvgDrawer(context.getCustomBlocks(), rotation, pt, pf);

        //register a drawer that will do the job once the position of the map is known
        paragraph.add(PDFUtils.createPlaceholderTable(maxWidth, maxHeight, spacingAfter, drawer, align, context.getCustomBlocks()));
    }

    /**
     * Gets an iText image with a cache that uses PdfTemplates to re-use the same
     * bitmap content multiple times in order to reduce the file size.
     */
    private static Image getImage(RenderingContext context, URI uri, float w, float h) throws IOException, DocumentException {
        //Check the image is not already used in the PDF file.
        //
        //This part is not protected against multi-threads... worst case, a single image can
        //be twice in the PDF, if used more than one time. But since only one !map
        //block is dealed with at a time, this should not happen
        Map<URI, PdfTemplate> cache = context.getTemplateCache();
        PdfTemplate template = cache.get(uri);
        if (template == null) {
            Image content = Image.getInstance(uri.toString());
            content.setAbsolutePosition(0, 0);
            final PdfContentByte dc = context.getDirectContent();
            synchronized (context.getPdfLock()) {  //protect against parallel writing on the PDF file
                template = dc.createTemplate(content.getPlainWidth(), content.getPlainHeight());
                template.addImage(content);
            }
            cache.put(uri, template);
        }


        //fix the size/aspect ratio of the image in function of what is specified by the user
        if (w == 0.0f) {
            if (h == 0.0f) {
                w = template.getWidth();
                h = template.getHeight();
            } else {
                w = h / template.getHeight() * template.getWidth();
            }
        } else {
            if (h == 0.0f) {
                h = w / template.getWidth() * template.getHeight();
            }
        }

        final Image result = Image.getInstance(template);
        result.scaleToFit(w, h);
        return result;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }

    public void setMaxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
        if (maxWidth < 0.0) throw new InvalidValueException("maxWidth", maxWidth);
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
        if (maxHeight < 0.0) throw new InvalidValueException("maxHeight", maxHeight);
    }

    public void setRotation(String rotation) {
        this.rotation = rotation;
    }

    private class SvgDrawer extends ChunkDrawer {
        private final float rotation;
        private final PrintTranscoder pt;
        private final PageFormat pf;

        public SvgDrawer(PDFCustomBlocks customBlocks, float rotation, PrintTranscoder pt, PageFormat pf) {
            super(customBlocks);
            this.rotation = rotation;
            this.pt = pt;
            this.pf = pf;
        }

        public void renderImpl(Rectangle rectangle, PdfContentByte dc) {
            dc.saveState();
            Graphics2D g2 = null;
            try {
                final AffineTransform t = AffineTransform.getTranslateInstance(rectangle.getLeft(), rectangle.getBottom());
                if (rotation != 0.0F) {
                    t.rotate(rotation, maxWidth / 2.0, maxHeight / 2.0);
                }
                dc.transform(t);
                g2 = dc.createGraphics((float) maxWidth, (float) maxHeight);

                //avoid a warning from Batik
                System.setProperty("org.apache.batik.warn_destination", "false");
                g2.setRenderingHint(RenderingHintsKeyExt.KEY_TRANSCODING, RenderingHintsKeyExt.VALUE_TRANSCODING_PRINTING);
                g2.setRenderingHint(RenderingHintsKeyExt.KEY_AVOID_TILE_PAINTING, RenderingHintsKeyExt.VALUE_AVOID_TILE_PAINTING_ON);

                pt.print(g2, pf, 0);
            } finally {
                if (g2 != null) {
                    g2.dispose();
                }
                dc.restoreState();
            }
        }

    }

    @Override
    public void validate() {
        super.validate();
        if (url == null) throw new InvalidValueException("url", "null");
    }
}