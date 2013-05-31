/* 
 * Copyright (c) 2001 - 20089 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

/**
 * 
 * Create FeatureCollection Template Model without copying features to memory
 * When using this in a FeatureWrapper, it is necessary to call purge() method after
 * processing template, to close any open database connections
 * 
 * @author Niels Charlier, Curtin University of Technology
 * 
 */
public class DirectTemplateFeatureCollectionFactory implements FeatureWrapper.TemplateFeatureCollectionFactory {

    static Logger LOGGER = Logging.getLogger(DirectTemplateFeatureCollectionFactory.class);

    /**
     * thread local to track open iterators
     */
    static ThreadLocal<List<TemplateFeatureIterator>> ITERATORS = 
            new ThreadLocal<List<TemplateFeatureIterator>>();

    public void purge() {
        List<TemplateFeatureIterator> its = ITERATORS.get();
        if (its != null) {
            for (TemplateFeatureIterator it : its) {
                try {
                    it.close();
                }
                catch(Throwable t) {
                    LOGGER.log(Level.WARNING, "Error closing iterator", t);
                }
            }
            its.clear();
            ITERATORS.remove();
        }
    }

    public TemplateCollectionModel createTemplateFeatureCollection(FeatureCollection collection,
            BeansWrapper wrapper) {
        return new TemplateFeatureCollection(collection, wrapper);
    }

    protected class TemplateFeatureCollection implements TemplateCollectionModel {
        protected BeansWrapper wrapper;

        protected FeatureCollection collection;

        public TemplateFeatureCollection(FeatureCollection collection, BeansWrapper wrapper) {
            this.collection = collection;
            this.wrapper = wrapper;
        }

        public TemplateModelIterator iterator() throws TemplateModelException {
            TemplateFeatureIterator it = new TemplateFeatureIterator(collection.features(), wrapper);
            List<TemplateFeatureIterator> open = ITERATORS.get();
            if (open == null) {
                open = new LinkedList();
                ITERATORS.set(open);
            }
            open.add(it);
            return it;
        }

    }

    protected class TemplateFeatureIterator implements TemplateModelIterator {

        protected BeansWrapper wrapper;

        protected FeatureIterator iterator;

        public TemplateFeatureIterator(FeatureIterator iterator, BeansWrapper wrapper) {
            this.iterator = iterator;
            this.wrapper = wrapper;
        }

        public TemplateModel next() throws TemplateModelException {
            return wrapper.wrap(iterator.next());
        }

        public boolean hasNext() throws TemplateModelException {
            return iterator.hasNext();
        }

        public void close() {
            iterator.close();
        }

    }

}
