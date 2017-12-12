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
package io.jenetics.ext.util;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static io.jenetics.internal.util.IndexSorter.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

import io.jenetics.internal.util.IndexSorter;
import io.jenetics.util.ISeq;
import io.jenetics.util.MSeq;
import io.jenetics.util.Seq;

import io.jenetics.ext.internal.IntList;

/**
 * This class contains static methods concerning <em>Pareto</em>- front and
 * dominance calculations.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @version !__version__!
 * @since !__version__!
 */
public final class Pareto<T> {

	private final ISeq<T> _front;
	private int[] _rank;
	private double[] _distance;

	public Pareto(
		final ISeq<T> front,
		final int[] rank,
		final double[] distance
	) {
		_front = requireNonNull(front);
		_rank = requireNonNull(rank);
		_distance = requireNonNull(distance);
	}


	/* *************************************************************************
	 * Crowding distance methods.
	 * ************************************************************************/

	/**
	 * The crowding distance value of a solution provides an estimate of the
	 * density of solutions surrounding that solution. The <em>crowding
	 * distance</em> value of a particular solution is the average distance of
	 * its two neighboring solutions.
	 *
	 * @see #crowdingDistance(Seq, ElementComparator, ElementDistance, ToIntFunction)
	 *
	 * @param set the point set used for calculating the <em>crowding distance</em>
	 * @param <T> the vector type
	 * @return the crowded distances fo the {@code set} points
	 * @throws NullPointerException if the input {@code set} is {@code null}
	 * @throws IllegalArgumentException if {@code set.get(0).length() < 2}
	 */
	public static <T> double[] crowdingDistance(final Seq<? extends Vec<T>> set) {
		return crowdingDistance(
			set,
			Vec::compareTo,
			Vec::distance,
			Vec::length
		);
	}

	/**
	 * The crowding distance value of a solution provides an estimate of the
	 * density of solutions surrounding that solution. The <em>crowding
	 * distance</em> value of a particular solution is the average distance of
	 * its two neighboring solutions.
	 *
	 * @see #crowdingDistance(Seq)
	 *
	 * @param set the point set used for calculating the <em>crowding distance</em>
	 * @param comparator the comparator which defines the (total) order of the
	 *        vector elements of {@code T}
	 * @param distance the distance of two vector elements
	 * @param dimension the dimension of vector type {@code T}
	 * @param <T> the vector type
	 * @return the crowded distances fo the {@code set} points
	 * @throws NullPointerException if one of the arguments is {@code null}
	 */
	public static <T> double[] crowdingDistance(
		final Seq<? extends T> set,
		final ElementComparator<? super T> comparator,
		final ElementDistance<? super T> distance,
		final ToIntFunction<? super T> dimension
	) {
		requireNonNull(set);
		requireNonNull(comparator);
		requireNonNull(distance);

		final double[] result = new double[set.size()];
		if (set.size() < 3) {
			Arrays.fill(result, Double.POSITIVE_INFINITY);
		} else {
			final int[] idx = new int[set.size()];
			final IndexSorter sorter = IndexSorter.sorter(set.size());

			for (int m = 0; m < dimension.applyAsInt(set.get(0)); ++m) {
				sorter.sort(set, init(idx), comparator.ofIndex(m));

				result[idx[0]] = Double.POSITIVE_INFINITY;
				result[idx[set.size() - 1]] = Double.POSITIVE_INFINITY;

				final T max = set.get(idx[0]);
				final T min = set.get(idx[set.size() - 1]);
				final double dm = distance.distance(max, min, m);

				if (Double.compare(dm, 0) == 0) {
					Arrays.fill(result, 0.0);
				} else {
					for (int i = 1; i < set.size() - 1; ++i) {
						final double d = distance.distance(
							set.get(idx[i - 1]), set.get(idx[i + 1]), m
						);

						result[idx[i]] += d/dm;
					}
				}
			}
		}

		return result;
	}

