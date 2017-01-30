package de.mydata.rest.client;

import de.mydata.http.RestRequest;
import de.mydata.rest.client.model.IResponseHandler;
import de.mydata.rest.server.JettyRestServer;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.annotation.Annotation;
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
    private IResponseHandler responseHandler;
    
    public static <T extends JettyRestServer.IRestService> RestConnection<T> consume(String host, int port, String resourcePath, Class<T> service, IResponseHandler handler) {
        RestConnection<T> serviceResource = new RestConnection<T>();
        serviceResource.service = host + ":" + port + resourcePath;
        //noinspection unchecked
        serviceResource.ref = (T) Proxy.newProxyInstance(service.getClassLoader(),
                                                         new Class<?>[]{service},
                                                         new RestResourceHandler(serviceResource));
        serviceResource.responseHandler = handler;
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
            return get(url, proxy, method, args);
        }
        
        private Object get(String url, Object proxy, Method method, Object[] args) throws IOException {
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
                if(isStringType(responseType)) {
                    String text = RestRequest.readLines(response);
                    if(responseType.equals(MediaType.APPLICATION_JSON_TYPE)) {
                        return RestRequest.StringConverter.toJson(text, method.getReturnType());
                    } else if(method.getReturnType().equals(Boolean.class)) {
                        return RestRequest.StringConverter.toBoolean(text);
                    } else if(method.getReturnType().equals(Integer.class)) {
                        return RestRequest.StringConverter.toInteger(text);
                    } else if(method.getReturnType().equals(Double.class)) {
                        return RestRequest.StringConverter.toDouble(text);
                    } else if(method.getReturnType().equals(Long.class)) {
                        return RestRequest.StringConverter.toLong(text);
                    } else if(method.getReturnType().equals(Short.class)) {
                        return RestRequest.StringConverter.toShort(text);
                    } else if(method.getReturnType().equals(Byte.class)) {
                        return RestRequest.StringConverter.toByte(text);
                    } else if(method.getReturnType().equals(Character.class)) {
                        return RestRequest.StringConverter.toChar(text);
                    } else if(method.getReturnType().equals(String[].class)) {
                        return RestRequest.StringConverter.toStringArray(text);
                    } else if(method.getReturnType().equals(int[].class)) {
                        return RestRequest.StringConverter.toIntegerArray(text);
                    } else if(method.getReturnType().equals(double[].class)) {
                        return RestRequest.StringConverter.toDoubleArray(text);
                    } else if(method.getReturnType().equals(long[].class)) {
                        return RestRequest.StringConverter.toLongArray(text);
                    } else {
                        return text;
                    }
                } else {
                    return handleRequest(response, method);
                }
            }
            return response;
        }
        
        private static boolean isStringType(MediaType responseType) {
            return responseType.toString().toLowerCase().startsWith("text")
                    || responseType.equals(MediaType.APPLICATION_JSON_TYPE);
        }
        
        private Object handleRequest(HttpResponse response, Method method) {
            if(response == null) {
                return null;
            }
            IResponseHandler responseHandler = serviceResource.responseHandler;
            if(responseHandler != null) {
                return responseHandler.getValue(response, method);
            }
            return response;
        }
        
        private static String getRequestProduces(Method method) {
            if(method.isAnnotationPresent(Produces.class)) {
                return method.getAnnotation(Produces.class).value()[0];
            }
            return MediaType.TEXT_PLAIN;
        }
        
        private static String getRequestMethod(Method method) {
            for (Annotation annotation : method.getAnnotations()) {
                String requestMethod = annotation.annotationType().getSimpleName();
                if(requestMethod.equals(requestMethod.toUpperCase())) {
                    if(annotation.annotationType().getPackage().getName().equals(GET.class.getPackage().getName())) {
                        return requestMethod;
                    }
                }
            }
            return GET.class.getSimpleName();
        }
    }
}
