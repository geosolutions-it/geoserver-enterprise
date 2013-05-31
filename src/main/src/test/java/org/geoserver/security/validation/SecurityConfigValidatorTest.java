package org.geoserver.security.validation;

import static org.geoserver.security.validation.SecurityConfigException.*;

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.MemoryRoleService;
import org.geoserver.security.impl.MemoryUserGroupService;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geotools.util.logging.Logging;

public class SecurityConfigValidatorTest extends GeoServerSecurityTestSupport {

    
    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");
        
    public void testMasterConfigValidation() throws Exception{
        SecurityManagerConfig config = new SecurityManagerConfig();
        config.setRoleServiceName(XMLRoleService.DEFAULT_NAME);
        config.setConfigPasswordEncrypterName(getPBEPasswordEncoder().getName());
        config.getAuthProviderNames().add(GeoServerAuthenticationProvider.DEFAULT_NAME);
        
        getSecurityManager().saveSecurityConfig(config);
        
                        
        boolean failed = false;
        try {
            config.setConfigPasswordEncrypterName("abc");
            getSecurityManager().saveSecurityConfig(config);
        } catch (SecurityConfigException ex){
            assertEquals(INVALID_PASSWORD_ENCODER_$1,ex.getId());
            LOGGER.info(ex.getMessage());
            failed=true;
        }
        assertTrue(failed);
        
        failed = false;
        try {
            config.setConfigPasswordEncrypterName(null);
            getSecurityManager().saveSecurityConfig(config);
        } catch (SecurityConfigException ex){
            assertEquals(PASSWORD_ENCODER_REQUIRED,ex.getId());
            LOGGER.info(ex.getMessage());
            failed=true;
        }
        assertTrue(failed);

        
        if (getSecurityManager().isStrongEncryptionAvailable()==false) {
            config.setConfigPasswordEncrypterName(getStrongPBEPasswordEncoder().getName());
            failed = false;
            try {
                getSecurityManager().saveSecurityConfig(config);
            } catch (SecurityConfigException ex){
                assertEquals(INVALID_STRONG_CONFIG_PASSWORD_ENCODER,ex.getId());
                LOGGER.info(ex.getMessage());
                failed=true;
            }
            assertTrue(failed);
        }

                
        config.setConfigPasswordEncrypterName(getPBEPasswordEncoder().getName());
        config.setRoleServiceName("XX");
        
        failed = false;
        try {
            getSecurityManager().saveSecurityConfig(config);
        } catch (SecurityConfigException ex){
            assertEquals(ROLE_SERVICE_NOT_FOUND_$1,ex.getId());
            LOGGER.info(ex.getMessage());
            failed=true;
        }
        assertTrue(failed);
        
        config.setRoleServiceName(null);        
        failed = false;
        try {
            getSecurityManager().saveSecurityConfig(config);
        } catch (SecurityConfigException ex){
            assertEquals(ROLE_SERVICE_NOT_FOUND_$1,ex.getId());
            LOGGER.info(ex.getMessage());
            failed=true;
        }
        assertTrue(failed);


        config.setRoleServiceName(XMLRoleService.DEFAULT_NAME);
        config.getAuthProviderNames().add("XX");
        
        failed = false;
        try {
            getSecurityManager().saveSecurityConfig(config);
        } catch (SecurityConfigException ex){
            assertEquals(AUTH_PROVIDER_NOT_FOUND_$1,ex.getId());
            LOGGER.info(ex.getMessage());
            failed=true;
        }
        assertTrue(failed);
        config.getAuthProviderNames().remove("XX");
                
    }
    
