package com.tradingzone.services.redis.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.*;

@Data
@Configuration
@EnableRedisRepositories
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String hostName;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.timeout}")
    private String timeout;

    @Value("${spring.data.redis.database}")
    private int database;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /*@Bean
    public RedisConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
    }*/
    @Bean
    @Qualifier("lettuceConnectionFactory")
    public LettuceConnectionFactory lettuceConnectionFactory() {

        RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration(getHostName(), port);
        redisConf.setPassword(password);
        return new LettuceConnectionFactory(redisConf);
    }

    @Bean
    public RedisTemplate<?, ?> redisTemplate(@Qualifier("lettuceConnectionFactory") RedisConnectionFactory lettuceConnectionFactory) {

        RedisTemplate<byte[], byte[]> template = new RedisTemplate<byte[], byte[]>();
        template.setConnectionFactory(lettuceConnectionFactory);
        return template;
    }

    @Bean
    public UnifiedJedis unifiedJedis(){

        //https://github.com/redis/jedis
        //https://www.baeldung.com/jedis-java-redis-client-library

        HostAndPort hostAndPort = new HostAndPort(hostName, port);

        //final JedisPoolConfig poolConfig = buildPoolConfig();

        JedisPooled jedisPooled = new JedisPooled(hostAndPort,
                DefaultJedisClientConfig.builder()
                        .password(password)
                        .database(database)
                        .socketTimeoutMillis(5000)  // set timeout to 5 seconds
                        .connectionTimeoutMillis(5000) // set connection timeout to 5 seconds
                        .build()
        );
        return jedisPooled;
    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        //poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        //poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }


}
