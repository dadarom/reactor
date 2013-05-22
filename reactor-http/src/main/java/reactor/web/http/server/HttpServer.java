package reactor.web.http.server;

import reactor.convert.Converter;
import reactor.core.Composable;
import reactor.fn.*;
import reactor.fn.dispatch.Dispatcher;
import reactor.io.Buffer;
import reactor.tcp.TcpServerReactor;
import reactor.tcp.codec.Codec;
import reactor.util.Assert;
import reactor.util.LinkedMultiValueMap;
import reactor.util.MultiValueMap;
import reactor.web.http.MediaType;
import reactor.web.http.ResponseEntity;
import reactor.web.http.codec.HttpCodec;

/**
 * @author Jon Brisbin
 */
public class HttpServer implements Lifecycle<HttpServer> {

	private final String host;
	private final int    port;

	private final MultiValueMap<MediaType, Converter> converters = new LinkedMultiValueMap<MediaType, Converter>();
	private final TcpServerReactor<Tuple2<ServerHttpRequest<Buffer>, Composable<ResponseEntity<Buffer>>>> tcpServerReactor;

	public HttpServer(Dispatcher dispatcher) {
		this(8080, dispatcher);
	}

	public HttpServer(int port, Dispatcher dispatcher) {
		this("127.0.0.1", port, dispatcher);
	}

	public HttpServer(String host, int port, Dispatcher dispatcher) {
		Assert.notNull(dispatcher, "Dispatcher cannot be null.");
		this.host = host;
		this.port = port;
		this.tcpServerReactor = new TcpServerReactor<Tuple2<ServerHttpRequest<Buffer>, Composable<ResponseEntity<Buffer>>>>(port, new Supplier<Codec<Tuple2<ServerHttpRequest<Buffer>, Composable<ResponseEntity<Buffer>>>>>() {
			@Override
			public Codec<Tuple2<ServerHttpRequest<Buffer>, Composable<ResponseEntity<Buffer>>>> get() {
				return new HttpCodec();
			}
		});
		this.tcpServerReactor.onRequest(
				new Consumer<Event<Tuple2<ServerHttpRequest<Buffer>, Composable<ResponseEntity<Buffer>>>>>() {
					@Override
					public void accept(Event<Tuple2<ServerHttpRequest<Buffer>, Composable<ResponseEntity<Buffer>>>> ev) {
						tcpServerReactor.notify(ev.getData().getT1(), ev);
					}
				}
		);
	}

	public HttpServer addConverter(MediaType mediaType, Converter converter) {
		converters.add(mediaType, converter);
		return this;
	}

	public <T> HttpServer on(ServerHttpRequestSelector selector, final ServerHttpRequestHandler<T> handler) {
		tcpServerReactor.on(
				selector,
				new Consumer<Event<Tuple2<ServerHttpRequest<?>, Composable<ResponseEntity<?>>>>>() {
					@SuppressWarnings("unchecked")
					@Override
					public void accept(final Event<Tuple2<ServerHttpRequest<?>, Composable<ResponseEntity<?>>>> ev) {
						final Object replyTo = ev.getReplyTo();
						ev.getData().getT2().consume(
								new Consumer<ResponseEntity<?>>() {
									@Override
									public void accept(ResponseEntity<?> response) {
										Event<ResponseEntity<?>> respEv = new Event<ResponseEntity<?>>(response);
										respEv.getHeaders().set("reactor.tcp.connection.id", ev.getHeaders().get("reactor.tcp.connection.id"));
										tcpServerReactor.notify(replyTo, respEv);
									}
								}
						);

						Class<?> bodyType = handler.getRequestBodyType();
						if (null == bodyType) {
							// stream the data
							handler.serve((ServerHttpRequest<T>) ev.getData().getT1(), ev.getData().getT2());
						} else {
							if (null == ev.getData().getT1().getBody().get()) {
								// TODO: data's not ready
							} else {
								Object body = ev.getData().getT1().getBody().get();
								if (!bodyType.getClass().isInstance(body)) {
									// TODO: convert the body
								}
							}
						}
					}
				}
		);
		return this;
	}

	@Override
	public HttpServer destroy() {
		tcpServerReactor.destroy();
		return this;
	}

	@Override
	public HttpServer stop() {
		tcpServerReactor.stop();
		return this;
	}

	@Override
	public HttpServer start() {
		tcpServerReactor.start();
		return this;
	}

	@Override
	public boolean isAlive() {
		return tcpServerReactor.isAlive();
	}

}
