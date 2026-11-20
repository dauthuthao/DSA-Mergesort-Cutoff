import java.util.Arrays;
import java.util.Random;

public class MergeCutoffTest {

    private static void checkCase(Integer[] input, int cutoff, String label) {
        Integer[] expected = input.clone();
        Integer[] actual = input.clone();

        Arrays.sort(expected);
        MergeCutoff.sort(actual, cutoff);

        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(
                    "Mismatch in " + label + ", cutoff=" + cutoff
                            + "\nExpected: " + Arrays.toString(expected)
                            + "\nActual:   " + Arrays.toString(actual));
        }

        if (!MergeCutoff.isSorted(actual)) {
            throw new AssertionError(
                    "isSorted returned false in " + label);
        }
    }

    private static void checkInvalidCutoff() {
        try {
            MergeCutoff.sort(new Integer[]{3, 1, 2}, 0);
            throw new AssertionError("cutoff=0 should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public static void main(String[] args) {
        checkCase(new Integer[]{}, 1, "empty array");
        checkCase(new Integer[]{7}, 1, "single element");
        checkCase(new Integer[]{2, 1}, 1, "two elements");
        checkCase(new Integer[]{5, 4, 3, 2, 1}, 1, "reverse order");
        checkCase(new Integer[]{1, 2, 3, 4, 5}, 3, "already sorted");
        checkCase(new Integer[]{4, 1, 4, 2, 2, 9, 0}, 4, "duplicates");
        checkCase(new Integer[]{-5, 3, 0, -5, 8, 1}, 10, "cutoff larger than array");

        Random random = new Random(20261118L);
        Integer[] randomInput = new Integer[20_000];
        for (int i = 0; i < randomInput.length; i++) {
            randomInput[i] = random.nextInt(2_001) - 1_000;
        }

        int[] cutoffs = {1, 2, 7, 10, 20, 45, 100, 20_000};
        for (int cutoff : cutoffs) {
            checkCase(randomInput, cutoff, "random array");
        }

        checkInvalidCutoff();

        System.out.println("All MergeCutoff tests passed.");
    }
}
