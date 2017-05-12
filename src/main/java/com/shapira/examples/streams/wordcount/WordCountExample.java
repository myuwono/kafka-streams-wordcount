package com.shapira.examples.streams.wordcount;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.shapira.examples.streams.wordcount.WordCountUtils.getEnvOrElseThrowException;

public class WordCountExample {
    private static final String KAFKA_BROKERS = "KAFKA_BROKERS";
    private static final String INPUT_TOPIC_ENV = "INPUT_TOPIC";
    private static final String OUTPUT_TOPIC_ENV = "OUTPUT_TOPIC";

    private static final String BROKERS = getEnvOrElseThrowException(KAFKA_BROKERS);
    private static final String INPUT_TOPIC = getEnvOrElseThrowException(INPUT_TOPIC_ENV);
    private static final String OUTPUT_TOPIC = getEnvOrElseThrowException(OUTPUT_TOPIC_ENV);

    private static final Logger log = LoggerFactory.getLogger(WordCountExample.class);

    public static void main(String[] args) throws Exception{
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BROKERS);
        props.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        // Note: To re-run the demo, you need to use the offset reset tool:
        // https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Application+Reset+Tool
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // work-around for an issue around timing of creating internal topics
        // Fixed in Kafka 0.10.2.0
        // don't use in large production apps - this increases network load
        // props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);

        KStreamBuilder builder = new KStreamBuilder();

        KStream<String, String> source = builder.stream(INPUT_TOPIC);

        final Pattern pattern = Pattern.compile("\\W+");
        KStream counts  = source.flatMapValues(value-> Arrays.asList(pattern.split(value.toLowerCase())))
                .map((key, value) -> new KeyValue<>(value, value))
                .filter((key, value) -> (!value.equals("the")))
                .groupByKey()
                .count("CountStore")
                .mapValues(value->Long.toString(value))
                .toStream();
        counts.to(OUTPUT_TOPIC);

        builder.<String, String>stream(OUTPUT_TOPIC)
                .foreach((key, value) -> log.info("{}: {}", key, value));

        KafkaStreams streams = new KafkaStreams(builder, props);

        // This is for reset to work. Don't use in production - it causes the app to re-load the state from Kafka on every start
        streams.cleanUp();

        streams.start();

        // usually the stream application would be running forever,
        // in this example we just let it run for some time and stop since the input data is finite.
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
