package reactor.web.http.codec;

import org.apache.http.*;
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.nio.codecs.DefaultHttpRequestParser;
import org.apache.http.impl.nio.codecs.DefaultHttpResponseWriter;
import org.apache.http.impl.nio.reactor.SessionInputBufferImpl;
import org.apache.http.impl.nio.reactor.SessionOutputBufferImpl;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineFormatter;
import org.apache.http.message.BasicLineParser;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Composable;
import reactor.fn.Consumer;
import reactor.fn.Tuple;
import reactor.fn.Tuple2;
import reactor.fn.dispatch.Dispatcher;
import reactor.fn.dispatch.SynchronousDispatcher;
import reactor.io.Buffer;
import reactor.tcp.codec.StreamingCodec;
import reactor.tcp.data.Buffers;
import reactor.web.http.MediaType;
import reactor.web.http.ResponseEntity;
import reactor.web.http.server.ServerHttpRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author Jon Brisbin
 */
public class HttpCodec implements StreamingCodec<Tuple2<ServerHttpRequest<Buffer>, Composable<ResponseEntity<Buffer>>>> {

	private static final Logger  LOG                 = LoggerFactory.getLogger(HttpCodec.class);
	private static final int     DEFAULT_BUFFER_SIZE = 8 * 1024;
	private static final Charset ASCII               = Charset.forName("US-ASCII");
	private static final Charset ISO_8859_1          = Charset.forName("ISO-8859-1");
	private static final Charset UTF_8               = Charset.forName("UTF-8");

	private final int     inputBufferSize;
	private final int     outputBufferSize;
	private final boolean keepAlive;
	private final boolean streaming;

	private long    contentLength = 0;
	private int     dataRead      = 0;
	private boolean chunked       = false;

	private final HttpRequestFactory  requestFactory;
	private final MessageConstraints  messageConstraints;
	private final ByteBufferAllocator byteBufferAllocator;

	private SessionInputBufferImpl    inputBuffer;
	private SessionOutputBufferImpl   outputBuffer;
	private DefaultHttpRequestParser  httpRequestParser;
	private DefaultHttpResponseWriter httpResponseWriter;

	private final Dispatcher                dispatcher;
	private       ServerHttpRequest<Buffer> currentRequest;
	private       Buffer                    content;

	public HttpCodec() {
		this(new SynchronousDispatcher());
	}

	public HttpCodec(@Nonnull Dispatcher dispatcher) {
		this(DEFAULT_BUFFER_SIZE,
				 DEFAULT_BUFFER_SIZE,
				 true,
				 false,
				 dispatcher);
	}

	public HttpCodec(int inputBufferSize,
									 int outputBufferSize,
									 boolean keepAlive,
									 boolean streaming,
									 @Nonnull Dispatcher dispatcher) {
		this(inputBufferSize,
				 outputBufferSize,
				 keepAlive,
				 streaming,
				 dispatcher,
				 new DefaultHttpRequestFactory(),
				 MessageConstraints.DEFAULT,
				 new HeapByteBufferAllocator());
	}

	public HttpCodec(int inputBufferSize,
									 int outputBufferSize,
									 boolean keepAlive,
									 boolean streaming,
									 @Nonnull Dispatcher dispatcher,
									 @Nonnull HttpRequestFactory requestFactory,
									 @Nonnull MessageConstraints messageConstraints,
									 @Nonnull ByteBufferAllocator byteBufferAllocator) {
		this.inputBufferSize = inputBufferSize;
		this.outputBufferSize = outputBufferSize;
		this.keepAlive = keepAlive;
		this.streaming = streaming;
		this.dispatcher = dispatcher;
		this.requestFactory = requestFactory;
		this.messageConstraints = messageConstraints;
		this.byteBufferAllocator = byteBufferAllocator;

		reset();
	}

