package xyw.handler.servlet;

import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;
import xyw.Tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyw.Constant.*;
import static xyw.Tool.toJson;

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
						return link(req,res,f);
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

	private static final Pattern RANGE = Pattern.compile("^bytes=(\\d*)(-)?(\\d*)$");
	protected boolean link(Request req, Response res, File f){
		try {
			Long length = f.length();
			res.setHeader("Content-Type", contentType(f.getName()) + "; charset=utf-8");
			res.setHeader("Content-Disposition",String.format("attachment; filename=%s",f.getName()));
			String range = req.getHeader("Range");
			if(null!=range){//断点下载
				Matcher matcher = RANGE.matcher(range);
				if(matcher.find()){
					String start = matcher.group(1);
					Long startIndex = null!=start&&start.length()>0?Long.parseLong(start):0L;
					String end = matcher.group(3);
					Long endIndex = null!=end&&end.length()>0?Long.parseLong(end):length-1;
					String contentRange = String.format("bytes %s-%s/%s",startIndex,endIndex,length);
					res.setCode(ResponseCode.PARTIAL_CONTENT);
					res.setHeader("Content-Length", String.valueOf(endIndex-startIndex+1));
					res.setHeader("Content-Range",contentRange);
					res.setBody(Tool.subInputStream(new FileInputStream(f),startIndex,endIndex));
					return true;
				}
			}
			res.setCode(ResponseCode.OK);
			res.setHeader("Accept-Ranges", "bytes");//支持断点下载
			res.setHeader("Content-Length", String.valueOf(length));
			res.setBody(new FileInputStream(f));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return quickFinish(res, ResponseCode.ERROR, e.getLocalizedMessage());
		}
	}
}
