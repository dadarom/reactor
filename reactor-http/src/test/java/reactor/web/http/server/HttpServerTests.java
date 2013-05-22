package reactor.web.http.server;

import org.junit.Test;
import reactor.core.Composable;
import reactor.fn.dispatch.BlockingQueueDispatcher;
import reactor.io.Buffer;
import reactor.web.http.HttpStatus;
import reactor.web.http.ResponseEntity;

import static reactor.web.http.server.ServerHttpRequestSelector.get;

/**
 * @author Jon Brisbin
 */
public class HttpServerTests {

	@Test
	public void testHttpServer() throws InterruptedException {
		HttpServer server = new HttpServer(new BlockingQueueDispatcher());

		server.on(get("/hello"), new ServerHttpRequestHandler<Buffer>() {
			@Override
			public void serve(ServerHttpRequest<Buffer> req, Composable<ResponseEntity<?>> resp) {
				System.out.println("req: " + req);
				resp.accept(new ResponseEntity<String>("Hello World!", HttpStatus.OK));
			}
		});

		server.start();

		while (true) {
			Thread.sleep(5000);
		}
	}

}
