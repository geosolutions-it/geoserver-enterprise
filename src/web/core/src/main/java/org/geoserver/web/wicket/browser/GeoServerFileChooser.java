package org.geoserver.web.wicket.browser;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.filechooser.FileSystemView;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.SetModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.vfny.geoserver.global.GeoserverDataDirectory;

@SuppressWarnings("serial")
public class GeoServerFileChooser extends Panel {
    static File USER_HOME = null;
    
    static {
        // try to safely determine the user home location
        try {
            File hf = null;
            String home = System.getProperty("user.home");
            if(home != null) {
                hf = new File(home);
            }
            if(hf != null && hf.exists()) {
                USER_HOME = hf;
            } 
        } catch(Throwable t) {
            // that's ok, we might not be able to get the user home
        }
    }
    
    FileBreadcrumbs breadcrumbs;
    FileDataView fileTable;
    IModel file;
    
    public GeoServerFileChooser(String id, IModel file) {
        super(id, file);

        this.file = file;
        
        // build the roots
        ArrayList<File> roots = new ArrayList<File>(Arrays.asList(File.listRoots()));
        Collections.sort(roots);
        
        // TODO: find a better way to deal with the data dir
        File dataDirectory = GeoserverDataDirectory.getGeoserverDataDirectory();
        roots.add(0, dataDirectory);
        
        // add the home directory as well if it was possible to determine it at all
        if(USER_HOME != null) {
            roots.add(1, USER_HOME);
        }
        
        // find under which root the selection should be placed
        File selection = (File) file.getObject();
        
        // first check if the file is a relative reference into the data dir
        if(selection != null) {
            File relativeToDataDir = GeoserverDataDirectory.findDataFile(selection.getPath()); 
            if(relativeToDataDir != null) {
                selection = relativeToDataDir;
            }
        }
        
        // select the proper root
        File selectionRoot = null;
        if(selection != null && selection.exists()) {
            for (File root : roots) {
                if(isSubfile(root, selection.getAbsoluteFile())) {
                    selectionRoot = root;
                    break;
                }
            }
            
            // if the file is not part of the known search paths, give up 
            // and switch back to the data directory
            if(selectionRoot == null) {
                selectionRoot = dataDirectory;
                file = new Model(selectionRoot);
            } else {
                if(!selection.isDirectory()) {
                    file = new Model(selection.getParentFile());
                } else {
                    file = new Model(selection);
                }
            }
        } else {
            selectionRoot = dataDirectory;
            file = new Model(selectionRoot);
        }
        this.file = file;
        setDefaultModel(file);
        
        
        // the root chooser
        final DropDownChoice choice = new DropDownChoice("roots", new Model(selectionRoot), new Model(roots), new FileRootsRenderer());
        choice.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                File selection = (File) choice.getModelObject();
                breadcrumbs.setRootFile(selection);
                updateFileBrowser(selection, target);
            }
            
        });
        choice.setOutputMarkupId(true);
        add(choice);
        
        // the breadcrumbs
        breadcrumbs = new FileBreadcrumbs("breadcrumbs", new Model(selectionRoot), file) {

            @Override
            protected void pathItemClicked(File file, AjaxRequestTarget target) {
                updateFileBrowser(file, target);
            }
            
        };
        breadcrumbs.setOutputMarkupId(true);
        add(breadcrumbs);
        
        // the file tables
        fileTable = new FileDataView("fileTable", new FileProvider(file)) {

            @Override
            protected void linkNameClicked(File file, AjaxRequestTarget target) {
                updateFileBrowser(file, target);
            }
            
        };
        fileTable.setOutputMarkupId(true);
        add(fileTable);
    }
    
    void updateFileBrowser(File file, AjaxRequestTarget target) {
        if(file.isDirectory()) {
            directoryClicked(file, target);
        } else if(file.isFile()) {
            fileClicked(file, target);
        }
    }

    /**
     * Called when a file name is clicked. By default it does nothing
     */
    protected void fileClicked(File file, AjaxRequestTarget target) {
        // do nothing, subclasses will override        
    }

    /**
     * Action undertaken as a directory is clicked. Default behavior is to drill down into
     * the directory.
     * @param file
     * @param target
     */
    protected void directoryClicked(File file, AjaxRequestTarget target) {
        // explicitly change the root model, inform the other components the model has changed
        GeoServerFileChooser.this.file.setObject(file);
        fileTable.getProvider().setDirectory(new Model(file));
        breadcrumbs.setSelection(file);
        
        target.addComponent(fileTable);
        target.addComponent(breadcrumbs);
    }

    private boolean isSubfile(File root, File selection) {
        if(selection == null || "".equals(selection.getPath()))
            return false;
        if(selection.equals(root))
            return true;
        
        return isSubfile(root, selection.getParentFile());
    }
    
    /**
     * 
     * @param fileFilter
     */
    public void setFilter(IModel<? extends FileFilter> fileFilter) {
        fileTable.provider.setFileFilter(fileFilter);
    }
    
    /**
     * Set the file table fixed height. Set it to null if you don't want fixed height
     * with overflow, and to a valid CSS measure if you want it instead.
     * Default value is "25em"
     * @param height
     */
    public void setFileTableHeight(String height) {
       fileTable.setTableHeight(height); 
    }
    
//    /**
//     * If the file is in the data directory builds a data dir relative path, otherwise
//     * returns an absolute path 
//     * @param file
//     * @return
//     */
//    public String getRelativePath(File file) {
//        File dataDirectory = GeoserverDataDirectory.getGeoserverDataDirectory();
//        if(isSubfile(dataDirectory, file)) {
//            File curr = file;
//            String path = null;
//            // paranoid check to avoid infinite loops
//            while(curr != null && !curr.equals(dataDirectory)){
//                if(path == null) {
//                    path = curr.getName();
//                } else {
//                    path = curr.getName() + "/" + path;
//                }
//                curr = curr.getParentFile();
//            } 
//            return "file:" + path; 
//        }
//        
//        return "file://" + file.getAbsolutePath();
//    }
//    
    class FileRootsRenderer implements IChoiceRenderer {

		public Object getDisplayValue(Object o) {
			File f = (File) o;
			
			if(f == USER_HOME) {
			    return new ParamResourceModel("userHome", GeoServerFileChooser.this).getString();
			} else if(f.equals(GeoserverDataDirectory.getGeoserverDataDirectory())) {
			    return new ParamResourceModel("dataDirectory", GeoServerFileChooser.this).getString();
			}
			
			try {
			    return FileSystemView.getFileSystemView().getSystemDisplayName(f);
			} catch(Exception e) {
			    // on windows we can get the occasional NPE due to 
			    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6973685
			    return f.getName();
			}
		}

		public String getIdValue(Object o, int count) {
			File f = (File) o;
			return "" + count;
		}
    	
    }
}
