package com.lucene_to_elastic.configuration;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@Configuration
public class ElasticsearchConfiguration {
    @Bean
    public ElasticsearchClient elasticsearchClient(
            @Value("${elasticsearch.uri}") String uri,
            @Value("${elasticsearch.apiKeyEncoded}") String apiKeyEncoded
    ) {
        RestClient restClient = RestClient.builder(HttpHost.create(uri)).setDefaultHeaders(
                new Header[] { new BasicHeader("Authorization", "ApiKey " + apiKeyEncoded) }).build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        JacksonJsonpMapper mapper = new JacksonJsonpMapper(objectMapper);

        ElasticsearchTransport transport = new RestClientTransport(restClient, mapper);

        return new ElasticsearchClient(transport);
    }
}