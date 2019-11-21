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
package io.jenetics.ext.moea;

import static io.jenetics.internal.util.SerialIO.readIntArray;
import static io.jenetics.internal.util.SerialIO.writeIntArray;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @version !__version__!
 * @since !__version__!
 */
final class IntVec implements Vec<int[]>, Serializable {
	private static final long serialVersionUID = 1L;
	private final int[] _data;

	IntVec(final int[] data) {
		Vecs.checkVecLength(data.length);
		_data = data;
	}

	@Override
	public int[] data() {
		return _data;
	}

	@Override
	public int length() {
		return _data.length;
	}

	@Override
	public ElementComparator<int[]> comparator() {
		return IntVec::cmp;
	}

	private static int cmp(final int[] u, final int[] v, final int i) {
		return Integer.compare(u[i], v[i]);
	}

	@Override
	public ElementDistance<int[]> distance() {
		return IntVec::dist;
	}

	private static double dist(final int[] u, final int[] v, final int i) {
		return u[i] - v[i];
	}

	@Override
	public Comparator<int[]> dominance() {
		return Pareto::dominance;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(_data);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj == this ||
			obj instanceof IntVec &&
			Arrays.equals(((IntVec) obj)._data, _data);
	}

	@Override
	public String toString() {
		return Arrays.toString(_data);
	}


	/* *************************************************************************
	 *  Java object serialization
	 * ************************************************************************/

	private Object writeReplace() {
		return new Serial(Serial.INT_VEC, this);
	}

	private void readObject(final ObjectInputStream stream)
		throws InvalidObjectException
	{
		throw new InvalidObjectException("Serialization proxy required.");
	}

	void write(final DataOutput out) throws IOException {
		writeIntArray(_data, out);
	}

	static IntVec read(final DataInput in) throws IOException {
		return new IntVec(readIntArray(in));
	}
}
