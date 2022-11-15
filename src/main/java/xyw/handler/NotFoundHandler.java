package xyw.handler;

import xyw.Logger;
import xyw.Request;
import xyw.Response;

public class NotFoundHandler implements Handler {
	public NotFoundHandler(){
	}
	@Override
	public boolean handler(Request req, Response res) {
		Logger.debug("NoFoundHandler 处理请求:{} {}",req.getMethod(),req.getPath());
		res.setCode(Response.ResponseCode.NOT_FOUND);
		res.setBody("404(NOT FOUND)");
		return true;
	}
}
