/* Copyright (c) 2012 GeoSolutions http://www.geo-solutions.it. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor.storage.dao.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.geoserver.wps.executor.storage.dao.ProcessDescriptorDAO;
import org.geoserver.wps.executor.storage.model.ProcessDescriptor;
import org.springframework.transaction.annotation.Transactional;

import com.googlecode.genericdao.dao.jpa.GenericDAOImpl;
import com.googlecode.genericdao.search.ISearch;
import com.googlecode.genericdao.search.jpa.JPASearchProcessor;

/**
 * Public implementation of the ProcessDescriptorDAO interface that relies on GenericDAO
 * 
 * @author Emanuele Tajariol (etj at geo-solutions.it)
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
@Transactional(value = "processStorageTransactionManager")
public class ProcessDescriptorDAOImpl extends GenericDAOImpl<ProcessDescriptor, Long> implements
        ProcessDescriptorDAO {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(ProcessDescriptorDAOImpl.class);

    /** The entity manager. */
    @PersistenceContext(unitName = "processStorageEntityManagerFactory")
    private EntityManager entityManager;

    /**
     * Persist.
     * 
     * @param entities the entities
     */
    @Override
    public void persist(ProcessDescriptor... entities) {
        super.persist(entities);
    }

    /**
     * Find all.
     * 
     * @return the list
     */
    @Override
    public List<ProcessDescriptor> findAll() {
        return super.findAll();
    }

    /**
     * Search.
     * 
     * @param search the search
     * @return the list
     */
    @Override
    public List<ProcessDescriptor> search(ISearch search) {
        return super.search(search);
    }

    /**
     * Merge.
     * 
     * @param entity the entity
     * @return the process descriptor
     */
    @Override
    public ProcessDescriptor merge(ProcessDescriptor entity) {
        return super.merge(entity);
    }

    /**
     * Removes the.
     * 
     * @param entity the entity
     * @return true, if successful
     */
    @Override
    public boolean remove(ProcessDescriptor entity) {
        return super.remove(entity);
    }

    /**
     * Removes the by id.
     * 
     * @param id the id
     * @return true, if successful
     */
    @Override
    public boolean removeById(Long id) {
        return super.removeById(id);
    }

    /**
     * Removes the by id.
     * 
     * @param id the id
     * @return true, if successful
     */
    @Override
    public void removeByIds(Long... ids) {
        super.removeByIds(ids);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.trg.dao.jpa.JPABaseDAO#em()
     */
    /**
     * Em.
     * 
     * @return the entity manager
     */
    @Override
    public EntityManager em() {
        return this.entityManager;
    }

    /**
     * EntityManager setting.
     * 
     * @param entityManager the entity manager to set
     */
    @Override
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        super.setEntityManager(this.entityManager);
    }

    /**
     * JPASearchProcessor setting.
     * 
     * @param searchProcessor the search processor to set
     */
    @Override
    public void setSearchProcessor(JPASearchProcessor searchProcessor) {
        super.setSearchProcessor(searchProcessor);
    }

}