	/* *************************************************************************
	 * Pareto ranks methods.
	 * ************************************************************************/

	/**
	 * Calculates the <em>non-domination</em> rank of the given input {@code set},
	 * using the <em>natural</em> order of the elements as <em>dominance</em>
	 * measure.
	 *
	 * <p>
	 *  <b>Reference:</b><em>
	 *      Kalyanmoy Deb, Associate Member, IEEE, Amrit Pratap,
	 *      Sameer Agarwal, and T. Meyarivan.
	 *      A Fast and Elitist Multiobjective Genetic Algorithm: NSGA-II,
	 *      IEEE TRANSACTIONS ON EVOLUTIONARY COMPUTATION, VOL. 6, NO. 2,
	 *      APRIL 2002.</em>
	 *
	 * @param set the input set
	 * @param <T> the element type
	 * @return the <em>non-domination</em> rank of the given input {@code set}
	 */
	public static <T> int[] ranks(final Seq<? extends Vec<T>> set) {
		return ranks(set, Vec::dominance);
	}

	/**
	 * Calculates the <em>non-domination</em> rank of the given input {@code set},
	 * using the given {@code dominance} comparator.
	 *
	 * <p>
	 *  <b>Reference:</b><em>
	 *      Kalyanmoy Deb, Associate Member, IEEE, Amrit Pratap,
	 *      Sameer Agarwal, and T. Meyarivan.
	 *      A Fast and Elitist Multiobjective Genetic Algorithm: NSGA-II,
	 *      IEEE TRANSACTIONS ON EVOLUTIONARY COMPUTATION, VOL. 6, NO. 2,
	 *      APRIL 2002.</em>
	 *
	 * @param set the input set
	 * @param dominance the dominance comparator used
	 * @param <T> the element type
	 * @return the <em>non-domination</em> rank of the given input {@code set}
	 */
	public static <T> int[] ranks(
		final Seq<? extends T> set,
		final Comparator<? super T> dominance
	) {
		// Pre-compute the dominance relations.
		final int[][] d = new int[set.size()][set.size()];
		for (int i = 0; i < set.size(); ++i) {
			for (int j = i + 1; j < set.size(); ++j) {
				if (i != j) {
					d[i][j] = dominance.compare(set.get(i), set.get(j));
					d[j][i] = -d[i][j];
				}
			}
		}

		// Compute for each element p the element q that it dominates and the
		// number of times it is dominated. Using the names as defined in the
		// referenced paper.
		final int[] nq = new int[set.size()];
		final List<IntList> fronts = new ArrayList<>();
		IntList Fi = new IntList();

		for (int p = 0; p < set.size(); ++p) {
			final IntList Sp = new IntList();
			int np = 0;

			for (int q = 0; q < set.size(); ++q) {
				if (p != q) {
					// If p dominates q, add q to the set of solutions
					// dominated by p.
					if (d[p][q] > 0) {
						Sp.add(q);

					// Increment the domination counter of p.
					} else if (d[q][p] > 0) {
						np += 1;
					}
				}
			}

			// p belongs to the first front.
			if (np == 0) {
				Fi.add(p);
			}

			fronts.add(Sp);
			nq[p] = np;
		}

		// Initialize the front counter.
		int i = 0;
		final int[] ranks = new int[set.size()];
		while (!Fi.isEmpty()) {
			// Used to store the members of the next front.
			final IntList Q = new IntList();

			for (int p = 0; p < Fi.size(); ++p) {
				final int fi = Fi.get(p);
				ranks[fi] = i;

				// Update the dominated counts as compute next front.
				for (int k = 0, n = fronts.get(fi).size(); k < n; ++k) {
					final int q = fronts.get(fi).get(k);
					nq[q] -= 1;

					// q belongs to the next front.
					if (nq[q] == 0) {
						Q.add(q);
					}
				}
			}

			++i;
			Fi = Q;
		}

		return ranks;
	}

	/* *************************************************************************
	 * 'front'
	 * ************************************************************************/

