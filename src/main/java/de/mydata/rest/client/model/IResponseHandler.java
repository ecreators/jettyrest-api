package de.mydata.rest.client.model;

import org.apache.http.HttpResponse;

import java.lang.reflect.Method;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public interface IResponseHandler {
    
    Object getValue(HttpResponse response, Method method);
}