package org.geoserver.wms.capabilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.geoserver.catalog.LayerInfo;

/**
 * A Class to manage the WMS Layer structure
 * 
 * @author fabiania
 */
class LayerTree {
    private String name;

    private Collection<LayerTree> childrens;

    private Collection<LayerInfo> data;

    /**
     * @param name
     *            String
     */
    public LayerTree(String name) {
        this.name = name;
        this.childrens = new ArrayList<LayerTree>();
        this.data = new ArrayList<LayerInfo>();
    }

    /**
     * @param c
     *            Collection
     */
    public LayerTree(Collection<LayerInfo> c) {
        this.name = "";
        this.childrens = new ArrayList<LayerTree>();
        this.data = new ArrayList<LayerInfo>();

        for (Iterator<LayerInfo> it = c.iterator(); it.hasNext();) {
            LayerInfo layer = it.next();
            // ask for enabled() instead of isEnabled() to account for disabled resource/store
            if (layer.enabled()) {
                String wmsPath = layer.getPath() == null ? "" : layer.getPath();

                if (wmsPath.startsWith("/")) {
                    wmsPath = wmsPath.substring(1, wmsPath.length());
                }

                String[] treeStructure = wmsPath.split("/");
                addToNode(this, treeStructure, layer);
            }
        }
    }

    /**
     * @param tree
     * @param treeStructure
     * @param layer
     */
    private void addToNode(LayerTree tree, String[] treeStructure, LayerInfo layer) {
        final int length = treeStructure.length;

        if ((length == 0) || (treeStructure[0].length() == 0)) {
            tree.data.add(layer);
        } else {
            LayerTree node = tree.getNode(treeStructure[0]);

            if (node == null) {
                node = new LayerTree(treeStructure[0]);
                tree.childrens.add(node);
            }

            String[] subTreeStructure = new String[length - 1];
            System.arraycopy(treeStructure, 1, subTreeStructure, 0, length - 1);
            addToNode(node, subTreeStructure, layer);
        }
    }

    /**
     * @param string
     * @return
     */
    public LayerTree getNode(String name) {
        for (LayerTree tmpNode : this.childrens) {
            if (tmpNode.name.equals(name)) {
                return tmpNode;
            }
        }

        return null;
    }

    public Collection<LayerTree> getChildrens() {
        return childrens;
    }

    public Collection<LayerInfo> getData() {
        return data;
    }

    public String getName() {
        return name;
    }
}