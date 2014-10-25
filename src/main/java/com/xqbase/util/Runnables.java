package com.xqbase.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Runnables {
	private static AtomicInteger threadNum = new AtomicInteger(0);

	public static int getThreadNum() {
		return threadNum.get();
	}

	public static Runnable wrap(Runnable runnable) {
		String suffix = Log.suffix.get();
		// t.getCause() is atop t, see Log.concat() for more details
		Throwable t = new Throwable(Log.throwable.get());
		return () -> {
			threadNum.incrementAndGet();
			Log.suffix.set(suffix);
			Log.throwable.set(t);
			try {
				runnable.run();
			} catch (Error | RuntimeException e) {
				Log.e(e);
			} finally {
				Log.throwable.remove();
				Log.suffix.remove();
				threadNum.decrementAndGet();
			}
		};
	}

	public static <V> Callable<V> wrap(Callable<V> callable) {
		String suffix = Log.suffix.get();
		// t.getCause() is atop t, see Log.concat() for more details
		Throwable t = new Throwable(Log.throwable.get());
		return () -> {
			threadNum.incrementAndGet();
			Log.suffix.set(suffix);
			Log.throwable.set(t);
			try {
				return callable.call();
			} catch (Error e) {
				throw new Exception(e);
			} finally {
				Log.throwable.remove();
				Log.suffix.remove();
				threadNum.decrementAndGet();
			}
		};
	}

	public static void shutdown(ExecutorService service) {
		service.shutdown();
		boolean interrupted = Thread.interrupted();
		boolean terminated = false;
		while (!terminated) {
			try {
				terminated = service.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}