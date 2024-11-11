package src.test.java.brava.core.tuples;

import java.util.function.Function;

public class TupleTestData {
    public static final Function<CharSequence, Integer> stringMapper = CharSequence::length;
}
