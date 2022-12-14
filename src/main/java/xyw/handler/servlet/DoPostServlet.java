package xyw.handler.servlet;

import lombok.Getter;
import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;

import java.io.*;
import java.util.*;

import static java.lang.String.format;
import static xyw.Constant.*;
import static xyw.Tool.*;


public class DoPostServlet extends Servlet{
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String DISPOSITION = "Content-Disposition";
	private static final String MULTIPART = "multipart/";
	private static final String BOUNDARY_FLAG = "boundary=";
	public DoPostServlet(ServletConfig config){
		super(config);
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		if(matchContext(req)){
			if(METHOD_POST.equals(req.getMethod())){
				String path = req.getPath();
				Logger.info("文件上传请求:{}", path);
				File dir = new File(baseFile, path.substring(config.context.length()));
				try{
					MultipartUploadRequest request = new MultipartUploadRequest(req);
					List<String> successFile = new ArrayList<String>();
					List<String> existsFile = new ArrayList<String>();
					List<String> failFile = new ArrayList<String>();
					while (request.hasNext()) {
						FormFile file = request.next();
						File saveTo = new File(dir,file.getFileName());
						if(saveTo.exists()){
							Logger.warn("文件已存在:{}",file.getFileName());
							existsFile.add(file.getFileName());
							continue;
						}else if(!file.saveAs(saveTo)){
							Logger.warn("文件转存失败{}->{}",file.getFileName(),saveTo.getAbsolutePath());
							failFile.add(file.getFileName());
							continue;
						}
						successFile.add(saveTo.getName());
					}
					ResponseCode status = (successFile.isEmpty()||!existsFile.isEmpty()||!failFile.isEmpty())?ResponseCode.ERROR:ResponseCode.OK;
					Map<String,Object> result = new HashMap<String, Object>();
					result.put("successFile",successFile);
					result.put("existsFile",existsFile);
					result.put("failFile",failFile);
					res.setCode(status);
					res.getHeaders().put("Content-Type", DEFAULT_JSON);
					res.setBody(result);
					return true;
				}catch (Throwable t){
					t.printStackTrace();
					Logger.error(t.getLocalizedMessage(),t);
					return quickFinish(res, ResponseCode.ERROR,t.getLocalizedMessage());
				}
			}
		}
		return false;
	}

	static class MultipartUploadRequest implements Iterator<FormFile> {
		private final Request request;

		private FormFile currentItem;
		private boolean over = false;

		final byte[] boundary;
		final byte[] stopBoundary;
		public MultipartUploadRequest(Request request){
			this.request = request;
			String contentType = request.getHeader(CONTENT_TYPE);
			if ((null == contentType)|| (!contentType.toLowerCase().startsWith(MULTIPART))) {
				throw new RuntimeException(format("the request doesn't contain a %s stream, content type header is %s", "multipart/form-data", contentType));
			}
			byte[] _boundary = boundary(contentType);
			if(null==_boundary){
				throw new RuntimeException("the request was rejected because no multipart boundary was found");
			}
			boundary = join(new byte[]{'-','-'},_boundary);
			this.stopBoundary = join(boundary,new byte[]{'-','-'});
		}
		private byte[] boundary(String contentType){
			if(null==contentType)return null;
			if(contentType.startsWith(MULTIPART)){
				for(String str:contentType.split(";")){
					if(null!=str&&str.trim().startsWith(BOUNDARY_FLAG)){
						return str.trim().substring(BOUNDARY_FLAG.length()).getBytes();
					}
				}
			}
			return null;
		}
		@Override
		public boolean hasNext() {
			if(over)return false;
			InputStream is = request.getBody();
			if(writeUntil(is, null, join(boundary,new byte[]{'\r','\n'}))){
				try{
					Map<String,String> header = new HashMap<String, String>();
					File tempFile = File.createTempFile("multipart",".dat");
					byte[] oneline;
					do{
						oneline = readLine(is, false);
						String line = new String(oneline,UTF8);
						if(line.contains(":")){
							header.put(line.split(":")[0], line.split(":")[1].trim());
						}
					}while(oneline.length>0);
					if(header.containsKey(DISPOSITION)){
						String disposition = header.get(DISPOSITION);
						String[] strs = disposition.split(";");
						for(String str:strs){
							if(str.contains("=")){
								String[] _strs = str.split("=");
								if(_strs.length==2){
									String v = _strs[1].trim();
									if(v.startsWith("\"")&&v.endsWith("\""))v=v.substring(1,v.length()-1);
									header.put(_strs[0].trim(), v);
								}
							}
						}
					}
					FileOutputStream fos = new FileOutputStream(tempFile);
					boolean flag = writeUntil(is, fos, stopBoundary);
					fos.close();
					this.currentItem = new FormFile(header,tempFile);
					return flag;
				}catch (IOException e){
					Logger.error("error {}",e.getLocalizedMessage(),e);
					over = true;
					return false;
				}
			}
			return false;
		}

		@Override
		public FormFile next() {
			return currentItem;
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	@Getter
	static class FormFile{
		String name;
		String fileName;
		Map<String,String> header;
		File tempFile;
		public FormFile(Map<String,String> header,File tempFile){
			this.name = header.containsKey("name")?header.get("name"):"";
			this.fileName = header.containsKey("filename")?header.get("filename"):"";
			this.header = header;
			this.tempFile = tempFile;
		}
		public boolean saveAs(File file){
			try{
				return tempFile.length() == link(new FileInputStream(tempFile),new FileOutputStream(file),true,true);
			}catch (IOException e){return false;}
		}
	}
}
