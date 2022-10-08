package xyw;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Tool {
	public static Integer BUFFER_SIZE = 1024;
	public static Integer POOL_SIZE = 2;
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
	@Deprecated
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
	@Deprecated
	public static int readBreakIsNextLine(InputStream is,byte[] bs){
		try {
			for (int i = 0; i < bs.length; i++) {
				int value = is.read();
				if(value==-1)return -1;
				bs[i] = (byte)value;
				if(bs[i]=='\n')return i;
			}
			return bs.length;
		}catch (IOException e){
			return -1;
		}
	}
	/**
	 * 读取流 直到遇到\r\n 或者 \n
	 * @param is 输入流
	 * @return 字节数组
	 */
	public static byte[] readLine(InputStream is,boolean keep){
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			byte[] end = readLine(is, os);
			if (keep) os.write(end);
		}catch (IOException e){
			e.printStackTrace();
			return new byte[0];
		}
		return os.toByteArray();
	}
	public static byte[] readLine(InputStream is,OutputStream os) throws IOException {
		boolean next = false;
		while(true){
			int b = is.read();
			if(-1==b)return new byte[0];
			if('\n'==b){
				return next?new byte[]{'\r','\n'}:new byte['\n'];
			}
			if(next){
				next = false;
				if(null!=os)os.write('\r');
			}
			if('\r'==b){
				next = true;
				continue;
			}
			if(null!=os)os.write(b);
		}
	}
	private static boolean equals(byte[] bs1,byte[] bs2){
		for(int i=0;i<bs2.length;i++){
			if(bs1[i]!=bs2[i])return false;
		}
		return true;
	}
	public static boolean linkUntilStartWith(InputStream is, OutputStream os, byte[] startWith){
		byte[] check = new byte[startWith.length];
		try{
			byte[] lineEnds = new byte[0];
			while(true){
				os.write(lineEnds);
				is.mark(check.length);
				int num = is.read(check);
				if(check.length != num)return false;
				if(equals(startWith,check))return true;
				is.reset();
				lineEnds = readLine(is,os);
			}
		}catch (IOException e){
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * 将in写入out直到遇到 stopbytes 停止写入或者in没有next
	 * @param is 输入流
	 * @param out 写入流
	 * @param stopbytes 终止字节数组
	 * @return 匹配(true) 或者 输入流终止(false)
	 */
	@Deprecated
	public static boolean writeUntil(InputStream is,OutputStream out,byte[] stopbytes){
		byte[] buffer = new byte[stopbytes.length];
		int index = 0;
		try {
			int readNum = is.read(buffer);
			if(readNum < buffer.length){
				if(readNum>0)out.write(buffer,0,readNum);
				return false;
			}
			while(true){
				if(equals(stopbytes, buffer,index)){
					Logger.info("writeUntil readNum:{}",readNum);
					return true;
				}
				int b = is.read();
				if(b==-1){
					byte[] _buffer = new byte[buffer.length];
					System.arraycopy(buffer,index,_buffer,0,buffer.length-index);
					System.arraycopy(buffer,0,_buffer,buffer.length-index,index);
					out.write(_buffer,0,buffer.length);
					Logger.warn("readNum:{} until endWith {}!",readNum,new String(_buffer));
					return false;
				}else{
					readNum++;
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
	public static byte[] readAsBytes(InputStream is,boolean close) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[BUFFER_SIZE];
		while(true){
			int size = is.read(buffer);
			if(size<=0){break;}
			baos.write(buffer, 0, size);
		}
		if(close)is.close();
		return baos.toByteArray();
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
	public static long link(InputStream is,OutputStream os,boolean closeIs, boolean closeOs) throws IOException{
		long length = 0;
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
	 * @param is 输入流
	 * @return 包装输入流
	 */
	public static InputStream waitTimeoutInputStream(final InputStream is, final long timeout){
		return new InputStream(){
			@Override
			public int read() {
				long start = System.currentTimeMillis();
				try {
					return waitAction(new Callable<Integer>() {
						@Override
						public Integer call() throws Exception {
							return is.read();
						}
					},timeout);
				}catch (TimeoutException e){
					Logger.warn("timeout:{}-{}",start,System.currentTimeMillis());
					return -1;
				} catch (Throwable e) {
					e.printStackTrace();
					return -1;
				}
			}
			@Override
			public void close() throws IOException {
				is.close();
			}

			@Override
			public synchronized void mark(int limit) {is.mark(limit);}
			@Override
			public boolean markSupported(){return is.markSupported();}
			@Override
			public void reset() throws IOException {is.reset();}
		};
	}
	public static InputStream limitInputStream(final InputStream is,final long limitSize){
		return new InputStream(){
			final AtomicInteger readNum = new AtomicInteger(0);
			final AtomicInteger limitNum = new AtomicInteger(0);
			@Override
			public int read() throws IOException {
				if(readNum.getAndIncrement()<limitSize){
					return is.read();
				}
				Logger.warn("limitInputStream over read!");
				return -1;
			}
			@Override
			public void close() throws IOException {
				is.close();
			}
			public synchronized void mark(int limit) {
				is.mark(limit);
				limitNum.set(readNum.get());
			}
			@Override
			public boolean markSupported(){return is.markSupported();}
			@Override
			public void reset() throws IOException {
				is.reset();
				readNum.set(limitNum.get());
			}
		};
	}
	/**
	 * 连接输入流,当一个输入流结束后再读取下一个输入流
	 * @param iss 将输入流串起来, 读完一个接着读另一个
	 * @return 包装输入流
	 */
	@Deprecated
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
			@Override
			public boolean markSupported(){return false;}
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
	private static boolean isEmpty(Collection<?> list){
		for(Object obj:list){
            if(!isEmpty(obj))return false;
        }
        return false;
	}
	private static boolean isEmpty(Map<?,?> map){
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
            Logger.info("temp dir:{}",dir.getPath());
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
		}else if(bs1==null){
			return true;
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
			dst[dp] = (byte)(bits >> 16);
		} else if (shiftto == 0) {
			dst[dp++] = (byte)(bits >> 16);
			dst[dp] = (byte)(bits >>  8);
		}
		return dst;
	}
	public static String toJson(Object obj){
		/*
		 * 避免对象间循环嵌套导致堆栈溢出
		 * 原打算用 对象内存地址比较,但java的GC会导致对象的地址发生变化(在==比较的一瞬间地址是有效的,存储地址后发生过一次GC就不能保证地址仍然有效)
		 * equals 方法会调用 hashCode方法, 部分对象的hashCode方法会递归调用子元素的hashCode方法,又会导致相互调用导致堆栈异常
		 * 目前按class分组, 用 == 判断是否已进行操作
		 */
		Map<Class<?>,Collection<Value<?>>> cache = new HashMap<Class<?>,Collection<Value<?>>>();
		return toJson(obj,"$",cache);
	}
	private static String toJson(Object obj,String thisPath,Map<Class<?>,Collection<Value<?>>> cache){
		if(null==obj){//判空
			return "null";
		}
		if(obj instanceof String){//String
			return "\""+((String)obj).replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"")+"\"";
		}
		if(obj instanceof Character || obj instanceof Boolean || obj instanceof Number){//判 基础类型
			return String.valueOf(obj);
		}
		Class<?> clazz = obj.getClass();
		Collection<Value<?>> cacheCollection;
		if(cache.containsKey(clazz)){
			cacheCollection = cache.get(clazz);
		}else{
			cacheCollection = new ArrayList<Value<?>>();
			cache.put(clazz, new ArrayList<Value<?>>());
		}
		Value<?> ref = null;
		for(Value<?> oneCache:cacheCollection){
			if(oneCache.value==obj){
				ref = oneCache;break;
			}
		}
		if(null!=ref){
			return new Ref(parent(thisPath),ref.path).toString();
		}else{
			cacheCollection.add(new Value<Object>(obj,thisPath));
		}
		if(obj instanceof Map){
			Map<?, ?> map = (Map<?, ?>)obj;
			StringBuilder builder = new StringBuilder("{");
			for(Map.Entry<?, ?> entry:map.entrySet()){
				if(!(entry.getKey() instanceof String)){throw new RuntimeException("Map.Entry.getKey() must return String!");}
				String key = "\""+((String)entry.getKey()).replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"")+"\"";
				String value = toJson(entry.getValue(),thisPath+"."+entry.getKey(),cache);
				builder.append(key).append(':').append(value);
				builder.append(',');
			}
			builder.setCharAt(builder.length()-1,'}');
			//builder.append("}");
			return builder.toString();
		}
		if(obj instanceof Collection){
			Collection<?> collection = (Collection<?>)obj;
			StringBuilder builder = new StringBuilder("[");
			int index = 0;
			for(Object _obj:collection){
				if(index>0){builder.append(",");}
				builder.append(toJson(_obj,thisPath+"["+index+"]",cache));
				index++;
			}
			builder.append("]");
			return builder.toString();
		}
		if(obj.getClass().isArray()){
			Object[] array = (Object[])obj;
			StringBuilder builder = new StringBuilder("[");
			int index = 0;
			for(Object _obj:array){
				if(index>0){builder.append(",");}
				builder.append(toJson(_obj,thisPath+"["+index+"]",cache));
				index++;
			}
			builder.append("]");
			return builder.toString();
		}
		StringBuilder builder = new StringBuilder("{");
		int index = 0;
		final Method[] methods = clazz.getDeclaredMethods();
		for(Field field:clazz.getDeclaredFields()){
			String key = field.getName();
			boolean match = false;
			for(Method method:methods){
				if(method.getName().equalsIgnoreCase("get"+key)&&method.getParameterTypes().length==0&&method.getReturnType()==field.getType()){
					match = true;break;
				}
			}
			if(match){
				if(index>0){builder.append(",");}
				boolean accessible = field.isAccessible();
				field.setAccessible(true);
				String value;
				try{
					value = toJson(field.get(obj),thisPath+"."+key,cache);
				}catch (IllegalAccessException e){
					value = "";
				}
				field.setAccessible(accessible);
				builder.append(key).append(":").append(value);
				index++;
			}

		}
		builder.append("}");
		return builder.toString();
	}
	private static String parent(String thisPath){
		boolean isArray = thisPath.endsWith("]");
		for(int i=thisPath.length()-1;i>=0;i--){
			if((isArray&&thisPath.charAt(i)=='[')||thisPath.charAt(i)=='.')return thisPath.substring(0,i);
		}
		return "";
	}
	static class Value<T>{
		Value(T v, String p){
			this.value = v;
			this.path = p;
		}
		T value;
		String path;
	}
	static class Ref{
		Ref(String thisPath,String ref){
			if(thisPath.length()==0){
				this.ref = ref;
				return;
			}
			if(thisPath.equals(ref)){
				this.ref = "@";
				return;
			}
			String[] subSame = subSame(thisPath,ref);
			if(subSame[0].length()==0){
				String _ref = "@"+subSame[1];
				this.ref = ref.length()>_ref.length()?_ref:ref;
				return;
			}else if(subSame[1].length()==0){
				String _ref = subSame[0].replaceAll("\\.[^.]+","../");
				_ref = _ref.substring(0,_ref.length()-1);
				this.ref = ref.length()>_ref.length()?_ref:ref;
				return;
			}
			this.ref = ref;

		}
		String ref;

		@Override
		public String toString() {
			return "{\"$ref\":\""+ref+"\"}";
		}
		/**
		 * @param compares String数组
		 * @return 截取掉相同的开始部分(仅 '.','[',']'结尾)后余下内容
		 */
		private String[] subSame(String...compares){
			if(null==compares||compares.length<2){throw new UnsupportedOperationException("too little compares");}
			int sameIndex = 0;
			/* 获取最短数组长度 */
			String minLength = null;
			for(String compare:compares){
				if(null==minLength||compare.length()<minLength.length()){
					minLength=compare;
				}
			}
			over:
			for(int i=0;i<minLength.length();i++){
				char c = minLength.charAt(i);
				for(String compare:compares){
					if(compare.equals(minLength)){
						continue;
					}
					if(compare.charAt(i)!=c){
						break over;//跳出双循环
					}
				}
				if(c=='.'||c=='['){//不包含
					sameIndex = i;
				}
				if(c==']'||i==compares[0].length()-1){//包含
					sameIndex = i+1;
				}
			}
			if(sameIndex==0){
				return compares;
			}else{
				String[] result = new String[compares.length];
				for(int i=0;i<compares.length;i++){
					result[i] = compares[i].substring(sameIndex);
				}
				return result;
			}
		}
	}
}
