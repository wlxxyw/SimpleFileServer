package xyw;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Tool {
	public static Integer BUFFER_SIZE = 1024;
	public static Integer TIME_OUT = 1000;
	public static Integer POOL_SIZE = 64;
	private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(POOL_SIZE);
	
	/**
	 * byte数组拼接
	 * @param bss
	 * @return
	 */
	public static byte[] join(byte[]...bss){
		byte[] bs = new byte[0];
		if(null==bss||0==bss.length)return bs;
		for(byte[]item : bss){
			byte[] temp = new byte[bs.length+item.length];
			System.arraycopy(bs, 0, temp, 0, bs.length);
			System.arraycopy(item, 0, temp, bs.length, item.length);
			bs = temp;
		}
		return bs;
	}
	/**
	 *  直到 \n 或者 \r 或者 \r\n 或者 \n\r
	 * @param byte[] 
	 * @param keep 是否保留换行符
	 * @return 字节数组
	 */
	public static byte[][] splitLine(byte[] bs,boolean keep){
		List<byte[]> result = new ArrayList<byte[]>();
		int lineStart = 0;
		int lineEnd = -1;
		for(int i=0;i<bs.length;i++){
			//当出现连续的\r\n时 i指向前一个 lineEnd指向后一个,否则i与lineEnd指向同一个
			if(bs[i]=='\n'){
				lineEnd = (i+1<bs.length && bs[i+1]=='\r')?i+1:i;
			}else
			if(bs[i]=='\r'){
				lineEnd = (i+1<bs.length && bs[i+1]=='\n')?i+1:i;
			}else
			if(i==bs.length-1){
				lineEnd = i;
			}
			//只有上述三种情况满足之一时下面的条件才会成立
			if(lineEnd>=i){
				//当不保留时从分隔符前结束,当保留时从分隔符后面结束
				//分隔符为一个时 lineEnd=i lineEnd+1与i相差1
				//当分隔符为两个时 lineEnd=i+1 lineEnd+1与i相差2
				int size = (keep?(lineEnd+1):i)-lineStart;
				byte[] line = new byte[size];
				System.arraycopy(bs, lineStart, line, 0, size);
				result.add(line);
				lineStart=lineEnd+1;
				//当发现两个分隔符时i跳过一个
				i=lineEnd;
			}
			
		}
		return result.toArray(new byte[result.size()][]);
	}
	/**
	 * 读取流
	 * @param is 输入流
	 * @param close 完成后是否关闭流
	 * @return 字节数组
	 * @throws IOException
	 */
	public static byte[] read(InputStream is,boolean close) throws IOException{
		int length = 0;
		try {
			length = is.available();
		} catch (IOException e) {
		}
		if(0==length){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[BUFFER_SIZE];
			while(true){
				int size = is.read(buffer);
				if(size<=0){break;}
				baos.write(buffer, 0, size);
			}
			if(close)is.close();
			return baos.toByteArray();
		}else{
			byte[] bs = new byte[length];
			is.read(bs);
			if(close)is.close();
			return bs;
		}
	}
	/**
	 * 将输入流的内容写入输出流
	 * @param is 输入流
	 * @param os 输出流
	 * @param closeIs 完成后关闭输入流
	 * @param closeOs 完成后关闭输出流
	 * @return 内容长度
	 * @throws IOException
	 */
	public static Integer link(InputStream is,OutputStream os,boolean closeIs, boolean closeOs) throws IOException{
		int length = 0;
		byte[] buffer = new byte[BUFFER_SIZE];
		while(true){
			int size = is.read(buffer);
			if(size<=0){break;}
			length += size;
			os.write(buffer, 0, size);
			os.flush();
		}
		if(closeIs)is.close();
		if(closeOs)os.close();
		return length;
	}
	/**
	 * 包装输入流,处理堵塞的输入流 使用默认超时时间
	 * @param is
	 * @return
	 */
	public static InputStream waitTimeout(final InputStream is){
		return new InputStream(){
			@Override
			public int read() throws IOException {
				Future<Integer> data = pool.submit(new Callable<Integer>() {
					@Override
					public Integer call() throws Exception {
						return is.read();
					}
				});
				try {
					return data.get(TIME_OUT, TimeUnit.MICROSECONDS);
				}catch (TimeoutException e){
					return -1;
				} catch (Throwable e) {
					e.printStackTrace();
					return -1;
				}
			}
			
		};
	}
	/**
	 * 连接输入流,当一个输入流结束后再读取下一个输入流
	 * @param iss
	 * @return
	 */
	public static InputStream append(final InputStream...iss){
		return new InputStream(){
			private final InputStream[] is = iss;
			private int index = 0;
			@Override
			public int read() throws IOException {
				int data = -1;
				try{
					data = is[index].read();
					if(data==-1){
						index += 1;
						if(index<is.length){return read();}
					}
				}catch(IOException e){
					e.printStackTrace();
					throw e;
				}catch (Throwable e) {
					e.printStackTrace();
					index += 1;
					if(index<is.length){return read();}
				}
				return data;
			}
		};
	}
	public static boolean isEmpty(Collection<?> list){
		if(null==list||list.isEmpty()){
			return true;
		}
		for(Object obj:list){
			if(obj instanceof Map){
				if(!isEmpty((Map<?,?>)obj))return false;
			}else if(obj instanceof Collection){
				if(!isEmpty((Collection<?>)obj))return false;
			}else if(obj.getClass().isArray()){
				if(isEmpty(Arrays.asList((Object[])obj)))return false;
			}else if(null!=obj)return false;
		}
		return true;
	}
	public static boolean isEmpty(Map<?,?> map){
		if(null==map||map.isEmpty()){
			return true;
		}
		return isEmpty(map.values());
	}
	@SuppressWarnings("resource")
	public static InputStream getInputStream(String path){
		InputStream is = null;
		File file = new File(path);
		if(file.isFile()){
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException e) {}
		}
		if(null==is){
			is = Tool.class.getResourceAsStream(path);
		}
		if(null==is){
			is = Tool.class.getClassLoader().getResourceAsStream(path);
		}
		if(null==is){
			throw new RuntimeException("Not Found path:"+path);
		}
		return is;
	}
}
