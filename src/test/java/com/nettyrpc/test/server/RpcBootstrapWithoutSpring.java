package com.nettyrpc.test.server;

import com.nettyrpc.registry.ServiceRegistry;
import com.nettyrpc.server.RpcServer;
import com.nettyrpc.test.client.HelloService;
import com.nettyrpc.test.client.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcBootstrapWithoutSpring {
    private static final Logger logger = LoggerFactory.getLogger(RpcBootstrapWithoutSpring.class);

    public static void main(String[] args) {

        /**
         * socket 服务地址
         */
        String serverAddress = "127.0.0.1:18866";
        /**
         * zookeeper服务地址
         */
        ServiceRegistry serviceRegistry = new ServiceRegistry("127.0.0.1:2181");
        /**
         * rpc服务初始化
         */
        RpcServer rpcServer = new RpcServer(serverAddress, serviceRegistry);
        HelloService helloService = new HelloServiceImpl();
        PersonService personService = new PersonServiceImpl();
        /**
         * 把对象和接口存入内存中
         * 后续可以用对象进行计算
         */
        rpcServer.addService("com.nettyrpc.test.client.PersonService",personService);
        rpcServer.addService("com.nettyrpc.test.client.HelloService", helloService);
        try {
            /**
             * 启动服务
             */
            rpcServer.start();
        } catch (Exception ex) {
            logger.error("Exception: {}", ex);
        }
    }
}
