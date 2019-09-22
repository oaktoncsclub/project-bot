package win.oakcsclub;

import java.util.*;
import java.util.function.Function;

public class Util {
    @SafeVarargs
    public static <T> Set<T> setOf(T...t){
        return new HashSet<>(Arrays.asList(t));
    }
    @SafeVarargs
    public static <T> List<T> listOf(T...t){
        return Arrays.asList(t);
    }

    public static <T> T first(Iterable<T> t, Function<T,Boolean> conditional){
        for (T next : t) {
            if (conditional.apply(next)) {
                return next;
            }
        }
        return null;
    }
}
