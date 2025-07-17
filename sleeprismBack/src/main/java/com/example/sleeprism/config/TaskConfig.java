package com.example.sleeprism.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class TaskConfig {

  /**
   * 애플리케이션의 @Async 처리를 위한 기본 TaskExecutor를 정의합니다.
   * @Primary 어노테이션을 통해 여러 TaskExecutor Bean 중에서 우선권을 갖도록 설정합니다.
   * 이렇게 하면 NoUniqueBeanDefinitionException 오류를 해결할 수 있습니다.
   */
  @Bean
  @Primary
  public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(25);
    executor.setThreadNamePrefix("async-executor-");
    executor.initialize();
    return executor;
  }
}
