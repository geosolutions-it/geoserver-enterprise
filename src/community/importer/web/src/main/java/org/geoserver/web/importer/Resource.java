/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.io.Serializable;

import org.apache.wicket.ResourceReference;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;


/**
 * The bean to be rendered in the layer chooser page
 *
 * TODO: this is a slightly modified copy of {@link org.geoserver.web.data.layer.Resource}, cannot
 * share code right now since GS 2.0 is undergoing freeze for the 2.0.1 release. Merge back changes
 * later
 *
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
public class Resource implements Comparable<Resource>, Serializable
{

    /**
     * The resource name
     */
    String name;

    /**
     * The namespace URI
     */
    String uri;

    /**
     * The layer type icon
     */
    ResourceReference icon;

    /**
     * The type of geometry in this resource
     */
    Class geometryType;

    /**
     * If this resource has already been published, or not
     */
    boolean published;

    public Resource(Name name)
    {
        super();
        this.name = name.getLocalPart();
        this.uri = name.getNamespaceURI();
    }

    public void setPublished(boolean published)
    {
        this.published = published;
    }

    public String getLocalName()
    {
        return name;
    }

    public Name getName()
    {
        return new NameImpl(uri, name);
    }

    public boolean isPublished()
    {
        return published;
    }

    public ResourceReference getIcon()
    {
        return icon;
    }

    public void setIcon(ResourceReference icon)
    {
        this.icon = icon;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        result = (prime * result) + ((uri == null) ? 0 : uri.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }

        Resource other = (Resource) obj;
        if (name == null)
        {
            if (other.name != null)
            {
                return false;
            }
        }
        else if (!name.equals(other.name))
        {
            return false;
        }
        if (uri == null)
        {
            if (other.uri != null)
            {
                return false;
            }
        }
        else if (!uri.equals(other.uri))
        {
            return false;
        }

        return true;
    }

    public int compareTo(Resource o)
    {
        // unpublished resources first
        if (published && !o.published)
        {
            return -1;
        }
        else if (!published && o.published)
        {
            return 1;
        }

        // the compare by local name, as it's unlikely the users will see the
        // namespace URI (and the prefix is not available in Name)
        return name.compareTo(o.name);
    }

    @Override
    public String toString()
    {
        return name + "(" + published + ")";
    }

    public Class getGeometryType()
    {
        return geometryType;
    }

    public void setGeometryType(Class geometryType)
    {
        this.geometryType = geometryType;
    }

}
