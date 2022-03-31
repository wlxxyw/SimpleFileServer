package xyw.handler;

import java.io.IOException;

import xyw.Handler;
import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Tool;
import xyw.Response.ResponseCode;

public class FaviconHandler implements Handler {
	private final byte[] favicon;
	private final String context = "favicon.ico";
	private static final String DEFAULT_MIME = "image/x-icon";

	public FaviconHandler(String favicon) throws IOException{
		this.favicon = Tool.read(Tool.getInputStream(favicon), true);
	}
	@Override
	public boolean handler(Request req, Response res) {
		String path = req.getPath();
		if(null==path||0==path.length()){return false;}
		while(path.startsWith("/")){
			path = path.substring(1);
		}
		if(!context.equalsIgnoreCase(path)){return false;}
		if(Handler.METHOD_GET.equals(req.getMethod())){
			Logger.debug("favicon请求");
			res.setCode(ResponseCode.OK);
			res.getHeaders().put("Content-Type", DEFAULT_MIME);
			res.setBody(favicon);
			return true;
		}
		return false;
	}
}
