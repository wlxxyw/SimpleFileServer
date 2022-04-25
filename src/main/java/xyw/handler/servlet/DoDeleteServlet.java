package xyw.handler.servlet;

import static xyw.Constant.*;
import java.io.File;
import java.io.IOException;
import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Tool;
import xyw.Response.ResponseCode;

public class DoDeleteServlet extends Servlet{
	private final String workPath;
	public DoDeleteServlet(String workPath,String context){
		super(context);
		if(!workPath.endsWith(File.separator)){workPath += File.separator;}
		this.workPath = workPath;
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		if(METHOD_DELETE.equals(req.getMethod())){
			String path = req.getPath();
			String dir = "";
			try {
				dir = new String(Tool.read(req.getBody(), false),UTF8);
			} catch (IOException e) {
				quickFinish(res, ResponseCode.ERROR,e.getLocalizedMessage());
				e.printStackTrace();
				return true;
			}
			Logger.debug("新建文件夹请求:{}", dir);
			File f = new File(workPath + path.substring(context.length()),dir);
			if(!f.exists()){
				quickFinish(res, ResponseCode.ERROR,"文件路径不存在!");
			}else {
				try {
					if(f.delete()){
						quickFinish(res, ResponseCode.OK,"文件删除成功!");
					}else{
						quickFinish(res, ResponseCode.ERROR,"文件删除失败!");
					}
				} catch (Throwable t) {
					quickFinish(res, ResponseCode.ERROR,t.getLocalizedMessage());
				}
			}
			return true;
		}
		return false;
	}
}