	/**
	 * Return the elements, from the given input {@code set}, which are part of
	 * the pareto front. The {@link Comparable} interface defines the dominance
	 * measure of the elements, used for calculating the pareto front.
	 * <p>
	 *  <b>Reference:</b><em>
	 *      E. Zitzler and L. Thiele.
	 *      Multiobjective Evolutionary Algorithms: A Comparative Case Study
	 *      and the Strength Pareto Approach,
	 *      IEEE Transactions on Evolutionary Computation, vol. 3, no. 4,
	 *      pp. 257-271, 1999.</em>
	 *
	 * @param set the input set
	 * @param <T> the element type
	 * @return the elements which are part of the pareto set
	 * @throws NullPointerException if one of the arguments is {@code null}
	 */
	public static <T> ISeq<Vec<T>> front(final Iterable<? extends Vec<T>> set) {
		return front(set, Vec::dominance);
	}

	/**
	 * Return the elements, from the given input {@code set}, which are part of
	 * the pareto front.
	 * <p>
	 *  <b>Reference:</b><em>
	 *      E. Zitzler and L. Thiele.
	 *      Multiobjective Evolutionary Algorithms: A Comparative Case Study
	 *      and the Strength Pareto Approach,
	 *      IEEE Transactions on Evolutionary Computation, vol. 3, no. 4,
	 *      pp. 257-271, 1999.</em>
	 *
	 * @param set the input set
	 * @param dominance the dominance comparator used
	 * @param <T> the element type
	 * @return the elements which are part of the pareto set
	 * @throws NullPointerException if one of the arguments is {@code null}
	 */
	public static <T> ISeq<T> front(
		final Iterable<? extends T> set,
		final Comparator<? super T> dominance
	) {
		final MSeq<T> front = MSeq.of(set);

		int n = front.size();
		int i = 0;
		while (i < n) {
			int j = i + 1;
			while (j < n) {
				if (dominance.compare(front.get(i), front.get(j)) > 0) {
					--n;
					front.swap(j, n);
				} else if (dominance.compare(front.get(j), front.get(i)) > 0) {
					--n;
					front.swap(i, n);
					--i;
					break;
				} else {
					++j;
				}
			}
			++i;
		}

		return front.subSeq(0, n).copy().toISeq();
	}


	/* *************************************************************************
	 * Common 'dominance' methods.
	 * ************************************************************************/

	/**
	 * Calculates the <a href="https://en.wikipedia.org/wiki/Pareto_efficiency">
	 *     <b>Pareto Dominance</b></a> of the two vectors <b>u</b> and <b>v</b>.
	 *
	 * @see Vec#dominance(Comparable[], Comparable[])
	 *
	 * @param u the first vector
	 * @param v the second vector
	 * @param <C> the element type of vector <b>u</b> and <b>v</b>
	 * @return {@code 1} if <b>u</b> ≻ <b>v</b>, {@code -1} if <b>v</b> ≻
	 *         <b>u</b> and {@code 0} otherwise
	 * @throws NullPointerException if one of the arguments is {@code null}
	 * @throws IllegalArgumentException if {@code u.length != v.length}
	 */
	public static <C extends Comparable<? super C>> int
	dominance(final C[] u, final C[] v) {
		return dominance(u, v, Comparator.naturalOrder());
	}

	/**
	 * Calculates the <a href="https://en.wikipedia.org/wiki/Pareto_efficiency">
	 *     <b>Pareto Dominance</b></a> of the two vectors <b>u</b> and <b>v</b>.
	 *
	 * @see Vec#dominance(Object[], Object[], Comparator)
	 *
	 * @param u the first vector
	 * @param v the second vector
	 * @param comparator the element comparator which is used for calculating
	 *        the dominance
	 * @param <T> the element type of vector <b>u</b> and <b>v</b>
	 * @return {@code 1} if <b>u</b> ≻ <b>v</b>, {@code -1} if <b>v</b> ≻
	 *         <b>u</b> and {@code 0} otherwise
	 * @throws NullPointerException if one of the arguments is {@code null}
	 * @throws IllegalArgumentException if {@code u.length != v.length}
	 */
	public static <T> int
	dominance(final T[] u, final T[] v, final Comparator<? super T> comparator) {
		requireNonNull(comparator);
		checkLength(u.length, v.length);

		return dominance(
			u, v, u.length,
			(a, b, i) -> comparator.compare(a[i], b[i])
		);
	}

