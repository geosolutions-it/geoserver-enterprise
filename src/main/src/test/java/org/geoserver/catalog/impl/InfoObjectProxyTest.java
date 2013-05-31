package org.geoserver.catalog.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class InfoObjectProxyTest extends TestCase {

    public void test() throws Exception {
        BeanImpl bean = new BeanImpl();
        ModificationProxy handler = new ModificationProxy( bean ); 
    
        Class proxyClass = Proxy.getProxyClass(
            Bean.class.getClassLoader(), new Class[] { Bean.class } );
        
        Bean proxy = (Bean) proxyClass.
            getConstructor(new Class[] { InvocationHandler.class }).
            newInstance(new Object[] { handler });
    
        bean.setFoo( "one" );
        bean.setBar( 1 );
        
        proxy.setFoo( "two" );
        proxy.setBar( 2 );
        
        proxy.getScratch().add( "x" );
        proxy.getScratch().add( "y" );
        
        assertEquals( "one", bean.getFoo() );
        assertEquals( new Integer(1), bean.getBar() );
        assertTrue( bean.getScratch().isEmpty() );
        
        assertEquals( "two", proxy.getFoo() );
        assertEquals( new Integer(2), proxy.getBar() );
        assertEquals( 2, proxy.getScratch().size() );
        
        handler.commit();
        assertEquals( "two", bean.getFoo() );
        assertEquals( new Integer(2), bean.getBar() );
        assertEquals( 2, bean.getScratch().size() );
    }
    
    static interface Bean {
        
        String getFoo();
        
        void setFoo( String foo );
        
        Integer getBar();
        
        void setBar( Integer bar );
        
        List getScratch();
    }
    
    static class BeanImpl implements Bean {

        String foo;
        Integer bar;
        List scratch = new ArrayList();
        
        public String getFoo() {
            return foo;
        }
        
        public void setFoo(String foo) {
            this.foo = foo;
        }
        
        public Integer getBar() {
            return bar;
        }
        
        public void setBar(Integer bar) {
            this.bar = bar;
        }

        public List getScratch() {
            return scratch;
        }
    }
}
