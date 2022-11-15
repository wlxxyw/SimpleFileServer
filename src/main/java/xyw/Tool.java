package xyw;


import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Tool {
	public static final Integer BUFFER_SIZE = 1024;
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
	public static byte[] readLine(InputStream is, OutputStream os) throws IOException {
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
	/**
	 * 将in写入out直到遇到 stopbytes 停止写入或者in没有next
	 * @param is 输入流
	 * @param out 写入流
	 * @param stopbytes 终止字节数组
	 * @return 匹配(true) 或者 输入流终止(false)
	 */
	public static boolean writeUntil(InputStream is,OutputStream out,byte[] stopbytes){
		try{
			long start = System.currentTimeMillis();
			int data,i = 0,readNum = 0;
			while (i<stopbytes.length) {
				data = is.read();
				readNum++;
				if(-1 == data){
					Logger.warn("writeUntil InputStream EOF! size:{}",readNum);
					return false;
				}
				if(data == stopbytes[i]){//当遇到相等时不进行自增外其他操作 直至全部相同(匹配上)跳出循环
					i++;
				}else{//或者存在不匹配数据
					if(null!=out){
						if(i!=0)out.write(stopbytes,0,i);//将前几次未写入的数据写入
						out.write(data);
					}
					i = 0;
				}
			}
			Logger.debug("writeUntil found! size:{} time:{}ms",readNum,System.currentTimeMillis()-start);
			return true;
		}catch (Throwable t){
			Logger.warn("writeUntil error! ");
			return false;
		}
	}
	/**
	 * 读取流
	 * @param is 输入流
	 * @param close 完成后是否关闭流
	 * @return 字节数组
	 * @throws IOException IO异常
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
	 * @throws IOException IO异常
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
	/**
	 * 包装输入流,处理堵塞的输入流 使用默认超时时间
	 * @param is 输入流
	 * @return 包装输入流
	 */
	public static InputStream waitTimeoutInputStream(final InputStream is, final long timeout){
		return new InputStream(){
			private final byte[] buffer = new byte[65525];
			private int head,tail;
			@Override
			public int read() {
				long start = System.currentTimeMillis();
				try {
					while(System.currentTimeMillis() < start+timeout && head==tail){
						tail = is.read(buffer,0,Math.min(buffer.length,is.available()));
						head = 0;
					}
					if(head<tail){
						return buffer[head++];
					}
					Logger.warn("timeout:{}-{}",start,System.currentTimeMillis());
					return -1;
				} catch (Throwable e) {
					Logger.warn("waitTimeoutInputStream error:{}",e.getMessage(),e);
					return -1;
				}
			}
			@Override
			public void close() throws IOException {
				is.close();
			}
		};
	}
// 效率过低 弃用
//	public static InputStream waitTimeoutInputStream(final InputStream is, final long timeout){
//		return new InputStream(){
//			private final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
//			@Override
//			public int read() {
//				long start = System.currentTimeMillis();
//				try {
//					Future<Integer> data = pool.submit(new Callable<Integer>() {
//						@Override
//						public Integer call() throws Exception {
//							return is.read();
//						}
//					});
//					return data.get(timeout,TimeUnit.MILLISECONDS);
//				}catch (TimeoutException e){
//					Logger.warn("timeout:{}-{}",start,System.currentTimeMillis());
//					return -1;
//				} catch (Throwable e) {
//					e.printStackTrace();
//					return -1;
//				}
//			}
//			@Override
//			public void close() throws IOException {
//				pool.shutdown();
//				is.close();
//			}
//
//			@Override
//			public synchronized void mark(int limit) {is.mark(limit);}
//			@Override
//			public boolean markSupported(){return is.markSupported();}
//			@Override
//			public void reset() throws IOException {is.reset();}
//		};
//	}

	/**
	 * 包装流 限制读取长度
	 */
	public static InputStream limitInputStream(final InputStream is,final long limitSize){
		return new InputStream(){
			long index;
			long markIndex;
			@Override
			public int read() throws IOException {
				if(index<limitSize){
					index++;
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
				markIndex = index;
				is.mark(limit);
			}
			@Override
			public boolean markSupported(){return is.markSupported();}
			@Override
			public void reset() throws IOException {
				index = markIndex;
				is.reset();
			}
		};
	}

	/**
	 * 包装流 截取范围
	 */
	public static InputStream subInputStream(final InputStream is,final long start,final long end){
		return new InputStream() {
			long index = 0;
			long markIndex;
			@Override
			public int read() throws IOException {
				if(index<start){
					if(start-index !=is.skip(start-index)){
						Logger.error("subInputStream skip 返回了非期望值");
						throw new IOException("subInputStream skip 返回了非期望值");
					}
					index = start;
				}
				if(index<=end){
					index++;
					return is.read();
				}
				return -1;
			}
			@Override
			public void close() throws IOException {
				is.close();
			}
			public synchronized void mark(int limit) {
				markIndex = index;
				is.mark(limit);
			}
			@Override
			public boolean markSupported(){return is.markSupported();}
			@Override
			public void reset() throws IOException {
				index = markIndex;
				is.reset();
			}
		};
	}

	/**
	 * 仅包含 '空'值的Map视为空
	 * 仅包含 '空'元素的集合/数组视为空
	 * {"a":null} -> true
	 * {null: "a"} -> false
	 * [null,null] -> true
	 * [{},{}] -> true
	 * {"a": []} -> true
	 * {"a": [{}]} -> true
	 */
	public static boolean isEmpty(Object obj){
		if(null==obj){return true;}
        boolean checked = false;
        if(obj instanceof Collection){
            checked = true;
            if(!isEmpty((Collection<?>) obj))return false;
        }
        if(obj instanceof Map){
            checked = true;
            if(!isEmpty((Map<?,?>) obj))return false;
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
            throw new RuntimeException("UNSUPPORTED DATA TYPE,Collection/Map/Array or String");
        }
        return true;
	}
	private static boolean isEmpty(Collection<?> list){
		if(null==list||list.isEmpty())return true;
		for(Object obj:list){
            if(!isEmpty(obj))return false;
        }
        return true;
	}
	private static boolean isEmpty(Map<?,?> map){
		if(null==map||map.isEmpty())return true;
		for(Object obj:map.values()){
            if(!isEmpty(obj))return false;
        }
        return true;
	}
	private static boolean isEmpty(Object[] array){
		if(null==array||0==array.length)return true;
        for(Object obj:array){
            if(!isEmpty(obj))return false;
        }
        return true;
    }

	/**
	 * 多方式获取输入流
	 */
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

	/**
	 * 解压文件到临时文件夹 返回临时文件夹路径
	 */
	public static String tempFile(String resource) {
        try {
            InputStream in = getResourceAsStream(resource);
            final File dir = new File(System.getProperty("java.io.tmpdir"),"temp-"+System.currentTimeMillis());
            if(dir.exists())dir.delete();
            dir.mkdirs();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    Logger.info("delete temp dir({})...",dir.getAbsolutePath());
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
            for (File subFile : defaultOfNull(file.listFiles(),new File[0]))
                delete(subFile);
        if (file.exists())
            file.delete();
    }
	public static <T> T defaultOfNull(T obj,T t){
		return null==obj?t:obj;
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

	/**
	 * 仅实现Base64解码 未实现Base编码
	 */
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

	/**
	 * 仅实现 对象转JSON 未实现JSON转对象
	 */
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

	/**
	 * 数据结构 包装数据及它的引用路径
	 * @param <T>
	 */
	static class Value<T>{
		Value(T v, String p){
			this.value = v;
			this.path = p;
		}
		T value;
		String path;
	}

	/**
	 * 为 toJson服务 JSON内的引用
	 */
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

	/**
	 * 包装流 读取满buffer/close时 打印读取内容
	 * @param is 原输入流
	 * @param bufferSize 缓存大小
	 * @return 包装流
	 */
	public static InputStream logInputStream(final InputStream is,final long bufferSize) {
		return new InputStream() {
			final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			@Override
			public int read() throws IOException {
				int data = is.read();
				if (-1 != data) {
					if (buffer.size() >= bufferSize) log();
					buffer.write(data);
				}else{
					log();
				}
				return data;
			}
			private synchronized void log(){
				Logger.debug("logInputStream >> \n{}", buffer.toString());
				buffer.reset();
			}
			public boolean markSupported() {
				return is.markSupported();
			}
			public synchronized void mark(int readlimit) {
				is.mark(readlimit);
			}
			public synchronized void reset() throws IOException {
				is.reset();
			}

			@Override
			public void close() throws IOException {
				is.close();
				log();
			}
		};
	}
}
