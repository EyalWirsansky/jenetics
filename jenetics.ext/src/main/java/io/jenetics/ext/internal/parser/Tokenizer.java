/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
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
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package io.jenetics.ext.internal.parser;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Interface for all tokenizers.
 *
 * @param <T> the token value type
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @since !__version__!
 * @version !__version__!
 */
@FunctionalInterface
public interface Tokenizer<T> {

	/**
	 * Return the next available <em>token</em>, or {@code null} if no further
	 * tokens are available.
	 *
	 * @return the next available token
	 */
	T next();

	default Tokenizer<T> filter(final Predicate<? super T> filter) {
		return () -> {
			var token = Tokenizer.this.next();
			while (token != null && !filter.test(token)) {
				token = Tokenizer.this.next();
			}
			return token;
		};
	}

	default <A> Tokenizer<A> map(final Function<? super T, ? extends A> f) {
		return () -> f.apply(next());
	}

	default Stream<T> tokens() {
		return Stream.generate(this::next).takeWhile(Objects::nonNull);
	}


	static <T> Iterator<T> toIterator(final Tokenizer<? extends T> tokenizer) {
		return new Iterator<T>() {
			private T next;
			private boolean hasNext = false;
			private boolean finished = false;

			@Override
			public boolean hasNext() {
				if (finished) return false;
				if (hasNext) return true;

				try {
					return (hasNext = (next = tokenizer.next()) != null);
				} finally {
					if (!hasNext) finished = true;
				}
			}

			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}

				hasNext = false;
				return next;
			}
		};
	}

}
