/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * Manages importer threads with a pool
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 */
public class ImporterThreadManager implements ApplicationListener<ContextClosedEvent>
{

    private static final Logger LOGGER = Logging.getLogger(ImporterThreadManager.class.toString());

    private final ThreadPoolTaskExecutor executor;

    Map<String, Importer> tasks = new HashMap<String, Importer>();

    public ImporterThreadManager(ThreadPoolTaskExecutor executor)
    {
        this.executor = executor;
    }

    public String startImporter(String id, Importer importer)
    {

        try
        {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            importer.setAuthentication(authentication);
            
            executor.execute(importer);
            tasks.put(id, importer);
        }
        catch (RejectedExecutionException e)
        {
            // rejected
            id = null;
            if (LOGGER.isLoggable(Level.WARNING))
            {
                LOGGER.log(Level.WARNING, "Import Task Rejected", e); // FIXME I18N
            }
        }

        return id;
    }

    public List<Importer> getImporters()
    {
        return new ArrayList<Importer>(this.tasks.values());
    }

    public Importer getImporter(String id)
    {
        return tasks.get(id);
    }

    public void cleanImporter(String id)
    {
        tasks.remove(id);
    }

    /*
     * Shutdowns pool when the app dies
     */
    public void onApplicationEvent(ContextClosedEvent event)
    {
        executor.shutdown();
    }

}
