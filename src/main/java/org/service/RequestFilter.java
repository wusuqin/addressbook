package org.service;

import com.google.common.base.Stopwatch;
import io.quarkus.logging.Log;
import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@Provider
public class RequestFilter implements ContainerRequestFilter {
    @Context
    HttpServerRequest request;

    public static final String STOPWATCH = "stopwatch_" + UUID.randomUUID().toString();;

    @ServerRequestFilter(preMatching = true)
    public void setStopWatchRequest(ContainerRequestContext context) {
        context.setProperty(STOPWATCH, Stopwatch.createStarted());
    }


    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        var method = context.getMethod();
        var uri = context.getUriInfo().getPath();
        var address = request.remoteAddress().toString();
        var params = request.params();
        var stopWatch =(Stopwatch) context.getProperty(STOPWATCH);
        var time = stopWatch.elapsed(); //获取整个接口调用时间
        if(params == null || params.isEmpty()) {
            Log.infof("[Request] %s %s from IP %s (%s)", method, uri, address, time);
        }else{
            var paramsList = params.entries().stream().map(Object::toString).collect(Collectors.joining(","));
            Log.infof("[Request] %s %s, params: {%s} from IP %s (%s)", method, uri, paramsList, address,time);
        }
    }
}
