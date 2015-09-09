package org.androidbook.net;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.http.HttpRequest;

public class HttpController
{
	private Thread[] httpHreadPool;

	private LinkedList<WinHttpRequest> sendQueue;

	private boolean cancel = false;

	/* 用于同步 */
	private int[] locker = new int[0];

	public HttpController(int threadCount)
	{
		httpHreadPool = new HttpThread[threadCount];

		sendQueue = new LinkedList<WinHttpRequest>();

		for (int i = 0; i < httpHreadPool.length; i++)
		{
			httpHreadPool[i] = new HttpThread();
			httpHreadPool[i].start();
		}
	}

	class HttpThread extends Thread
	{
		public void run()
		{
			while (!cancel)
			{
				try
				{
					WinHttpRequest request = null;
					synchronized (locker)
					{
						while (sendQueue.size() == 0)
						{
							locker.wait();
							if (cancel)
							{
								return;
							}
						}
						request = sendQueue.poll();
						if( request == null)
						{
							continue;
						}
					}
					
					HttpTask httpTask = new HttpTask(request);
					httpTask.excute();

				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public synchronized boolean cancel(WinHttpRequest request)
	{
		request.iscanceled = true;
		if( sendQueue.contains(request))
		{
			sendQueue.remove(request);
			return true;
		}
		
		return false;
	}
	
	public void send(WinHttpRequest request)
	{
		synchronized (locker)
		{
			sendQueue.addLast(request);
			locker.notifyAll();
		}
	}
	
	public void close()
	{
		cancel = true;
	}
}