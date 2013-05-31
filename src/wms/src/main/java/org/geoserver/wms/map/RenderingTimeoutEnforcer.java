/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Graphics;
import java.util.Timer;
import java.util.TimerTask;

import org.geotools.renderer.GTRenderer;

/**
 * An utility class that can be used to set a strict timeout on rendering operations:
 * if the timeout elapses, the renderer will be asked to stop rendering and the graphics
 * will be disposed of to make extra sure the renderer cannot keep going on.
 * @author Andrea Aime - OpenGeo
 */
public class RenderingTimeoutEnforcer {
    
    long timeout;
    GTRenderer renderer;
    Graphics graphics;
    Timer timer;
    boolean timedOut = false;

    public RenderingTimeoutEnforcer(long timeout, GTRenderer renderer, Graphics graphics) {
        this.timeout = timeout;
        this.renderer = renderer;
        this.graphics = graphics;
    }

    /**
     * Starts checking the rendering timeout (if timeout is positive, does nothing otherwise)
     */
    public void start() {
        if(timer != null)
            throw new IllegalStateException("The timeout enforcer has already been started");
        
        if(timeout > 0) {
            timedOut = false;
            timer = new Timer();
            timer.schedule(new StopRenderingTask(), timeout);
        }
    }
    
    /**
     * Stops the timeout check
     */
    public void stop() {
        if(timer != null) {
            timer.cancel();
            timer.purge();
            // timer.getTheHellOutOfDodge();
            timer = null;
        }
    }
    
    /**
     * Returns true if the renderer has been stopped mid-way due to the timeout occurring
     */
    public boolean isTimedOut() {
        return timedOut;
    }
    
    class StopRenderingTask extends TimerTask {

        @Override
        public void run() {
            // mark as timed out
            timedOut = true;
            
            // ask gently...
            renderer.stopRendering();
            // ... but also be rude for extra measure (coverage rendering is
            // an atomic call to the graphics, it cannot be stopped
            // by the above)
            graphics.dispose();
            
        }
        
    }

}
