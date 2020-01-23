package com.github.simplealex.kafka.tutorial1;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoAssignSeek {

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(ConsumerDemo.class);

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);

        TopicPartition partittionReadFrom = new TopicPartition("first_topic",0);
        long offsetToReadFrom = 15l;
        consumer.assign(Arrays.asList(partittionReadFrom));

        consumer.seek(partittionReadFrom, offsetToReadFrom);

        int numberOfMessagesToRead = 5;
        boolean keepOnReading = true;
        int numberOfMessagesReadSoFar = 0;

        while(keepOnReading){
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord record : records) {
                numberOfMessagesReadSoFar += 1;
                logger.info("KEY: " + record.key() + ", Value: " + record.value());
                logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());

                if  (numberOfMessagesReadSoFar >= numberOfMessagesToRead) {
                    keepOnReading = false;
                    break;
                }
            }
        }

        logger.info("Exiting the application");
    }
}
