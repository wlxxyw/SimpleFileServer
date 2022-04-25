package xyw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class Constant {
	public final static String METHOD_GET = "GET";
	public final static String METHOD_PUT = "PUT";
	public final static String METHOD_POST = "POST";
	public final static String METHOD_DELETE = "DELETE";
	public final static String METHOD_OPTION = "OPTION";
	public final static String DEFAULT_MIME = "application/octet-stream";
	public final static String DEFAULT_HTML = "text/html;charset=utf-8";
	public final static String DEFAULT_JSON = "text/json;charset=utf-8";
	public final static Charset UTF8 = Charset.forName("UTF-8");
	public final static SimpleDateFormat YYYY_MM_DD_HH_MM_SS = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
	public final static Map<String, String> MIME = new HashMap<String, String>();
	public static byte[] HTML_TEMPLATE;
	static{
		BufferedReader bReader = new BufferedReader(new InputStreamReader(Tool.getResourceAsStream("mime.ini")));
		while(true){
			try {
				String line = bReader.readLine();
				if(null==line){break;}
				String[]kv = line.split("=",2);
				if(kv.length!=2)continue;
				MIME.put(kv[0].trim(), kv[1].trim());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			HTML_TEMPLATE = Tool.read(Tool.getResourceAsStream("template.html"), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static String contentType(String name){
		int index = name.lastIndexOf('.');
		if(-1==index||index==name.length()-1){
			return DEFAULT_MIME;
		}
		String type = MIME.get(name.substring(index+1));
		return null == type?DEFAULT_MIME:type;
	}
}
