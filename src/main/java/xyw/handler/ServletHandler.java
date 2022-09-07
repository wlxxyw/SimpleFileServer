package xyw.handler;

import java.util.Arrays;
import java.util.List;

import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.handler.servlet.Servlet;

public class ServletHandler implements Handler {
	final List<Servlet> servlets;
	public ServletHandler(Servlet...servlets){
		Logger.info("初始化Servlet:{}个", servlets.length);
		this.servlets = Arrays.asList(servlets);
	}
	@Override
	public boolean handler(Request req, Response res) {
		Logger.debug("ServletHandler 处理请求:{} {}",req.getMethod(),req.getPath());
		for(Servlet servlet:servlets){
			if(servlet.doServlet(req, res)){
				Logger.debug("Servlet{} 处理了请求:{} {}",servlet.getClass().getName(),req.getMethod(),req.getPath());
				return true;
			}
		}
		Logger.debug("ServletHandler 未找到合适Servlet处理请求:{} {}",req.getMethod(),req.getPath());
		return false;
	}
}
