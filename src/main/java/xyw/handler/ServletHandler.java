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
		for(Servlet servlet:servlets){
			if(servlet.doServlet(req, res))return true;
		}
		return false;
	}
}
