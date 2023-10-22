package mu.mns.demo.download.app2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration class to customize the {@link WebMvcConfigurer}.
 *
 * @author yoven.ayassamy
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Create a {@link Bean Spring Bean} for the WEB MVC Asynchronous processes thread pool.
     * <p>
     * This is used when a {@link RestController} method returns an async object such as:
     * <ul>
     *      <li>{@link CompletableFuture}</li>
     *      <li>{@link Future}</li>
     *      <li>{@link Callable}</li>
     *      <li>{@link StreamingResponseBody}</li>
     * </ul>
     * <p>
     * Instead of creating a new thread everything for handling the asynchronous WEB MVC task, the threads will be
     * taken from this thread pool instead.
     * <p>
     * Don't use this thread pool for other asynchronous processes. Leave it only for handling asynchronous responses on
     * {@link RestController} side.
     */
    @Bean("WEB_MVC_ASYNC_THREAD_POOL")
    public ThreadPoolTaskExecutor createWebMvcAsyncThreadPool() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(600);
        executor.setThreadNamePrefix("web-mvc-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(createWebMvcAsyncThreadPool());
    }
}
