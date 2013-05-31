/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.feature.FeatureTypes;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Halo;
import org.geotools.styling.ImageOutline;
import org.geotools.styling.LinePlacement;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.OverlapBehavior;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.ShadedRelief;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleVisitor;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.UserLayer;
import org.opengis.feature.type.FeatureType;

/**
 * Extracts the active raster symbolizers, as long as there are some, and only raster symbolizers 
 * are available, without rendering transformations in place.
 * In the case of mixed symbolizers it will return null
 * TODO: extend this class so that it handles the case of other symbolizers applied after a raster
 * symbolizer one (e.g., to draw a rectangle around a coverage)
 * 
 * @author Andrea Aime
 *
 */
public class RasterSymbolizerVisitor implements StyleVisitor {

    double scaleDenominator;

    FeatureType featureType;

    List<RasterSymbolizer> symbolizers = new ArrayList<RasterSymbolizer>();
    
    boolean otherSymbolizers = false;
    
    boolean renderingTransformations = false;

    public RasterSymbolizerVisitor(double scaleDenominator, FeatureType featureType) {
        this.scaleDenominator = scaleDenominator;
        this.featureType = featureType;
    }
    
    public void reset() {
        symbolizers.clear();
        otherSymbolizers = false;
        renderingTransformations = false;
    }
    
    public List<RasterSymbolizer> getRasterSymbolizers() {
        if(otherSymbolizers || renderingTransformations)
            return Collections.emptyList();
        else
            return symbolizers;
    }

    public void visit(StyledLayerDescriptor sld) {
        for (StyledLayer sl : sld.getStyledLayers()) {
            if (sl instanceof UserLayer) {
                ((UserLayer) sl).accept(this);
            } else if (sl instanceof NamedLayer) {
                ((NamedLayer) sl).accept(this);
            }
        }
    }

    public void visit(NamedLayer layer) {
        for (Style s : layer.getStyles())
            s.accept(this);
    }

    public void visit(UserLayer layer) {
        for (Style s : layer.getUserStyles())
            s.accept(this);
    }

    public void visit(FeatureTypeConstraint ftc) {
        // nothing to do
    }

    public void visit(Style style) {
        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            if(fts.getTransformation() != null) {
                renderingTransformations = true;
            }
            fts.accept(this);
        }
    }

    public void visit(Rule rule) {
        if (rule.getMinScaleDenominator() < scaleDenominator
                && rule.getMaxScaleDenominator() > scaleDenominator) {
            for(Symbolizer s : rule.symbolizers())
                s.accept(this);
        }
    }

    public void visit(FeatureTypeStyle fts) {
        // use the same logic as streaming renderer to decide if a fts is active
        if((featureType.getName().getLocalPart() != null)
                && (featureType.getName().getLocalPart().equalsIgnoreCase(fts.getFeatureTypeName()) || 
                        FeatureTypes.isDecendedFrom(featureType, null, fts.getFeatureTypeName()))) {
            for (Rule r : fts.rules())
                r.accept(this);
        }
    }

    public void visit(Fill fill) {
        // nothing to do

    }

    public void visit(Stroke stroke) {
        // nothing to do
    }

    public void visit(Symbolizer sym) {
        if (sym instanceof RasterSymbolizer) {
            visit((RasterSymbolizer) sym);
        } else {
            otherSymbolizers = true;
        }
    }

    public void visit(PointSymbolizer ps) {
        otherSymbolizers = true;
    }

    public void visit(LineSymbolizer line) {
        otherSymbolizers = true;

    }

    public void visit(PolygonSymbolizer poly) {
        otherSymbolizers = true;
    }

    public void visit(TextSymbolizer text) {
        otherSymbolizers = true;
    }

    public void visit(RasterSymbolizer raster) {
        this.symbolizers.add(raster);
    }

    public void visit(Graphic gr) {
        // nothing to do

    }

    public void visit(Mark mark) {
        // nothing to do

    }

    public void visit(ExternalGraphic exgr) {
        // nothing to do

    }

    public void visit(PointPlacement pp) {
        // nothing to do

    }

    public void visit(AnchorPoint ap) {
        // nothing to do
    }

    public void visit(Displacement dis) {
        // nothing to do
    }

    public void visit(LinePlacement lp) {
        // nothing to do
    }

    public void visit(Halo halo) {
        // nothing to do
    }

    public void visit(ColorMap colorMap) {
        // nothing to do
    }

    public void visit(ColorMapEntry colorMapEntry) {
        // nothing to do
    }

    public void visit(ContrastEnhancement contrastEnhancement) {
        // nothing to do
    }

    public void visit(ImageOutline outline) {
        // nothing to do
    }

    public void visit(ChannelSelection cs) {
        // nothing to do
    }

    public void visit(OverlapBehavior ob) {
        // nothing to do
    }

    public void visit(SelectedChannelType sct) {
        // nothing to do
    }

    public void visit(ShadedRelief sr) {
        // nothing to do
    }

}
