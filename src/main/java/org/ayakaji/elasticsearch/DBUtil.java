/****************************************************
 * Encapsulation of basic operations of ES database *
 ****************************************************/
package org.ayakaji.elasticsearch;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class DBUtil {
	private final static Logger logger = Logger.getLogger(DBUtil.class.getName());
	private RestHighLevelClient client = null;

	public DBUtil(String ip, int port, String scheme) {
		client = new RestHighLevelClient(RestClient.builder(new HttpHost(ip, port, scheme)));
	}

	public void close() throws IOException {
		client.close();
		client = null;
	}

	public static void main(String[] args) throws IOException {
		DBUtil util = new DBUtil("10.19.249.28", 9200, "http");
		util.createIndex("idx_alive_agent",
				"{\n" + "  \"properties\": {\n" + "    \"sys_id\": {\n" + "      \"type\": \"text\"\n" + "    },\n"
						+ "    \"agent_type\": {\n" + "      \"type\": \"text\"\n" + "    },\n" + "    \"ip_list\": {\n"
						+ "      \"type\": \"text\"\n" + "    },\n" + "    \"update_time\": {\n"
						+ "      \"type\": \"date\"\n" + "    }\n" + "  }\n" + "}");
		util.close();
	}

	/**
	 * Test Case for createIndex()
	 * 
	 * @throws IOException
	 */
	private void createIndexTestCase(int caseId) throws IOException {
		DBUtil util = new DBUtil("10.19.249.28", 9200, "http");
		boolean result = false;
		switch (caseId) {
		case 1:
			// @formatter:off
			result = util.createIndex("idx_alive_agent",
				"{\n" +
				"  \"properties\": {\n" +
				"    \"sys_id\": {\n" +
				"      \"type\": \"text\"\n" +
				"    },\n" +
				"    \"agent_type\": {\n" +
				"      \"type\": \"text\"\n" +
				"    },\n" +
				"    \"ip_list\": {\n" +
				"      \"type\": \"text\"\n" +
				"    },\n" +
				"    \"update_time\": {\n" +
				"      \"type\": \"date\"\n" +
				"    }\n" +
				"  }\n" +
				"}");
			// @formatter:on
			logger.info(result ? "Success!" : "Failure!");
			util.close();
			break;
		case 2:
			result = util.createIndex("idx_alive_agent", new LinkedHashMap<String, Object>() {
				private static final long serialVersionUID = -8928246401952360883L;
				{
					put("sys_id", new HashMap<String, String>() {
						private static final long serialVersionUID = 2192410954381485446L;
						{
							put("type", "text");
						}
					});
					put("agent_type", new HashMap<String, String>() {
						private static final long serialVersionUID = 3842940342102922604L;
						{
							put("type", "text");
						}
					});
					put("ip_list", new HashMap<String, String>() {
						private static final long serialVersionUID = -682972382569420418L;
						{
							put("type", "text");
						}
					});
					put("update_time", new HashMap<String, String>() {
						private static final long serialVersionUID = 674480682958959120L;

						{
							put("type", "date");
						}
					});
				}
			});
			logger.info(result ? "Success!" : "Failure!");
			util.close();
			break;
		case 3:
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			{
				builder.startObject("properties");
				{
					builder.startObject("sys_id");
					{
						builder.field("type", "text");
					}
					builder.endObject();
					builder.startObject("agent_type");
					{
						builder.field("type", "text");
					}
					builder.endObject();
					builder.startObject("ip_list");
					{
						builder.field("type", "text");
					}
					builder.endObject();
					builder.startObject("update_time");
					{
						builder.field("type", "date");
					}
					builder.endObject();
				}
				builder.endObject();
			}
			builder.endObject();
			break;
		default:
			break;
		}
		util.close();
	}

	/**
	 * Create Index Variant 1
	 * 
	 * @param indexName
	 * @throws IOException
	 */
	private boolean createIndex(String indexName, String source) throws IOException {
		CreateIndexRequest request = new CreateIndexRequest(indexName);
		request.settings(Settings.builder().put("index.number_of_shards", 3).put("index.number_of_replicas", 2));
		request.mapping(source, XContentType.JSON);
		request.setTimeout(TimeValue.timeValueMinutes(2));
		request.setMasterTimeout(TimeValue.timeValueMinutes(1));
		request.waitForActiveShards(ActiveShardCount.from(2));
		CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
		return response.isAcknowledged() && response.isShardsAcknowledged();
	}

	/**
	 * Create Index Variant 2
	 * 
	 * @param indexName
	 * @param message
	 * @return
	 * @throws IOException
	 */
	private boolean createIndex(String indexName, Map<String, Object> properties) throws IOException {
		CreateIndexRequest request = new CreateIndexRequest(indexName);
		request.settings(Settings.builder().put("index.number_of_shards", 3).put("index.number_of_replicas", 2));
		Map<String, Object> mapping = new HashMap<String, Object>();
		mapping.put("properties", properties);
		request.mapping(mapping);
		request.setTimeout(TimeValue.timeValueMinutes(2));
		request.setMasterTimeout(TimeValue.timeValueMinutes(1));
		request.waitForActiveShards(ActiveShardCount.from(2));
		CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
		return response.isAcknowledged() && response.isShardsAcknowledged();
	}

	/**
	 * Create Index Variant 3
	 * 
	 * @param indexName
	 * @return
	 * @throws IOException
	 */
	private boolean createIndex(String indexName, XContentBuilder builder) throws IOException {
		CreateIndexRequest request = new CreateIndexRequest(indexName);
		request.settings(Settings.builder().put("index.number_of_shards", 3).put("index.number_of_replicas", 2));
		request.mapping(builder);
		request.setTimeout(TimeValue.timeValueMinutes(2));
		request.setMasterTimeout(TimeValue.timeValueMinutes(1));
		request.waitForActiveShards(ActiveShardCount.from(2));
		CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
		return response.isAcknowledged() && response.isShardsAcknowledged();
	}

	private static void dropIndex() throws IOException {
		RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("10.19.249.28", 9200, "http")));
		DeleteIndexRequest request = new DeleteIndexRequest("twitter");
		request.timeout(TimeValue.timeValueMinutes(2));
		request.masterNodeTimeout(TimeValue.timeValueMinutes(1));
		request.indicesOptions(IndicesOptions.lenientExpandOpen());
		AcknowledgedResponse response = client.indices().delete(request, RequestOptions.DEFAULT);
		boolean acknowledged = response.isAcknowledged();
		System.out.println(acknowledged);
		client.close();
	}

	private static void getIndex() throws IOException {
		RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("10.19.249.28", 9200, "http")));
		GetIndexRequest request = new GetIndexRequest("twitter");
		request.includeDefaults(true);
		request.indicesOptions(IndicesOptions.lenientExpandOpen());
		GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);
		System.out.println(response.getIndices().length);
		client.close();
	}
	
	private void insert(String indexName, int docId, String json) {
		IndexRequest request = new IndexRequest("posts");
		request.id("1");
		String jsonString = "{" +
		        "\"user\":\"kimchy\"," +
		        "\"postDate\":\"2013-01-30\"," +
		        "\"message\":\"trying out Elasticsearch\"" +
		        "}";
		request.source(jsonString, XContentType.JSON);
	}

	private void insert() throws IOException {
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		builder.field("sys_id", "c774b658-7139-42db-b1ea-daea422d3737");
		builder.field("agent_type", "remote");
		builder.field("ip_list", "134.80.19.88");
		builder.field("update_time", new Date());
		builder.endObject();
		IndexRequest request = new IndexRequest("idx_alive_agent").id("1").source(builder);
		IndexResponse response = null;
		try {
			response = client.index(request, RequestOptions.DEFAULT);
		} catch (ElasticsearchException e) {
			if (e.status() == RestStatus.CONFLICT) {
				System.out.println("Encountered version conflict.");
				System.exit(0);
			}
		}
		String index = response.getIndex();
		String id = response.getId();
		if (response.getResult() == DocWriteResponse.Result.CREATED) {
			System.out.println("A record has been created!");
		} else if (response.getResult() == DocWriteResponse.Result.UPDATED) {
			System.out.println("A record has been updated!");
		}
		ReplicationResponse.ShardInfo shardInfo = response.getShardInfo();
		if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
			System.out.println("The number of successful shards is less than total shards!");
		}
		if (shardInfo.getFailed() > 0) {
			for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
				String reason = failure.reason();
				System.out.println(reason);
			}
		}
		client.close();
	}

	private static void search() throws IOException {
		RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(new HttpHost("10.19.249.28", 9200, "http")));
		SearchRequest request = new SearchRequest("idx_alive_agent");
		SearchSourceBuilder builder = new SearchSourceBuilder();
		builder.query(QueryBuilders.termQuery("agent_type", "remote"));
		builder.size(1);
		builder.timeout(new TimeValue(60, TimeUnit.SECONDS));
		request.source(builder);
		SearchResponse response = client.search(request, RequestOptions.DEFAULT);
		RestStatus status = response.status();
		TimeValue took = response.getTook();
		Boolean terminatedEarly = response.isTerminatedEarly();
		boolean timedOut = response.isTimedOut();
		int totalShards = response.getTotalShards();
		int successfulShards = response.getSuccessfulShards();
		int failedShards = response.getFailedShards();
		for (ShardSearchFailure failure : response.getShardFailures()) {
			;
		}
		SearchHits hits = response.getHits();
		System.out.println(hits);
		client.close();
	}

}
