package xyw.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import xyw.Handler;
import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Tool;
import xyw.Response.ResponseCode;

public class FileHandler implements Handler {
	private final String workPath;
	private final String context;
	private static final Map<String, String> MIME = new HashMap<String, String>();
	private static final String DEFAULT_MIME = "application/octet-stream";
	private static final String DEFAULT_HTML = "text/html;charset=utf-8";
	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	public static final SimpleDateFormat YYYY_MM_DD_HH_MM_SS = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
	public static final String HTML_TEMPLATE =  "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><title>SimpleFileServer</title></head><body><p>Path:{{title}}</p><table><tr><th>Name</th><th>Size</th><th>LastModified</th></tr>{{tbody}}</table><br /><footer></footer></body></html>";
    public static final String BACK_IMG_BASE64 = "<img width='16px' height='16px' src='data:image/jpeg;base64,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'>";
 
	static{
		BufferedReader bReader = new BufferedReader(new InputStreamReader(Tool.getInputStream("mime.ini")));
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
	}
	public FileHandler(String workPath,String context){
		if(!workPath.endsWith(File.separator)){workPath += File.separator;}
		if(!context.endsWith("/")){context += "/";}
		this.workPath = workPath;
		this.context = context;
		Logger.info("初始化FileHandler,workPath:{}; context:{}", workPath,context);
	}
	@Override
	public boolean handler(Request req, Response res) {
		String path = req.getPath();
		if(null==path||0==path.length()||!path.startsWith(context)){return false;}
		if(Handler.METHOD_GET.equals(req.getMethod())){
			path = path.substring(context.length());
			Logger.debug("文件请求:{}", path);
			File f = new File(workPath + path);
			if(f.isFile()){
				try {
					byte[] body = Tool.read(new FileInputStream(f), true);
					res.setCode(ResponseCode.OK);
					res.getHeaders().put("Content-Type", contentType(f.getName()));
					res.setBody(body);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if(f.isDirectory()){
				byte[] body = template(path,f.listFiles()).getBytes(DEFAULT_CHARSET);
				res.setCode(ResponseCode.OK);
				res.getHeaders().put("Content-Type", DEFAULT_HTML);
				res.setBody(body);
				return true;
			}
			Logger.warn("文件请求:{},文件不存在!", path);
		}
		return false;
	}
	private String template(String path, File...files){
		StringBuilder stringBuilder = new StringBuilder();
		if(!path.isEmpty()&&!"/".equals(path)){
			stringBuilder.append("<tr><td><a href=\"../\">")
        		.append(BACK_IMG_BASE64)
        		.append("..</a></td><td>---</td><td>---</td></tr>");
		}
		if (null == files || files.length == 0) {
            stringBuilder.append("<tr><td colspan=3>Empty Directory</td></tr>");
        } else {
            for (File _f : files) {
                stringBuilder.append("<tr><td><a href=\"")
                        .append(path)
                        .append(_f.getName())
                        .append(_f.isFile() ?"":"/")
                        .append("\">")
                        .append(_f.getName())
                        .append("</a></td><td>")
                        .append(_f.isFile() ? humanSize(_f.length()) : "---")
                        .append("</td><td>")
                        .append(YYYY_MM_DD_HH_MM_SS.format(new Date(_f.lastModified())))
                        .append("</td></tr>");
            }
        }
		return HTML_TEMPLATE.replaceAll("\\{\\{title\\}\\}", path).replaceAll("\\{\\{tbody\\}\\}", stringBuilder.toString());
	}
	private String humanSize(long size) {
        DecimalFormat df = new DecimalFormat("#.##");
        if (size > 1024 * 1024 * 1024) {
            return df.format(size / (1024 * 1024 * 1024.00)) + "G";
        }
        if (size > 1024 * 1024) {
            return df.format(size / (1024 * 1024.00)) + "M";
        }
        if (size > 1024) {
            return df.format(size / (1024.00)) + "K";
        }
        return size + "B";
    }
	private String contentType(String name){
		int index = name.lastIndexOf('.');
		if(-1==index||index==name.length()-1){
			return DEFAULT_MIME;
		}
		String type = MIME.get(name.substring(index+1));
		return null == type?DEFAULT_MIME:type;
	}
}
