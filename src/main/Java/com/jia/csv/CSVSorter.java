package com.jia.csv;

import com.jia.sort.MergeSort;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Read a CSV file. Optionally, sort by one or more columns, in ascending or descending order as requested.
 *  
 * Assumptions:
 * - Field names are in the first line and they are unique
 * - "Name" is the first column
 * - Values for the "Name" column are in the format of "String, String" (without the double quotes)
 * - Values for the "Address" column does NOT contain comma
 * - Values for the "Invoice Amount" column can be integer or floating point numbers but NOT both
 * - Values for the "Date of Sale" column are in the format of "MM/DD/YYYY" (without the double quotes)
 * - No schema is required, therefore, we cannot assume the data type. If a schema was supplied, we shall be able to 
 *   validate the values.
 * 
 * How to Run the program:
 * 
 * CSVSortReader [[column number][sort order] ...] csv_file_path
 * Column number: starts at 0
 * Sort order: a-acending (default); d-descending
 * 
 * Examples:
 * 
 * CSVSortReader 0a 1d sort.csv
 * The above command will sort file sort.csv primarily on column 0 in ascending order, secondarily on column 1 in descending order
 * 
 * Tested with 
 * - CVSSortReaderUnitTest.java
 * - assigned.csv: supplied by assigner
 * - sort.csv: Based on assigned.csv, but adding more rows to test larger size case of mergeSort. Also added cases 
 *             when the primary sorting column are identical to test secondary column sorting.
 *             
 *  Future work:
 *  - Performance tuning for buffer size. If the whole things won't fit in memory, group them on disk then sort
 *    each group. Iterate until it fits in memory.
 * 
*/
final public class CSVSorter {
    private static final String COMMA_DELIMITER = ",";
    private static final String INTEGER = "[0-9]+";
    private static final String DOUBLE = "[0-9]{1,13}([.][0-9]*)?";
    private static final String SORT_OPTION = "[0-9]+[ad]";
    // private static final char ASENDING = 'a'; This is the default. We don't need to test for it.
    private static final char DESCENDING = 'd';

    // precompile the above regex for repeated use
    private static final Pattern COMMA_PATTERN = Pattern.compile(COMMA_DELIMITER);
    private static final Pattern INTEGER_PATTERN = Pattern.compile(INTEGER);
    private static final Pattern DOUBLE_PATTERN = Pattern.compile(DOUBLE);
    private static final Pattern SORT_OPTION_PATTERN = Pattern.compile(SORT_OPTION);

    public static final int SERIAL_READ = 0;
    public static final int PARALLEL_READ = 1;
    public static final boolean NOT_THREADED = false;
    public static final boolean THREADED = true;

    private String[] names; // We don't need this yet. But keep it here for future extension (e.g., match names with values)

