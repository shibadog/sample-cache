package com.github.shibadog.sample.samplecache;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;

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

	// @see https://qiita.com/koya3jp/items/d632e95ebc57ee07695c
	// 以下設定によって、 `@Cacheable` で指定したキャッシュに対して、
	// 明示的に `application.yml` で指定しなくても、
	// 動的にメトリクス登録をするための設定。
	@EnableConfigurationProperties(CacheProperties.class)
	@EnableCaching
	@Configuration
	public static class CacheConfig {
		private final MeterRegistry meterRegistry;
		private final CacheProperties cacheProperties;

		public CacheConfig(MeterRegistry meterRegistry, CacheProperties cacheProperties) {
			this.cacheProperties = cacheProperties;
			this.meterRegistry = meterRegistry;
		}
	
		// CaffeineCacheConfigurationの実装をcopy
		@Bean
		public CaffeineCacheManager cacheManager() {
			CaffeineCacheManager cacheManager = createCacheManager();
			List<String> cacheNames = cacheProperties.getCacheNames();
			if (!CollectionUtils.isEmpty(cacheNames)) {
				cacheManager.setCacheNames(cacheNames);
			}
			return cacheManager;
		}
		// ここで独自CaffeineCacheManagerを生成
		private CaffeineCacheManager createCacheManager() {
			CaffeineCacheManager cacheManager = new InstrumentedCaffeineCacheManager(meterRegistry);
			setCacheBuilder(cacheManager);
			return cacheManager;
		}
	
		private void setCacheBuilder(CaffeineCacheManager cacheManager) {
			String specification = cacheProperties.getCaffeine().getSpec();
			if (StringUtils.hasText(specification)) {
				cacheManager.setCacheSpecification(specification);
			}
		}
	}
	
	public static class InstrumentedCaffeineCacheManager extends CaffeineCacheManager {

		private final MeterRegistry meterRegistry;

		public InstrumentedCaffeineCacheManager(MeterRegistry meterRegistry) {
			this.meterRegistry = meterRegistry;
		}

		@Override
		protected Cache<Object, Object> createNativeCaffeineCache(String name) {
			Cache<Object, Object> nativeCache = super.createNativeCaffeineCache(name);
			CaffeineCacheMetrics.monitor(meterRegistry, nativeCache, name, Collections.emptyList());
			return nativeCache;
		}
	}

	// ---- ここまで可視化のための設定 ----

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
