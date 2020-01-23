package com.github.simplealex.kafka.tutorial1;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoWithThread {

    public static void main(String[] args) {
        new ConsumerDemoWithThread().run();
    }

    private ConsumerDemoWithThread() {

    }

    private void run() {

        CountDownLatch latch = new CountDownLatch(1);

        Runnable myConsumerRunnable = new ConsumerRunnable(latch);

        Thread myThread = new Thread(myConsumerRunnable);
        myThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread( () -> {
            System.out.println("Caught shutdown hook");
            ((ConsumerRunnable) myConsumerRunnable).shutDown();
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("Application got interrupted");
        }

    }

    public class ConsumerRunnable implements Runnable {

        private CountDownLatch latch;
        private KafkaConsumer<String, String> consumer;
        private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class);

        public ConsumerRunnable(CountDownLatch latch) {
            this.latch = latch;

            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "my-fourth-application");
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            consumer = new KafkaConsumer<String, String>(properties);

            consumer.subscribe(Arrays.asList("first_topic"));


        }

        @Override
        public void run() {
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord record : records) {
                        logger.info("KEY: " + record.key() + ", Value: " + record.value());
                        logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                    }
                }
            } catch (WakeupException e) {
                logger.info("Received shutdown signal!");
            } finally {
                consumer.close();
                latch.countDown();
            }

        }

        public void shutDown() {
            consumer.wakeup();
        }
    }
}
