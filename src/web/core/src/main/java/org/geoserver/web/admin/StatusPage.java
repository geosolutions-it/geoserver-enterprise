package org.geoserver.web.admin;

import java.awt.GraphicsEnvironment;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.media.jai.JAI;
import javax.media.jai.TileCache;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.LockingManager;

import com.sun.media.imageioimpl.common.PackageUtil;
import com.sun.media.jai.util.CacheDiagnostics;
import com.sun.media.jai.util.SunTileCache;

public class StatusPage extends ServerAdminPage {

    /**
     * The map used as the model source so the label contents are updated
     */
    private final Map<String, String> values;
    
    private static final String KEY_DATA_DIR = "dataDir";

    private static final String KEY_LOCKS = "locks";

    private static final String KEY_CONNECTIONS = "connections";

    private static final String KEY_MEMORY = "memory";

    private static final String KEY_JVM_VERSION = "jvm_version";
    
    private static final String KEY_JAI_AVAILABLE = "jai_available";
    private static final String KEY_JAI_IMAGEIO_AVAILABLE = "jai_imageio_available";
    

    private static final String KEY_JAI_MAX_MEM = "jai_max_mem";

    private static final String KEY_JAI_MEM_USAGE = "jai_mem_usage";

    private static final String KEY_JAI_MEM_THRESHOLD = "jai_mem_threshold";

    private static final String KEY_JAI_TILE_THREADS = "jai_tile_threads";

    private static final String KEY_JAI_TILE_THREAD_PRIORITY = "jai_tile_thread_priority";
    
    private static final String KEY_COVERAGEACCESS_CORE_POOL_SIZE = "coverage_thread_corepoolsize";
    
    private static final String KEY_COVERAGEACCESS_MAX_POOL_SIZE = "coverage_thread_maxpoolsize";
    
    private static final String KEY_COVERAGEACCESS_KEEP_ALIVE_TIME = "coverage_thread_keepalivetime";
    
    private static final String KEY_UPDATE_SEQUENCE = "update_sequence";

