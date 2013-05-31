.. _sld_reference_pointsymbolizer:

PointSymbolizer
===============

A **PointSymbolizer** styles features as **points**.  
Points are depicted as graphic symbols at a single location on the map.


Syntax
------

A ``<PointSymbolizer>`` contains an optional ``<Geometry>`` element,
and a required ``<Graphic>`` element specifying the point symbology.

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Geometry>``
     - No
     - Specifies the geometry to be rendered.
   * - ``<Graphic>``
     - Yes
     - Specifies the styling for the point symbol.

Geometry
^^^^^^^^

The ``<Geometry>`` element is optional.  
If present, it specifies the featuretype property from which to obtain the geometry to style
using a ``<PropertyName>`` element.
See also :ref:`geometry_transformations` for GeoServer extensions for specifying geometry.

Any kind of geometry may be styled with a ``<PointSymbolizer>``.
For non-point geometries, a representative point is used (such as the centroid of a line or polygon).


.. _sld_reference_graphic:

Graphic
^^^^^^^

Symbology is specified using a ``<Graphic>`` element. 
The point symbol is specified by either an ``<ExternalGraphic>`` or a ``<Mark>`` element. 
**External Graphics** are image files (in formats such as PNG or SVG) that contain the shape and color information defining how to render a symbol.
**Marks** are vector shapes whose stroke and fill are defined explicitly in the symbolizer.  

There are five possible sub-elements of the ``<Graphic>`` element.
One of ``<ExternalGraphic>`` or ``<Mark>`` must be specified; the others are optional.

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<ExternalGraphic>``
     - No (when using ``<Mark>``)
     - Specifies an image file to use as the symbol.  
   * - ``<Mark>``
     - No (when using ``<ExternalGraphic>``)
     - Specifies a common shape to use as the symbol.
   * - ``<Opacity>``
     - No
     - Determines the opacity (transparency) of the symbol.  
       Values range from ``0`` (completely transparent) to ``1`` (completely opaque).  
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is ``1`` (opaque).
   * - ``<Size>``
     - No 
     - Determines the size of the symbol, in pixels.  
       When used with an image file, this specifies the height of the image, with the width being scaled accordingly.
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
   * - ``<Rotation>``
     - No
     - Determines the rotation of the symbol, in degrees.  
       The rotation increases in the clockwise direction.  
       Negative values indicate counter-clockwise rotation. 
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is ``0``.

ExternalGraphic
^^^^^^^^^^^^^^^

**External Graphics** are image files (in formats such as PNG or SVG) that contain the shape and color information defining how to render a symbol.
For GeoServer extensions for specifying external graphics, see :ref:`pointsymbols`.

The ``<ExternalGraphic>`` element has the sub-elements:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<OnlineResource>``
     - Yes
     - The ``xlink:href`` attribute specifies the location of the image file.  
       The value can be either a URL or a local pathname relative to the SLD.
       Also requires the attribute ``xlink:type="simple"``.
   * - ``<Format>``
     - Yes
     - The MIME type of the image format.  
       Most standard web image formats are supported.  
       Common MIME types are ``image/png``, ``image/jpeg``, ``image.png``, and ``image/svg+xml``  

Mark
^^^^

**Marks** are predefined vector shapes identified by a well-known name.  
Their fill and stroke can be defined explicitly in the SLD.  
For GeoServer extensions for specifying mark symbols, see :ref:`pointsymbols`.

The ``<Mark>`` element has the sub-elements:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<WellKnownName>``
     - Yes
     - The name of the common shape.  
       Options are ``circle``, ``square``, ``triangle``, ``star``, ``cross``, or ``x``.  Default is ``square``.
   * - ``<Fill>``
     - No
     - Specifies how the symbol should be filled.  
       Some options are using ``<CssParameter name="fill">`` to specify a fill color, or using ``<GraphicFill>`` for a repeated graphic.
       See the ``PolygonSymbolizer`` :ref:`sld_reference_fill`  for the full syntax.
   * - ``<Stroke>``
     - No
     - Specifies how the symbol border should be drawn. 
       Some options are using ``<CssParameter name="stroke">`` to specify a stroke color, or using ``<GraphicStroke>`` for a repeated graphic.
       See the ``LineSymbolizer`` :ref:`sld_reference_stroke` for the full syntax.
   

Example
-------

The following symbolizer is taken from the :ref:`sld_cookbook_points` section in the :ref:`sld_cookbook`.

.. code-block:: xml 
   :linenos: 

    <PointSymbolizer>
      <Graphic>
        <Mark>
	  <WellKnownName>circle</WellKnownName>
          <Fill>
  	    <CssParameter name="fill">#FF0000</CssParameter>
  	  </Fill>
        </Mark>
        <Size>6</Size>
      </Graphic>
    </PointSymbolizer>

The symbolizer contains the required ``<Graphic>`` element.  
Inside this element is the ``<Mark>`` element and ``<Size>`` element, which are the minimum required element inside ``<Graphic>`` (when not using the ``<ExternalGraphic>`` element).  
The ``<Mark>`` element contains the ``<WellKnownName>`` element and a ``<Fill>`` element.  
No other element are required.  In summary, this example specifies the following:
   
#. Features will be rendered as points
#. Points will be rendered as circles
#. Circles will be rendered with a diameter of 6 pixels and filled with the color red

The next example uses an external graphic loaded from the file system:

.. code-block:: xml 
   :linenos: 

    <PointSymbolizer>
      <Graphic>
        <ExternalGraphic>
          <OnlineResource xlink:type="simple" 
                          xlink:href="file:///var/www/htdocs/sun.png" />
          <Format>image.png</Format>
        </ExternalGraphic>
      </Graphic>
    </PointSymbolizer>

For ``file://`` URLs, the file must be readable by the user the Geoserver process is running as. You can also use ``href://`` URLs to reference remote graphics. 

Further examples can be found in the :ref:`sld_cookbook_points` section of the :ref:`sld_cookbook`.


.. _sld_reference_parameter_expressions:

Using expressions in parameter values
-------------------------------------

Many SLD parameters allow their values to be of **mixed type**. 
This means that the element content can be:

* a constant string value,
* an OGC Filter expression,
* any combination of strings and expressions.

Using expressions in parameter values provides the ability to determine styling dynamically
on a per-feature basis,
by computing parameter values from feature properties. 
Using computed parameters is an alternative to using rules
in some situations, 
and may provide a more compact SLD document.

GeoServer also supports using substitution variables provided in WMS requests.
This is described in :ref:`sld_variable_substitution`.

