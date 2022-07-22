package xyw;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Tool {
	public static Integer BUFFER_SIZE = 1024;
	public static Integer TIME_OUT = 1000;
	public static Integer POOL_SIZE = 32;
	private static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(POOL_SIZE);
	
	/**
	 * byte数组拼接
	 * @param bss 多个byte数组
	 * @return 合并后的数组
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
	 *  将 byte[] 数组按换行分割
	 *  直到 \n 或者 \r 或者 \r\n 或者 \n\r
	 * @param bs 数据
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
		ReadLineStrust(byte[] line,InputStream input,boolean endWithNewLine){
			this.line = line;
			this.input = input;
			this.endWithNewLine = endWithNewLine;
		}
		public byte[] line;
		public final InputStream input;
		public boolean endWithNewLine;
	}

	/**
	 * 读取流 直到遇到\r\n \n\r \r \n 中一种
	 * @param is 输入流
	 * @param keep 保留换行子节
	 * @return ReadLineStrust 结构体
	 */
	public static ReadLineStrust readLine(InputStream is,int retryTimes,boolean keep){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int _retryTimes = 0;
		try {
			while(true){
				int b = is.read();
				if(-1==b){
					_retryTimes ++;
					if(_retryTimes<retryTimes){
						continue;
					}else{
						break;
					}
				}else{
					_retryTimes = 0;
				}
				if('\r'==b||'\n'==b){
					if(keep){baos.write(b);}
					int _b = is.read();
					if(('\r'==_b||'\n'==_b)&&_b!=b){
						if(keep){baos.write(b);}
						return new ReadLineStrust(baos.toByteArray(),is,true);
					}else{
						return new ReadLineStrust(baos.toByteArray(),append(true,new ByteArrayInputStream(new byte[]{(byte) _b}),is),true);
					}
				}
				baos.write(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ReadLineStrust(baos.toByteArray(),is,false);
	}
	/**
	 * 将in写入out直到遇到 stopbytes 停止写入或者in没有next
	 * @param is 输入流
	 * @param out 写入流
	 * @param stopbytes 终止字节数组
	 * @return 匹配(true) 或者 输入流终止(false)
	 */
	public static boolean writeUntil(InputStream is,OutputStream out,byte[] stopbytes){
		byte[] buffer = new byte[stopbytes.length];
		int index = 0;
		try {
			int readnum = is.read(buffer);
			if(readnum < buffer.length){
				if(readnum>0)out.write(buffer,0,readnum);
				return false;
			}
			while(true){
				if(equals(stopbytes, buffer,index)){
					return true;
				}
				int b = is.read();
				if(b==-1){
					out.write(buffer,index,buffer.length-index);
					out.write(buffer,0,index);
					return false;
				}else{
					out.write(buffer[index]);
					buffer[index] = (byte)b;
					index++;
					if(index>=buffer.length)index=0;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
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
		} catch (IOException ignored) {
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
	public static <T> T waitAction(final Callable<T> action,long timeout) throws ExecutionException, InterruptedException, TimeoutException {
		Future<T> data = pool.submit(action);
		return data.get(timeout,TimeUnit.MILLISECONDS);
	}
	/**
	 * 包装输入流,处理堵塞的输入流 使用默认超时时间
	 * @param is
	 * @return
	 */
	public static InputStream waitTimeout(final InputStream is){
		return waitTimeout(is,TIME_OUT);
	}
	public static InputStream waitTimeout(final InputStream is, final long timeout){
		return new InputStream(){
			@Override
			public int read() throws IOException {
				long start = System.currentTimeMillis();
				Future<Integer> data = pool.submit(new Callable<Integer>() {
					@Override
					public Integer call() throws Exception {
						return is.read();
					}
				});
				try {
					return data.get(timeout, TimeUnit.MILLISECONDS);
				}catch (TimeoutException e){
					Logger.warn("timeout:{}-{}",start,System.currentTimeMillis());
					return -1;
				} catch (Throwable e) {
					e.printStackTrace();
					return -1;
				}
			}

		};
	}
	public static InputStream logInputStream(final InputStream is,final long bufferSize){
		return new InputStream(){
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			@Override
			public int read() throws IOException {
				int data = is.read();
				if(-1!=data){
					if(buffer.size()>=bufferSize){
						Logger.debug("logInputStream >> \n{}", buffer.toString());
						buffer.reset();
					}
					buffer.write(data);
				}
				return data;
			}
			@Override
			public void close() throws IOException {
				super.close();
				Logger.debug("logInputStream >> \n{}", buffer.toString());
			}

		};
	}
	/**
	 * 连接输入流,当一个输入流结束后再读取下一个输入流
	 * @param iss 将输入流串起来, 读完一个接着读另一个
	 * @return 包装输入流
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
            } catch (FileNotFoundException ignored) {}
        if (in == null)
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (null == in)
            in = Tool.class.getResourceAsStream(resource);
        if (null == in)
            in = Tool.class.getClassLoader().getResourceAsStream(resource);
        return in;
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
                    Logger.debug("delete temp dir({})...",dir.getAbsolutePath());
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

	/**
	 * 数组错位比较 eg: [1,2,3,4] [4,1,2,3] 错位 1 相等
	 * @param bs1 数组1
	 * @param bs2 数组2
	 * @param index 错位
	 * @return 是否相等
	 */
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

	private static final char[] toBase64 = {
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
			'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
	};
	private static final int[] fromBase64 = new int[256];
	static {
		Arrays.fill(fromBase64, -1);
		for (int i = 0; i < toBase64.length; i++)
			fromBase64[toBase64[i]] = i;
		fromBase64['='] = -2;
	}
	private static int outLength(byte[] src){
		int paddings = 0;
		int len = src.length;
		if (len == 0)return 0;
		if (src[len - 1] == '=') {
			paddings++;
			if (src[len - 2] == '=')
				paddings++;
		}
		if (paddings == 0 && (len & 0x3) !=  0)
			paddings = 4 - (len & 0x3);
		return 3 * ((len + 3) / 4) - paddings;
	}
	public static byte[] decode(byte[] base64Data) {
		byte[] dst = new byte[outLength(base64Data)];
		int dp = 0;
		int bits = 0;
		int shiftto = 18;
		int index =0;
		while(index<base64Data.length){
			int b = fromBase64[base64Data[index++] & 0xff];
			bits |= (b << shiftto);
			shiftto -= 6;
			if (shiftto < 0) {
				dst[dp++] = (byte)(bits >> 16);
				dst[dp++] = (byte)(bits >>  8);
				dst[dp++] = (byte)(bits);
				shiftto = 18;
				bits = 0;
			}
		}
		if (shiftto == 6) {
			dst[dp++] = (byte)(bits >> 16);
		} else if (shiftto == 0) {
			dst[dp++] = (byte)(bits >> 16);
			dst[dp++] = (byte)(bits >>  8);
		}
		return dst;
	}
}
