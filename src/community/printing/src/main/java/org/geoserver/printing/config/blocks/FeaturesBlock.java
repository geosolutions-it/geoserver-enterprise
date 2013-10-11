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

import org.apache.log4j.Logger;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.config.layout.Block;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Bean to configure a !legends block.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Legendsblock
 */
public abstract class FeaturesBlock extends Block {
    public static final Logger LOGGER = Logger.getLogger(FeaturesBlock.class);

    private double layerSpace = 5;
    private String layerFont = "Helvetica";
    protected double layerFontSize = 10;
    private String classFont = "Helvetica";
    protected double classFontSize = 8;
    private String fontEncoding = BaseFont.WINANSI;

    public void render(PJsonObject params, PdfElement target, RenderingContext context) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);

        Font layerPdfFont = getLayerPdfFont();
        Font classPdfFont = getClassPdfFont();
        
        PJsonArray features = context.getGlobalParams().optJSONArray("features");
        if (features != null && features.size() > 0) {
            for (int i = 0; i < features.size(); ++i) {
            	PJsonObject feature = features.getJSONArray(i).getJSONObject(0);
                final PdfPCell cell = createLine(context, 0.0, feature, layerPdfFont, classPdfFont, params);
                if (i > 0) {
                    cell.setPaddingTop((float) layerSpace);
                }
                table.addCell(cell);
            }
        }
        table.setSpacingAfter((float) spacingAfter);
        target.add(table);
    }

    protected abstract PdfPCell createLine(RenderingContext context, double indent, PJsonObject node, Font layerPdfFont, Font classPdfFont, PJsonObject params) throws DocumentException;

    public void setClassFont(String classFont) {
        this.classFont = classFont;
    }

    public void setClassFontSize(double classFontSize) {
        this.classFontSize = classFontSize;
        if (classFontSize < 0.0) throw new InvalidValueException("classFontSize", classFontSize);
    }

    public String getClassFont() {
        return classFont;
    }

    protected Font getLayerPdfFont() {
        return FontFactory.getFont(layerFont, fontEncoding, (float) layerFontSize);
    }

    protected Font getClassPdfFont() {
        return FontFactory.getFont(classFont, fontEncoding, (float) classFontSize);
    }

    public void setLayerSpace(double layerSpace) {
        this.layerSpace = layerSpace;
        if (layerSpace < 0.0) throw new InvalidValueException("layerSpace", layerSpace);
    }

    public void setLayerFont(String layerFont) {
        this.layerFont = layerFont;
    }

    public void setLayerFontSize(double layerFontSize) {
        this.layerFontSize = layerFontSize;
        if (layerFontSize < 0.0) throw new InvalidValueException("layerFontSize", layerFontSize);
    }

    public void setFontEncoding(String fontEncoding) {
        this.fontEncoding = fontEncoding;
    }
}