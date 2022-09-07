package xyw;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipInputStream;

public class Constant {
	public final static String METHOD_GET = "GET";
	public final static String METHOD_PUT = "PUT";
	public final static String METHOD_POST = "POST";
	public final static String METHOD_DELETE = "DELETE";
	public final static String METHOD_OPTIONS = "OPTIONS";
	public final static String DEFAULT_MIME = "application/octet-stream";
	public final static String DEFAULT_HTML = "text/html;charset=utf-8";
	public final static String DEFAULT_JSON = "text/json;charset=utf-8";
	public final static Charset UTF8 = Charset.forName("UTF-8");
	public final static Map<String, String> MIME = new HashMap<String, String>();
	public final static String TEMPLATE_DIR;
	public static byte[] HTML_TEMPLATE;
	static{
		TEMPLATE_DIR = Tool.tempFile("resource.zip");
		try {
			BufferedReader bReader = new BufferedReader(new FileReader(new File(TEMPLATE_DIR,"mime.ini")));
			while(true){
				String line = bReader.readLine();
				if(null==line){break;}
				String[]kv = line.split("=",2);
				if(kv.length!=2)continue;
				MIME.put(kv[0].trim(), kv[1].trim());
			}
			HTML_TEMPLATE = Tool.readAsBytes(new FileInputStream(new File(TEMPLATE_DIR,"template.html")), true);
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
