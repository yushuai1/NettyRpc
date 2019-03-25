package com.nettyrpc.client;

import com.nettyrpc.protocol.RpcRequest;
import com.nettyrpc.protocol.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by luxiaoxun on 2016-03-14.
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    private ConcurrentHashMap<String, RPCFuture> pendingRPC = new ConcurrentHashMap<>();

    private volatile Channel channel;
    private SocketAddress remotePeer;

    public Channel getChannel() {
        return channel;
    }

    public SocketAddress getRemotePeer() {
        return remotePeer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client client client client ------> 客户端长连接完成");
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client client client client ------> channelRegistered");
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    /**
     * 获取服务端返回的结果信息
     * @param ctx
     * @param response
     * @throws Exception
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        System.out.println("client client client client ------> 获取服务端返回的response  根据存入的id取出rpcFuture！");
        String requestId = response.getRequestId();
        /**
         * 根据ID找出相应的缓存本地的future
         */
        RPCFuture rpcFuture = pendingRPC.get(requestId);
        if (rpcFuture != null) {
            //删除缓存
            pendingRPC.remove(requestId);
            //调用
            rpcFuture.done(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("client caught exception", cause);
        ctx.close();
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 向服务端发送请求
     * @param request
     * @return
     */
    public RPCFuture sendRequest(RpcRequest request) {
        final CountDownLatch latch = new CountDownLatch(1);
        //创建一个future
        RPCFuture rpcFuture = new RPCFuture(request);
        //把请求id和相应的future存入map中
        pendingRPC.put(request.getRequestId(), rpcFuture);
        /**
         * 把请求发送到客户端
         */
        channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                latch.countDown();
            }
        });
        System.out.println("client client client client ------> 向服务端发送请求，把请求的ID和rpcFuture存起来");
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        return rpcFuture;
    }
}
