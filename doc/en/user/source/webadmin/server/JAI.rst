.. _JAI:

JAI
===
`Java Advanced Imaging <http://java.sun.com/javase/technologies/desktop/media/jai/>`_ (JAI) is an image manipulation library built by Sun Microsystems and distributed with an open source license.
`JAI Image I/O Tools <https://jai-imageio.dev.java.net/>`_ provides reader, writer, and stream plug-ins for the standard Java Image I/O Framework. 
Several JAI parameters, used by both WMS and WCS operations, can be configured in the JAI Settings page. 

.. figure:: ../images/server_JAI.png
   :align: center
   
   *JAI Settings*
   
Memory & Tiling 
---------------

When supporting large images it is efficient to work on image subsets without loading everything to memory. A widely used approach is tiling which basically builds a tessellation of the original image so that image data can be read in parts rather than whole. Since very often processing one tile involves surrounding tiles, tiling needs to be accompanied by a tile-caching mechanism. The following JAI parameters allow you to manage the JAI cache mechanism for optimized performance.   

**Memory Capacity**—For memory allocation for tiles, JAI provides an interface called TileCache. Memory Capacity sets the global JAI TileCache as a percentage of the available heap. A number between 0 and 1 exclusive. If the Memory Capacity is smaller than the current capacity, the tiles in the cache are flushed to achieve the desired settings. If you set a large amount of memory for the tile cache, interactive operations are faster but the tile cache fills up very quickly. If you set a low amount of memory for the tile cache, the performance degrades.

**Memory Threshold**—Sets the global JAI TileCache Memory threshold. Refers to the fractional amount of cache memory to retain during tile removal. JAI Memory Threshold value must be between 0.0 and 1.0. The Memory Threshold visible on the :ref:`status` page. 

**Tile Threads**—JAI utilizes a TileScheduler for tile calculation. Tile computation may make use of multithreading for improved performance. The Tile Threads parameter sets the TileScheduler, indicating the number of threads to be used when loading tiles. 
 
**Tile Threads Priority**—Sets the global JAI Tile Scheduler thread priorities. Values range from 1 (Min) to 10 (Max), with default priority set to 5 (Normal).

**Tile Recycling**—Enable/Disable JAI Cache Tile Recycling. If selected, Tile Recycling allows JAI to re-use already loaded tiles, with vital capability for performances. 

**Native Acceleration**—To improve the computation speed of image processing applications, the JAI comes with both Java Code and native code for many platform. If the Java Virtual Machine (JVM) finds the native code, then that will be used. If the native code is not available, the Java code will be used. As such, the JAI package is able to provide optimized implementations for different platforms that can take advantage of each platform's capabilities.    

**JPEG Native Acceleration**—Enables/disable JAI JPEG Native Acceleration. When selected, enables JPEG native code, which may speed performance, but compromise security and crash protection. 

**PNG Native Acceleration**—Enables/disables JAI PNG Native Acceleration. When selected, enables PNG native code, which may speed performance, but compromise security and crash protection. 

**Mosaic Native Acceleration**—To reduce the overhead of handling them, large data sets are often split into smaller chunks and then combined to create an image mosaic. An example of this is aerial imagery which is usually comprises thousands of small images at very high resolution. Both native and JAI implementations of mosaic are provided. When selected, Mosaic Native Acceleration use the native implementation for creating mosaics. 