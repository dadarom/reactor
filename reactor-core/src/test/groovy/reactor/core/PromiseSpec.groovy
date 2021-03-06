/*
 * Copyright (c) 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core

import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static reactor.GroovyTestUtils.*

/**
 * @author Stephane Maldini
 */
class PromiseSpec extends Specification {


	def "Promise error handling"() {
		given:
		def latch = new CountDownLatch(1)
		def success = false

		when: "we create a plain Promise"
		def promise = R.promise(supplier { throw new Exception('bad') }).sync().build()

		and:
		promise.onSuccess(consumer { success = true })
		promise.onError(consumer { latch.countDown() })
		//promise.get()

		then:
		latch.await(1, TimeUnit.SECONDS)
		!success
		latch.count == 0

	}

	def "Test promise chaining with exception"() {
		when: "A promise is chained"
		def promise = R.promise(supplier { 1 + 1 }).sync().build()
		promise = promise.map function { it * 2 } map function { throw new RuntimeException("bad") } map function {
			it + 6
		}

		def val = promise.get()

		then: 'the chain is executed'
		val == null
		thrown RuntimeException
	}

}