	/**
	 * Calculates the <a href="https://en.wikipedia.org/wiki/Pareto_efficiency">
	 *     <b>Pareto Dominance</b></a> of the two vectors <b>u</b> and <b>v</b>.
	 *
	 * @see Vec#dominance(int[], int[])
	 *
	 * @param u the first vector
	 * @param v the second vector
	 * @return {@code 1} if <b>u</b> ≻ <b>v</b>, {@code -1} if <b>v</b> ≻
	 *         <b>u</b> and {@code 0} otherwise
	 * @throws NullPointerException if one of the arguments is {@code null}
	 * @throws IllegalArgumentException if {@code u.length != v.length}
	 */
	public static int dominance(final int[] u, final int[] v) {
		checkLength(u.length, v.length);

		return dominance(
			u, v, u.length,
			(a, b, i) -> Integer.compare(a[i], b[i])
		);
	}

	/**
	 * Calculates the <a href="https://en.wikipedia.org/wiki/Pareto_efficiency">
	 *     <b>Pareto Dominance</b></a> of the two vectors <b>u</b> and <b>v</b>.
	 *
	 * @see Vec#dominance(long[], long[])
	 *
	 * @param u the first vector
	 * @param v the second vector
	 * @return {@code 1} if <b>u</b> ≻ <b>v</b>, {@code -1} if <b>v</b> ≻
	 *         <b>u</b> and {@code 0} otherwise
	 * @throws NullPointerException if one of the arguments is {@code null}
	 * @throws IllegalArgumentException if {@code u.length != v.length}
	 */
	public static int dominance(final long[] u, final long[] v) {
		checkLength(u.length, v.length);

		return dominance(
			u, v, u.length,
			(a, b, i) -> Long.compare(a[i], b[i])
		);
	}

	/**
	 * Calculates the <a href="https://en.wikipedia.org/wiki/Pareto_efficiency">
	 *     <b>Pareto Dominance</b></a> of the two vectors <b>u</b> and <b>v</b>.
	 *
	 * @see Vec#dominance(double[], double[])
	 *
	 * @param u the first vector
	 * @param v the second vector
	 * @return {@code 1} if <b>u</b> ≻ <b>v</b>, {@code -1} if <b>v</b> ≻
	 *         <b>u</b> and {@code 0} otherwise
	 * @throws NullPointerException if one of the arguments is {@code null}
	 * @throws IllegalArgumentException if {@code u.length != v.length}
	 */
	public static int dominance(final double[] u, final double[] v) {
		checkLength(u.length, v.length);

		return dominance(
			u, v, u.length,
			(a, b, i) -> Double.compare(a[i], b[i])
		);
	}

	private static void checkLength(final int i, final int j) {
		if (i != j) {
			throw new IllegalArgumentException(format(
				"Length are not equals: %d != %d.", i, j
			));
		}
	}

	private static <T> int dominance(
		final T u,
		final T v,
		final int length,
		final ElementComparator<? super T> comparator
	) {
		boolean udominated = false;
		boolean vdominated = false;

		for (int i = 0; i < length; ++i) {
			final int cmp = comparator.compare(u, v, i);

			if (cmp > 0) {
				udominated = true;
				if (vdominated) {
					return 0;
				}
			} else if (cmp < 0) {
				vdominated = true;
				if (udominated) {
					return 0;
				}
			}
		}

		if (udominated == vdominated) {
			return 0;
		} else if (udominated) {
			return 1;
		} else {
			return -1;
		}
	}

}
