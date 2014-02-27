package org.geoserver.data.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.geoserver.config.util.XStreamPersister;
import org.geotools.util.logging.Logging;

/**
 * Utility class for IO related utilities
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class IOUtils {
    private static final Logger LOGGER = Logging.getLogger(IOUtils.class);
    
    private IOUtils() {
        // singleton
    }

    /**
     * Copies the provided input stream onto a file
     * 
     * @param from
     * @param to
     * @throws IOException
     */
    public static void copy(InputStream from, File to) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(to);

            byte[] buffer = new byte[1024 * 16];
            int bytes = 0;
            while ((bytes = from.read(buffer)) != -1)
                out.write(buffer, 0, bytes);

            out.flush();
        } finally {
            if(from != null) {
                from.close();
            }
            if(out != null) {
                out.close();
            }
        }
    }

    /**
     * Copies from a file to another by performing a filtering on certain
     * specified tokens. In particular, each key in the filters map will be
     * looked up in the reader as ${key} and replaced with the associated value.
     * @param to
     * @param filters
     * @param reader
     * 
     * @throws IOException
     */
    public static void filteredCopy(File from, File to, Map<String, String> filters)
            throws IOException {
        filteredCopy(new BufferedReader(new FileReader(from)), to, filters);
    }

    /**
     * Copies from a reader to a file by performing a filtering on certain
     * specified tokens. In particular, each key in the filters map will be
     * looked up in the reader as ${key} and replaced with the associated value.
     * @param to
     * @param filters
     * @param reader
     * 
     * @throws IOException
     */
    public static void filteredCopy(BufferedReader from, File to, Map<String, String> filters)
            throws IOException {
        BufferedWriter out = null;
        // prepare the escaped ${key} keys so that it won't be necessary to do
        // it over and over
        // while parsing the file
        Map<String, String> escapedMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            escapedMap.put("${" + entry.getKey() + "}", entry.getValue());
        }
        try {
            out = new BufferedWriter(new FileWriter(to));

            String line = null;
            while ((line = from.readLine()) != null) {
                for (Map.Entry<String, String> entry : escapedMap.entrySet()) {
                    line = line.replace(entry.getKey(), entry.getValue());
                }
                out.write(line);
                out.newLine();
            }
            out.flush();
        } finally {
            from.close();
            out.close();
        }
    }

    /**
     * Copies the provided file onto the specified destination file
     * 
     * @param from
     * @param to
     * @throws IOException
     */
    public static void copy(File from, File to) throws IOException {
        copy(new FileInputStream(from), to);
    }

    /**
     * Copy the contents of fromDir into toDir (if the latter is missing it will
     * be created)
     * 
     * @param fromDir
     * @param toDir
     * @throws IOException
     */
    public static void deepCopy(File fromDir, File toDir) throws IOException {
        if (!fromDir.isDirectory() || !fromDir.exists())
            throw new IllegalArgumentException("Invalid source directory "
                    + "(it's either not a directory, or does not exist");
        if (toDir.exists() && toDir.isFile())
            throw new IllegalArgumentException("Invalid destination directory, "
                    + "it happens to be a file instead");

        // create destination if not available
        if (!toDir.exists())
            if (!toDir.mkdir())
                throw new IOException("Could not create " + toDir);

        File[] files = fromDir.listFiles();
        for (File file : files) {
            File destination = new File(toDir, file.getName());
            if (file.isDirectory())
                deepCopy(file, destination);
            else
                copy(file, destination);
        }
    }

    /**
     * Creates a directory as a child of baseDir. The directory name will be
     * preceded by prefix and followed by suffix
     * 
     * @param basePath
     * @param prefix
     * @return
     * @throws IOException
     */
    public static File createRandomDirectory(String baseDir, String prefix, String suffix)
            throws IOException {
        File tempDir = File.createTempFile(prefix, suffix, new File(baseDir));
        tempDir.delete();
        if (!tempDir.mkdir())
            throw new IOException("Could not create the temp directory " + tempDir.getPath());
        return tempDir;
    }
    
    /**
     * Creates a temporary directory whose name will start by prefix
     *
     * Strategy is to leverage the system temp directory, then create a sub-directory.
     * @return
     */
    public static File createTempDirectory(String prefix) throws IOException {
        File dummyTemp = File.createTempFile("blah", null);
        String sysTempDir = dummyTemp.getParentFile().getAbsolutePath();
        dummyTemp.delete();

        File reqTempDir = new File(sysTempDir + File.separator + prefix + Math.random());
        reqTempDir.mkdir();

        return reqTempDir;
    }

    /**
     * Recursively deletes the contents of the specified directory, 
     * and finally wipes out the directory itself. For each
     * file that cannot be deleted a warning log will be issued. 
     * 
     * @param dir
     * @throws IOException
     * @returns true if the directory could be deleted, false otherwise
     */
    public static boolean delete(File directory) throws IOException {
        emptyDirectory(directory);
        return directory.delete();
    }

    /**
     * Recursively deletes the contents of the specified directory 
     * (but not the directory itself). For each
     * file that cannot be deleted a warning log will be issued.
     * 
     * @param dir
     * @throws IOException
     * @returns true if all the directory contents could be deleted, false otherwise
     */
    public static boolean emptyDirectory(File directory) throws IOException {
        if (!directory.isDirectory())
            throw new IllegalArgumentException(directory
                    + " does not appear to be a directory at all...");

        boolean allClean = true;
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                allClean &= delete(files[i]);
            } else {
                if (!files[i].delete()) {
                    LOGGER.log(Level.WARNING, "Could not delete {0}", files[i].getAbsolutePath());
                    allClean = false;
                }
            }
        }
        
        return allClean;
    }
    
    /**
     * Zips up the directory contents into the specified {@link ZipOutputStream}.
     * <p>
     * Note this method does not take ownership of the provided zip output stream, meaning the
     * client code is responsible for calling {@link ZipOutputStream#finish() finish()} when it's
     * done adding zip entries.
     * </p>
     *
     * @param directory
     *            The directory whose contents have to be zipped up
     * @param zipout
     *            The {@link ZipOutputStream} that will be populated by the files found
     * @param filter
     *            An optional filter that can be used to select only certain files. Can be null, in
     *            that case all files in the directory will be zipped
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static void zipDirectory(File directory, ZipOutputStream zipout, final FilenameFilter filter)
            throws IOException, FileNotFoundException {
        zipDirectory(directory, "", zipout, filter);
    }
    
    /**
     * See {@link #zipDirectory(File, ZipOutputStream, FilenameFilter)}, this version handles the prefix needed
     * to recursively zip data preserving the relative path of each
     */
    private static void zipDirectory(File directory, String prefix, ZipOutputStream zipout, final FilenameFilter filter)
            throws IOException, FileNotFoundException {
        File[] files = directory.listFiles(filter);
        // copy file by reading 4k at a time (faster than buffered reading)
        byte[] buffer = new byte[4 * 1024];
        for (File file : files) {
            if (file.exists()) {
                if(file.isDirectory()) {
                    // recurse and append
                    zipDirectory(file, prefix + file.getName() + "/", zipout, filter);
                } else {
                    ZipEntry entry = new ZipEntry(prefix  + file.getName());
                    zipout.putNextEntry(entry);
   
                    InputStream in = new FileInputStream(file);
                    int c;
                    try {
                        while (-1 != (c = in.read(buffer))) {
                            zipout.write(buffer, 0, c);
                        }
                        zipout.closeEntry();
                    } finally {
                        in.close();
                    }
                }
            }
        }
        zipout.flush();
    }
    
    public static void decompress(InputStream input, File destDir) throws IOException {
        ZipInputStream zin = new ZipInputStream(input);
        ZipEntry entry = null;
        
        byte[] buffer = new byte[1024];
        while((entry = zin.getNextEntry()) != null) {
            File f = new File(destDir, entry.getName()); 
            if (entry.isDirectory()) {
                f.mkdirs();
                continue;
            }
            
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
            
            int n = -1;
            while((n = zin.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            
            out.flush();
            out.close();
        }
    }
    
    public static void decompress(final File inputFile, final File destDir)
    throws IOException {
        ZipFile zipFile = new ZipFile(inputFile);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while(entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry)entries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);

            if(entry.isDirectory()) {
                // Assume directories are stored parents first then children.
                (new File(destDir, entry.getName())).mkdir();
                continue;
            }

            File newFile = new File(destDir, entry.getName());
            FileOutputStream fos = new FileOutputStream(newFile);
            try {
                byte[] buf = new byte[1024];
                int len;

                while((len = stream.read(buf)) >= 0)
                    saveCompressedStream(buf, fos, len);

            } catch (IOException e) {
                zipFile.close();
                IOException ioe = new IOException("Not valid COAMPS archive file type.");
                ioe.initCause(e);
                throw ioe;
            } finally {
                fos.flush();
                fos.close();

                stream.close();
            }
        }
        zipFile.close();
    }
    
    /**
     * @param len 
     * @param stream
     * @param fos
     * @return 
     * @throws IOException
     */
    public static void saveCompressedStream(final byte[] buffer, final OutputStream out, final int len) throws IOException {
        try {
            out.write(buffer, 0, len);

        } catch (Exception e) {
            out.flush();
            out.close();
            IOException ioe = new IOException("Not valid archive file type.");
            ioe.initCause(e);
            throw ioe;
        }
    }

    /**
     * Performs serialization with an {@link XStreamPersister} in a safe manner in which a temp
     * file is used for the serialization so that the true destination file is not partially 
     * written in the case of an error. 
     * 
     * @param f The file to write to, only modified if the temp file serialization was error free.
     * @param obj The object to serialize.
     * @param xp The persister.
     * 
     * @throws Exception
     */
    public static void xStreamPersist(File f, Object obj, XStreamPersister xp) throws IOException {
        //first save to a temp file
        File temp = File.createTempFile(f.getName(),null);
        
        BufferedOutputStream out = null;
        try{
            out=new BufferedOutputStream( new FileOutputStream( temp ) );
            xp.save( obj, out );
            out.flush();
        } finally {
            if (out != null)
                org.apache.commons.io.IOUtils.closeQuietly(out);
        }
        
        //no errors, overwrite the original file
        rename(temp,f);
    }

    /**
     * Backs up a directory <tt>dir</tt> by creating a .bak next to it.
     *  
     * @param dir The directory to back up.
     */
    public static void backupDirectory(File dir) throws IOException {
        File bak = new File( dir.getCanonicalPath() + ".bak");
        if ( bak.exists() ) {
            FileUtils.deleteDirectory( bak );
        }
        dir.renameTo( bak );
    }

    /**
     * Renames a file.
     *  
     * @param f The file to rename.
     * @param newName The new name of the file.
     */
    public static void rename(File f, String newName) throws IOException {
        rename( f, new File( f.getParentFile(), newName ) );
    }
    
    /**
     * Renames a file.
     *  
     * @param source The file to rename.
     * @param dest The file to rename to. 
     */
    public static void rename( File source, File dest ) throws IOException {
        // same path? Do nothing
        if (source.getCanonicalPath().equalsIgnoreCase(dest.getCanonicalPath()))
            return;

        // windows needs special treatment, we cannot rename onto an existing file
        boolean win = System.getProperty("os.name").startsWith("Windows");
        if ( win && dest.exists() ) {
            // windows does not do atomic renames, and can not rename a file if the dest file
            // exists
            if (!dest.delete()) {
                throw new IOException("Could not delete: " + dest.getCanonicalPath());
            }
        }
        // make sure the rename actually succeeds
        if(!source.renameTo(dest)) {
            throw new IOException("Failed to rename " + source.getAbsolutePath() + " to " + dest.getAbsolutePath());
        }
    }
}
