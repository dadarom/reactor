/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.tcp;

import java.nio.channels.SocketChannel;

import reactor.fn.Supplier;
import reactor.tcp.codec.Codec;


/**
 * Implementation of {@link TcpNioConnectionConfigurer} for non-SSL
 * NIO connections.
 * @author Gary Russell
 *
 */
public class DefaultTcpNioConnectionConfigurer implements TcpNioConnectionConfigurer {

	public <T> TcpNioConnection<T> createNewConnection(SocketChannel socketChannel, boolean server, boolean lookupHost,
			 ConnectionFactorySupport<T> connectionFactory, Supplier<? extends Codec<T>> codecSupplier) throws Exception {
		return new TcpNioConnection<T>(socketChannel, server, lookupHost, connectionFactory, codecSupplier.get());
	}
}
