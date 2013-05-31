package org.geoserver.monitor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class InternalHostname {

    static String internalHostname;
    
    public static final String get() {
        if (internalHostname == null) {
            synchronized(InternalHostname.class) {
                if (internalHostname == null) {
                    InetAddress addr;
                    try {
                        addr = InetAddress.getLocalHost();
                        internalHostname = addr.getHostName();
                    } 
                    catch (UnknownHostException e) {
                        internalHostname = "unknown";
                    }
                }
            }
        }
        return internalHostname;
    }
}
