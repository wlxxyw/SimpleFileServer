package xyw;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static xyw.Tool.*;

public class Request {
	String method;
	String path;
	Map<String, String> headers = new HashMap<String, String>();
	byte[] body;
	boolean skip = false;
	public Request(InputStream is) throws IOException{
		byte[] bs = read(waitTimeout(is),false);
		initMethod(bs);
	}
	private void initMethod(byte[]bs){
		byte[][] bss = splitLine(bs,false);
		String line;
		if(0<bss.length){
			line = new String(bss[0]);
			String[] strs = line.split("\\s");
			if(strs.length!=3){
				throw new RuntimeException("");
			}
			this.method = strs[0];
			this.path = strs[1];
		}else{
			skip = true;
		}
		int bodyStrtLine = -1;
		for(int i=1;i<bss.length;i++){
			if(bss[i].length==0){bodyStrtLine=i+1;break;}
			line = new String(bss[i]);
			String[] strs = line.split(":\\s",2);
			if(2!=strs.length){
				Logger.warn("无法分析的请求头:{}", line);
				continue;
			}
			this.headers.put(strs[0], strs[1].trim());
		}
		if(-1!=bodyStrtLine&&0<bss.length-bodyStrtLine){
			byte[][] bodys = new byte[bss.length-bodyStrtLine][];
			System.arraycopy(bss, bodyStrtLine, bodys, 0, bodys.length);
			this.body = join(bodys);
		}else{
			this.body = new byte[0];
		}
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
	public byte[] getBody() {
		return body;
	}
	public void setBody(byte[] body) {
		this.body = body;
	}
	public boolean skip(){return skip;}
}
