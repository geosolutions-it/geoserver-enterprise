/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.importer.ImportSummary;
import org.geoserver.web.GeoServerBasePage;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;


/**
 * Manages pages dealing with the import processes.
 * The life cycles is the following:
 * 1) RasterChooserPage or VectorChooserPage starts up an importer task, adds an importer
 * to the ImporterThreadManager, adds a summary and a progressPage to this bean.
 * 2) When an import task is completed, the IMportSummary is set to "completed" state and
 * its importer removed from the list of active tasks in the ImportThreadManager
 * 3) The summary of the task has to be explicitely removed by this bean using the TaskPanelPage.
 *
 * @author Luca Morandini lmorandini@ieee.org
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
public class ImporterPageManager implements ApplicationListener<ContextClosedEvent>
{

    private Map<String, ImportSummary> summaries = Collections.synchronizedMap(new HashMap<String, ImportSummary>());

    private Map<String, ImportProgressPage> progresses = new HashMap<String, ImportProgressPage>();

    public ImportSummary getSummary(String id)
    {
        synchronized (summaries)
        {
            return this.summaries.get(id);
        }
    }

    public void addSummary(String id, ImportSummary summary)
    {
        synchronized (summaries)
        {
            this.summaries.put(id, summary);
        }
    }

    public void addProgressPage(String id, ImportProgressPage progress)
    {
        synchronized (summaries)
        {
            this.progresses.put(id, progress);
        }
    }

    public ImportSummary removeSummary(String id)
    {
        synchronized (summaries)
        {
            final ImportSummary temp = this.summaries.get(id);
            ;
            this.progresses.remove(id);
            this.summaries.remove(id);

            return temp;
        }
    }

    public void clear()
    {
        synchronized (summaries)
        {
            ArrayList<String> tasksToClear = new ArrayList<String>();
            for (ImportSummary summary : this.summaries.values())
            {
                if (summary.isCompleted())
                {
                    tasksToClear.add(summary.getId());
                }
            }
            for (String id : tasksToClear)
            {
                this.summaries.remove(id);
                this.progresses.remove(id);
            }
        }
    }

    public List<ImportSummary> getSummaries()
    {
        synchronized (summaries)
        {
            return new ArrayList<ImportSummary>(this.summaries.values());
        }
    }

    private ImportSummaryPage getSummaryPage(String id)
    {
        synchronized (summaries)
        {
            if (this.summaries.get(id) != null)
            {
                return new ImportSummaryPage(this.summaries.get(id), id);
            }
            else
            {
                return null;
            }
        }

    }

    private ImportProgressPage getProgressPage(String id)
    {
        synchronized (summaries)
        {
            return this.progresses.get(id);
        }
    }

    /*
     * Returns progress page if task id is still active, otherwise
     * returns the summary page and delete the progress page reference
     */
    public GeoServerBasePage getPage(String id)
    {
        synchronized (summaries)
        {
            if (!this.summaries.get(id).isCompleted())
            {
                return this.getProgressPage(id);
            }
            else
            {
                this.progresses.remove(id);

                return this.getSummaryPage(id);
            }
        }
    }

    /*
     * Removes all inactive summaries when context is closed ???
     */
    public void onApplicationEvent(ContextClosedEvent event)
    {
        synchronized (summaries)
        {
            this.summaries.clear();
            this.progresses.clear();
        }

    }
}
