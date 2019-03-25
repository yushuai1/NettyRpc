package com.nettyrpc.client;

import com.nettyrpc.protocol.RpcRequest;
import com.nettyrpc.protocol.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RPCFuture for async RPC call
 * Created by luxiaoxun on 2016-03-15.
 */
public class RPCFuture implements Future<Object> {
    private static final Logger logger = LoggerFactory.getLogger(RPCFuture.class);

    private final static class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 1L;

        //future status
        private final int done = 1;
        private final int pending = 0;


        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        /**
         * 是否为true
         * @return
         */
        public boolean isDone() {
            return getState() == done;
        }
    }
    private Sync sync;
    private RpcRequest request;
    private RpcResponse response;
    private long startTime;
    private long responseTimeThreshold = 5000;

    private List<AsyncRPCCallback> pendingCallbacks = new ArrayList<AsyncRPCCallback>();
    private ReentrantLock lock = new ReentrantLock();

    public RPCFuture(RpcRequest request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        //获取独占锁。会调用tryAcquire方法，如果未获取成功，则会进入同步队列等待
        sync.acquire(-1);
        if (this.response != null) {
            return this.response.getResult();
        } else {
            return null;
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //响应中断+带超时版本的acquire
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (this.response != null) {
                return this.response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getClassName()
                    + ". Request method: " + this.request.getMethodName());
        }
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    public void done(RpcResponse reponse) {
        this.response = reponse;
        /**
         * 释放独占锁
         */
        sync.release(1);
        invokeCallbacks();
        // Threshold
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            logger.warn("Service response time is too slow. Request id = " + reponse.getRequestId() + ". Response Time = " + responseTime + "ms");
        }
    }

    private void invokeCallbacks() {
        lock.lock();
        try {
            /**
             * 遍历呗阻塞的回调队列
             */
            for (final AsyncRPCCallback callback : pendingCallbacks) {
                runCallback(callback);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 加入callback
     * @param callback
     * @return
     */
    public RPCFuture addCallback(AsyncRPCCallback callback) {
        lock.lock();
        try {
            /**
             * 如果同步状态为空闲执行runCallback
             * return getState() == done=1;  如果加锁成功
             */
            if (isDone()) {
                runCallback(callback);
            } else {
                //否则把callback放入缓存列表  理论上会先放到列表里面
                this.pendingCallbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    private void runCallback(final AsyncRPCCallback callback) {
        /**
         * 检查返回结果是否正确
         */
        final RpcResponse res = this.response;
        RpcClient.submit(new Runnable() {
            @Override
            public void run() {
                if (!res.isError()) {
                    callback.success(res.getResult());
                } else {
                    callback.fail(new RuntimeException("Response error", new Throwable(res.getError())));
                }
            }
        });
    }

//    static class Sync extends AbstractQueuedSynchronizer {
//
//        private static final long serialVersionUID = 1L;
//
//        //future status
//        private final int done = 1;
//        private final int pending = 0;
//
//        @Override
//        protected boolean tryAcquire(int arg) {
//            return getState() == done;
//        }
//
//        @Override
//        protected boolean tryRelease(int arg) {
//            if (getState() == pending) {
//                if (compareAndSetState(pending, done)) {
//                    return true;
//                } else {
//                    return false;
//                }
//            } else {
//                return true;
//            }
//        }
//
//        public boolean isDone() {
//            getState();
//            return getState() == done;
//        }
//    }
}