    public void testNamedServices() {
        boolean fail;
        SecurityConfigValidator validator = new SecurityConfigValidator(getSecurityManager()); 
        Class<?>[] extensionPoints = new Class<?>[] {
                GeoServerUserGroupService.class,
                GeoServerRoleService.class,
                PasswordValidator.class,
                GeoServerAuthenticationProvider.class,
                GeoServerSecurityFilter.class                
        };
        
        for (Class<?> ep : extensionPoints) {
           fail=false;
           try {
               validator.checkExtensionPont(ep, "a.b.c");
           } catch (SecurityConfigException ex) {
               assertEquals(ex.getId(), CLASS_NOT_FOUND_$1);
               assertEquals(ex.getArgs()[0],"a.b.c");
               LOGGER.info(ex.getMessage());
               fail=true;               
           }
           assertTrue(fail);
           fail=false;
           try {
               validator.checkExtensionPont(ep, "java.lang.String");
           } catch (SecurityConfigException ex) {
               assertEquals(ex.getId(), CLASS_WRONG_TYPE_$2);
               assertEquals(ex.getArgs()[0],ep);
               assertEquals(ex.getArgs()[1],"java.lang.String");
               LOGGER.info(ex.getMessage());
               fail=true;               
           }
           assertTrue(fail);
           
           fail=false;
           String className = ep == GeoServerUserGroupService.class ? null : "";
           try {               
               validator.checkExtensionPont(ep, className);
           } catch (SecurityConfigException ex) {
               assertEquals(ex.getId(), CLASSNAME_REQUIRED);
               assertEquals(0,ex.getArgs().length);
               LOGGER.info(ex.getMessage());
               fail=true;               
           }
           assertTrue(fail);

           fail=false;
           String name = ep == GeoServerUserGroupService.class ? null : "";
           try {               
               validator.checkServiceName(ep, name);
           } catch (SecurityConfigException ex) {
               assertEquals(ex.getId(), NAME_REQUIRED);
               assertEquals(0,ex.getArgs().length);
               LOGGER.info(ex.getMessage());
               fail=true;               
           }
           assertTrue(fail);
           
           
           
        }

        // test names
        fail=false;
        try {
            validator.validateAddPasswordPolicy(
                getPolicyConfig(PasswordValidator.DEFAULT_NAME, PasswordValidatorImpl.class, 1,10));
        } catch (SecurityConfigException ex) {
            assertEquals(PASSWD_POLICY_ALREADY_EXISTS_$1, ex.getId());
            assertEquals(ex.getArgs()[0],PasswordValidator.DEFAULT_NAME);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        PasswordPolicyConfig pwConfig = getPolicyConfig("default2", PasswordValidatorImpl.class, 1,10);
        fail=false;
        try {
            validator.validateModifiedPasswordPolicy(pwConfig,pwConfig);
        } catch (SecurityConfigException ex) {
            assertEquals(PASSWD_POLICY_NOT_FOUND_$1, ex.getId());
            assertEquals(ex.getArgs()[0],"default2");
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            validator.validateAddUserGroupService(
                getUGConfig(XMLUserGroupService.DEFAULT_NAME, GeoServerUserGroupService.class, 
                    getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME));
        } catch (SecurityConfigException ex) {
            assertEquals(ex.getId(), USERGROUP_SERVICE_ALREADY_EXISTS_$1);
            assertEquals(ex.getArgs()[0],XMLUserGroupService.DEFAULT_NAME);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        SecurityUserGroupServiceConfig ugConfig =  
                getUGConfig("default2", GeoServerUserGroupService.class, 
                getPlainTextPasswordEncoder().getName(), PasswordValidator.DEFAULT_NAME); 
        fail=false;
        try {
            validator.validateModifiedUserGroupService(ugConfig,ugConfig);
        } catch (SecurityConfigException ex) {
            assertEquals(ex.getId(), USERGROUP_SERVICE_NOT_FOUND_$1);
            assertEquals(ex.getArgs()[0],"default2");
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            validator.validateAddRoleService(
                getRoleConfig(XMLRoleService.DEFAULT_NAME, GeoServerRoleService.class, 
                        GeoServerRole.ADMIN_ROLE.getAuthority()));
        } catch (SecurityConfigException ex) {
            assertEquals(ex.getId(), ROLE_SERVICE_ALREADY_EXISTS_$1);
            assertEquals(ex.getArgs()[0],XMLRoleService.DEFAULT_NAME);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        SecurityRoleServiceConfig config = getRoleConfig("default2", GeoServerRoleService.class, 
                GeoServerRole.ADMIN_ROLE.getAuthority());
        try {
            validator.validateModifiedRoleService(config,config);                    
        } catch (SecurityConfigException ex) {
            assertEquals(ex.getId(), ROLE_SERVICE_NOT_FOUND_$1);
            assertEquals(ex.getArgs()[0],"default2");
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        
        assertTrue(fail);
        fail=false;
        try {
            validator.validateAddAuthProvider(
                getAuthConfig(GeoServerAuthenticationProvider.DEFAULT_NAME, UsernamePasswordAuthenticationProvider.class,
                        XMLUserGroupService.DEFAULT_NAME));                         
        } catch (SecurityConfigException ex) {
            assertEquals(ex.getId(), AUTH_PROVIDER_ALREADY_EXISTS_$1);
            assertEquals(ex.getArgs()[0],GeoServerAuthenticationProvider.DEFAULT_NAME);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        SecurityAuthProviderConfig aConfig = getAuthConfig("default2", 
                UsernamePasswordAuthenticationProvider.class, XMLUserGroupService.DEFAULT_NAME);  
        try {
            validator.validateModifiedAuthProvider(aConfig,aConfig);
        } catch (SecurityConfigException ex) {
            assertEquals(ex.getId(), AUTH_PROVIDER_NOT_FOUND_$1);
            assertEquals(ex.getArgs()[0],"default2");
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
    }
        
    protected SecurityAuthProviderConfig getAuthConfig(String name, Class<?> aClass,String userGroupServiceName) {
        SecurityAuthProviderConfig config = new UsernamePasswordAuthenticationProviderConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setUserGroupServiceName(userGroupServiceName);
        return config;
    }
    
    protected SecurityUserGroupServiceConfig getUGConfig(String name, Class<?> aClass,
            String encoder, String policyName) {
        MemoryUserGroupServiceConfigImpl config = new MemoryUserGroupServiceConfigImpl();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setPasswordEncoderName(encoder);
        config.setPasswordPolicyName(policyName);
        return config;
    }
    
    protected SecurityRoleServiceConfig getRoleConfig(String name, Class<?> aClass,String adminRole) {
        SecurityRoleServiceConfig config = new MemoryRoleServiceConfigImpl();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setAdminRoleName(adminRole);
        return config;
    }

    protected PasswordPolicyConfig getPolicyConfig(String name, Class<?> aClass,int min, int max) {
        PasswordPolicyConfig config = new PasswordPolicyConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setMinLength(min);
        config.setMaxLength(max);
        return config;
    }
    
    protected SecurityNamedServiceConfig getFilterConfig(String name, Class<?> aClass) {
        SecurityNamedServiceConfig config = new BaseSecurityNamedServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        return config;
    }

    
    public void testPasswordPolicy() throws IOException{
        
        PasswordPolicyConfig config = getPolicyConfig(PasswordValidator.DEFAULT_NAME, PasswordValidatorImpl.class, -1,10);
        boolean fail;
        
        fail=false;
        try {
            config.setName("default2");
            getSecurityManager().savePasswordPolicy(config);
        } catch (SecurityConfigException ex) {
            assertEquals( INVALID_MIN_LENGTH,ex.getId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            getSecurityManager().savePasswordPolicy(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( INVALID_MIN_LENGTH,ex.getId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setMinLength(1);
        config.setMaxLength(0);
        
        fail=false;
        try {
            getSecurityManager().savePasswordPolicy(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( INVALID_MAX_LENGTH,ex.getId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            getSecurityManager().savePasswordPolicy(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( INVALID_MAX_LENGTH,ex.getId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setMaxLength(-1);
        
        
        fail=false;
        try {
            config.setName("");
            getSecurityManager().removePasswordValidator(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( NAME_REQUIRED,ex.getId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        
        fail=false;
        try {
            config.setName(PasswordValidator.DEFAULT_NAME);
            getSecurityManager().removePasswordValidator(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( PASSWD_POLICY_ACTIVE_$2,ex.getId());
            assertEquals(PasswordValidator.DEFAULT_NAME,ex.getArgs()[0]);
            assertEquals(XMLUserGroupService.DEFAULT_NAME,ex.getArgs()[1]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        
        fail=false;
        try {
            config.setName(PasswordValidator.MASTERPASSWORD_NAME);
            getSecurityManager().removePasswordValidator(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( PASSWD_POLICY_MASTER_DELETE,ex.getId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        
    }

    public void testRoleConfig() throws IOException {
        
        SecurityRoleServiceConfig config = getRoleConfig(XMLRoleService.DEFAULT_NAME, MemoryRoleService.class, 
                GeoServerRole.ADMIN_ROLE.getAuthority());
        boolean fail;

        
//        fail=false;
//        try {
//            config.setName("default2");
//            config.setAdminRoleName("adminrole");
//            getSecurityManager().saveRoleService(config, true);                         
//        } catch (SecurityConfigException ex) {
//            assertEquals( SEC_ERR_50,ex.getErrorId());
//            assertEquals(GeoserverRole.ADMIN_ROLE.getAuthority(),ex.getArgs()[0]);
//            LOGGER.info(ex.getMessage());
//            fail=true;
//        }
//        assertTrue(fail);
//
//        fail=false;
//        try {
//            config.setName(XMLRoleService.DEFAULT_NAME);
//            config.setAdminRoleName("adminrole");
//            getSecurityManager().saveRoleService(config, false);                         
//        } catch (SecurityConfigException ex) {
//            assertEquals( SEC_ERR_50,ex.getErrorId());
//            assertEquals(GeoserverRole.ADMIN_ROLE.getAuthority(),ex.getArgs()[0]);
//            LOGGER.info(ex.getMessage());
//            fail=true;
//        }
//        assertTrue(fail);

        
        fail=false;
        try {
            config.setName(null);
            getSecurityManager().removeRoleService(config) ;                        
        } catch (SecurityConfigException ex) {
            assertEquals( NAME_REQUIRED,ex.getId());
            assertEquals(0,ex.getArgs().length);            
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setName("abcd");
        for (GeoServerRole role : GeoServerRole.SystemRoles) {
            fail=false;
            config.setAdminRoleName(role.getAuthority());
            try {                            	
                getSecurityManager().saveRoleService(config);                        
            } catch (SecurityConfigException ex) {
                assertEquals( RESERVED_ROLE_NAME,ex.getId());
                assertEquals(role.getAuthority(),ex.getArgs()[0]);            
                LOGGER.info(ex.getMessage());
                fail=true;
            }
            assertTrue(fail);        	
        }
        
        for (GeoServerRole role : GeoServerRole.SystemRoles) {
            fail=false;
            config.setGroupAdminRoleName(role.getAuthority());
            try {                            	
                getSecurityManager().saveRoleService(config);                        
            } catch (SecurityConfigException ex) {
                assertEquals( RESERVED_ROLE_NAME,ex.getId());
                assertEquals(role.getAuthority(),ex.getArgs()[0]);            
                LOGGER.info(ex.getMessage());
                fail=true;
            }
            assertTrue(fail);        	
        }
                
        fail=false;
        try {
            config.setName(XMLRoleService.DEFAULT_NAME);
            getSecurityManager().removeRoleService(config) ;                         
        } catch (SecurityConfigException ex) {
            assertEquals( ROLE_SERVICE_ACTIVE_$1,ex.getId());
            assertEquals(XMLRoleService.DEFAULT_NAME,ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        

        
    }

    
    public void testAuthenticationProvider() throws IOException {
        
        SecurityAuthProviderConfig config = getAuthConfig(GeoServerAuthenticationProvider.DEFAULT_NAME, 
                UsernamePasswordAuthenticationProvider.class, "default2");
        boolean fail;
        
        fail=false;
        try {
            config.setName("default2");            
            getSecurityManager().saveAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assertEquals( USERGROUP_SERVICE_NOT_FOUND_$1,ex.getId());
            assertEquals("default2",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            config.setName("other");
            getSecurityManager().saveAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assertEquals( USERGROUP_SERVICE_NOT_FOUND_$1,ex.getId());
            assertEquals("default2",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
                        
        fail=false;
        try {
            config.setName("");
            getSecurityManager().removeAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assertEquals( NAME_REQUIRED,ex.getId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        
        
        fail=false;
        try {
            config.setName(GeoServerAuthenticationProvider.DEFAULT_NAME);
            getSecurityManager().removeAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assertEquals( AUTH_PROVIDER_ACTIVE_$1,ex.getId());
            assertEquals(GeoServerAuthenticationProvider.DEFAULT_NAME,ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
    }


    public void testUserGroupConfig() throws IOException {
        
        SecurityUserGroupServiceConfig config = getUGConfig(XMLUserGroupService.DEFAULT_NAME, MemoryUserGroupService.class, 
            getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME);
        boolean fail;
        
        fail=false;
        try {
            config.setName("default2");
            config.setPasswordEncoderName("xxx");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( INVALID_CONFIG_PASSWORD_ENCODER_$1,ex.getId());
            assertEquals("xxx",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        if (getSecurityManager().isStrongEncryptionAvailable()==false) {
            config.setPasswordEncoderName(getStrongPBEPasswordEncoder().getName());
            fail = false;
            try {
                getSecurityManager().saveUserGroupService(config);
            } catch (SecurityConfigException ex){
                assertEquals(INVALID_STRONG_PASSWORD_ENCODER,ex.getId());
                LOGGER.info(ex.getMessage());
                fail=true;
            }
            assertTrue(fail);
        }

        
        fail=false;
        try {
            config.setName("other");
            config.setPasswordEncoderName("xxx");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( INVALID_CONFIG_PASSWORD_ENCODER_$1,ex.getId());
            assertEquals("xxx",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            config.setName("default2");
            config.setPasswordEncoderName("");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( PASSWD_ENCODER_REQUIRED_$1,ex.getId());
            assertEquals("default2",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            config.setName("default3");
            config.setPasswordEncoderName(null);
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( PASSWD_ENCODER_REQUIRED_$1,ex.getId());
            assertEquals("default3", ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        

        config.setPasswordEncoderName(getPlainTextPasswordEncoder().getName());
        
        fail=false;
        try {
            config.setName("default2");
            config.setPasswordPolicyName("default2");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( PASSWD_POLICY_NOT_FOUND_$1,ex.getId());
            assertEquals("default2",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            config.setName("default3");
            config.setPasswordPolicyName("default2");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( PASSWD_POLICY_NOT_FOUND_$1,ex.getId());
            assertEquals("default2",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            config.setName("default2");
            config.setPasswordPolicyName("");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( PASSWD_POLICY_REQUIRED_$1,ex.getId());
            assertEquals("default2",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {
            config.setName("default3");
            config.setPasswordPolicyName(null);
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( PASSWD_POLICY_REQUIRED_$1,ex.getId());
            assertEquals("default3",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);


                
        fail=false;
        try {
            config.setName(null);
            getSecurityManager().removeUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( NAME_REQUIRED,ex.getId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        
        fail=false;
        try {
            config.setName(XMLUserGroupService.DEFAULT_NAME);
            getSecurityManager().removeUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( USERGROUP_SERVICE_ACTIVE_$2,ex.getId());
            assertEquals(XMLUserGroupService.DEFAULT_NAME,ex.getArgs()[0]);
            assertEquals(GeoServerAuthenticationProvider.DEFAULT_NAME,ex.getArgs()[1]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
    }

}
