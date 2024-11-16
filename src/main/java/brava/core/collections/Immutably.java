package brava.core.collections;

import brava.core.BiTruth;
import brava.core.exceptions.UnreachableException;
import brava.core.functional.TriFunction;
import brava.core.tuples.Tuple;
import brava.core.tuples.Tuple2;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Methods for "updating" {@link ImmutableCollection}s.
 */
public final class Immutably {
    /**
     * Similar to {@link List#add(T)}, but instead of modifying the original list,
     * creates a <b><i>new</i></b> {@link ImmutableList} with the new {@link T} at the end.
     * 
     * @param list The original {@link List}, which will <b><i>NOT be modified</i></b>.
     * @param element The new {@link T} item you want {@link List#add(T)}
     * @return An {@link ImmutableList} containing the original {@code list} + {@code element}.
     * @param <T> The list element type.
     * @throws NullPointerException If {@code element}, {@code list}, or any of {@code list}'s elements are {@code null}.
     */
    @Contract(pure = true)
    public static <T> @NotNull ImmutableList<@NotNull T> add(@NotNull List<@NotNull T> list, @NotNull T element) {
        if(list.isEmpty()) {
            return ImmutableList.of(element);
        }

        return Stream.concat(
            list.stream(),
            Stream.of(element)
        ).collect(ImmutableList.toImmutableList());
    }

    /**
     * Similar to {@link List#add(int, Object)}, but instead of modifying the original list,
     * creates a <b><i>new</i></b> {@link ImmutableList} with the new {@link T} inserted in it.
     * 
     * @param list The original {@link List}, which will <b><i>NOT be modified</i></b>.
     * @param element The new {@link T} element that you want to {@link List#add(int, T)}
     * @param index The index that you'd like the new {@link T} element to have. If something is already at {@code index}, it, and everything after it, will be have their indices increased by 1.
     * @return An {@link ImmutableList} that contains {@code element} at {@code index}.
     * @param <T> The list element type.
     */
    @Contract(pure = true)
    public static <T> @NotNull ImmutableList<@NotNull T> insert(@NotNull List<@NotNull T> list, @NotNull T element, int index) {
        Preconditions.checkPositionIndex(index, list.size());

        if (list.isEmpty()) {
            assert index == 0;
            return ImmutableList.of(element);
        }

        var erator = list.iterator();
        var result = IntStream.range(0, list.size() + 1)
            .mapToObj(i -> i == index ? element : erator.next())
            .collect(ImmutableList.toImmutableList());

        assert erator.hasNext() == false;
        return result;
    }

    /**
     * Similar to {@link Map#put(K, V)}, but instead of modifying the original {@link Map},
     * creates a <b><i>new</i></b> {@link ImmutableMap} with the updated element.
     * @param map The original {@link Map}, which will <b><i>NOT be modified</i></b>.
     * @param key The {@link Map.Entry#getKey()} of the updated element.
     * @param value The {@link Map.Entry#getValue()} of the updated element.
     * @return An {@link ImmutableMap} with {@code key} set to {@code value}.
     * @param <K> The {@link Map.Entry#getKey()} type.
     * @param <V> The {@link Map.Entry#getValue()} type.
     */
    @Contract(pure = true, value = "_,_,_->new")
    public static <K, V> @NotNull ImmutableMap<K, V> put(
        @NotNull Map<@NotNull K, @NotNull V> map,
        @NotNull K key,
        @NotNull V value
    ) {
        if (map.isEmpty()) {
            return ImmutableMap.of(key, value);
        }

        return ImmutableMap.<K, V>builder()
            .putAll(map)
            .put(key, value)
            .buildKeepingLast();
    }

    @ApiStatus.Experimental
    @SuppressWarnings("java:S2583")
    public static <K, V> Tuple2<ImmutableMap<K, V>, V> remove(
        @NotNull Map<@NotNull K, @NotNull V> map,
        @NotNull K key
    ) {
        var oldValue = map.get(key);
        if (oldValue == null) {
            throw new IllegalArgumentException(
                "Can't remove the key %s because is isn't present in %s!".formatted(key, map));
        }

        var newMap = map.entrySet()
            .stream()
            .filter(it -> it.getKey().equals(key) == false)
            .collect(ImmutableMap.toImmutableMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));

        return Tuple.of(newMap, oldValue);
    }

    @ApiStatus.Experimental
    public static <K, V> ImmutableMap<K, V> merge(
        Map<K, V> a,
        Map<K, V> b,
        TriFunction<? super K, ? super V, ? super V, ? extends V> arbiter
    ) {
        if (a.isEmpty()) {
            return ImmutableMap.copyOf(b);
        }

        if (b.isEmpty()) {
            return ImmutableMap.copyOf(a);
        }

        return Stream.concat(
                a.keySet().stream(),
                b.keySet().stream()
            )
            .distinct()
            .map(k -> {
                var value = switch (BiTruth.of(a, b, it -> it.containsKey(k))) {
                    case A -> a.get(k);
                    case B -> b.get(k);
                    case BOTH -> arbiter.apply(k, a.get(k), b.get(k));
                    case NEITHER -> throw new UnreachableException("How did %s get in here?!".formatted(k));
                };

                return Map.entry(k, value);
            })
            .collect(ImmutableMap.toImmutableMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }
}
