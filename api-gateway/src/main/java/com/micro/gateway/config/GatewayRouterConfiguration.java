package com.micro.gateway.config;

import com.micro.common.model.ErrorResponse;
import com.micro.common.model.factory.ObjectMapperFactor;
import com.micro.gateway.properties.RouterProperties;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Configuration
@AllArgsConstructor
public class GatewayRouterConfiguration {
    private static final String CACHED_RESPONSE_BODY_OBJECT = "cachedResponseBodyObject";
    private static final String CACHED_REQUEST_BODY_OBJECT = "cachedRequestBodyObject";

    private final RouterProperties routerProperties;


    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        routerProperties.getRoute().forEach((routeId, routingInfo) ->
                routes.route(routeId,
                        p -> p.path(routingInfo.getPath())
                                .filters(f -> f
                                        .filter(this::handleUnavailableService)
//                                        .filter((exchange, chain) -> {
//                                            if (exchange.getRequest().getHeaders().getContentType() != null &&
//                                                    exchange.getRequest().getHeaders().getContentType().toString().startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
//                                                return chain.filter(exchange);
//                                            }
//                                            return cacheResponseBody(exchange, chain);
//                                        }))
                                        .modifyResponseBody(String.class, String.class, this::handleErrorResponse))
                                .uri(routingInfo.getEndpoint())));

        return routes.build();
    }

    private Mono<Void> cacheResponseBody(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        exchange.getAttributes().put(CACHED_RESPONSE_BODY_OBJECT, new String(content, StandardCharsets.UTF_8));
                        return bufferFactory.wrap(content);
                    }));
                }
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(p -> p));
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    private Mono<Void> handleUnavailableService(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).onErrorResume(ex -> {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer buffer = response.bufferFactory().wrap(Objects.requireNonNull(ObjectMapperFactor.writeValuesAsString(ErrorResponse.builder().message("Service is currently unavailable. Please try again later.").build())).getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        });
    }

    private Mono<String> handleErrorResponse(ServerWebExchange exchange, String originalBody) {
        if (exchange.getResponse().getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
            return Mono.just(Objects.requireNonNull(ObjectMapperFactor.writeValuesAsString(ErrorResponse.builder().message("Service is currently unavailable. Please try again later.").build())));
        }
        return Mono.just(originalBody);
    }
}
