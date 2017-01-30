package de.mydata.http;

import org.apache.http.HttpResponse;

import java.io.IOException;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class RestResponse {
    
    public static <T> T readObject(HttpResponse response, Class<T> responseType) throws IOException, ClassNotFoundException {
        String lines = RestRequest.readLines(response);
        return RestRequest.StringConverter.toJson(lines, responseType);
    }
}
