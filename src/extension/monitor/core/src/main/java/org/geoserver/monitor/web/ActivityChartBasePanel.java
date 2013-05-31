/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Resource;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.image.resource.BufferedDynamicImageResource;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.web.GeoServerApplication;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.HorizontalAlignment;

public abstract class ActivityChartBasePanel extends Panel {
    
    protected static long PAGE_OFFSET = 1000;
    protected static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    Date from;
    Date to;
    NonCachingImage chartImage;
    
    public ActivityChartBasePanel(String id, Monitor monitor) {
        super(id);
        
        Date[] range = getDateRange();
        
        BufferedDynamicImageResource resource = queryAndRenderChart(monitor, range);
        add(chartImage = new NonCachingImage("chart", resource));
        chartImage.setOutputMarkupId(true);
        
        Form form = new Form("form");
        add(form);
        
        from = new Date(range[0].getTime());
        to = new Date(range[1].getTime());
        
        form.add(new DateTimeField("from", new PropertyModel<Date>(this,"from")) {
            @Override
            protected boolean use12HourFormat() {
                return false;
            }
        });
        form.add(new DateTimeField("to", new PropertyModel<Date>(this, "to")) {
            @Override
            protected boolean use12HourFormat() {
                return false;
            }
        });
        
        form.add(new AjaxButton("refresh") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                Monitor monitor = 
                    ((GeoServerApplication)getApplication()).getBeanOfType(Monitor.class);
                
                Date[] range = new Date[]{ from, to };
                
                chartImage.setImageResource(queryAndRenderChart(monitor, range));
                target.addComponent(chartImage);
            }
        });
    }
    
    BufferedDynamicImageResource queryAndRenderChart(Monitor monitor, Date[] range) {
        Query q = new Query();
        q.properties("startTime").between(range[0], range[1]);
        
        DataGatherer gatherer = new DataGatherer();
        monitor.query(q, gatherer);
        
        HashMap<RegularTimePeriod,Integer> data = gatherer.getData();
        
        Class timeUnitClass = getTimePeriod(range[0]).getClass();
        TimeSeries series = new TimeSeries("foo", timeUnitClass);
        for (Map.Entry<RegularTimePeriod, Integer> d : data.entrySet()) {
            series.add(new TimeSeriesDataItem(d.getKey(), d.getValue()));
        }
        
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        
        final JFreeChart chart = createTimeSeriesChart(getChartTitle(range),
            "Time ("+timeUnitClass.getSimpleName()+")", "Requests", dataset);
        
        BufferedDynamicImageResource resource = new BufferedDynamicImageResource();
        resource.setImage(chart.createBufferedImage(700,500));
        return resource;
    }
    
    JFreeChart createTimeSeriesChart(String title, String timeAxisLabel, String valueAxisLabel,
        XYDataset dataset) {
        
        ValueAxis timeAxis = new DateAxis(timeAxisLabel);
        timeAxis.setLowerMargin(0.02);  // reduce the default margins 
        timeAxis.setUpperMargin(0.02);
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);  // override default

        XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, null);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        plot.setRenderer(renderer);
        
        JFreeChart chart = new JFreeChart(plot);
        
        //TextTitle t = new TextTitle(title);
        //t.setTextAlignment(HorizontalAlignment.LEFT);
        
        //chart.setTitle(t);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setAntiAlias(true);
        chart.clearSubtitles();
        
        return chart;
    }
    class DataGatherer implements RequestDataVisitor {

        HashMap<RegularTimePeriod,Integer> data = new HashMap<RegularTimePeriod, Integer>();
        
        public void visit(RequestData r, Object... aggregates) {
            RegularTimePeriod period = getTimePeriod(r.getStartTime());
            Integer count = data.get(period);
       
            if (count == null) {
                count = new Integer(1);
            }
            else {
                count = new Integer(count.intValue()+1);
            }
            
            data.put(period,count);
        }
        
        public HashMap<RegularTimePeriod, Integer> getData() {
            return data;
        }
    }
    
    protected String getChartTitle(Date[] range) {
        return "Activity " + FORMAT.format(range[0]) + " - " + FORMAT.format(range[1]);
    }
    
    protected abstract Date[] getDateRange();
    
    protected abstract RegularTimePeriod getTimePeriod(Date time);
    
    

}
