/**
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.decision.utils;

import com.google.gson.Gson;
import com.stratio.decision.commons.constants.STREAMING;
import com.stratio.decision.commons.messages.StratioStreamingMessage;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZKUtils {

    private static Logger logger = LoggerFactory.getLogger(ZKUtils.class);

    private static ZKUtils self;
    private CuratorFramework client;
    private ExecutorService backgroundZookeeperCleanerTasks;

    private ZKUtils(String zookeeperCluster) throws Exception {

        // ZOOKEPER CONNECTION
        client = CuratorFrameworkFactory.newClient(zookeeperCluster, 25 * 1000, 10 * 1000, new ExponentialBackoffRetry(
                1000, 3));
        client.start();
        client.getZookeeperClient().blockUntilConnectedOrTimedOut();

        if (client.getState().compareTo(CuratorFrameworkState.STARTED) != 0) {
            throw new Exception("Connection to Zookeeper timed out after seconds");
        } else {
            backgroundZookeeperCleanerTasks = Executors.newFixedThreadPool(1);
            backgroundZookeeperCleanerTasks.submit(new ZookeeperBackgroundCleaner(client));
        }

    }

    public static ZKUtils getZKUtils(String zookeeperCluster) throws Exception {
        if (self == null) {
            self = new ZKUtils(zookeeperCluster);
        }
        return self;
    }

    public static void shutdownZKUtils() {
        if (self != null) {
            self.backgroundZookeeperCleanerTasks.shutdownNow();
            self.client.close();
        }
    }

    public void createEphemeralZNode(String path, byte[] data) throws Exception {

        if (client.checkExists().forPath(path) != null) {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        }

        client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, data);
    }

    public void createZNodeJsonReply(StratioStreamingMessage request, Object reply) throws Exception {

        String path = STREAMING.ZK_BASE_PATH + "/" + request.getOperation().toLowerCase() + "/"
                + request.getRequest_id();

        if (client.checkExists().forPath(path) != null) {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        }

        client.create().creatingParentsIfNeeded().forPath(path, new Gson().toJson(reply).getBytes());

        logger.info("**** ZKUTILS " + request.getOperation() + "//" + request.getRequest_id() + "//" + reply + "//"
                + path);

    }

    public void createZNode(String path, byte[] data) throws Exception {
        if (client.checkExists().forPath(path) != null) {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        }

        client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, data);
    }

    public byte[] getZNode(String path) throws Exception {
        return client.getData().forPath(path);
    }

    public boolean existZNode(String path) throws Exception {
        Stat stat = client.checkExists().forPath(path);
        return (stat == null) ? false : true;
    }

    private class ZookeeperBackgroundCleaner implements Runnable {

        private Logger logger = LoggerFactory.getLogger(ZookeeperBackgroundCleaner.class);

        private CuratorFramework client;
        private static final long ZNODES_TTL = 600000; // 10 minutes
        private static final long CLEAN_INTERVAL = 30000; // 5 minutes

        /**
         *
         */
        public ZookeeperBackgroundCleaner(CuratorFramework client) {
            this.client = client;
            logger.debug("Starting ZookeeperBackgroundCleaner...");
            logger.info("ZookeeperBackgroundCleaner BASE path " + STREAMING.ZK_BASE_PATH);
        }

        private int removeOldChildZnodes(String path) throws Exception {

            int counter = 0;
            Iterator<String> children = client.getChildren().forPath(path).iterator();

            while (children.hasNext()) {

                String childrenPath = children.next();
                if (!STREAMING.ZK_HIGH_AVAILABILITY_NODE.equals('/'+childrenPath) && !STREAMING.ZK_PERSISTENCE_NODE.equals('/'+childrenPath)) {
                    if (client.getChildren().forPath(path + "/" + childrenPath).size() > 0) {
                        counter += removeOldChildZnodes(path + "/" + childrenPath);
                    } else {

                        Stat znode = client.checkExists().forPath(path + "/" + childrenPath);
                        // avoid nulls and ephemeral znodes
                        if (znode != null && znode.getEphemeralOwner() == 0) {
                            client.delete().deletingChildrenIfNeeded().forPath(path + "/" + childrenPath);
                            counter++;
                        }

                    }
                }
            }

            return counter;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    if (client.getState().compareTo(CuratorFrameworkState.STARTED) == 0) {
                        int childsRemoved = removeOldChildZnodes(STREAMING.ZK_BASE_PATH);

                        logger.debug(childsRemoved + " old zNodes removed from ZK");
                    }

                    Thread.sleep(CLEAN_INTERVAL);

                } catch (InterruptedException ie) {
                    // no need to clean anything, as client is shared
                    logger.info("Shutting down Zookeeper Background Cleaner");
                }

                catch (Exception e) {
                    logger.info("Error on Zookeeper Background Cleaner: " + e.getMessage());
                }

            }

        }

    }

}
