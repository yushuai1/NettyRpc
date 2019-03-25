package com.nettyrpc.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import com.nettyrpc.client.ConnectManage;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务发现
 *
 * @author huangyong
 * @author luxiaoxun
 */
public class ServiceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>();

    private String registryAddress;
    private ZooKeeper zookeeper;

    /**
     * 获取zookeeper中的服务列表，接通socket连接
     * @param registryAddress
     */
    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
        /**
         * 连接zookeeper
         */
        zookeeper = connectServer();
        if (zookeeper != null) {
            watchNode(zookeeper);
        }
    }

    public String discover() {
        String data = null;
        int size = dataList.size();
        if (size > 0) {
            if (size == 1) {
                data = dataList.get(0);
                logger.debug("using only data: {}", data);
            } else {
                data = dataList.get(ThreadLocalRandom.current().nextInt(size));
                logger.debug("using random data: {}", data);
            }
        }
        return data;
    }

    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, 30*1000, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
            logger.info("zookeeper 发现连接成功");
        } catch (IOException | InterruptedException e) {
            logger.error("", e);
        }
        return zk;
    }

    /**
     * 监控节点并且连接socket
     * @param zk
     */
    private void watchNode(final ZooKeeper zk) {
        try {
            /**
             * 获取子节点并且监控子节点接点
             */
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        /**
                         * 如果直接点有变化直接递归更新相关服务
                         */
                        watchNode(zk);
                    }
                }
            });
            List<String> dataList = new ArrayList<>();
            /**
             * 把节点中的所有服务存入list列表
             */
            for (String node : nodeList) {
                byte[] bytes = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }
            logger.info("node data: {}", dataList);
            this.dataList = dataList;

            logger.info("Service discovery triggered updating connected server node.");
            /**
             * 对所有的服务列表建立socket连接
             */
            UpdateConnectedServer();
        } catch (KeeperException | InterruptedException e) {
            logger.error("", e);
        }
    }
    /**
     * 对所有的服务列表建立socket连接
     */
    private void UpdateConnectedServer(){
        ConnectManage.getInstance().updateConnectedServer(this.dataList);
    }

    public void stop(){
        if(zookeeper!=null){
            try {
                zookeeper.close();
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        }
    }
}
