package com.nettyrpc.test.app;

import com.nettyrpc.client.AsyncRPCCallback;
import com.nettyrpc.client.RPCFuture;
import com.nettyrpc.client.RpcClient;
import com.nettyrpc.client.proxy.IAsyncObjectProxy;
import com.nettyrpc.registry.ServiceDiscovery;
import com.nettyrpc.test.client.PersonService;
import com.nettyrpc.test.client.Person;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by luxiaoxun on 2016/3/17.
 */
public class PersonCallbackTest {
    public static void main(String[] args) {
        System.out.println("client client client client ------>开始了 ");
        //获取zookeeper中的服务列表，接通socket连接以及自动更新连接
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("10.3.1.166:2181");
        System.out.println("client client client client ------> 所有连接全部搞定");

        /**
         * 初始化客户端的远程调用类
         */
        final RpcClient rpcClient = new RpcClient(serviceDiscovery);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        try {
            /**
             * 生成同步代理客户端
             */
            IAsyncObjectProxy client = rpcClient.createAsync(PersonService.class);
            System.out.println(">>>>>>>>>>>><<<<<<<<<<<<<");
            long t1= System.currentTimeMillis();
            RPCFuture helloPersonFuture = client.call("GetTestPerson", "xiaoming", 5);
            helloPersonFuture.addCallback(new AsyncRPCCallback() {
                @Override
                public void success(Object result) {
                    List<Person> persons = (List<Person>) result;
                    for (int i = 0; i < persons.size(); ++i) {
                        System.out.println(persons.get(i));
                    }
                    countDownLatch.countDown();
                }

                @Override
                public void fail(Exception e) {
                    System.out.println(e);
                    countDownLatch.countDown();
                }
            });

            System.out.println(">>>>>>>>>>>>"+(System.currentTimeMillis()-t1)+"<<<<<<<<<<<<<");
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        rpcClient.stop();

        System.out.println("End");
    }
}
