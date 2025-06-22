package com.virtualcompanion.gateway.exception;

public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {
    
    public GlobalExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties.Resources resources,
            ApplicationContext applicationContext,
            ServerCodecConfigurer configurer) {
        super(errorAttributes, resources, applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }
    
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }
    
    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, 
            ErrorAttributeOptions.of(ErrorAttributeOptions.Include.EXCEPTION,
                                    ErrorAttributeOptions.Include.MESSAGE));
        
        int status = (int) errorPropertiesMap.getOrDefault("status", 500);
        
        return ServerResponse.status(HttpStatus.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorPropertiesMap));
    }
}
