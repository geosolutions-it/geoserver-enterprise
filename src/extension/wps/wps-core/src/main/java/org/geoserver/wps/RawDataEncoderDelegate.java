/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.geoserver.wps.process.RawData;
import org.geotools.xml.EncoderDelegate;
import org.xml.sax.ContentHandler;

/**
 * Encodes objects as base64 binaries
 * 
 * @author Andrea Aime - OpenGeo
 */
public class RawDataEncoderDelegate implements EncoderDelegate {


    private RawData rawData;

    public RawDataEncoderDelegate(RawData rawData) {
        this.rawData = rawData;
    }

    public void encode(ContentHandler output) throws Exception {
        InputStream is = null;
        try {
            is = rawData.getInputStream();
            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                char[] chars;
                if (read == 4096) {
                    chars = new String(Base64.encodeBase64(buffer)).toCharArray();
                } else {
                    byte[] reducedBuffer = new byte[read];
                    System.arraycopy(buffer, 0, reducedBuffer, 0, read);
                    chars = new String(Base64.encodeBase64(reducedBuffer)).toCharArray();
                }

                output.characters(chars, 0, chars.length);
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public void encode(OutputStream os) throws IOException {
        IOUtils.copy(rawData.getInputStream(), os);
    }
}
