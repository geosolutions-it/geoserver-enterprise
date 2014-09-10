/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wps.executor.storage.dao;

import java.util.UUID;

import org.geoserver.wps.executor.storage.model.ProcessDescriptor;

/**
 * The Class ProcessDescriptorDAOTest.
 * 
 * @author Alessio Fabiani <alessio.fabiani at geo-solutions.it>
 */
public class ProcessDescriptorDAOTest extends BaseDAOTest {

    /**
     * Test persist process.
     * 
     * @throws Exception the exception
     */
    public void testPersistProcess() throws Exception {

        long id;
        {
            ProcessDescriptor process = createProcessDescriptor(UUID.randomUUID().toString(), UUID
                    .randomUUID().toString());
            processDAO.persist(process);
            id = process.getId();
        }

        // test save & load
        {
            ProcessDescriptor loaded = processDAO.find(id);
            assertNotNull("Can't retrieve process", loaded);
        }

        processDAO.removeById(id);
        assertNull("Process not deleted", processDAO.find(id));
    }
}
