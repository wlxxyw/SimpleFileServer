package xyw;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
	public static class ReadLineStrust{
		ReadLineStrust(byte[] line,InputStream input){
			this.line = line;
			this.input = input;
		}
		public byte[] line;
		public final InputStream input;
	}
	/**
	 * 读取流 直到遇到\r\n \n\r \r \n 中一种
	 * @param is
	 * @param keep 
	 * @return
	 */
	public static ReadLineStrust readLine(InputStream is,boolean keep){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			while(true){
				int b = is.read();
				if(-1==b){
					break;
				}
				if('\r'==b||'\n'==b){
					if(keep){baos.write(b);}
					int _b = is.read();
					if(('\r'==_b||'\n'==_b)&&_b!=b){
						if(keep){baos.write(b);}
						return new ReadLineStrust(baos.toByteArray(),is);
					}else{
						return new ReadLineStrust(baos.toByteArray(),append(true,new ByteArrayInputStream(new byte[]{(byte) _b}),is));
					}
				}
				baos.write(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ReadLineStrust(baos.toByteArray(),is);
	}
	/**
	 * 将in写入out直到遇到 stopbytes 停止写入或者in没有next
	 * @param is
	 * @param out
	 * @param stopbytes
	 * @return
	 */
	public static InputStream writeUntil(InputStream is,OutputStream out,byte[] stopbytes){
		byte[] buffer = new byte[stopbytes.length];
		int index = 0;
		try {
			int readnum = is.read(buffer);
			if(readnum < buffer.length){
				out.write(buffer);
				return new ByteArrayInputStream(new byte[0]);
			}
			while(true){
				if(equals(stopbytes, buffer,index)){
					return is;
				}
				int b = is.read();
				if(b==-1){
					out.write(buffer,index,buffer.length-index);
					out.write(buffer,0,index);
					return new ByteArrayInputStream(new byte[0]);
				}else{
					out.write(buffer[index]);
					buffer[index] = (byte)b;
					index++;
					if(index>=buffer.length)index=0;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
	public static InputStream append(final boolean autoClose,final InputStream...iss){
		return new InputStream(){
			private final InputStream[] is = iss;
			private int index = 0;
			@Override
			public int read() throws IOException {
				int data = -1;
				try{
					data = is[index].read();
					if(data==-1){
						if(autoClose)is[index].close();
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
	@SuppressWarnings("unchecked")
	public static boolean isEmpty(Object obj){
		if(null==obj){return true;}
        boolean checked = false;
        if(obj instanceof Collection){
            checked = true;
            if(!isEmpty((Collection<Object>) obj))return false;
        }
        if(obj instanceof Map){
            checked = true;
            if(!isEmpty((Map<Object,Object>) obj))return false;
        }
        if(obj.getClass().isArray()){
            checked = true;
            if(!isEmpty((Object[]) obj))return false;
        }
        if(obj instanceof String){
            checked = true;
            if(!((String)obj).isEmpty())return false;
        }
        if(!checked){
            throw new RuntimeException("UNSUPPORTE DATA TYPE,Collection/Map/Array or String");
        }
        return checked;
	}
	public static boolean isEmpty(Collection<?> list){
		for(Object obj:list){
            if(!isEmpty(obj))return false;
        }
        return false;
	}
	public static boolean isEmpty(Map<?,?> map){
		for(Object obj:map.values()){
            if(!isEmpty(obj))return false;
        }
        return true;
	}
	private static boolean isEmpty(Object[] array){
        for(Object obj:array){
            if(!isEmpty(obj))return false;
        }
        return false;
    }
	@SuppressWarnings("resource")
	public static InputStream getResourceAsStream(String resource) {
        InputStream in = null;
        File resourceFile = new File(System.getProperty("user.dir")+File.separator+resource);
        if (resourceFile.exists())
            try {
                in = new FileInputStream(resourceFile);
            } catch (FileNotFoundException fileNotFoundException) {}
        if (in == null)
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (null == in)
            in = Tool.class.getResourceAsStream(resource);
        if (null == in)
            in = Tool.class.getClassLoader().getResourceAsStream(resource);
        return in;
    }
	public static InputStream getResource(String path){
		InputStream is = Tool.class.getResourceAsStream(path);
		if(null==is){
			is = Tool.class.getClassLoader().getResourceAsStream(path);
		}
		return is;
	}
	public static String tempFile(String resource) {
        try {
            InputStream in = getResourceAsStream(resource);
            final File dir = new File(System.getProperty("java.io.tmpdir"),"temp-"+System.currentTimeMillis());
            if(dir.exists())dir.delete();
            dir.mkdirs();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    Logger.debug("delete temp dir ...");
                    delete(dir);
                }
            }));
            ZipInputStream zIn = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = zIn.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    File file = new File(dir, entry.getName());
                    if (!file.exists())
                        (new File(file.getParent())).mkdirs();
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = zIn.read(buf)) != -1)
                        bos.write(buf, 0, len);
                    bos.close();
                }
            }
            zIn.close();
            Logger.info("path:{}",dir.getPath());
            return dir.getPath();
        } catch (IOException e) {
        	Logger.warn("fail to read {}", resource);
            e.printStackTrace();
            return null;
        }
    }
	static void delete(File file) {
        if (file.isDirectory())
            for (File subFile : file.listFiles())
                delete(subFile);
        if (file.exists())
            file.delete();
    }
	public static boolean equals(byte[] bs1,byte[] bs2){
		return equals(bs1,bs2,0);
	}
	public static boolean equals(byte[] bs1,byte[] bs2,int index){
		if((null==bs1)^(null==bs2)){
			return false;
		}
		if(bs1.length!=bs2.length){
			return false;
		}
		if(index>bs1.length||index<bs1.length*-1){
			throw new IndexOutOfBoundsException();
		}
		if(index < 0){
			index += bs1.length;
		}
		for(int i=0;i<bs1.length;i++){
			int _i = (i+index>=bs1.length)?(i+index-bs1.length):(i+index);
			if(bs1[i]!=bs2[_i])return false;
		}
		return true;
	}
	public static String toJSON(Map<?, ?> map){
		boolean first = true;
		StringBuilder builder = new StringBuilder("{");
		for(Map.Entry<?, ?> entry:map.entrySet()){
			if(first){
				first=false;
			}else{
				builder.append(",");
			}
			builder.append(safeJson(entry.getKey())).append(":").append(safeJson(entry.getValue()));
		}
		builder.append("}");
		return builder.toString();
	}
	public static String toJSON(Collection<?> collection){
		boolean first = true;
		StringBuilder builder = new StringBuilder("[");
		for(Object obj:collection){
			if(first){
				first=false;
			}else{
				builder.append(",");
			}
			builder.append(safeJson(obj));
		}
		builder.append("]");
		return builder.toString();
	}
	private static String safeJson(Object obj){
		if(null==obj){
			return "null";
		}
		if(obj instanceof String){
			return "\""+((String)obj).replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"")+"\"";
		}
		if(obj instanceof Map){
			return toJSON((Map<?, ?>)obj);
		}
		if(obj instanceof Collection){
			return toJSON((Collection<?>)obj);
		}
		return String.valueOf(obj);
	}
}
