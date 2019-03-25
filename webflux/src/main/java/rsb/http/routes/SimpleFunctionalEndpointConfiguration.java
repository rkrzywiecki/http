package rsb.http.routes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
class SimpleFunctionalEndpointConfiguration {

	@Bean
	RouterFunction<ServerResponse> simple(GreetingsHandlerFunction handlerFunction) { // <1>

		// <2>
		return route() //
				.GET("/hello/{name}", request -> { // <3>
					var namePathVariable = request.pathVariable("name");
					var message = String.format("Hello %s!", namePathVariable);
					return ok().syncBody(message);
				}) //
				.GET("/hodor", handlerFunction) // <4>
				.GET("/sup", handlerFunction::handle) // <5>
				.build();
	}

	@Bean
	GreetingsHandlerFunction greetingsHandlerFunction() {
		return new GreetingsHandlerFunction();
	}

}

class GreetingsHandlerFunction implements HandlerFunction<ServerResponse> {

	@Override
	public Mono<ServerResponse> handle(ServerRequest request) {
		return ok().syncBody("Hodor!");
	}

}
