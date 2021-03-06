package net.launcher.utils;


import org.to2mbn.jmccc.mcdownloader.download.concurrent.Callback;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author ci010
 */
public class Tasks
{
	public static <T> Optional<T> optional(Callable<T> callable)
	{
		try {return Optional.ofNullable(callable.call());}
		catch (Exception e) {return Optional.empty();}
	}

	public static <T> T optional(Callable<T> callable, Supplier<T> fallback)
	{
		Objects.requireNonNull(fallback);
		try {return callable.call();}
		catch (Exception e) {return fallback.get();}
	}

	public interface TaskBuilder<T> extends javafx.util.Builder<Callable<T>>
	{
		TaskBuilder<T> setDone(Consumer<T> done);

		TaskBuilder<T> setException(Consumer<Throwable> exception);

		TaskBuilder<T> setCancel(Runnable cancel);
	}

	public static <T> TaskBuilder<T> builder(Callable<T> callable)
	{
		return new TaskBuilderImpl<>(callable);
	}

	public static <T> Callable<T> wrap(Callable<T> callable, Callback<T> callback)
	{
		Objects.requireNonNull(callable);

		Callback<T> back = callback == null ? empty() : callback;
		return () ->
		{
			try
			{
				T call = callable.call();
				back.done(call);
				return call;
			}
			catch (Exception e)
			{
				back.failed(e);
				throw e;
			}
		};
	}

	public static <T> Callable<T> wrapFallback(Callable<T> primary, Callable<T> fallback, Callback<T> callback)
	{
		Objects.requireNonNull(primary);
		Objects.requireNonNull(fallback);
		Callback<T> back = callback == null ? empty() : callback;

		return () ->
		{
			try
			{
				T call = primary.call();
				back.done(call);
				return call;
			}
			catch (Exception e)
			{
				try
				{
					T call = fallback.call();
					back.done(call);
					return call;
				}
				catch (Exception fail)
				{
					fail.addSuppressed(e);
					back.failed(fail);
					throw fail;
				}
			}
		};
	}

	public static <T> Callback<T> empty()
	{
		return new Callback<T>()
		{
			@Override
			public void done(T result) {}

			@Override
			public void failed(Throwable e) {}

			@Override
			public void cancelled() {}
		};
	}

	private static class TaskDeco<T> implements Callback<T>, Callable<T>
	{
		private Consumer<T> r;
		private Consumer<Throwable> ex;
		private Runnable can;
		private Callable<T> c;

		public TaskDeco(Consumer<T> r, Consumer<Throwable> ex, Runnable can, Callable<T> c)
		{
			this.r = r;
			this.ex = ex;
			this.can = can;
			this.c = c;
		}

		@Override
		public void done(T result)
		{
			if (r != null) r.accept(result);
		}

		@Override
		public void failed(Throwable e)
		{
			if (ex != null) ex.accept(e);
		}

		@Override
		public void cancelled()
		{
			if (can != null) can.run();
		}

		@Override
		public T call() throws Exception
		{
			try
			{
				T call = c.call();
				done(call);
				return call;
			}
			catch (Exception e)
			{
				failed(e);
				throw e;
			}
		}
	}

	public static class TaskBuilderImpl<T> implements TaskBuilder<T>
	{
		private Consumer<T> done;
		private Consumer<Throwable> exception;
		private Runnable cancel;

		private Callable<T> call;

		public TaskBuilderImpl(Callable<T> call)
		{
			this.call = call;
		}

		public TaskBuilderImpl<T> setDone(Consumer<T> done)
		{
			this.done = done;
			return this;
		}

		public TaskBuilderImpl<T> setException(Consumer<Throwable> exception)
		{
			this.exception = exception;
			return this;
		}

		public TaskBuilderImpl<T> setCancel(Runnable cancel)
		{
			this.cancel = cancel;
			return this;
		}

		@Override
		public Callable<T> build()
		{
			return new TaskDeco<>(done, exception, cancel, call);
		}
	}
}
