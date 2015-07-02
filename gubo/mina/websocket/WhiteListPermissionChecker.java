package gubo.mina.websocket;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.session.IoSession;

public class WhiteListPermissionChecker implements IPermissionChecker {

	@Override
	public boolean check(Map<String, String> headers, IoSession session) {
		String origin = headers.get("Origin");
		if (origin == null)
			return false;
		if (this.whitelist.contains(origin))
			return true;
		return false;
	}

	Set<String> whitelist = new HashSet<String>();
	
	public Set<String> getWhitelist() {
		return whitelist;
	}

	public void setWhitelist(Set<String> whitelist) {
		this.whitelist = whitelist;
	}

	public void addWhiteList(String v) {
		whitelist.add(v);
	}
	
	public void removeWhiteList(String v) {
		whitelist.remove(v);
	}
}
