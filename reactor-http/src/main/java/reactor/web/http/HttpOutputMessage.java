/*
 * Copyright 2002-2010 the original author or authors.
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

package reactor.web.http;

import reactor.core.Composable;

/**
 * Represents an HTTP output message, consisting of {@linkplain #getHeaders() headers} and a writable {@linkplain
 * #getBody() body}.
 * <p/>
 * <p>Typically implemented by an HTTP request on the client-side, or a response on the server-side.
 *
 * @author Arjen Poutsma
 * @author Jon Brisbin
 */
public interface HttpOutputMessage<T> extends HttpMessage {

	/**
	 * Return the body of the message as a {@link Composable}.
	 *
	 * @return the body as a {@link Composable}
	 */
	Composable<T> getBody();

}