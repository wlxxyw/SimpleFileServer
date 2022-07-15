package xyw.handler.servlet;

import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;
import static xyw.Constant.*;

public abstract class Servlet{
	protected final String context;
	protected Servlet(String context){
		this.context = context;
	}
	public abstract boolean doServlet(Request req, Response res);
	protected void quickFinish(Response res,ResponseCode code){
		res.setCode(code);
		res.getHeaders().put("Content-Type", DEFAULT_HTML);
		res.setBody(new byte[0]);
	}
	protected void quickFinish(Response res,ResponseCode code,String msg){
		res.setCode(code);
		res.getHeaders().put("Content-Type", DEFAULT_HTML);
		res.setBody(msg);
	}
	protected boolean matchContext(Request req){
		String path = req.getPath();
		if(null==path||0==path.length()){
			Logger.warn("请求地址为空!");
			return false;
		}
		if(!path.startsWith(context)){
			Logger.warn("请求地址不匹配!");
			return false;
		}
		return true;
	}
}