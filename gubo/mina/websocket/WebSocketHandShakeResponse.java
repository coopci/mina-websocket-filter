package gubo.mina.websocket;

//copied from com.shephertz.appwarp.websocket.binary.WebSocketHandShakeResponse, changed visibility of static methods from private to public.
public class WebSocketHandShakeResponse {
    
    private String response;
    public WebSocketHandShakeResponse(String response){
        this.response = response;
    }
    
    public String getResponse(){
        return this.response;
    }
}
