package com.github.simplealex.kafka;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ElasticSearchConsumer {

    private static Properties prop;

    public static void main(String[] args) throws IOException, InterruptedException {

        Logger logger = LoggerFactory.getLogger(ElasticSearchConsumer.class.getName());
        prop = new PropertiesConfig().readPropertiesFile("config.properties");

        RestHighLevelClient client = null;

        try {
            client = createClient();
        } catch (Exception e) {
            logger.error("error to connect on Elasticsearch");
            System.exit(1);
        }

        KafkaConsumer<String, String> consumer = createConsumer();

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

            logger.info("Received " + records.count());

            BulkRequest bulkRequest = new BulkRequest();

            for (ConsumerRecord<String, String> record: records) {

                try {
                    String idKafka = extractIdFromTweet(record.value());
                    IndexRequest indexRequest = new IndexRequest("twitter", "tweets", idKafka).source(record.value(), XContentType.JSON);

                    bulkRequest.add(indexRequest);
                } catch (NullPointerException e) {
                    logger.warn("skipping bad data: " + record.value());
                }
            }

            if (records.count() > 0){
                BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                logger.info("Commiting offsets...");
                consumer.commitAsync();
                logger.info("Offsets have been committed...");
                Thread.sleep(1000);
            }
        }

        //client.close();
    }

    public static RestHighLevelClient createClient() throws Exception {

        String hostname = prop.getProperty("elasticsearch.connect.ipaddress");
        String username = prop.getProperty("elasticsearch.connect.username");
        String password = prop.getProperty("elasticsearch.connect.password");

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username,password));

        RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, 443, "https"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                        return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

    public static KafkaConsumer<String, String> createConsumer() {

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"kafka-demo-elasticsearch");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"false");
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,"100");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);

        consumer.subscribe(Arrays.asList("twitter-tweets"));

        return consumer;
    }

    private static JsonParser jsonParser = new JsonParser();

    private static String extractIdFromTweet(String tweetJson) {
        return jsonParser.parse(tweetJson).getAsJsonObject().get("id_str").getAsString();
    }



}
