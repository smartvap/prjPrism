package org.ayakaji.elasticsearch;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * ElasticSearch 连接池工具类
 * @author zhangdatong
 * @version 1.0.0
 *  date 2021.05.06
 */
public class ElasticSearchPoolUtil {
//    对象配置类，默认为8个
    private static GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

    static {
        poolConfig.setMaxIdle(8);
    }

//    要池化的对象工厂类
    private static ESClientPoolFactory esClientPoolFactory = new ESClientPoolFactory();

//    利用对象工厂类和配置类生成对象池
    private static GenericObjectPool<RestHighLevelClient> clientPool = new GenericObjectPool<>(esClientPoolFactory, poolConfig);

    /**
     * 获得对象
     * @return ES连接对象
     * @throws Exception
     */
    public static RestHighLevelClient getClient() throws Exception {
        RestHighLevelClient client = clientPool.borrowObject();
        return client;
    }

    /**
     * 使用完毕后，归还对象
     * @param client 使用完毕后的连接对象
     */
    public static void returnClient(RestHighLevelClient client) {
        clientPool.returnObject(client);
    }

}
