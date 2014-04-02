package it.geosolutions.geoserver.jms.impl.events;

import java.util.Iterator;
import java.util.List;

import org.geoserver.rest.DispatcherCallback;
import org.restlet.Restlet;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestDispatcherCallback implements DispatcherCallback {
	final static Logger LOGGER = LoggerFactory
		.getLogger(RestDispatcherCallback.class);
	public static final ThreadLocal<List<Parameter>> parameters=new ThreadLocal<List<Parameter>>();
	
	public static List<Parameter> get(){
		return parameters.get();
	}

	@Override
	public void init(Request request, Response response) {

        if (LOGGER.isDebugEnabled()){
        	Iterator<Parameter> it= request.getResourceRef().getQueryAsForm().iterator();
	        while (it.hasNext()){
	        	Parameter p=it.next();
	        	LOGGER.info( "Registering incoming parameter: " + p.toString());
	        }
        }
        parameters.set(request.getResourceRef().getQueryAsForm());
        
        //check purge parameter to determine if the underlying file 
        // should be deleted
//        boolean purge = (p != null) ? Boolean.parseBoolean(p) : false;
//        catalog.getResourcePool().deleteStyle(s, purge);
        
//        LOGGER.info( "DELETE style " + style);
		
	}

	@Override
	public void dispatched(Request request, Response response, Restlet restlet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exception(Request request, Response response, Exception error) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finished(Request request, Response response) {
		// TODO Auto-generated method stub
		
	}
	
}
