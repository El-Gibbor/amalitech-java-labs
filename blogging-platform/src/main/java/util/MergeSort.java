package util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A textbook top down merge sort. Worst case, average, and best case time
 * complexity are all O(n log n), since the list is halved log n times and
 * each of the n items is visited once per merge. Space complexity is O(n)
 * for the merged output lists.
 */
public final class MergeSort {
    private MergeSort() {
    }

    public static <T> List<T> sort(List<T> items, Comparator<T> comparator) {
        if (items.size() <= 1) {
            return new ArrayList<>(items);
        }
        int middle = items.size() / 2;
        List<T> left = sort(items.subList(0, middle), comparator);
        List<T> right = sort(items.subList(middle, items.size()), comparator);
        return merge(left, right, comparator);
    }

    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<T> comparator) {
        List<T> merged = new ArrayList<>(left.size() + right.size());
        int i = 0;
        int j = 0;
        while (i < left.size() && j < right.size()) {
            if (comparator.compare(left.get(i), right.get(j)) <= 0) {
                merged.add(left.get(i++));
            } else {
                merged.add(right.get(j++));
            }
        }
        while (i < left.size()) {
            merged.add(left.get(i++));
        }
        while (j < right.size()) {
            merged.add(right.get(j++));
        }
        return merged;
    }
}
