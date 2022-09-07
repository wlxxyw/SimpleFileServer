package xyw.handler.servlet;

import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;

import java.io.File;

import static xyw.Constant.*;

public abstract class Servlet{
	protected final ServletConfig config;
	protected final File baseFile;
	protected Servlet(ServletConfig config){
		this.config = config;
		this.baseFile = new File(config.workPath.endsWith(File.separator)?config.workPath:(config.workPath+File.separator));
	}
	public abstract boolean doServlet(Request req, Response res);
	protected boolean quickFinish(Response res,ResponseCode code){
		res.setCode(code);
		res.getHeaders().put("Content-Type", DEFAULT_HTML);
		res.setBody(new byte[0]);
		return true;
	}
	protected boolean quickFinish(Response res,ResponseCode code,String msg){
		res.setCode(code);
		res.getHeaders().put("Content-Type", DEFAULT_HTML);
		res.setBody(msg);
		return true;
	}
	protected boolean matchContext(Request req){
		String path = req.getPath();
		if(null==path||0==path.length()){
			Logger.warn("请求地址为空!");
			return false;
		}
		if(!path.startsWith(config.context)){
			Logger.debug("请求地址不匹配!{},{}",config.context,path);
			return false;
		}
		return true;
	}
}