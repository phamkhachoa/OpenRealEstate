package com.proptech.realestate.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.time.Duration;

/**
 * Elasticsearch Configuration for Phase 8 Implementation
 * Advanced Search with Elasticsearch
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.proptech.realestate.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elasticsearch.host:localhost}")
    private String elasticsearchHost;

    @Value("${elasticsearch.port:9200}")
    private Integer elasticsearchPort;

    @Value("${elasticsearch.protocol:http}")
    private String elasticsearchProtocol;

    @Value("${elasticsearch.username:}")
    private String elasticsearchUsername;

    @Value("${elasticsearch.password:}")
    private String elasticsearchPassword;

    @Value("${elasticsearch.connect-timeout:10s}")
    private Duration connectTimeout;

    @Value("${elasticsearch.socket-timeout:60s}")
    private Duration socketTimeout;

    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.ClientConfigurationBuilder builder = ClientConfiguration.builder()
                .connectedTo(elasticsearchHost + ":" + elasticsearchPort);

        // Add authentication if provided
        if (elasticsearchUsername != null && !elasticsearchUsername.isEmpty()) {
            builder.withBasicAuth(elasticsearchUsername, elasticsearchPassword);
        }

        // Configure timeouts
        builder.withConnectTimeout(connectTimeout)
               .withSocketTimeout(socketTimeout);

        // Enable SSL if using HTTPS
        if ("https".equals(elasticsearchProtocol)) {
            builder.usingSsl();
        }

        return builder.build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClientBuilder restClientBuilder = RestClient.builder(
                new HttpHost(elasticsearchHost, elasticsearchPort, elasticsearchProtocol)
        );

        // Configure authentication
        if (elasticsearchUsername != null && !elasticsearchUsername.isEmpty()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword)
            );

            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }

        // Configure timeouts
        restClientBuilder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()))
                        .setSocketTimeout(Math.toIntExact(socketTimeout.toMillis()))
        );

        RestClient restClient = restClientBuilder.build();
        ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }
}
