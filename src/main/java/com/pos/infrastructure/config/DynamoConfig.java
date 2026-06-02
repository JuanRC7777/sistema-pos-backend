package com.pos.infrastructure.config;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoConfig {

    public static DynamoDbClient buildClient() {
        return DynamoDbClient.builder()
            .region(Region.of(System.getenv("AWS_REGION")))
            .build();
    }
}
