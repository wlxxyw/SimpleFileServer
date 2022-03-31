package xyw.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import xyw.Handler;
import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;

public class AuthHandler implements Handler {
	private static final String AUTH_KEY = "Authorization";
	private static final String AUTH_BASIC = "Basic";
	private final String AUTH;
	private final List<Pattern> whiteList;
	public AuthHandler(String name,String pwd,String ...whiteUrls){
		AUTH = name+":"+pwd;
		whiteList = new ArrayList<Pattern>();
		if(null!=whiteUrls&&whiteUrls.length>0){
			for(String whiteUrl : whiteUrls){
				whiteList.add(Pattern.compile(whiteUrl));
			}
		}
	}
	@SuppressWarnings("restriction")
	@Override
	public boolean handler(Request req, Response res) {
		String url = req.getPath();
		for(Pattern p:whiteList){
			if(p.matcher(url).find()){
				Logger.debug("认证白名单地址:{}",url);
				return false;
			}
		}
		boolean pass = false;
		Map<String, String> header = req.getHeaders();
		if(header.containsKey(AUTH_KEY)){
			String authLine =  header.get(AUTH_KEY);
			try {
				authLine = new String(new sun.misc.BASE64Decoder().decodeBuffer(authLine.substring(AUTH_BASIC.length()).trim()));
				pass = AUTH.equals(authLine.trim());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(pass){
			Logger.debug("认证成功!");
			res.getHeaders().put(AUTH_KEY, header.get(AUTH_KEY));
			return false;
		}else{
			Logger.info("认证拦截!请求地址:{}",url);
			res.setCode(ResponseCode.AUTH);
			res.getHeaders().put("WWW-Authenticate", "Basic realm=\"default\"");
			return true;
		}
	}
}