	@Override
	public void decode(Buffers buffers, Consumer<Tuple2<ServerHttpRequest<Buffer>, Composable<ResponseEntity<Buffer>>>> consumer) {
		try {
			dataRead += httpRequestParser.fillBuffer(buffers.getReadableByteChannel());
			if (LOG.isTraceEnabled()) {
				LOG.trace("read {} total data from input buffers...", dataRead);
			}

			if (null == currentRequest) {
				HttpRequest req = httpRequestParser.parse();
				if (null != req) {
					currentRequest = new HttpComponentsServerHttpRequest<Buffer>(req, new Composable<Buffer>(dispatcher));
					contentLength = currentRequest.getHeaders().getContentLength();
					if (contentLength > -1) {
						content = new Buffer((int) contentLength, true);
					} else {
						content = new Buffer();
					}
					dataRead = 0;

					Composable<ResponseEntity<Buffer>> response = new Composable<ResponseEntity<Buffer>>(dispatcher);
					consumer.accept(Tuple.of(currentRequest, response));
				}
			}

			if (inputBuffer.hasData()) {
				int avail = inputBuffer.available();
				dataRead += inputBuffer.read(new WritableByteChannel() {
					@Override
					public int write(ByteBuffer src) throws IOException {
						int start = content.position();
						content.append(src);
						return content.position() - start;
					}

					@Override
					public boolean isOpen() {
						return content.remaining() > 0;
					}

					@Override
					public void close() throws IOException {
					}
				});

				if (contentLength >= 0 && dataRead == contentLength) {
					// body is complete
					currentRequest.getBody().accept(content);

					currentRequest = null;
				}
			} else {
				currentRequest = null;
			}
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}

	@Override
	public Buffers encode(ByteBuffer buffer) {
		return null;
	}

	@Override
	public void encode(Object data, OutputStream stream) {
		if (!ResponseEntity.class.isInstance(data)) {
			throw new IllegalArgumentException("Only ResponseEntity is currently supported");
		}

		ResponseEntity<?> responseEntity = (ResponseEntity<?>) data;
		MediaType contentType = responseEntity.getHeaders().getContentType();

		HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1,
																											responseEntity.getStatusCode().value(),
																											responseEntity.getStatusCode().getReasonPhrase());
		for (Map.Entry<String, String> entry : responseEntity.getHeaders().toSingleValueMap().entrySet()) {
			httpResponse.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
		}
		httpResponse.setHeader("Server", "Reactor/1.0");

		try {
			Object body = responseEntity.getBody();
			if (null != body && Composable.class.isInstance(body)) {
				// TODO: body isn't ready yet
			} else if (null != body) {
				ContentType ct = ContentType.create(null != contentType ? contentType.toString() : "text/plain");
				HttpEntity entity;
				if (String.class.isInstance(body)) {
					entity = new StringEntity(((String) body), ct);
				} else if (byte[].class.isInstance(body)) {
					entity = new ByteArrayEntity((byte[]) body, ct);
				} else if (InputStream.class.isInstance(body)) {
					BasicHttpEntity bhe = new BasicHttpEntity();
					bhe.setContentType(ct.toString());
					bhe.setContent((InputStream) body);
					bhe.setContentLength(responseEntity.getHeaders().getContentLength());
					entity = bhe;
				} else {
					throw new IllegalArgumentException(body + " not supported as output type");
				}

				httpResponse.setEntity(entity);

				httpResponseWriter.write(httpResponse);

//				Buffer buff = new Buffer();
//				buff.flip();
//				if (LOG.isTraceEnabled()) {
//					LOG.trace("sending: {}", buff.asString());
//				}
//				stream.write(buff.asBytes());

				outputBuffer.flush(Channels.newChannel(stream));
				//entity.writeTo(stream);
				stream.flush();

				reset();
			}
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}

	private void reset() {
		inputBuffer = new SessionInputBufferImpl(inputBufferSize,
																						 inputBufferSize,
																						 UTF_8.newDecoder(),
																						 byteBufferAllocator);
		outputBuffer = new SessionOutputBufferImpl(outputBufferSize,
																							 outputBufferSize,
																							 UTF_8.newEncoder(),
																							 byteBufferAllocator);

		httpRequestParser = new DefaultHttpRequestParser(inputBuffer,
																										 BasicLineParser.INSTANCE,
																										 requestFactory,
																										 messageConstraints);
		httpResponseWriter = new DefaultHttpResponseWriter(outputBuffer,
																											 BasicLineFormatter.INSTANCE);
	}
}
