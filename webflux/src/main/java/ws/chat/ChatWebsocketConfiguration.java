package ws.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Log4j2
@Configuration
public class ChatWebsocketConfiguration {

	ChatWebsocketConfiguration(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	private final Map<String, Connection> sessions = new ConcurrentHashMap<>();

	private final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();

	private final ObjectMapper objectMapper;

	@SneakyThrows
	private Message from(String json) {
		return objectMapper.readValue(json, Message.class);
	}

	@SneakyThrows
	private String from(Message msg) {
		return objectMapper.writeValueAsString(msg);
	}

	@Bean
	public WebSocketHandler chatWsh(ExecutorService executorService) {

		var messagesToBroadcast = Flux
			.<Message>create(sink -> {
				var submit = executorService.submit(() -> {
					while (true) {
						try {
							sink.next(this.messages.take());
						}
						catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
				});
				sink.onCancel(() -> submit.cancel(true));
			}) //
			.share();

		return session -> {

			var sessionId = session.getId();

			this.sessions.put(sessionId, new Connection(sessionId, session));

			var in = session //
				.receive() //
				.map(WebSocketMessage::getPayloadAsText) //
				.map(this::from) //
				.map(msg -> new Message(sessionId, msg.getText(), new Date())) //
				.map(this.messages::offer)//
				.doFinally(st -> {//
					if (st.equals(SignalType.ON_COMPLETE)) {//
						this.sessions.remove(sessionId);//
					}
				}); //

			var out = messagesToBroadcast //
				.map(this::from)//
				.map(session::textMessage);

			return session.send(out).and(in);
		};
	}

}
