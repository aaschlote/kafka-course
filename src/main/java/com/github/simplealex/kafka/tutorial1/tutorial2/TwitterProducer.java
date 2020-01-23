package com.github.simplealex.kafka.tutorial1.tutorial2;

import com.github.simplealex.kafka.tutorial1.PropertiesConfig;
import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {

    private final Logger logger = LoggerFactory.getLogger(TwitterProducer.class);

    private static Properties prop;

    private final List<String> terms = Lists.newArrayList("bitcoin","usa","bolsonaro","neymar");

    public TwitterProducer() {

    };

    public static void main(String[] args) {
        new TwitterProducer().run();
    }

    public void run() {
        logger.info("Setup");

        try {
            prop = new PropertiesConfig().readPropertiesFile("config.properties");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(1);
        }

        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(1000);

        Client client = createTwitterClient(msgQueue);
        client.connect();

        KafkaProducer<String, String> producer = createKafkaProducer();

        while (!client.isDone()) {
            String msg = "";
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
                producer.close();
            }

            if  (msg != null) {
                logger.info(msg);
                producer.send(new ProducerRecord<String, String>("twitter-tweets", msg), new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        if  (e != null) {
                            logger.error("Something bad happend", e);
                        }
                    }
                });
            }
        }

        logger.info("End of application");

    }

    public Client createTwitterClient(BlockingQueue<String> msgQueue) {
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        hosebirdEndpoint.trackTerms(terms);


        System.out.println(prop.getProperty("twitter.connect.consumerkey"));
        System.out.println(prop.getProperty("twitter.connect.consumersecret"));
        System.out.println(prop.getProperty("twitter.connect.token"));
        System.out.println(prop.getProperty("twitter.connect.secret"));

        Authentication hosebirdAuth = new OAuth1(prop.getProperty("twitter.connect.consumerkey"), prop.getProperty("twitter.connect.consumersecret"),
                prop.getProperty("twitter.connect.token"), prop.getProperty("twitter.connect.secret"));

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        Client hosebirdClient = builder.build();
        return hosebirdClient;

    }

    public Properties getPropertiesKafkaProducer() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG,"all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG,Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,"5");

        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG,"20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG,Integer.toString(32*1024));

        return properties;
    }

    public KafkaProducer<String, String> createKafkaProducer() {
        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(getPropertiesKafkaProducer());
        return producer;
    }

}
