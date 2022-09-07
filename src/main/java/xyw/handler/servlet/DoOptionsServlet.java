package xyw.handler.servlet;

import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static xyw.Constant.*;
import static xyw.Tool.toJson;

public class DoOptionsServlet extends Servlet{
	public DoOptionsServlet(ServletConfig config){
		super(config);
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		if(matchContext(req)) {
			if (METHOD_OPTIONS.equals(req.getMethod())) {
				String path = req.getPath();
				Logger.info("options {}", req);
				File f = new File(baseFile, path.substring(config.context.length()));
				res.setCode(ResponseCode.OK);
				if (f.isFile()) {
					res.getHeaders().put("Allow", "GET,DELETE");
				}else if(f.isDirectory()){
					if(f.list().length==0){
						res.getHeaders().put("Allow", "GET,POST,PUT,DELETE");
					}else{
						res.getHeaders().put("Allow", "GET,POST,PUT");
					}
				}else{
					res.getHeaders().put("Allow", "POST");
				}
				return config.defaultReturn;
			}
		}
		return false;
	}
}
