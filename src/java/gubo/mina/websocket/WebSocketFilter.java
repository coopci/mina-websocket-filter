package gubo.mina.websocket;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class WebSocketFilter extends IoFilterAdapter {
	public static Logger logger = LoggerFactory.getLogger(WebSocketFilter.class);
	
	/**
     * example:
     * 	see static public void example
     *
     */
	@Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {

    	logger.trace("gubo.mina.websocket.WebSocketFilter.");
    	IoBuffer in = (IoBuffer) message;
    	
    	
    	IoBuffer resultBuffer;
    	
    	if(!session.containsAttribute(WebSocketUtils.SessionAttribute)){
            // first message on a new connection. see if its from a websocket or a 
            // native socket.
            // if(tryWebSockeHandShake(session, in, out)){
            if (tryWebSockeHandShake(session, in, null, nextFilter)){
                // websocket handshake was successful. Don't write anything to output
                // as we want to abstract the handshake request message from the handler.
                in.position(in.limit());
                return ;
            } else{
                // message is from a native socket. Simply wrap and pass through.
                resultBuffer = IoBuffer.wrap(in.array(), 0, in.limit());
                in.position(in.limit());
                session.setAttribute(WebSocketUtils.SessionAttribute, false);
            }
        } else if(session.containsAttribute(WebSocketUtils.SessionAttribute) && true==(Boolean)session.getAttribute(WebSocketUtils.SessionAttribute)){            
            // there is incoming data from the websocket. Decode and send to handler or next filter.     
            int startPos = in.position();
            // resultBuffer = buildWSDataBuffer(in, session);
            ParsedFrame parsedFrame = buildWSDataBuffer(in, session);
            if(parsedFrame == null){
                // There was not enough data in the buffer to parse. Reset the in buffer
                // position and wait for more data before trying again.
                in.position(startPos);
                return ;
            }
            if (parsedFrame.opCode == 9) { // PING
            	// do nothing instead of sending a PONG. It works fine with firefox 38.0.5
            	// TODO send a PONG frame
            	return;
            } else if (parsedFrame.opCode == 0xA) { // PONG
            	return;
            }
            resultBuffer = parsedFrame.unMaskedPayLoad;
        } else {
            // session is known to be from a native socket. So
            // simply wrap and pass through.
            resultBuffer = IoBuffer.wrap(in.array(), 0, in.limit());    
            in.position(in.limit());
        }                  
        // out.write(resultBuffer);
		nextFilter.messageReceived(session, resultBuffer);
        return ;
    }
    
    static public class ParsedFrame {
    	byte opCode;
    	IoBuffer unMaskedPayLoad; 
    }
    
    private static ParsedFrame buildWSDataBuffer(IoBuffer in, IoSession session) {
    	ParsedFrame parsedFrame = new ParsedFrame();
        IoBuffer resultBuffer = null;
        do{
            byte frameInfo = in.get();            
            byte opCode = (byte) (frameInfo & 0x0f);
            parsedFrame.opCode = opCode;
            if (opCode == 8) {
                // opCode 8 means close. See RFC 6455 Section 5.2
                // return what ever is parsed till now.
                session.close(true);
                return null;
            }
            
            if (opCode == 9) { // PING
            	// TODO handler ping frame
            	return parsedFrame;
            }
            int frameLen = (in.get() & (byte) 0x7F);
            if(frameLen == 126){
                frameLen = in.getShort();
            }
            
            // Validate if we have enough data in the buffer to completely
            // parse the WebSocket DataFrame. If not return null.
            if(frameLen+4 > in.remaining()){                
                return null;
            }
            byte mask[] = new byte[4];
            for (int i = 0; i < 4; i++) {
                mask[i] = in.get();
            }

            /*  now un-mask frameLen bytes as per Section 5.3 RFC 6455
                Octet i of the transformed data ("transformed-octet-i") is the XOR of
                octet i of the original data ("original-octet-i") with octet at index
                i modulo 4 of the masking key ("masking-key-octet-j"):

                j                   = i MOD 4
                transformed-octet-i = original-octet-i XOR masking-key-octet-j
            * 
            */
             
            byte[] unMaskedPayLoad = new byte[frameLen];
            for (int i = 0; i < frameLen; i++) {
                byte maskedByte = in.get();
                unMaskedPayLoad[i] = (byte) (maskedByte ^ mask[i % 4]);
            }
            
            if(resultBuffer == null){
                resultBuffer = IoBuffer.wrap(unMaskedPayLoad);
                resultBuffer.position(resultBuffer.limit());
                resultBuffer.setAutoExpand(true);
            }
            else{
                resultBuffer.put(unMaskedPayLoad);
            }
        }
        while(in.hasRemaining());
        
        resultBuffer.flip();
        parsedFrame.unMaskedPayLoad = resultBuffer;
        return parsedFrame;

    }    
    
    
    private static IoBuffer buildWSDataBufferOld(IoBuffer in, IoSession session) {

        IoBuffer resultBuffer = null;
        do{
            byte frameInfo = in.get();            
            byte opCode = (byte) (frameInfo & 0x0f);
            if (opCode == 8) {
                // opCode 8 means close. See RFC 6455 Section 5.2
                // return what ever is parsed till now.
                session.close(true);
                return resultBuffer;
            }
            
            if (opCode == 9) { // PING
            	// TODO handler ping frame
            }
            int frameLen = (in.get() & (byte) 0x7F);
            if(frameLen == 126){
                frameLen = in.getShort();
            }
            
            // Validate if we have enough data in the buffer to completely
            // parse the WebSocket DataFrame. If not return null.
            if(frameLen+4 > in.remaining()){                
                return null;
            }
            byte mask[] = new byte[4];
            for (int i = 0; i < 4; i++) {
                mask[i] = in.get();
            }

            /*  now un-mask frameLen bytes as per Section 5.3 RFC 6455
                Octet i of the transformed data ("transformed-octet-i") is the XOR of
                octet i of the original data ("original-octet-i") with octet at index
                i modulo 4 of the masking key ("masking-key-octet-j"):

                j                   = i MOD 4
                transformed-octet-i = original-octet-i XOR masking-key-octet-j
            * 
            */
             
            byte[] unMaskedPayLoad = new byte[frameLen];
            for (int i = 0; i < frameLen; i++) {
                byte maskedByte = in.get();
                unMaskedPayLoad[i] = (byte) (maskedByte ^ mask[i % 4]);
            }
            
            if(resultBuffer == null){
                resultBuffer = IoBuffer.wrap(unMaskedPayLoad);
                resultBuffer.position(resultBuffer.limit());
                resultBuffer.setAutoExpand(true);
            }
            else{
                resultBuffer.put(unMaskedPayLoad);
            }
        }
        while(in.hasRemaining());
        
        resultBuffer.flip();
        return resultBuffer;

    }    
    
    
    //IPermissionChecker permissionChecker = new ForbidAllPermissionChecker();
    IPermissionChecker permissionChecker = new AllowAllPermissionChecker();
    	
    public IPermissionChecker getPermissionChecker() {
		return permissionChecker;
	}


	public void setPermissionChecker(IPermissionChecker permissionChecker) {
		this.permissionChecker = permissionChecker;
	}


	public void sendReponse(WebSocketHandShakeResponse wsResponse, IoSession session, NextFilter nextFilter) throws UnsupportedEncodingException {
		byte[] bytes = wsResponse.getResponse().getBytes("utf-8");
        IoBuffer buf = IoBuffer.allocate(bytes.length);
        buf.put(bytes);
        buf.flip();
        nextFilter.filterWrite(session, new DefaultWriteRequest(buf));
	}
	/**
     *   Try parsing the message as a websocket handshake request. If it is such
     *   a request, then send the corresponding handshake response (as in Section 4.2.2 RFC 6455).
     */
     private boolean tryWebSockeHandShake(IoSession session, IoBuffer in, ProtocolDecoderOutput out, NextFilter nextFilter) {
         
         try{
             String payLoadMsg = new String(in.array());
             
             logger.trace("payLoadMsg: \n{}", payLoadMsg);
             
             Map<String, String> headers = WebSocketUtils.parseRequest(payLoadMsg);
             
             
             String socketKey = headers.get("Sec-WebSocket-Key");
             
             if(socketKey == null || socketKey.length() <= 0){
                 return false;
             }
             if (!this.permissionChecker.check(headers, session)) {
            	 WebSocketHandShakeResponse wsResponse = WebSocketUtils.buildForbiddenResponse();
            	 sendReponse(wsResponse, session, nextFilter);
            	 return true;
             }
             String challengeAccept = WebSocketUtils.getWebSocketKeyChallengeResponse(socketKey);            
             WebSocketHandShakeResponse wsResponse = WebSocketUtils.buildWSHandshakeResponse(challengeAccept);
             session.setAttribute(WebSocketUtils.SessionAttribute, true);
             
             logger.trace("session.write: \n{}", wsResponse.getResponse());
             
             sendReponse(wsResponse, session, nextFilter);
             logger.trace("session.written.");
             return true;
         }
         catch(Exception e){
             // input is not a websocket handshake request.
             return false;
         }        
     }

     @Override
     public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest request) {
    	 
    	 if(session.containsAttribute(WebSocketUtils.SessionAttribute) && true==(Boolean)session.getAttribute(WebSocketUtils.SessionAttribute)){
	    	 IoBuffer resultBuffer = WebSocketFilter.buildWSDataFrameBuffer((IoBuffer)request.getMessage());
	    	 nextFilter.filterWrite(
	    	        session, 
	    	        new DefaultWriteRequest(resultBuffer, request.getFuture(), request.getDestination())
	    	        );
    	 } else {
    		 nextFilter.filterWrite(session, request);
    	 }
    }
     
  // Encode the in buffer according to the Section 5.2. RFC 6455
     private static IoBuffer buildWSDataFrameBuffer(IoBuffer buf) {
         
         IoBuffer buffer = IoBuffer.allocate(buf.limit() + 2, false);
         buffer.setAutoExpand(true);
         buffer.put((byte) 0x82);
         if(buffer.capacity() <= 125){
             byte capacity = (byte) (buf.limit());
             buffer.put(capacity);
         }
         else{
             buffer.put((byte)126);
             buffer.putShort((short)buf.limit());
         }        
         buffer.put(buf);
         buffer.flip();
         return buffer;
     }
     
     public void example() {
    	 gubo.mina.websocket.WebSocketFilter websocketFilter = new gubo.mina.websocket.WebSocketFilter();
    	 
    	 websocketFilter.setPermissionChecker(new AllowAllPermissionChecker());
    	 // there are two other out-of-box IPermissionChecker implementation:
		//    	 websocketFilter.setPermissionChecker(new ForbidAllPermissionChecker());
		// 		 WhiteListPermissionChecker whiteListPermissionChecker = new WhiteListPermissionChecker();
		// 		 whiteListPermissionChecker.addWhiteList("http://localhost");
		// 		 websocketFilter.setPermissionChecker(whiteListPermissionChecker);
 		
 		
    	 NioSocketAcceptor acceptor = new NioSocketAcceptor();
    	 DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
    	 chain.addLast("wsfilter", websocketFilter);
    	 
     }
}
