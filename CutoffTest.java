import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CutoffTest {

    private static final int[] SIZES = {200_000, 1_000_000};
    private static final int MAX_CUTOFF = 45;
    private static final int ROUNDS = 15; 
    private static final int WARMUP_SIZE = 10_000;
    private static final int WARMUP_REPETITIONS = 2;

    private enum InputType {
        RANDOM,
        SORTED,
        REVERSE
    }

    private static Integer[] generateArray(int n, long seed, InputType type) {
        Integer[] a = new Integer[n];
        Random random = new Random(seed);
        int bound = Math.max(1, n * 10);

        for (int i = 0; i < n; i++) {
            a[i] = random.nextInt(bound);
        }

        if (type == InputType.SORTED || type == InputType.REVERSE) {
            Arrays.sort(a);
        }

        if (type == InputType.REVERSE) {
            reverse(a);
        }

        return a;
    }

    private static void reverse(Integer[] a) {
        int left = 0;
        int right = a.length - 1;

        while (left < right) {
            Integer temp = a[left];
            a[left] = a[right];
            a[right] = temp;
            left++;
            right--;
        }
    }

    private static void warmUp() {
        System.out.println("Warming up the JVM...");

        Integer[] warmBase =
                generateArray(WARMUP_SIZE, 20261118L, InputType.RANDOM);

        for (int repetition = 0; repetition < WARMUP_REPETITIONS; repetition++) {
            for (int cutoff = 1; cutoff <= MAX_CUTOFF; cutoff++) {
                Integer[] warm = warmBase.clone();
                MergeCutoff.sort(warm, cutoff);
            }
        }

        System.out.println("Warm-up completed.");
    }

    private static List<Integer> createCutoffOrder() {
        List<Integer> order = new ArrayList<>();
        for (int cutoff = 1; cutoff <= MAX_CUTOFF; cutoff++) {
            order.add(cutoff);
        }
        return order;
    }

    private static long createSeed(int n, InputType type, int round) {
        return 20261118L + 31L * n + 997L * type.ordinal() + round;
    }

    private static void runExperiment() throws IOException {
        Path output = Path.of("cutoff_measurements.csv");
        warmUp();

        try (BufferedWriter writer =
                     Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {

            writer.write("InputType,N,Cutoff,Round,ElapsedMs");
            writer.newLine();

            for (InputType type : InputType.values()) {
                for (int n : SIZES) {
                    System.out.printf(
                            "%n=== InputType=%s, N=%d ===%n", type, n);

                    // times[cutoff][round]
                    double[][] times = new double[MAX_CUTOFF + 1][ROUNDS];
                    List<Integer> cutoffOrder = createCutoffOrder();

                    for (int round = 0; round < ROUNDS; round++) {
                        // Every cutoff in this round receives the same input.
                        Integer[] base =
                                generateArray(n, createSeed(n, type, round), type);

                        Collections.shuffle(
                                cutoffOrder,
                                new Random(2026L + round + type.ordinal() * 100L + n));

                        for (int cutoff : cutoffOrder) {
                            Integer[] working = base.clone();

                            long start = System.nanoTime();
                            MergeCutoff.sort(working, cutoff);
                            long end = System.nanoTime();

                            // Correctness validation is deliberately outside timing.
                            if (!MergeCutoff.isSorted(working)) {
                                throw new IllegalStateException(
                                        "Sorting failed: type=" + type
                                                + ", N=" + n
                                                + ", cutoff=" + cutoff
                                                + ", round=" + (round + 1));
                            }

                            double elapsedMs = (end - start) / 1_000_000.0;
                            times[cutoff][round] = elapsedMs;

                            writer.write(String.format(
                                    java.util.Locale.US,
                                    "%s,%d,%d,%d,%.6f",
                                    type, n, cutoff, round + 1, elapsedMs));
                            writer.newLine();
                        }

                        writer.flush();
                        System.out.printf("Completed round %d/%d%n",
                                round + 1, ROUNDS);
                    }

                    // Print the best median for this input type and size.
                    int bestCutoff = -1;
                    double bestMedian = Double.POSITIVE_INFINITY;

                    for (int cutoff = 1; cutoff <= MAX_CUTOFF; cutoff++) {
                        double[] samples = times[cutoff].clone();
                        Arrays.sort(samples);
                        double median = samples[ROUNDS / 2];

                        if (median < bestMedian) {
                            bestMedian = median;
                            bestCutoff = cutoff;
                        }
                    }

                    System.out.printf(
                            "Best observed cutoff for %s, N=%d: %d (median %.4f ms)%n",
                            type, n, bestCutoff, bestMedian);
                }
            }
        }

        System.out.println();
        System.out.println("Experiment completed.");
        System.out.println("Raw measurements: " + output.toAbsolutePath());
        System.out.println("Next: python scripts/result.py");
    }

    public static void main(String[] args) {
        try {
            runExperiment();
        } catch (IOException e) {
            System.err.println("Could not write experiment output: " + e.getMessage());
            System.err.println("Run the program from the project root directory.");
            System.exit(1);
        }
    }
}
