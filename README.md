# mina-websocket-filter
A simple websocket filter for mina with customazable permission checker. 

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
