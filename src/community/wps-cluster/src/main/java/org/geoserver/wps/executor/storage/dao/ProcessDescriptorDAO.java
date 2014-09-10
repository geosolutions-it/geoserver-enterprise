/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor.storage.dao;

import java.util.List;

import org.geoserver.wps.executor.storage.model.ProcessDescriptor;

import com.googlecode.genericdao.search.ISearch;

/**
 * Public interface to define operations on ProcessDescriptor.
 * 
 * @author Emanuele Tajariol (etj at geo-solutions.it)
 */

public interface ProcessDescriptorDAO {

    /**
     * Find all.
     * 
     * @return the list
     */
    public List<ProcessDescriptor> findAll();

    /**
     * Find.
     * 
     * @param id the id
     * @return the ProcessDescriptor
     */
    public ProcessDescriptor find(Long id);

    /**
     * Persist.
     * 
     * @param entities the entities
     */
    public void persist(ProcessDescriptor... entities);

    /**
     * Merge.
     * 
     * @param ProcessDescriptor the ProcessDescriptor
     * @return the ProcessDescriptor
     */
    public ProcessDescriptor merge(ProcessDescriptor ProcessDescriptor);

    /**
     * Removes the.
     * 
     * @param ProcessDescriptor the ProcessDescriptor
     * @return true, if successful
     */
    public boolean remove(ProcessDescriptor ProcessDescriptor);

    /**
     * Removes the by id.
     * 
     * @param id the id
     * @return true, if successful
     */
    public boolean removeById(Long id);

    /**
     * Removes the by id.
     * 
     * @param id the ids
     */
    public void removeByIds(Long... id);

    /**
     * Search.
     * 
     * @param search the search
     * @return the list
     */
    public List<ProcessDescriptor> search(ISearch search);

    /**
     * Count.
     * 
     * @param search the search
     * @return the int
     */
    public int count(ISearch search);
}
