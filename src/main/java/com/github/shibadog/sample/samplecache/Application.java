package com.github.shibadog.sample.samplecache;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.core.instrument.MeterRegistry;

@EnableCaching
@SpringBootApplication
@RestController
public class Application {

	@Configuration
	public static class AppConfig {
		@Bean
		MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
			return registry -> registry.config().commonTags("application", "sample-cache");
		}
	}

	private final CacheService cacheService;

	public Application(CacheService cacheService) {
		this.cacheService = cacheService;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@GetMapping(value = "/{key}")
	public String getMethodName(@PathVariable String key) {
		return cacheService.getCacheableData(key);
	}

	@Service
	public static class CacheService {
		@Cacheable("key-cache")
		public String getCacheableData(String key) {
			try {
				TimeUnit.SECONDS.sleep(5L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return UUID.randomUUID().toString();
		}
	}
}
