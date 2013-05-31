/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Extension point for validation rules for catalog objects.
 * 
 * @author David Winslow, OpenGeo
 *
 */
public interface CatalogValidator {
    /**
     * Validate a resource.
     *
     * @param store the Resourceinfo to be validated
     * @param isNew a boolean; if true then the resource is not expected to
     *     already exist in the catalog.
     *
     * @throws RuntimeError if validation fails
     */
    void validate(ResourceInfo resource, boolean isNew);

    /**
     * Validate a store.
     *
     * @param store the StoreInfo to be validated
     * @param isNew a boolean; if true then the store is not expected to
     *     already exist in the catalog.
     * 
     * @throws RuntimeError if validation fails
     */
    void validate(StoreInfo store, boolean isNew);

    /**
     * Validate a workspace.
     *
     * @param store the WorkspaceInfo to be validated
     * @param isNew a boolean; if true then the workspace is not expected to
     *     already exist in the catalog.
     * 
     * @throws RuntimeError if validation fails
     */
    void validate(WorkspaceInfo workspace, boolean isNew);

    /**
     * Validate a layer.
     *
     * @param layer the LayerInfo to be validated
     * @param isNew a boolean; if true then the layer is not expected to
     *     already exist in the catalog.
     * 
     * @throws RuntimeError if validation fails
     */
    void validate(LayerInfo layer, boolean isNew);

    /**
     * Validate a style.
     *
     * @param style the StyleInfo to be validated
     * @param isNew a boolean; if true then the style is not expected to
     *     already exist in the catalog.
     * 
     * @throws RuntimeError if validation fails
     */
    void validate(StyleInfo style, boolean isNew);

    /**
     * Validate a layergroup.
     *
     * @param layerGroup the LayerGroupInfo to be validated
     * @param isNew a boolean; if true then the layergroup is not expected to
     *     already exist in the catalog.
     * 
     * @throws RuntimeError if validation fails
     */
    void validate(LayerGroupInfo layerGroup, boolean isNew);

    /**
     * Validate a namespace.
     *
     * @param namespace the NamespaceInfo to be validated
     * @param isNew a boolean; if true then the layer is not expected to
     *     already exist in the catalog
     * 
     * @throws RuntimeError if validation fails
     */
    void validate(NamespaceInfo namespace, boolean isNew);
}
