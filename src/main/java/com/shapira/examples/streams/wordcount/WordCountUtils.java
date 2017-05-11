package com.shapira.examples.streams.wordcount;

import java.util.Optional;

/**
 * Created by admin on 11/05/2017.
 */
public class WordCountUtils {
    static String getEnvOrElseThrowException(String environmentVariableKey) {
        return Optional
                .ofNullable(System.getenv(environmentVariableKey))
                .orElseThrow(() -> new IllegalArgumentException(
                        environmentVariableKey + " environment variable must be set"));
    }
}
