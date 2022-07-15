package xyw;

import lombok.Getter;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static xyw.Tool.*;
import static xyw.Constant.*;

@Getter
public class Request {
	String method;
	String path;
	Map<String, String> headers = new HashMap<String, String>();
	Map<String, String> pathParams = new HashMap<String, String>();
	InputStream body;
	boolean skip = false;
	public Request(InputStream is){
		is = waitTimeout(is);
		ReadLineStrust readLineStrust = readLine(is,5,false);
		String line = new String(readLineStrust.line,UTF8);
		Logger.debug("接收到请求>{}",line);
		if(isEmpty(line)){this.skip = true;return;}
		String[] strs = line.split("\\s{1,}");
		if(strs.length!=3){
			Logger.info("无法分析的请求: {}", line);
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
			Logger.debug("解析请求>{} --> {}",line,this.path);
			if(paths.length==2){
				Logger.debug("解析请求url参数>{}",paths[1]);
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
		ReadLineStrust header = readLine(is,5,false);
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
			throw new RuntimeException("无法分析的请求头:"+headerStr);
		}
		return initHeader(header.input);
	}
	public boolean skip(){return skip;}
	public String getParam(String key){
		return this.pathParams.get(key);
	}
	public String getHeader(String key){
		return this.headers.get(key);
	}
}
