package de.mydata.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class RestRequest {
    
    protected RestRequest() {
    }
    
    public static String getRequestText(HttpUriRequest request) {
        String result = null;
        
        HttpResponse response = null;
        try {
            response = doRequestOrNull(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Get the response
        if(response != null) {
            try {
                result = readLines(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(String.format("RESULT: '%s'", result));
        return result;
    }
    
    public static HttpResponse doRequestOrNull(HttpUriRequest request) {
        try {
            System.out.println("SEND TO: " + request.getURI().toURL().toString() + " ...");
            
            HttpClient   client   = new DefaultHttpClient();
            HttpResponse response = client.execute(request);
            System.out.println("RESPONSE STATUS: " + response.getStatusLine().getStatusCode());
            
            if(response.getStatusLine().getStatusCode() == OK.getStatusCode()) {
                return response;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class GET {
        
        protected GET() {
        }
        
        public static HttpUriRequest request(String url, MediaType responseType) {
            HttpGet request = new HttpGet(url);
            request.addHeader(CONTENT_TYPE, responseType.withCharset("UTF-8").toString());
            return request;
        }
    }
    
    public static String readLines(HttpResponse response) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String         line;
            while ((line = in.readLine()) != null) {
                if(sb.length() > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(line);
            }
            in.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }
    
    public static class StringConverter {
        
        protected StringConverter() {
        }
        
        public static boolean toBoolean(String out) {
            boolean pingResult = false;
            try {
                pingResult = Boolean.parseBoolean(out);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
            return pingResult;
        }
        
        public static int toInteger(String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        public static double toDouble(String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        public static long toLong(String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        public static byte toByte(String text) {
            try {
                return Byte.parseByte(text);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        public static short toShort(String text) {
            try {
                return Short.parseShort(text);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        public static char toChar(String text) {
            try {
                return text.toCharArray()[0];
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        public static <T> T toJson(String out, Class<T> jsonType) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(out, jsonType);
            } catch (IOException e) {
                return null;
            }
        }
        
        public static String[] toStringArray(String text) {
            return text.split(",\\s*");
        }
        
        public static int[] toIntegerArray(String text) {
            return Arrays.stream(toStringArray(text)).mapToInt(StringConverter::toInteger).toArray();
        }
    
        public static double[] toDoubleArray(String text) {
            return Arrays.stream(toStringArray(text)).mapToDouble(StringConverter::toDouble).toArray();
        }
    
        public static long[] toLongArray(String text) {
            return Arrays.stream(toStringArray(text)).mapToLong(StringConverter::toLong).toArray();
        }
    }
}
