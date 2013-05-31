/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.file;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

/**
 * This class is based on the concept  from the FileWatchDog
 * class in log4j. 
 * 
 * Objects of this class watch for modifications of files 
 * periodically. If a file has changed an action is triggered,
 * this action has to be implemented in a concrete subclass.  
 * 
 * @author christian
 *
 */
public abstract class FileWatcher extends Thread{
    

    static protected Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security");
    /**
       The default delay between every file modification check, set to 10
       seconds.  */
    static final public long DEFAULT_DELAY = 10000; 
    /**
       The name of the file to observe  for changes.
     */
    protected String filename;
    
    /**
       The delay to observe between every check. By default set {@link
       #DEFAULT_DELAY}. */
    protected long delay = DEFAULT_DELAY; 
    
    File file;
    long lastModified = 0;

    boolean warnedAlready = false;
    boolean terminate = false;
    Object terminateLock= new Object();
    Object lastModifiedLock= new Object();

    public boolean isTerminated() {
        synchronized (terminateLock) {
            return terminate;    
        }        
    }

    /**
     * Use this method for stopping the thread
     * @param terminated
     */
    public void setTerminate(boolean terminated) {
        synchronized (terminateLock) {
            this.terminate = terminated;    
        }        
    }

    protected FileWatcher(String filename) {
      this.filename = filename;
      file = new File(filename);
      setDaemon(true);
    }

    /**
     * Set the delay to observe between each check of the file changes.
     * Use values > 1000, most file systems have a time granularity
     * of seconds 
     * @param delay
     */
    public void setDelay(long delay) {
      this.delay = delay;
    }

    /**
     * Subclasses must override
     */
    abstract protected void doOnChange();

    /**
     * Test constellation and call
     * {@link #doOnChange()} if necessary 
     */
    protected void checkAndConfigure() {
      boolean fileExists;
      try {
        fileExists = file.exists();
      } catch(SecurityException  e) {
        LOGGER.warning("Was not allowed to read check file existance, file:["+
                    filename+"].");
        setTerminate(true); // there is no point in continuing
        return;
      }

      if(fileExists) {
        long l = file.lastModified(); // this can also throw a SecurityException
        if(testAndSetLastModified(l)) {           // however, if we reached this point this
            doOnChange();              // is very unlikely.          
            warnedAlready = false;
        }
      } else {
        if(!warnedAlready) {
          LOGGER.warning("["+filename+"] does not exist.");
          warnedAlready = true;
        }
      }
    }

    
    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    public void run() {    
      while(!isTerminated()) {
        try {
        Thread.sleep(delay);
        } catch(InterruptedException e) {
          // no interruption expected
        }
        checkAndConfigure();
      }
    }
    
    /**
     * @return info about the watched file
     */
    public String getFileInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        StringBuffer buff = new StringBuffer(file.getPath());
        buff.append( " last modified: ");
        buff.append(sdf.format(file.lastModified()));
        return buff.toString();
    }

    @Override
    public String toString() {
        return "FileWatcher: " + getFileInfo();
    }
    
    /**
     * Test if l > last modified
     * 
     * @param l
     * @return true if file was modified
     */
    public boolean testAndSetLastModified(long l) {
        synchronized (lastModifiedLock) {
            if (l > lastModified) {
                lastModified=l;
                return true;
            }
            return false;
        }
        
    }

    /**
     * Method intended to set last modified
     * from a client which is up to date.
     * This avoids unnecessary reloads
     * 
     * @param lastModified
     */
    public void setLastModified(long lastModified) {
        synchronized (lastModifiedLock) {
            this.lastModified = lastModified;
        }        
    }

}
