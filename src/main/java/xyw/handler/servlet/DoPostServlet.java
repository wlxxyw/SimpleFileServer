package xyw.handler.servlet;

import static xyw.Constant.*;
import static xyw.Tool.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Tool;
import xyw.Response.ResponseCode;
import xyw.Tool.ReadLineStrust;


public class DoPostServlet extends Servlet{
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String DISPOSITION = "Content-Disposition";
	private static final String MULTIPART = "multipart/form-data";
	private static final String BOUNDARY = "boundary=";
	private static final byte[] BOUNDARY_START = new byte[]{'\r','\n'};
	private static final byte[] BOUNDARY_END = new byte[]{'-','-'};
	private final String workPath;
	public DoPostServlet(String workPath,String context){
		super(context);
		if(!workPath.endsWith(File.separator)){workPath += File.separator;}
		this.workPath = workPath;
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		if(METHOD_POST.equals(req.getMethod())){
			String path = req.getPath();
			Logger.debug("文件上传请求:{}", path);
			String boundary = boundary(req);
			File dir = new File(workPath + path.substring(context.length()));
			if(null==boundary){
				quickFinish(res, ResponseCode.ERROR,"未能从请求头获取boundary!");
			}else if(!dir.isDirectory()){
				quickFinish(res, ResponseCode.ERROR,"父级路径不是文件夹!");
			}else{
				if(paser(req.getBody(), boundary.getBytes(), dir)){
					quickFinish(res,ResponseCode.OK,"上传成功!");
//					res.setCode(ResponseCode.SEE_OTHER);
//					res.getHeaders().put("Location", path);
//					res.setBody(new byte[0]);
				}else{
					quickFinish(res, ResponseCode.ERROR,"文件上传解析出错!");
				}
			}
			return true;
		}
		return false;
	}

	private String boundary(Request req){
		String contentType = req.getHeaders().get(CONTENT_TYPE);
		if(null==contentType)return null;
		if(contentType.startsWith(MULTIPART)){
			String[] strs = contentType.split(";");
			for(String str:strs){
				if(null!=str&&str.trim().startsWith(BOUNDARY)){
					return str.trim().substring(BOUNDARY.length());
				}
			}
		}
		return null;
	}
	private boolean paser(InputStream is,byte[] boundary,File dir){
		byte[] _boundary = new byte[boundary.length+4];
		System.arraycopy(BOUNDARY_START, 0, _boundary, 0, 2);
		System.arraycopy(BOUNDARY_END, 0, _boundary, 2, 2);
		System.arraycopy(boundary, 0, _boundary, 4, boundary.length);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		is = writeUntil(is,baos,boundary);
		byte[] flag = new byte[2];
		while(true){
			try {
				if(0==is.read(flag))break;
				if(Tool.equals(flag, BOUNDARY_END))break;
				if(Tool.equals(flag,BOUNDARY_START)){
					FormData formData = new FormData(dir, is, _boundary);
					is = formData.input;
				}else{
					Logger.warn("未预见的定义:{},{}",(char)flag[0],(char)flag[1]);
					return false;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	public static class FormData extends HashMap<String, String>{
		private static final long serialVersionUID = 1L;
		public final InputStream input;
		public final String name;
		public FormData(File dir,InputStream is, byte[] boundary){
			ReadLineStrust lineStrust;
			do{
				lineStrust = readLine(is, false);
				String line = new String(lineStrust.line,UTF8);
				if(line.contains(":")){
					this.put(line.split(":")[0], line.split(":")[1].trim());
				}
				is = lineStrust.input;
			}while(lineStrust.line.length>0);
			if(this.containsKey(DISPOSITION)){
				String disposition = this.get(DISPOSITION);
				String[] strs = disposition.split(";");
				for(String str:strs){
					if(str.contains("=")){
						String[] _strs = str.split("=");
						if(_strs.length==2){
							String v = _strs[1].trim();
							if(v.startsWith("\"")&&v.endsWith("\""))v=v.substring(1,v.length()-1);
							this.put(_strs[0].trim(), v);
						}
					}
				}
			}
			if(this.containsKey("name")){
				name = this.get("name");
			}else{
				name = null;
			}
			if(this.containsKey("filename")){
				String fileName = this.get("filename");
				if(Tool.isEmpty(fileName)){
					Logger.info("检测到上传空白文件({})!", name);
				}else{
					try {
						File file = this.newFileIfExist(dir,fileName);				
						this.put("file", file.getAbsolutePath());
						FileOutputStream fos = new FileOutputStream(file);
						is = writeUntil(is, fos, boundary);
						fos.close();
						Logger.info("上传文件({}:{})成功!", dir.getAbsolutePath(),fileName);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}else{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				is = writeUntil(is, baos, boundary);
				String value = new String(baos.toByteArray(),UTF8);
				put(name, value);
				Logger.info("成功获取到参数!{}:{}", name,value);
			}
			this.input = is;
		}
		private synchronized File newFileIfExist(File dir,String fileName) throws IOException{
			File file = new File(dir,fileName);	
			if(file.exists())
			if(fileName.contains(".")){
				String type = fileName.substring(fileName.lastIndexOf('.'));
				String name = fileName.substring(0,fileName.lastIndexOf('.'));
				return newFileIfExist(dir,name,type,1);
			}else{
				return newFileIfExist(dir,fileName,null,1);
			}
			file.createNewFile();
			return file;
		}
		private File newFileIfExist(File dir,String fileName,String fileType,Integer times) throws IOException{
			File file = new File(dir,null==fileType?(fileName+"."+times):(fileName+"-"+times+"."+fileType));
			if(file.exists())return newFileIfExist(dir,fileName,fileType,times+1);
			file.createNewFile();
			return file;
		}
	}
}
