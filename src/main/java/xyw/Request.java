package xyw;

import lombok.Getter;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static xyw.Constant.UTF8;
import static xyw.Tool.*;
import static xyw.Tool.readLine;

@Getter
public class Request {
	String method;
	String path;
	Map<String, String> headers = new HashMap<String, String>();
	Map<String, String> pathParams = new HashMap<String, String>();
	InputStream body;
	boolean skip = false;
	public Request(InputStream is){
		init(is);
	}
	private void init(InputStream is){
		byte[] firstLine = readLine(is,false);
		if(0==firstLine.length){
			this.skip = true;
			return;
		}
		String line = new String(firstLine,UTF8);
		String[] strs = line.split("\\s+");
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
			Logger.debug("解析请求 >> {} --> {}",line,this.path);
			if(paths.length==2){
				Logger.debug("解析请求url参数 >> {}",paths[1]);
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
		while(true){
			byte[] header = readLine(is,false);
			String headerStr = new String(header,UTF8);
			Logger.debug("请求头 >> {}",headerStr);
			if(isEmpty(headerStr)){
				break;
			}
			String[] headers = headerStr.split(":\\s",2);
			if(2==headers.length){
				this.headers.put(headers[0], headers[1].trim());
			}else{
				Logger.warn("无法分析的请求头:{}", headerStr);
				throw new RuntimeException("无法分析的请求头:"+headerStr);
			}
		}
		if(headers.containsKey("Content-Length")){
			this.body = limitInputStream(is,Long.parseLong(headers.get("Content-Length")));
		}else{
			this.body = is;
		}
	}
	public boolean skip(){return skip;}
	public String getParam(String key){
		return this.pathParams.get(key);
	}
	public String getHeader(String key){
		return this.headers.get(key);
	}
}
