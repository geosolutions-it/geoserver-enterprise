<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<NamedLayer>
<Name>A Test Layer</Name>
<UserStyle>
  <Name>population</Name>
  <Title>Population in the United States</Title>
  <Abstract>A sample filter that filters the United States into three 
            categories of population, drawn in different colors</Abstract>
    <FeatureTypeStyle>
      <Rule>
        <Title>&gt; 4M</Title>
        <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
          <ogc:PropertyIsGreaterThanOrEqualTo>
           <ogc:PropertyName>PERSONS</ogc:PropertyName>
           <ogc:Literal>4000000</ogc:Literal>
          </ogc:PropertyIsGreaterThanOrEqualTo>
        </ogc:Filter>
        <PointSymbolizer>
           <Fill>
              <CssParameter name="fill">#4D4DFF</CssParameter>
              <CssParameter name="fill-opacity">0.7</CssParameter>
           </Fill>     
        </PointSymbolizer>
      </Rule>
      <Rule>
        <Title>2M - 4M</Title>
        <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
          <ogc:And>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>PERSONS</ogc:PropertyName>
              <ogc:Literal>4000000</ogc:Literal>
            </ogc:PropertyIsLessThan>
            <ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyName>PERSONS</ogc:PropertyName>
              <ogc:Literal>2000000</ogc:Literal>
            </ogc:PropertyIsGreaterThanOrEqualTo>
          </ogc:And>
        </ogc:Filter>
        <PointSymbolizer>
           <Fill>
              <CssParameter name="fill">#FF4D4D</CssParameter>
              <CssParameter name="fill-opacity">0.7</CssParameter>
           </Fill>     
        </PointSymbolizer>
      </Rule>
      <Rule>
        <Title>&lt; 2M</Title>
        <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
          <ogc:PropertyIsLessThan>
           <ogc:PropertyName>PERSONS</ogc:PropertyName>
           <ogc:Literal>2000000</ogc:Literal>
          </ogc:PropertyIsLessThan>
        </ogc:Filter>
        <PointSymbolizer>
           <Fill>
              <CssParameter name="fill">#4DFF4D</CssParameter>
              <CssParameter name="fill-opacity">0.7</CssParameter>
           </Fill>     
        </PointSymbolizer>
      </Rule>
    </FeatureTypeStyle>
</UserStyle>
</NamedLayer>
</StyledLayerDescriptor>

