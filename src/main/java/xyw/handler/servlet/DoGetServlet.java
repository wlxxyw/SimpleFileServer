package xyw.handler.servlet;

import static xyw.Constant.*;
import static xyw.Tool.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;

public class DoGetServlet extends Servlet{
	private final String workPath;
	public DoGetServlet(String workPath,String context){
		super(context);
		if(!workPath.endsWith(File.separator)){workPath += File.separator;}
		this.workPath = workPath;
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		String path = req.getPath();
		if(null==path||0==path.length()||!path.startsWith(context)){return false;}
		if(METHOD_GET.equals(req.getMethod())){
			String action = req.getParam("action");
			File f = new File(workPath + path.substring(context.length()));
			if(!f.exists()){
				Logger.info("DoGetServlet: {}文件不存在!", f.getAbsolutePath());
				return false;
			}
			if(f.isFile()){
				Logger.info("{} --> {}", path,f.getAbsolutePath());
				String lastModified = String.valueOf(f.lastModified());
				String ifLastModified = req.getHeader("If-Modified-Since");
				if(!lastModified.equals(ifLastModified)){
					try {
						res.setCode(ResponseCode.OK);
						res.getHeaders().put("Content-Type", contentType(f.getName())+"; charset=utf-8");
						res.getHeaders().put("Content-Length", String.valueOf(f.length()));
						res.getHeaders().put("Last-Modified", lastModified);
						res.setBody(new FileInputStream(f));
					} catch (IOException e) {
						e.printStackTrace();
						quickFinish(res, ResponseCode.ERROR,e.getLocalizedMessage());
					}
				}else{
					Logger.info("{} user cache!", path);
					res.setCode(ResponseCode.NOT_MODIFIED);
				}
			}else{
				if(null==action){
					res.setCode(ResponseCode.OK);
					res.getHeaders().put("Content-Type", DEFAULT_HTML);
					res.setBody(HTML_TEMPLATE);
				}else{
					if(f.isDirectory()){
						res.setCode(ResponseCode.OK);
						res.getHeaders().put("Content-Type", DEFAULT_JSON);
						res.setBody(toJSON(fileList(f.listFiles())).getBytes(UTF8));
					}else{
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
	private List<Map<String, Object>> fileList(File...files){
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		if(null==files||0==files.length){
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("_no_sub_file", true);
			list.add(map);
		}else{
			for(File file:files){
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("name", file.getName());
				map.put("isFile", file.isFile());
				map.put("size", file.length());
				map.put("lastModified", file.lastModified());
				list.add(map);
			}
		}
		return list;
	}
}
