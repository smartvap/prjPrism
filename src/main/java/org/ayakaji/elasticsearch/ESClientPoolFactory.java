package org.ayakaji.elasticsearch;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.http.HttpHost;
import org.ayakaji.util.IniConfigFactory;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * ElasticSearch 连接池工厂对象
 * @author zhangdatong
 * @version 1.0.0
 *  date 2021.05.06
 */
public class ESClientPoolFactory implements PooledObjectFactory<RestHighLevelClient> {



    /**
     * 生产连接对象
     * @return
     * @throws Exception
     */
    @Override
    public PooledObject<RestHighLevelClient> makeObject() throws Exception {
        RestHighLevelClient client = null;

        try {
//            读取配置信息（ip和port信息组，用逗号隔开）
            String urlConfig = IniConfigFactory.getCommonConfig("elsticsearch.addrs");
//            配置信息分组
            String[] urls = urlConfig.split(",");
            HttpHost[] hosts = new HttpHost[urls.length];
            for (int i = 0; i < urls.length; i++) {
                String url = urls[i].trim();
                String[] hostAndPort = url.split(":");
                String ip = hostAndPort[0];
                String portStr = hostAndPort[1];
                int port = Integer.valueOf(portStr);
                hosts[i] = new HttpHost(ip, port);
            }
//            创建连接对象
            client = new RestHighLevelClient(RestClient.builder(hosts));
        } catch (Exception e) { //如果遇到异常，直接抛出
            throw e;
        }
        return new DefaultPooledObject<RestHighLevelClient>(client);
    }

    /**
     * 销毁对象
     * @param pooledObject
     * @throws Exception
     */
    @Override
    public void destroyObject(PooledObject<RestHighLevelClient> pooledObject) throws Exception {
        RestHighLevelClient highLevelClient = pooledObject.getObject();
        highLevelClient.close();
    }

    @Override
    public boolean validateObject(PooledObject<RestHighLevelClient> pooledObject) {
        return true;
    }

    @Override
    public void activateObject(PooledObject<RestHighLevelClient> pooledObject) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<RestHighLevelClient> pooledObject) throws Exception {

    }
}
