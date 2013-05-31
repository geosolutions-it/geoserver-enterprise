package org.geoserver.security.decorators;

import java.io.IOException;

import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.wms.WebMapServer;
import org.opengis.util.ProgressListener;

public class SecuredWMSStoreInfo extends DecoratingWMSStoreInfo {

    WrapperPolicy policy;

    public SecuredWMSStoreInfo(WMSStoreInfo delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public WebMapServer getWebMapServer(ProgressListener listener) throws IOException {
        WebMapServer wms = super.getWebMapServer(null);
        if (wms == null)
            return null;
        else if(policy.level == AccessLevel.METADATA)
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        else
            return (WebMapServer) SecuredObjects.secure(wms, policy);
    }

}
