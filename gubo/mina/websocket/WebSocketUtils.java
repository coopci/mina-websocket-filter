package gubo.mina.websocket;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.message.BasicLineParser;

import com.shephertz.appwarp.websocket.binary.Base64;

// copied from com.shephertz.appwarp.websocket.binary.WebSocketUtils, changed visibility of static methods from private to public.
public class WebSocketUtils {
    
    public static final String SessionAttribute = "isWEB";
    // Construct a successful websocket handshake response using the key param
    // (See RFC 6455).
    public static WebSocketHandShakeResponse buildForbiddenResponse(){
    	
        // String response = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n";
    	String response = "HTTP/1.1 403 Forbidden\r\n";
    	response += "Connection: close\r\n";
    	response += "Content-Length: 0\r\n";
        response += "\r\n";        
        return new WebSocketHandShakeResponse(response);
    }
    
    public static WebSocketHandShakeResponse buildWSHandshakeResponse(String key){
    	
        // String response = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n";
    	String response = "HTTP/1.1 101 Switching Protocols\r\n";
    	
        response += "Upgrade: websocket\r\n";
        response += "Connection: Upgrade\r\n";
        response += "Sec-WebSocket-Accept: " + key + "\r\n";
        
        // added by cooper 2015-06-30
        response += "Sec-WebSocket-Protocol: v10.stomp\r\n"; 
        
        response += "\r\n";        
        return new WebSocketHandShakeResponse(response);
    }


    public static Map<String, String> parseRequest(String WSRequest) {
    	HashMap<String, String> ret = new HashMap<String, String>(); 
    	String[] headers = WSRequest.split("\r\n");
        String socketKey = "";
        for (int i = 1; i < headers.length; i++) {
        	String line = headers[i];
        	int delimiter = line.indexOf(":");
        	if (delimiter <= 0)
        		break;
        	String name = line.substring(0, delimiter);
        	String value = line.substring(delimiter+1).trim();
        	ret.put(name, value);
        }
        return ret;
    }
    // Parse the string as a websocket request and return the value from
    // Sec-WebSocket-Key header (See RFC 6455). Return empty string if not found.
    public static String getClientWSRequestKey(String WSRequest) {
    	
    	
    	
        String[] headers = WSRequest.split("\r\n");
        String socketKey = "";
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].contains("Sec-WebSocket-Key")) {
                socketKey = (headers[i].split(":")[1]).trim();
                break;
            }
        }
        return socketKey;
    }    
    
    // 
    // Builds the challenge response to be used in WebSocket handshake.
    // First append the challenge with "258EAFA5-E914-47DA-95CA-C5AB0DC85B11" and then
    // make a SHA1 hash and finally Base64 encode it. (See RFC 6455)
    public static String getWebSocketKeyChallengeResponse(String challenge) throws NoSuchAlgorithmException, UnsupportedEncodingException {

        challenge += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest cript = MessageDigest.getInstance("SHA-1");
        cript.reset();
        cript.update(challenge.getBytes("utf8"));
        byte[] hashedVal = cript.digest();        
        return Base64.encodeBytes(hashedVal);
    }
}