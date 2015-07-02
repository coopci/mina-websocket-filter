# mina-websocket-filter
A simple websocket filter for mina with customazable permission checker. 

Disclaimer: Much code is copied from https://issues.apache.org/jira/browse/DIRMINA-907. 
<pre>
My work includes:
1. Change it from a ProtocolCodec to a filter, so that it can cooperate with other application level filters.
2. Add IPermissionChecker and three implementations of IPermissionChecker.
3. Handle incoming PING frame.
</pre>
<pre>
example:
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
</pre>
