package xyw.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;
import xyw.Tool;
import static xyw.Constant.*;

public class AuthHandler implements Handler {
	private static final String AUTH_KEY = "Authorization";
	private static final String AUTH_BASIC = "Basic";
	private final String AUTH;
	private final List<Pattern> whiteList;
	public AuthHandler(String name,String pwd,String ...whiteUrls){
		Logger.info("init AuthHandler: {}:{}", name,pwd);
		AUTH = name+":"+pwd;
		whiteList = new ArrayList<Pattern>();
		if(null!=whiteUrls&&whiteUrls.length>0){
			for(String whiteUrl : whiteUrls){
				whiteList.add(Pattern.compile(whiteUrl));
			}
		}
	}
	@Override
	public boolean handler(Request req, Response res) {
		String url = req.getPath();
		String method = req.getMethod();
		if(!METHOD_POST.equals(method))
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
			authLine = new String(Tool.decode(authLine.substring(AUTH_BASIC.length()).trim().getBytes()));
			pass = AUTH.equals(authLine.trim());
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
