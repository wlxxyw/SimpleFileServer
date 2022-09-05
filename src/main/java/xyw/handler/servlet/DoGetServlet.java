package xyw.handler.servlet;

import static xyw.Constant.*;
import static xyw.Tool.toJson;

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
	public DoGetServlet(ServletConfig config){
		super(config);
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		if(matchContext(req)) {
			if (METHOD_GET.equals(req.getMethod())) {
				String path = req.getPath();
				String action = req.getParam("action");
				File f = new File(baseFile, path.substring(config.context.length()));
				if (!f.exists()) {
					if(config.defaultReturn)Logger.warn("DoGetServlet: {}文件不存在!", f.getAbsolutePath());
					return config.defaultReturn;
				}
				if (null == action){
					if(f.isDirectory()){
						Logger.info("Get template!");
						res.setCode(ResponseCode.OK);
						res.getHeaders().put("Content-Type", DEFAULT_HTML);
						res.setBody(HTML_TEMPLATE);
						return true;
					}
					if(f.isFile()){
						Logger.info("downloading {} --> {}", path, f.getAbsolutePath());
						if(config.useCache){
							String lastModified = String.valueOf(f.lastModified());
							res.getHeaders().put("Last-Modified", lastModified);
							String ifLastModified = req.getHeader("If-Modified-Since");
							if (lastModified.equals(ifLastModified)) {
								Logger.debug("{} user cache!", path);
								res.setCode(ResponseCode.NOT_MODIFIED);
								return true;
							}
						}
						try {
							res.setCode(ResponseCode.OK);
							res.getHeaders().put("Content-Type", contentType(f.getName()) + "; charset=utf-8");
							res.getHeaders().put("Content-Length", String.valueOf(f.length()));
							res.setBody(new FileInputStream(f));
							return true;
						} catch (IOException e) {
							e.printStackTrace();
							return quickFinish(res, ResponseCode.ERROR, e.getLocalizedMessage());
						}
					}
				}else if(config.useAction){
					if (f.isDirectory()) {
						Logger.info("获取文件列表:{}", f.getAbsolutePath());
						res.setCode(ResponseCode.OK);
						res.getHeaders().put("Content-Type", DEFAULT_JSON);
						res.setBody(toJson(fileList(f.listFiles())).getBytes(UTF8));
						return true;
					}
				}
				return config.defaultReturn;
			}
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
				if(file.isFile()||file.isDirectory()){
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("name", file.getName());
					map.put("isFile", file.isFile());
					map.put("size", file.length());
					map.put("lastModified", file.lastModified());
					list.add(map);
				}
			}
		}
		return list;
	}
}
