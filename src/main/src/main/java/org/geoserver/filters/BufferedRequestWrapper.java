package org.geoserver.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.geotools.util.Converters;

public class BufferedRequestWrapper extends HttpServletRequestWrapper{
    protected HttpServletRequest myWrappedRequest;
    protected String myBuffer;
    protected ServletInputStream myStream = null;
    protected BufferedReader myReader = null;
	protected Map myParameterMap;
    protected Logger logger = 
        org.geotools.util.logging.Logging.getLogger("org.geoserver.filters");

    public BufferedRequestWrapper(HttpServletRequest req, String buff){
        super(req);
        myWrappedRequest = req;
        myBuffer = buff;
		logger.fine("Created BufferedRequestWrapper with String: \"" + buff + "\" as buffer");
    }

    public ServletInputStream getInputStream() throws IOException{
        if (myStream == null){
            if (myReader == null){
                myStream = new BufferedRequestStream(myBuffer);
            } else {
                throw new IOException("Requesting a stream after a reader is already in use!!");
            }
        }

        return myStream;
    }

    public BufferedReader getReader() throws IOException{
        if (myReader == null){
            if (myStream == null){

                myReader = new BufferedReader(
                        new InputStreamReader(
                            new BufferedRequestStream(myBuffer) 
                            )
                        );
            } else {
                throw new IOException("Requesting a reader after a stream is already in use!!");
            }
        } 

        return myReader;
    }

	public String getParameter(String name){
		parseParameters();
		List allValues = (List)myParameterMap.get(name);
		if (allValues != null && allValues.size() > 0){
			return (String) allValues.get(0);
		} else return null;
	}
    
	public Map getParameterMap(){
		parseParameters();
		Map toArrays = new TreeMap();
		Iterator it = myParameterMap.entrySet().iterator();

		while (it.hasNext()){
			Map.Entry entry = (Map.Entry)it.next();
			toArrays.put(entry.getKey(), 
			  (String[])((List)entry.getValue()).toArray(new String[0]));
		}

		return Collections.unmodifiableMap(toArrays);
	}

	public Enumeration getParameterNames(){
		parseParameters();
		return new IteratorAsEnumeration(myParameterMap.keySet().iterator());
	}

	public String[] getParameterValues(String name){
		parseParameters();
		List allValues = (List)myParameterMap.get(name);
		if (allValues != null && allValues.size() > 0){
			return (String[])allValues.toArray(new String[0]);
		} else return null;
	}

	protected void parseParameters(){
		if (myParameterMap != null) return;
		if (myWrappedRequest.getMethod().equals("POST") &&
			myWrappedRequest.getContentType().startsWith("application/x-www-form-urlencoded")) {
			parseFormBody();
		} else {
			myParameterMap = new HashMap(super.getParameterMap());
			
            for (Object key : myParameterMap.keySet()) {
                Object value = myParameterMap.get(key);
                if (value instanceof List) {
                    // ok, nothing to do
                } else if (value instanceof String[]) {
                    myParameterMap.put(key, Arrays.asList(((String[]) value)));
                } else {
                    myParameterMap.put(key, Converters.convert(value, List.class));
                }
            }
		}
	}

	protected void parseFormBody(){
		myParameterMap = new TreeMap();
		
		// parse the body
		String[] pairs = myBuffer.split("\\&");
		
		for (int i = 0; i < pairs.length; i++){
			parsePair(pairs[i]);
		}
		
		// we should also parse parameters that came into the request thought
		if(myWrappedRequest.getQueryString() != null) {
		    pairs = myWrappedRequest.getQueryString().split("\\&");
	        
	        for (int i = 0; i < pairs.length; i++){
	            parsePair(pairs[i]);
	        }
		}
	}

	protected void parsePair(String pair){
		int index = 0;
		String[] split = pair.split("=", 2);
		try{
			String key = URLDecoder.decode(split[0], "UTF-8");
			String value = (split.length > 1 ? URLDecoder.decode(split[1], "UTF-8") : "");

			if (!myParameterMap.containsKey(key)){
				myParameterMap.put(key, new ArrayList());
			}

			((List)myParameterMap.get(key)).add(value);

		} catch (UnsupportedEncodingException e){
			logger.severe("Failed to decode form values in LoggingFilter");
			// we have the encoding hard-coded for now so no exceptions should be thrown...
		}
	}

	private class IteratorAsEnumeration implements Enumeration{
		Iterator it;

		public IteratorAsEnumeration(Iterator it){
			this.it = it;
		}

		public boolean hasMoreElements(){
			return it.hasNext();
		}

		public Object nextElement(){
			return it.next();
		}
	}
}
