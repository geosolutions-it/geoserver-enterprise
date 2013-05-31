/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * $Id: ImportSummary.java 174 2012-01-23 15:11:17Z alessio $
 */
package org.geoserver.importer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.geoserver.catalog.LayerInfo;

import static org.geoserver.importer.TaskImportStatus.*;


/**
 * Contains summary information about the whole import process
 *
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
@SuppressWarnings("serial")
public class ImportSummary implements Serializable
{

    private long startTime;

    private long endTime;

    private int totalLayers;

    private int processedLayers;

    private int successes;

    private int failures;

    private String currentLayer;

    private Exception error;

    private String id;

    private String project;

    private boolean workspaceNew;

    private boolean storeNew;

    private boolean done;

    private TaskImportStatus status = INPROGRESS;

    // Holds layer progress percentage and message
    Map<String, LayerProgress> layerProgress = new HashMap<String, LayerProgress>();

    // concurrent list so that we can manipulate it while it's being iterated over
    List<LayerSummary> layers = new CopyOnWriteArrayList<LayerSummary>();

    public ImportSummary(String id, String project, boolean workspaceNew, boolean storeNew)
    {
        this.id = id;
        this.project = project;
        this.startTime = System.currentTimeMillis();
        this.workspaceNew = workspaceNew;
        this.storeNew = storeNew;
    }

    public TaskImportStatus getStatus()
    {
        return this.status;
    }

    public void setStatus(TaskImportStatus status)
    {
        this.status = status;
    }

    void setTotalLayers(int totalLayers)
    {
        this.totalLayers = totalLayers;
    }

    public String getId()
    {
        return this.id;
    }

    public String getProject()
    {
        return this.project;
    }

    public void newLayer(String currentLayer)
    {
        this.currentLayer = currentLayer;
    }

    void end(Exception error)
    {
        this.error = error;
        this.endTime = System.currentTimeMillis();
        this.done = true;
        this.setStatus(ERROR);
    }

    void end(boolean isCanceled)
    {
        this.done = true;
        this.endTime = System.currentTimeMillis();

        if (isCanceled)
        {
            this.setStatus(CANCELED);
        }
        else
        {
            if ((this.status != ERROR) && (this.getFailures() > 0))
            {
                this.setStatus(ISSUES);
            }
            else
            {
                this.setStatus(SUCCESS);
            }
        }
    }

    public boolean isCompleted()
    {
        return done;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

    public int getTotalLayers()
    {
        return totalLayers;
    }

    public int getSuccesses()
    {
        return successes;
    }

    public List<LayerSummary> getLayers()
    {
        return layers;
    }

    public int getProcessedLayers()
    {
        return processedLayers;
    }

    public int getFailures()
    {
        return failures;
    }

    public String getCurrentLayer()
    {
        return currentLayer;
    }

    public void setLayerProgress(String layerName, String message, double percentage)
    {
        this.layerProgress.put(layerName, new LayerProgress(percentage, message));
    }

    public LayerProgress getLayerProgress(String layerName)
    {
        if (this.layerProgress.get(layerName) != null)
        {
            return this.layerProgress.get(layerName);
        }
        else
        {
            return null;
        }
    }

    void completeLayer(String layerName, LayerInfo layer, String message, LayerImportStatus status)
    {
        layers.add(new LayerSummary(layerName, layer, status));
        processedLayers++;
        this.setLayerProgress(layerName, message, 100);
        if (!status.successful())
        {
            failures++;
        }
        else
        {
            successes++;
        }
    }

    void completeLayer(String layerName, LayerInfo layer, String message, Exception error)
    {
        layers.add(new LayerSummary(layerName, layer, error));
        processedLayers++;
        this.setLayerProgress(layerName, message, 100);
        failures++;
    }

    public boolean isWorkspaceNew()
    {
        return workspaceNew;
    }

    public boolean isStoreNew()
    {
        return storeNew;
    }

    public Exception getError()
    {
        return error;
    }

    /**
     *
     * Inner class to hold progress data for a single layer
     *
     * @author Simone Giannecchini, GeoSolutions SAS
     *
     */
    public static final class LayerProgress implements Serializable
    {

        private final Double progress;

        private final String message;

        public LayerProgress(double progress, String message)
        {
            this.progress = new Double(progress);
            this.message = message;
        }

        public double getProgress()
        {
            return progress.doubleValue();
        }

        public String getMessage()
        {
            return message;
        }

        @Override
        public String toString()
        {
            return "LayerProgress= " + progress + ", message=" + message + "]";
        }

    }
}
