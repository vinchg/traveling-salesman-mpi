/**
 * Main - Provides a console for customizing TSP inputs.
 * User has a choice between Static and Dynamic implementations,
 * as well as thread count and verbosity.
 *
 * Expected adjacency matrix format from text file is stated below.
 *
 * @author Vincent Cheong
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * @input Filename: text file containing matrix input
 * @input Threads: # of threads to allow
 * @input Verbose: t - true, otherwise - false
 */
public class Main {
    public static void main(String[] args) {
        // Use -1 to represent a non-existing edge
        // Adjacency matrix for the class example
        int[][] adjacency_matrix;

        Scanner s = new Scanner(System.in);

        while(true) {
            System.out.println("[Traveling Salesman Multithreaded Branch and Bound]");
            System.out.println("1. Static");
            System.out.println("2. Dynamic");
            System.out.println("3. Exit");
            int mode = s.nextInt();
            if (mode == 3) {
                s.close();
                return;
            }
            if (mode > 3 || mode < 1) {
                System.out.println("Invalid input");
                continue;
            }
            try {
                System.out.print("Filename = ? ");
                String line = s.next();
                adjacency_matrix = parse(line);  // Extract adjacency matrix from file
                System.out.print("Threads = ? ");
                int t = s.nextInt();
                System.out.print("Verbose (t/f) = ? ");
                line = s.next();
                boolean verbose;
                if ("t".equalsIgnoreCase(line))
                    verbose = true;
                else
                    verbose = false;
                double startTime = System.nanoTime();  // Start timer
                if (mode == 1) {
                    new TSP(false, adjacency_matrix, verbose, t);
                } else if (mode == 2)
                    new TSP(true, adjacency_matrix, verbose, t);
                double end_time = (System.nanoTime() - startTime) / 1000000000.0;
                System.out.println("Execution Time (s) = " + end_time + "\n");

                // Press any key to continue.
                System.out.println("Press any key to continue.");
                try {
                    System.in.read();
                } catch (IOException e) {
                }
            } catch (FileNotFoundException e) {
                System.out.println("Error: File not found\n");
            }
        }
    }


    /**
     * Converts a text file into matrix format.
     * The expected format is that the first 2 integers are the matrix dimensions and each
     * number following it would represent the next incremental entry into the matrix.
     * @param file_name
     * @return
     * @throws FileNotFoundException
     */
    private static int[][] parse(String file_name) throws FileNotFoundException {
        Scanner s;
        int[][] matrix;
        s = new Scanner(new File(file_name));

        int a = 0;
        int b = 0;
        // First 2 ints specify the dimensions
        if (s.hasNext())
            a = s.nextInt();
        if (s.hasNext())
            b = s.nextInt();
        if (a == 0 || b == 0)
            return new int[0][0];

        matrix = new int[a][b];
        for(int i = 0; i < a; i++) {
            for (int j = 0; j < b; j++) {
                if (s.hasNext())
                    matrix[i][j] = s.nextInt();
                else
                    break;
            }
        }
        s.close();
        return matrix;
    }
}
