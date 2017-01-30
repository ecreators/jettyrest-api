package de.mydata.rest.client;

import de.mydata.http.RestRequest;
import de.mydata.http.RestResponse;
import de.mydata.rest.server.JettyRestServer;
import org.apache.http.HttpResponse;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class RestConnection<T extends JettyRestServer.IRestService> {
    
    private String           service;
    private T                ref;
    
    public static <T extends JettyRestServer.IRestService> RestConnection<T> consume(String host, int port, String resourcePath, Class<T> service) {
        RestConnection<T> serviceResource = new RestConnection<T>();
        serviceResource.service = host + ":" + port + resourcePath;
        //noinspection unchecked
        serviceResource.ref = (T) Proxy.newProxyInstance(service.getClassLoader(),
                                                         new Class<?>[]{service},
                                                         new RestResourceHandler(serviceResource));
        return serviceResource;
    }
    
    protected RestConnection() {
    }
    
    public T getService() {
        return ref;
    }
    
    private static class RestResourceHandler implements InvocationHandler {
        
        private final RestConnection<? extends JettyRestServer.IRestService> serviceResource;
        
        public <T extends JettyRestServer.IRestService> RestResourceHandler(RestConnection<T> serviceResource) {
            this.serviceResource = serviceResource;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String url = serviceResource.service;
            url += method.getAnnotation(Path.class).value();
            
            Matcher matcher = Pattern.compile("(?<p>\\{.*\\})").matcher(url);
            if(matcher.find()) {
                for (int i = 1; i < matcher.groupCount(); i++) {
                    String pName = matcher.group(i);
                    url = url.replace(pName, String.valueOf(args[i - 1]));
                }
            }
    
            Object content = request(url, method);
            System.out.println(String.format("[RESULT] - %s", String.valueOf(content)));
            return content;
        }
    
        private Object request(String url, Method method) throws IOException {
            String contentType = MediaType.TEXT_PLAIN;
            if(method.isAnnotationPresent(Produces.class)) {
                contentType = method.getAnnotation(Produces.class).value()[0];
            }
            
            MediaType responseType = MediaType.valueOf(contentType);
            
            HttpResponse response = RestRequest.doRequestOrNull(RestRequest.GET.request(url, responseType));
            if(response != null) {
                try {
                    return RestResponse.readObject(response, method.getReturnType());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }
    }
}