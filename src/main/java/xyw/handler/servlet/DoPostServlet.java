package xyw.handler.servlet;

import static java.lang.String.format;
import static xyw.Constant.*;
import static xyw.Tool.*;

import java.io.*;
import java.util.*;

import lombok.Getter;
import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Response.ResponseCode;
import xyw.Tool;


public class DoPostServlet extends Servlet{
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String DISPOSITION = "Content-Disposition";
	private static final String MULTIPART = "multipart/";
	private static final String BOUNDARY_FLAG = "boundary=";
	private final String workPath;
	public DoPostServlet(String workPath,String context){
		super(context);
		if(!workPath.endsWith(File.separator)){workPath += File.separator;}
		this.workPath = workPath;
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		if(matchContext(req)){
			if(METHOD_POST.equals(req.getMethod())){
				String path = req.getPath();
				Logger.debug("文件上传请求:{}", path);
				File dir = new File(workPath + path.substring(context.length()));
				try{
					MultipartUploadRequest request = new MultipartUploadRequest(req);
					while (request.hasNext()) {
						FormFile file = request.next();
						if(!file.saveAs(new File(dir,file.getFileName()))){
							quickFinish(res,ResponseCode.ERROR,"文件转存失败!");
						}
						Logger.debug("save temp file {} to {}",file.getTempFile().getAbsolutePath(),dir.getAbsolutePath()+File.separator+file.getFileName());
					}
					quickFinish(res,ResponseCode.OK,"上传成功!");
				}catch (Throwable t){
					t.printStackTrace();
					quickFinish(res, ResponseCode.ERROR,t.getLocalizedMessage());
				}
				return true;
			}
		}
		return false;
	}

	static class MultipartUploadRequest implements Iterator<FormFile> {
		private final Request request;

		private FormFile currentItem;
		private boolean over = false;

		final byte[] boundary;
		final byte[] startBoundary;
		final byte[] stopBoundary;
		public MultipartUploadRequest(Request request){
			this.request = request;
			String contentType = request.getHeader(CONTENT_TYPE);
			if ((null == contentType)|| (!contentType.toLowerCase().startsWith(MULTIPART))) {
				throw new RuntimeException(format("the request doesn't contain a %s stream, content type header is %s", "multipart/form-data", contentType));
			}
			boundary = boundary(contentType);
			if(null==boundary){
				throw new RuntimeException("the request was rejected because no multipart boundary was found");
			}
			this.startBoundary = join(new byte[]{'-','-'},boundary,new byte[]{'\r','\n'});
			this.stopBoundary = join(new byte[]{'\r','\n','-','-'},boundary,new byte[]{'-','-'});
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
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if(writeUntil(is, baos, startBoundary)){
				try{
					Map<String,String> header = new HashMap<String, String>();
					File tempFile = File.createTempFile("multipart",".dat");
					byte[] oneline;
					do{
						oneline = readLine(is, 5);
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
					Logger.warn("error {}",e.getLocalizedMessage(),e);
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
			if(file.exists()){
				return false;
			}
			try{
				return tempFile.length() == Tool.link(new FileInputStream(tempFile),new FileOutputStream(file),true,true);
			}catch (IOException e){return false;}
		}
	}
}