    @SuppressWarnings("unchecked")
    public void sort(String filePath, String[] sortOpts, int readMethod, boolean threaded) throws IOException {
        File file = new File(filePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            // Read the header line
            String nameLine = reader.readLine();
            names = splitHeader(nameLine);
            names = Arrays.stream(names).map(n -> parseString(n)).toArray(size -> new String[size]);

            /* 
             * Parse the sort options:
             * [column number][sort order] ...
             * Column number: starts at 0
             * Sort order: a-acending (default); d-descending
             */
            Comparator<Comparable<Object>[]>[] comparators = new Comparator[sortOpts.length];
            IntStream.range(0, sortOpts.length).forEach(index -> {
                String opt = sortOpts[index];
                Comparator<Comparable<Object>[]> c;
                int columnNo = Integer.valueOf(opt.substring(0, opt.length() - 1));
                if (columnNo > names.length - 1) {
                    throw new IllegalArgumentException(
                            "Column number " + columnNo + " exceeds the width of the CVS file 0-" + (names.length - 1));
                }
                if (opt.charAt(opt.length() - 1) == DESCENDING)
                    c = createDesendingComparator(columnNo);
                else
                    c = createAscendingComparator(columnNo);
                comparators[index] = c;
            });
            Comparator<Comparable<Object>[]> comparator = createComparator(comparators);

            // read the value lines and sort

            MergeSort sorter = new MergeSort();

            if (readMethod == SERIAL_READ) {
                List<Comparable<Object>[]> records = reader.lines().map(line -> splitValue(line))
                        .map(line -> parseValues(line)).collect(Collectors.toList());
                sorter.mergeSort(records, comparator, threaded);
                records.stream().forEach(e -> System.out.println(Arrays.toString(e)));

            } else if (readMethod == PARALLEL_READ) {
                List<Comparable<Object>[]> sorted = reader.lines().parallel().map(line -> splitValue(line))
                        .map(line -> parseValues(line)).collect(Collectors.toList());
                sorter.mergeSort(sorted, comparator, threaded);
                sorted.stream().forEach(e -> System.out.println(Arrays.toString(e)));
            } else {
                String s = "Not support read method: " + readMethod;
                System.out.println(s);
                throw new IllegalArgumentException(s);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File " + filePath + " can not be found.");
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            System.out.println("Problem encountered reading file " + filePath);
            e.printStackTrace();
            throw e;
        }

    }

    private static <T> Comparator<T> createComparator(Comparator<? super T>[] delegates) {
        return (t0, t1) -> {
            for (Comparator<? super T> delegate : delegates) {
                try {
                    int n = delegate.compare(t0, t1);
                    if (n != 0) {
                        return n;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return 0;
        };
    }

    private static <T extends Comparable<? super T>> Comparator<T[]> createAscendingComparator(int index) {
        return createArrayAtIndexComparator(Comparator.naturalOrder(), index);
    }

    private static <T extends Comparable<? super T>> Comparator<T[]> createDesendingComparator(int index) {
        return createArrayAtIndexComparator(Comparator.reverseOrder(), index);
    }

    private static <T> Comparator<T[]> createArrayAtIndexComparator(Comparator<? super T> delegate, int index) {
        return (array0, array1) -> delegate.compare(array0[index], array1[index]);
    }

    public static String[] splitHeader(String line) {
        return COMMA_PATTERN.split(line);
    }

    public static String[] splitValue(String line) {
        String[] splits = COMMA_PATTERN.split(line);

        if (splits.length == 0)
            return splits;

        // Combine "lastname,firstname" into name
        String[] combined = new String[splits.length - 1];
        combined[0] = splits[0];
        IntStream.range(1, splits.length).forEach(i -> {
            combined[i - 1] = (i == 1) ? combined[i - 1] + ',' + splits[i] : splits[i];
        });

        return combined;
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparable<T>[] parseValues(String[] s) {
        return Arrays.stream(s).map(v -> parseValue(v)).toArray(size -> new Comparable[size]);
    }

    public static Object parseValue(String s) {
        if (s == null)
            return "";
        s = s.trim();
        if (s == "")
            return "";
        Object o;
        if ((o = parseInteger(s)) != null)
            return o;
        if ((o = parseDouble(s)) != null)
            return o;
        return parseString(s);
    }

    public static Integer parseInteger(String s) {
        if (INTEGER_PATTERN.matcher(s).matches())
            return Integer.valueOf(Integer.parseInt(s));
        return null;
    }

    public static Double parseDouble(String s) {
        if (DOUBLE_PATTERN.matcher(s).matches())
            return Double.valueOf(Double.parseDouble(s));
        return null;
    }

    public static String parseString(String s) {
        s = s.trim();
        if (s.charAt(0) == '\"' && s.charAt(s.length() - 1) == '\"')
            s = s.substring(1, s.length() - 1);
        return s;
    }

    public static void main(String[] args) {

        /*
         * Parse args:
         * serialr][sort order] ...
         * Column number: starts at 0
         * Sort order: a-acending (default); d-descending
         */
        String[] sortOpts = null;
        String filepath = null;
        int length = args.length;
        if (length > 1) {
            int sortOptsLength = length - 1;
            sortOpts = new String[sortOptsLength];
            sortOpts = Arrays.copyOfRange(args, 0, sortOptsLength);
            if (!Arrays.stream(sortOpts).allMatch(o -> validate(o))) {
                printUsage();
                return;
            }
            System.out.println("sortOpts: " + Arrays.toString(sortOpts));

            filepath = args[length - 1];
            System.out.println("filepath: " + filepath);
        } else if (length == 1) { // no sorting
            sortOpts = new String[0];
            filepath = args[0];
        } else {
            printUsage();
            return;
        }

        CSVSorter sorter = new CSVSorter();

        try {
            System.out.println("\n=================Serial read, single thread sort");
            long serialStart = System.currentTimeMillis();
            sorter.sort(filepath, sortOpts, SERIAL_READ, NOT_THREADED);
            long serialEnd = System.currentTimeMillis();

            System.out.println("\n=================Parallel read, multi-threaded sort");
            long parallelStart = System.currentTimeMillis();
            sorter.sort(filepath, sortOpts, PARALLEL_READ, THREADED);
            long parallelEnd = System.currentTimeMillis();

            System.out.println("\nPerformance measurements:");
            System.out.println("Serial read, single threaded sorting: execusion time used: "
                    + (serialEnd - serialStart) + " (ms)");
            System.out.println("Parallel read, multi-threaded sorting: execusion time used: "
                    + (parallelEnd - parallelStart) + " (ms)");

        } catch (IOException e) {
            System.out.println("Error reading");
            e.printStackTrace();
        } finally {
            MergeSort.ThreadedMergeSort.shutdownAndAwaitTermination();
        }
    }

    private static boolean validate(String sortOpt) {
        return SORT_OPTION_PATTERN.matcher(sortOpt).matches();
    }

    private static void printUsage() {
        System.out.println("Usage: CSVSortReader [[column number][order] ...] csv_file_path");
    }
}
