// package com.example.demo.adapters.out.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.cache.RedisCacheConfiguration;
// import org.springframework.data.redis.cache.RedisCacheManager;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.*;

// @Configuration
// public class RedisConfig {

//     @Bean
//     public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
//         RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
//                 .serializeKeysWith(
//                         RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
//                 .serializeValuesWith(RedisSerializationContext.SerializationPair
//                         .fromSerializer(new GenericJackson2JsonRedisSerializer()))
//                 .disableCachingNullValues();

//         return RedisCacheManager.builder(connectionFactory)
//                 .cacheDefaults(config)
//                 .build();
//     }

//     @Bean
//     public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//         RedisTemplate<String, Object> template = new RedisTemplate<>();
//         template.setConnectionFactory(connectionFactory);

//         // Key serializer: String
//         template.setKeySerializer(new StringRedisSerializer());
//         template.setHashKeySerializer(new StringRedisSerializer());

//         // Value serializer: Jackson (hỗ trợ complex objects)
//         template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//         template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

//         return template;
//     }
// }