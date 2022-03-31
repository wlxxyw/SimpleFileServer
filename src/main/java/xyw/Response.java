package xyw;

import java.util.HashMap;
import java.util.Map;

import static xyw.Tool.*;

public class Response {
	public Response(){
		this.code = ResponseCode.NOT_FOUNT;
		this.headers = new HashMap<String, String>();
		this.body = new byte[0];
	}
	public Response(int code){
		for(ResponseCode _code:ResponseCode.values()){
			if(_code.code == code){
				this.code = _code;break;
			}
		}
		this.headers = new HashMap<String, String>();
		this.body = new byte[0];
	}
	public static enum ResponseCode{
		OK(200,"OK"),
		AUTH(401,"NOT_FOUNT","WWW-Authenticate: Basic realm=\"default\"","Content-Type: text/html"),
		NOT_FOUNT(404,"NOT_FOUNT","Content-Type: text/html"),
		ERROR(500,"SERVER ERROR","Content-Type: text/html");
		private final int code;
		private final String msg;
		private final Map<String, String> headers = new HashMap<String, String>();
		ResponseCode(int code,String msg){
			this.code = code;
			this.msg = msg;
		}
		ResponseCode(int code,String msg,String...headers){
			this.code = code;
			this.msg = msg;
			for(String header:headers){
				String[] strs = header.split(":\\s",2);
				this.headers.put(strs[0],strs[1]);
			}
		}
	}
	ResponseCode code;
	Map<String, String> headers;
	byte[] body;
	public byte[] toByte(){
		StringBuilder stringBuilder = new StringBuilder("HTTP/1.1 ");
		stringBuilder.append(this.code.code).append(" ").append(this.code.msg).append("\r\n");
		Map<String, String> _temp;
		if(this.code.headers.isEmpty()){
			_temp = this.headers;
		}else{
			_temp = new HashMap<String, String>(this.headers);
			_temp.putAll(this.code.headers);
		}
		_temp.putAll(this.code.headers);
		for(Map.Entry<String, String>entry:_temp.entrySet()){
			stringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
		}
		stringBuilder.append("\r\n");
		return join(stringBuilder.toString().getBytes(),body);
	}
	public ResponseCode getCode() {
		return code;
	}
	public void setCode(ResponseCode code) {
		this.code = code;
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
}
