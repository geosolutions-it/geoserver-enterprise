package org.geoserver.security.password;

import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;

public class GeoserverTestPasswordEncoder extends  AbstractGeoserverPasswordEncoder{

    @Override
    public String getPrefix() {
        return "plain4711";
    }

    @Override
    protected PasswordEncoder createStringEncoder() {
        return new PlaintextPasswordEncoder();
    }

    @Override
    protected CharArrayPasswordEncoder createCharEncoder() {
        return new CharArrayPasswordEncoder() {
            
            @Override
            public boolean isPasswordValid(String encPass, char[] rawPass, Object salt) {
                return encPass.equals(new String(rawPass));
            }
            
            @Override
            public String encodePassword(char[] rawPass, Object salt) {
                return new String(rawPass);
            }
        };
    }
    
    @Override
    public PasswordEncodingType getEncodingType() {
        return PasswordEncodingType.PLAIN;
    }

}
