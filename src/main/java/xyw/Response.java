package xyw;

import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static xyw.Tool.*;
import static xyw.Constant.*;

@Getter@Setter
public class Response {
	public Response(){
		this.code = ResponseCode.NOT_FOUND;
		this.headers = new HashMap<String, String>();
		this.body = new ByteArrayInputStream(new byte[0]);
	}
	public Response(int code){
		for(ResponseCode _code:ResponseCode.values()){
			if(_code.code == code){
				this.code = _code;break;
			}
		}
		this.headers = new HashMap<String, String>();
		this.body = new ByteArrayInputStream(new byte[0]);
	}
	public enum ResponseCode{
		OK(200,"OK"),
		PARTIAL_CONTENT(206,"Partial Content"),
		MOVED_PERMANENTLY(301,"MOVED_PERMANENTLY"),
		Found(302,"Found"),
		SEE_OTHER(303,"SEE_OTHER"),
		NOT_MODIFIED(304,"Not Modified"),
		AUTH(401,"NOT_FOUNT","WWW-Authenticate: Basic realm=\"default\"","Content-Type: text/html"),
		NOT_FOUND(404,"NOT_FOUNT","Content-Type: text/html"),
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
	InputStream body;
	private boolean wrote = false;
	public void write(OutputStream os) throws IOException{
		if(wrote)throw new RuntimeException("Multiple Operation!");
		wrote = true;
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
		os.write(stringBuilder.toString().getBytes(UTF8));
		link(body, os, true, true);
	}
	public void setBody(byte[] body) {
		this.body = new ByteArrayInputStream(body);
	}
	public void setBody(Object data) {
		setBody(Tool.toJson(data));
	}
	public void setBody(String msg) {
		setBody(msg.getBytes(UTF8));
	}
	public void setBody(InputStream body) {
		this.body = body;
	}
	public void setHeader(String k,String v){
		this.headers.put(k,v);
	}
}
