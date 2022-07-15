package xyw.handler.servlet;

import static xyw.Constant.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;

public class ResourceServlet extends DoGetServlet{
	private final String workPath;
	public ResourceServlet(String workPath,String context){
		super(workPath, context);
		if(!workPath.endsWith(File.separator)){workPath += File.separator;}
		this.workPath = workPath;
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		if(matchContext(req))
		if(METHOD_GET.equals(req.getMethod())){
			String path = req.getPath();
			File f = new File(workPath + path.substring(context.length()));
			if(f.isFile()){
				Logger.debug("{} --> {}", path,f.getAbsolutePath());
				String lastModified = String.valueOf(f.lastModified());
				String ifLastModified = req.getHeader("If-Modified-Since");
				if(!lastModified.equals(ifLastModified)){
					try {
						res.setCode(ResponseCode.OK);
						res.getHeaders().put("Content-Type", contentType(f.getName())+"; charset=utf-8");
						res.getHeaders().put("Content-Length", String.valueOf(f.length()));
						res.getHeaders().put("Last-Modified", lastModified);
						res.getHeaders().put("Cache-Control", "max-age=31536000");
						res.setBody(new FileInputStream(f));
					} catch (IOException e) {
						e.printStackTrace();
						quickFinish(res, ResponseCode.ERROR,e.getLocalizedMessage());
					}
				}else{
					Logger.debug("{} user cache!", path);
					res.setCode(ResponseCode.NOT_MODIFIED);
				}
				return true;
			}
		}
		return false;
	}
}
