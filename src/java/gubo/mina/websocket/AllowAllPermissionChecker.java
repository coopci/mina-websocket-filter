package gubo.mina.websocket;

import java.util.Map;

import org.apache.mina.core.session.IoSession;

public class AllowAllPermissionChecker implements IPermissionChecker {

	@Override
	public boolean check(Map<String, String> headers, IoSession session) {
		return true;
	}

}
