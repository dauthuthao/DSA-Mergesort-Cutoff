import java.util.Objects;

/** Top-down Mergesort with an Insertion Sort cutoff.*/

public final class MergeCutoff {

    private MergeCutoff() { }

    private static void merge(Comparable[] a, Comparable[] aux, int lo, int mid, int hi) {

        // Copy mảng phụ aux[]
        for (int k = lo; k <= hi; k++) aux[k] = a[k];

        // Merge ngược lại mảng a[]
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if      (i > mid)              a[k] = aux[j++];
            else if (j > hi)               a[k] = aux[i++];
            else if (less(aux[j], aux[i])) a[k] = aux[j++];
            else                           a[k] = aux[i++];
        }
    }

    private static void insertionSort(Comparable[] a, int lo, int hi) {
        for (int i = lo; i <= hi; i++) {
            for (int j = i; j > lo && less(a[j], a[j - 1]); j--) {
                exch(a, j, j - 1);
            }
        }
    }

    // Mergesort có tham số cutoff
    private static void sort(Comparable[] a, Comparable[] aux, int lo, int hi, int cutoff) {
        // Khi (hi - lo + 1) <= cutoff thì dùng Insertion Sort
        if (hi <= lo + cutoff - 1) {
            if (hi > lo) {
                insertionSort(a, lo, hi);
            }
            return;
        }

        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid, cutoff);
        sort(a, aux, mid + 1, hi, cutoff);
        merge(a, aux, lo, mid, hi);
    }

    public static void sort(Comparable[] a, int cutoff) {
        Objects.requireNonNull(a, "Input array must not be null.");
        if (cutoff < 1)     throw new IllegalArgumentException("cutoff must be >= 1.");
        if (a.length <= 1)  return;

        Comparable[] aux = new Comparable[a.length];
        sort(a, aux, 0, a.length - 1, cutoff);
    }

    private static boolean less(Comparable v, Comparable w) {
        return v.compareTo(w) < 0;
    }

    private static void exch(Comparable[] a, int i, int j) {
        Comparable temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    public static boolean isSorted(Comparable[] a) {
        Objects.requireNonNull(a, "Input array must not be null.");

        for (int i = 1; i < a.length; i++) {
            if (less(a[i], a[i - 1])) return false;
        }
        return true;
    }
}
