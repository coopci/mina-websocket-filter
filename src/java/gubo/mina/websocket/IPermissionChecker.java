package gubo.mina.websocket;

import java.util.Map;

import org.apache.mina.core.session.IoSession;

public interface IPermissionChecker {
	boolean check(Map<String, String> headers, IoSession session);
}