    public StatusPage() {
        values = new HashMap<String, String>();
        updateModel();

        // TODO: if we just provide the values directly as the models they won't
        // be refreshed on a page reload (ugh).
        add(new Label("dataDir", new MapModel(values, KEY_DATA_DIR)));
        add(new Label("locks", new MapModel(values, KEY_LOCKS)));
        add(new Label("connections", new MapModel(values, KEY_CONNECTIONS)));
        add(new Label("memory", new MapModel(values, KEY_MEMORY)));
        add(new Label("jvm.version", new MapModel(values, KEY_JVM_VERSION)));
        add(new Label("jai.available", new MapModel(values, KEY_JAI_AVAILABLE)));
        add(new Label("jai.imageio.available", new MapModel(values, KEY_JAI_IMAGEIO_AVAILABLE)));
        add(new Label("jai.memory.available", new MapModel(values, KEY_JAI_MAX_MEM)));
        add(new Label("jai.memory.used", new MapModel(values, KEY_JAI_MEM_USAGE)));
        add(new Label("jai.memory.threshold", new MapModel(values, KEY_JAI_MEM_THRESHOLD)));
        add(new Label("jai.tile.threads", new MapModel(values, KEY_JAI_TILE_THREADS)));
        add(new Label("jai.tile.priority", new MapModel(values, KEY_JAI_TILE_THREAD_PRIORITY)));
        add(new Label("coverage.corepoolsize", new MapModel(values, KEY_COVERAGEACCESS_CORE_POOL_SIZE)));
        add(new Label("coverage.maxpoolsize", new MapModel(values, KEY_COVERAGEACCESS_MAX_POOL_SIZE)));
        add(new Label("coverage.keepalivetime", new MapModel(values, KEY_COVERAGEACCESS_KEEP_ALIVE_TIME)));
        add(new Label("updateSequence", new MapModel(values, KEY_UPDATE_SEQUENCE)));

        add(new Link("free.locks") {
            private static final long serialVersionUID = 1L;

            public void onClick() {
                // TODO: see GEOS-2130
                updateModel();
            }
        });
        add(new Link("free.memory") {
            private static final long serialVersionUID = 1L;

            public void onClick() {
                System.gc();
                System.runFinalization();
                updateModel();
            }
        });
        
        add(new Link("free.memory.jai") {
            private static final long serialVersionUID = 1L;

            public void onClick() {
                TileCache jaiCache = getGeoServer().getGlobal().getJAI().getTileCache();
                final long capacityBefore = jaiCache.getMemoryCapacity();
                jaiCache.flush();
                jaiCache.setMemoryCapacity(0); // to be sure we realease all tiles
                System.gc();
                System.runFinalization();
                jaiCache.setMemoryCapacity(capacityBefore);
                updateModel();
            }
        });
        
        int fontCount =  GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts().length;
        add(new Label("fontCount", new ParamResourceModel("StatusPage.fontCount", this, fontCount)));
        add(new BookmarkablePageLink("show.fonts", JVMFontsPage.class));

        add(new AjaxLink("clear.resourceCache") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    getGeoServer().reset();
                    info(getLocalizer().getString("resourceCacheClearedSuccessfully", this));
                } catch(Throwable t) {
                    LOGGER.log(Level.SEVERE, "Error resetting resource caches", t);
                    error(t);
                }
                target.addComponent(feedbackPanel);
            }
        });
        
        add(new AjaxLink("reload.catalogConfig") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    getGeoServer().reload();
                    info(getLocalizer().getString("catalogConfigReloadedSuccessfully", StatusPage.this));
                } catch(Throwable t) {
                    LOGGER.log(Level.SEVERE, "An error occurred while reloading the catalog", t);
                    error(t);
                }
                target.addComponent(feedbackPanel);
            }
        });
        
        add(new Label("reload.date.geoserver", "Jul 14, 3:07 PM"));
        add(new Label("reload.date.configuration", "Jul 14, 3:07 PM"));
        add(new Label("reload.date.xml", "Mar 14, 2:15 PM"));
    }

    private void updateModel() {
        values.put(KEY_DATA_DIR, getDataDirectory());
        values.put(KEY_LOCKS, Long.toString(getLockCount()));
        values.put(KEY_CONNECTIONS, Long.toString(getConnectionCount()));
        values.put(KEY_MEMORY, formatUsedMemory());
        values.put(KEY_JVM_VERSION, System.getProperty("java.vendor") + ": "
                + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")");
        
        values.put(KEY_JAI_AVAILABLE, Boolean.toString(isNativeJAIAvailable()));
        values.put(KEY_JAI_IMAGEIO_AVAILABLE, Boolean.toString(PackageUtil.isCodecLibAvailable()));

        GeoServerInfo geoServerInfo = getGeoServer().getGlobal();
        JAIInfo jaiInfo = geoServerInfo.getJAI();
        JAI jai =  jaiInfo.getJAI();
        CoverageAccessInfo coverageAccess = geoServerInfo.getCoverageAccess();
        TileCache jaiCache = jaiInfo.getTileCache();

        values.put(KEY_JAI_MAX_MEM, formatMemory(jaiCache.getMemoryCapacity()));
        if(jaiCache instanceof CacheDiagnostics) {
            values.put(KEY_JAI_MEM_USAGE, formatMemory(((CacheDiagnostics) jaiCache).getCacheMemoryUsed()));
        } else {
            values.put(KEY_JAI_MEM_USAGE, "-");
        }
        values.put(KEY_JAI_MEM_THRESHOLD, Float.toString(100.0f * jaiCache.getMemoryThreshold()));
        values.put(KEY_JAI_TILE_THREADS, Integer.toString(jai.getTileScheduler().getParallelism()));
        values.put(KEY_JAI_TILE_THREAD_PRIORITY, Integer.toString(jai.getTileScheduler()
                .getPriority()));
        
        values.put(KEY_COVERAGEACCESS_CORE_POOL_SIZE, Integer.toString(coverageAccess.getCorePoolSize()));
        values.put(KEY_COVERAGEACCESS_MAX_POOL_SIZE, Integer.toString(coverageAccess.getMaxPoolSize()));
        values.put(KEY_COVERAGEACCESS_KEEP_ALIVE_TIME, Integer.toString(coverageAccess.getKeepAliveTime()));

        values.put(KEY_UPDATE_SEQUENCE, Long.toString(geoServerInfo.getUpdateSequence()));
    }

    /**
     * Retrieves the GeoServer data directory
     * @return
     */
    private String getDataDirectory() {
        GeoServerDataDirectory dd = getGeoServerApplication().getBeanOfType(GeoServerDataDirectory.class);
        return dd.root().getAbsolutePath();
    }

    boolean isNativeJAIAvailable() {
        // we directly access the Mlib Image class, if in the classpath it will tell us if
        // the native extensions are available, if not, an Error will be thrown
        try {
            Class image = Class.forName("com.sun.medialib.mlib.Image");
            return (Boolean) image.getMethod("isAvailable").invoke(null);
        } catch(Throwable e) {
            return false;
        }
    }
    
    /**
     * @return a human friendly string for the VM used memory
     */
    private String formatUsedMemory() {
        final Runtime runtime = Runtime.getRuntime();
        final long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        String formattedUsedMemory = formatMemory(usedBytes);

        return formattedUsedMemory;
    }

    private String formatMemory(final long bytes) {
        final long KB = 1024;
        final long MB = KB * KB;
        final long GB = KB * MB;
        final NumberFormat formatter = NumberFormat.getInstance();
        formatter.setMaximumFractionDigits(2);

        String formattedUsedMemory;
        if (bytes > GB) {
            formattedUsedMemory = formatter.format(bytes / GB) + " GB";
        } else if (bytes > MB) {
            formattedUsedMemory = formatter.format(bytes / MB) + " MB";
        } else {
            formattedUsedMemory = formatter.format(bytes / KB) + " KB";
        }
        return formattedUsedMemory;
    }

    private synchronized int getLockCount() {
        int count = 0;

        for (Iterator i = getDataStores().iterator(); i.hasNext();) {
            DataStoreInfo meta = (DataStoreInfo) i.next();

            if (!meta.isEnabled()) {
                // Don't count locks from disabled datastores.
                continue;
            }

            try {
                DataAccess store = meta.getDataStore(null);
                if(store instanceof DataStore) {
                    LockingManager lockingManager = ((DataStore) store).getLockingManager();
                    if (lockingManager != null){
                        // we can't actually *count* locks right now?
                        // count += lockingManager.getLockSet().size();
                    }
                }
            } catch (IllegalStateException notAvailable) {
                continue;
            } catch (Throwable huh) {
                continue;
            }
        }

        return count;
    }

    private synchronized int getConnectionCount() {
        int count = 0;

        for (Iterator i = getDataStores().iterator(); i.hasNext();) {
            DataStoreInfo meta = (DataStoreInfo) i.next();

            if (!meta.isEnabled()) {
                // Don't count connections from disabled datastores.
                continue;
            }

            try {
                meta.getDataStore(null);
            } catch (Throwable notAvailable) {
                // TODO: Logging.
                continue;
            }

            count += 1;
        }

        return count;
    }

    private List<DataStoreInfo> getDataStores() {
        return getGeoServer().getCatalog().getDataStores();
    }

}
