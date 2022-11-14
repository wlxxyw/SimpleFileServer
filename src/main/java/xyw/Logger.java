package xyw;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logger {
	@AllArgsConstructor@Getter
	enum LoggerLevel{
		DEBUG(30,"\033[1;32m[DEBUG]\033[0m ","[DEBUG]"),
		INFO(20,"[INFO] ","[INFO]"),
		WARN(10,"\033[1;33m[WARN]\033[0m ","[WARN]"),
		ERROR(0,"\033[1;31m[DEBUG]\033[0m ","[DEBUG]");
		public final int level;
		public final String ansiText;
		public final String text;
	}
	interface Supplier<PrintStream> { PrintStream get();}
	static class RefreshableSupplier<PrintStream> implements Supplier<PrintStream> {
		private volatile PrintStream cache;
		private final String cacheName;
		private final Supplier<PrintStream> supplier;
		private final int refreshInterval;
		private ScheduledThreadPoolExecutor executor;
		private Future<?> refreshFuture;
		public RefreshableSupplier(String cacheName,Supplier<PrintStream> supplier, int refreshInterval) {
			if (supplier == null) {
				throw new IllegalArgumentException("数据提供器不能为空");
			}
			this.cacheName = cacheName;
			this.supplier = supplier;
			this.refreshInterval = refreshInterval;
			runRefresher();
		}
		private void runRefresher() {
			if (executor != null) {
				return;
			}
			synchronized (this) {
				if (executor != null) {return;}
				cache = supplier.get();
				if (refreshInterval <= 0) {return;}
				executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory(){
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(r, cacheName);
					}
				});
				refreshFuture = executor.scheduleAtFixedRate(
						new Runnable() {
							@Override
							public void run() {
								cache = supplier.get();
							}
						}
				, refreshInterval, refreshInterval, TimeUnit.MILLISECONDS);
			}
		}
		protected void finalize() {
			if (executor != null && !executor.isShutdown()) {
				executor.shutdown();
			}
			if (refreshFuture != null) {
				refreshFuture.cancel(true);
			}
		}
		@Override
		public PrintStream get() {
			return cache;
		}
	}
	private static final Executor LOG_POOL = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r,"Logger");
		}
	});
	private static final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.SSS");
	private static final Pattern paramPattern = Pattern.compile("\\{(\\d*)}");
	private static final Supplier<PrintStream> LOG_PRINT;
	private static final Boolean ANSI_TEXT;
	private static final LoggerLevel LOGGER_LEVEL;
	static {
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));
		boolean defaultConfig = Boolean.getBoolean("DEV");
		LOG_PRINT = defaultConfig?new Supplier<PrintStream>() {
			@Override
			public PrintStream get() {
				return System.out;
			}
		} : new RefreshableSupplier<PrintStream>("LoggerPrintStreamRefreshableSupplier", new Supplier<PrintStream>() {
			@Override
			public PrintStream get() {
				try{
					return new PrintStream(new FileOutputStream(new File(System.getProperty("user.dir"),String.format("Logger-%tF.log",new Date())),true));
				}catch (Throwable t){
					t.printStackTrace();
					return System.out;
				}
			}
		},1000);
		ANSI_TEXT = defaultConfig;
		LOGGER_LEVEL = defaultConfig?LoggerLevel.DEBUG:LoggerLevel.INFO;
	}

	public static void debug(final String regex, Object... args) {
		print(Thread.currentThread().getName(), stackTraceElement(), LoggerLevel.DEBUG, new Date(), regex, args);
	}

	public static void info(final String regex, Object... args) {
		print(Thread.currentThread().getName(), stackTraceElement(), LoggerLevel.INFO, new Date(), regex, args);
	}

	public static void warn(final String regex, Object... args) {
		print(Thread.currentThread().getName(), stackTraceElement(), LoggerLevel.WARN, new Date(), regex, args);
	}
	public static void error(final String regex, Object... args) {
		print(Thread.currentThread().getName(), stackTraceElement(), LoggerLevel.ERROR, new Date(), regex, args);
	}

	private static StackTraceElement stackTraceElement(){
		StackTraceElement[] stes = new Exception().getStackTrace();
		for(StackTraceElement ste:stes){
			if(!Logger.class.getName().equals(ste.getClassName())){
				return ste;
			}
		}
		return null;
	}
	private static Throwable formatter(StringBuffer sb, String template, Object... args) {
		Matcher matcher = paramPattern.matcher(template);
		int index = 0;
		while (matcher.find()) {
			String _index = matcher.group(1);
			if (null != _index && _index.trim().length() > 0) {
				matcher.appendReplacement(
						sb,
						String.valueOf(args[Integer.parseInt(_index)]).replace("\\", "\\\\").replace("$","\\$"));
			} else {
				matcher.appendReplacement(sb, String.valueOf(args[index])
						.replace("\\", "\\\\").replace("$","\\$"));
			}
			if (index++ == args.length) {
				break;
			}
		}
		matcher.appendTail(sb);
		if(args.length>0&&args[args.length-1] instanceof Throwable){
			return ((Throwable)args[args.length-1]);
		}
		return null;
	}

	private static void print(final String threadName,final StackTraceElement ste,final LoggerLevel level,final Date time,final String msg,final Object...args) {
		if(level.level<=LOGGER_LEVEL.level)
		LOG_POOL.execute(new Runnable() {
			@Override
			public void run() {
				StringBuilder cache = new StringBuilder();
				cache.append(threadName);
				cache.append(" ");
				synchronized (sdf){
					cache.append(sdf.format(time));
				}
				cache.append(" ");
				if(null!=ste)cache.append(ste.getClassName()).append(".").append(ste.getMethodName()).append("[").append(ste.getLineNumber()).append("] ");
				StringBuffer sub = new StringBuffer();
				Throwable t = formatter(sub,msg,args);
				cache.append(sub);
				PrintStream printStream = LOG_PRINT.get();
				printStream.print(ANSI_TEXT ?level.ansiText:level.text);
				printStream.println(cache);
				if(null!=t){
					t.printStackTrace(printStream);
				}
				printStream.flush();
			}
		});
	}
}
