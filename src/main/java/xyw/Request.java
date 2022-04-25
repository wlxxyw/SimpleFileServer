package xyw;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static xyw.Tool.*;
import static xyw.Constant.*;

public class Request {
	String method;
	String path;
	Map<String, String> headers = new HashMap<String, String>();
	Map<String, String> pathParams = new HashMap<String, String>();
	InputStream body;
	boolean skip = false;
	public Request(InputStream is){
		ReadLineStrust readLineStrust = readLine(waitTimeout(is),false);
		String line = new String(readLineStrust.line,UTF8);
		Logger.info("接收到请求>{}",line);
		if(isEmpty(line)){this.skip = true;return;}
		String[] strs = line.split("\\s{1,}");
		if(strs.length!=3){
			Logger.warn("无法分析的请求: {}", line);
			this.skip = true;
			return;
		}
		this.method = strs[0];
		String path = strs[1];
		if(!isEmpty(path)){
			try {
				path = URLDecoder.decode(path, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			String[] paths = path.split("\\?",2);
			this.path = paths[0];
			Logger.info("解析请求>{} --> {}",line,this.path);
			if(paths.length==2){
				for(String paramStr:paths[1].split("&")){
					if(!isEmpty(paramStr)&&paramStr.contains("=")){
						String[] paramStrs = paramStr.split("=",2);
						if(paramStrs.length==2){
							pathParams.put(paramStrs[0], paramStrs[1]);
						}
					}
				}
			}
		}
		this.body = initHeader(readLineStrust.input);
	}
	private InputStream initHeader(InputStream is){
		ReadLineStrust header = readLine(is,false);
		String headerStr = new String(header.line,UTF8);
		Logger.debug(">{}",headerStr);
		if(isEmpty(headerStr)){
			return header.input;
		}
		String[] strs = headerStr.split(":\\s",2);
		if(2==strs.length){
			this.headers.put(strs[0], strs[1].trim());
		}else{
			Logger.warn("无法分析的请求头:{}", headerStr);
		}
		return initHeader(header.input);
	}
	
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Map<String, String> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	public InputStream getBody() {
		return body;
	}
	public boolean skip(){return skip;}
	public String getParam(String key){
		return this.pathParams.get(key);
	}
	public String getHeader(String key){
		return this.headers.get(key);
	}
}
