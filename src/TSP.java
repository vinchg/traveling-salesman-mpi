/**
 * TSP - Given an adjacency matrix for a graph, we utilize the branch and bound technique
 * to find the lowest cost tour. The algorithm allows user to choose between static and dynamic
 * approaches as well as the amount of threads to utilize.
 *
 * @author Vincent Cheong
 */


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;


public class TSP implements Runnable {
    int[][] map;
    boolean verbose;

    private PriorityBlockingQueue<Tour> pq = new PriorityBlockingQueue<>();
    Semaphore c_mutex;  // Console mutex
    private Tour lowest_tour;
    private ExecutorService es;  // Manages threads
    Semaphore es_mutex;  // ES mutex (Used to keep track of amount of active threads)
    private int thread_amount;
    private int[][] constraints1;
    private int[][] constraints2;
    boolean isDynamic;


    /**
     * Constructor just to automatically initialize constraints for ease of use.
     * @param input_dynamic: True - Dynamic, False - Static
     * @param input_map: Adjacency matrix
     * @param input_verbose: True - Display steps, False - Don't
     * @param input_thread_amount: Amount of threads to utilize. Must be greater than 0.
     */
    public TSP(boolean input_dynamic, int[][] input_map, boolean input_verbose, int input_thread_amount) {
        this(input_dynamic, input_map, input_verbose, input_thread_amount, initializeConstraints(input_map.length));
    }


    /**
     * The core TSP constructor that allows either static or dynamic execution (executed only once per problem set).
     * @param input_constraints: Allow customized input constraints
     */
    public TSP(boolean input_dynamic, int[][] input_map, boolean input_verbose, int input_thread_amount, int[][] input_constraints) {
        // Initialize Executor service and lowest cost tour here
        this(input_dynamic, input_map, input_verbose, input_thread_amount, input_constraints, new Tour(), new Semaphore(1));

        if (!input_dynamic) {
            while (!es.isTerminated()) {  // Wait for all tasks to terminate
            }
        }

        // Print results
        if (lowest_tour.cost != Double.MAX_VALUE) {
            System.out.println("Best tour map:");
            printMap(lowest_tour.constraints);  // Print the lowest cost tour
            System.out.println("Best tour cost: " + lowest_tour.cost);
        }
        else
            System.out.println("Error: No tour found.");
    }


    /**
     * Bulk of the initialization is done in this constructor.
     * This constructor is also designed to support recursion.
     * Passing the Executor service and the lowest cost tour is necessary to avoid unnecessary computations.
     * We also pass in the c_mutex (console mutex) so that processes aren't stepping on each other's toes
     * when printing to console.
     * @param input_lowest_tour: Pass lowest tour to improve pruning in child instances
     * @param input_mutex: Pass c_mutex to child instances
     */
    public TSP(boolean input_dynamic, int[][] input_map, boolean input_verbose, int input_thread_amount, int[][] input_constraints, Tour input_lowest_tour, Semaphore input_mutex) {
        if (input_thread_amount < 1) {
            System.out.println("Invalid thread amount");
            return;
        }

        isDynamic = input_dynamic;
        map = input_map;
        verbose = input_verbose;
        thread_amount = input_thread_amount;
        constraints1 = deepCopy(input_constraints);
        lowest_tour = input_lowest_tour;
        c_mutex = input_mutex;
        if (isDynamic)
            es = Executors.newFixedThreadPool(input_thread_amount);
        else
            es = Executors.newFixedThreadPool(2);  // Prepare 2 threads

        if (isDynamic) {
            es_mutex = new Semaphore(thread_amount);
            run_dynamic();
        } else
            run_static();
    }


    /**
     * Default values - Dynamic execution. Verbose set to false. Utilizes 4 threads.
     * @param input_map: Adjacency matrix input.
     */
    public TSP(int[][] input_map) {
        this(true, input_map, false, 4, initializeConstraints(input_map.length));
    }


    /**
     * Default Constructor - Empty
     */
    public TSP() { }


    /**
     * TSP Algorithm Core executed statically (and recursively).
     */
    public void run_static() {
        if (thread_amount == 1)
            run();
        else {
            Tour temp = new Tour(map, constraints1, this);
            temp.run();
            TSP tsp1, tsp2;
            if (!pq.isEmpty()) {
                temp = pq.poll();

                if (!temp.complete) {
                    split(temp);

                    // thread_amount/2 will round down because of integer division
                    tsp1 = new TSP(false, map, verbose, thread_amount/2, constraints1, lowest_tour, c_mutex);
                    es.execute(tsp1);  // Execute the TSP algorithm recursively on another thread
                    tsp2 = new TSP(false, map, verbose, thread_amount/2, constraints2, lowest_tour, c_mutex);
                    es.execute(tsp2);

                    // Look for the lowest cost completed tour among the 2 TSP instances
                    if (tsp1.lowest_tour.cost < tsp2.lowest_tour.cost)
                        temp = tsp1.lowest_tour;
                    else
                        temp = tsp2.lowest_tour;

                    // If that cost is lower than the current, then it is our new lowest
                    if (temp.cost < lowest_tour.cost)
                        lowest_tour = temp;

                    es.shutdown();
                }
                // If the tour is complete, do nothing because it's a lowest complete tour
            }
        }
    }


    /**
     * TSP Algorithm Core for thread execution (Static)
     * Best tour will be stored in lowest_tour
     */
    public void run() {
        Tour temp = new Tour(map, constraints1, this);
        temp.run();  // Run normally for first tour

        while(!pq.isEmpty()) {
            temp = pq.poll();

            if (!temp.complete) {
                split(temp);  // Split the tour into an inclusion/exclusion of an edge
                              // and store in constraints1 and constraints2

                temp = new Tour(map, constraints1, this);  // Tour with edge inclusion
                temp.run();  // Calculates cost of tour and "handles" it
                             // This includes pruning/adding back to pq

                temp = new Tour(map, constraints2, this);  // Tour with edge exclusion
                temp.run();
            }
        }
        // Result is automatically stored in lowest tour
    }


    /**
     * TSP Algorithm Core executed Dynamically
     */
    public void run_dynamic() {
        Tour temp = new Tour(map, constraints1, this);
        temp.run();  // Run normally for first tour

        while(true) {
            try {
                if (es_mutex.availablePermits() == thread_amount && pq.isEmpty()) {  // End loop if no threads running and pq is empty
                    break;
                } else if (!pq.isEmpty()) {
                    temp = pq.poll();  // Pop lowest cost tour from priority queue

                    if (!temp.complete) {
                        split(temp);  // Split the tour into an inclusion/exclusion of an edge
                                      // and store in constraints1 and constraints2

                        temp = new Tour(map, constraints1, this);  // Tour with edge inclusion
                        es_mutex.acquire();  // Keep track of amount of active threads
                        es.execute(temp);

                        temp = new Tour(map, constraints2, this);  // Tour with edge exclusion
                        es_mutex.acquire();
                        es.execute(temp);
                    }
                }

            } catch(InterruptedException e) {
                System.out.println("Error: Unable to access PQ");
                return;
            }
        }

        es.shutdown();  // Shut down threads
    }


    /**
     * Handles a tour by adding it to either the priority queue or the
     * completed priority queue. Prunes the tour when necessary.
     * @param t
     */
    protected void handle(Tour t) {
        if (t.invalid) {  // If the tour is invalid, say so and do not add back to queue
            if (verbose)
                System.out.println("**INVALID TOUR**");
        } else if (t.complete) {  // Is it a completed tour?
            if (verbose)
                System.out.println("**COMPLETED TOUR**");
            if (t.cost  < lowest_tour.cost) {
                lowest_tour = t;
                prune();
            }
        }
        else {
            // If the new tour is not a complete tour, check to see if it should be added to
            // the priority queue:  If a completed tour exists that has a lower cost, then
            // implicitly prune the new tour (simply do not add it to the pq).
            if (lowest_tour.cost != Double.MAX_VALUE) {
                if (t.cost < lowest_tour.cost)
                    pq.add(t);
                else {
                    // Implicitly pruned by not adding it
                    if (verbose)
                        System.out.println("Pruned..." + t.cost);
                }
            } else
                pq.add(t);  // There are no completed tours, so just add it (no pruning).
        }
    }


    /**
     * Split a tour by pivoting around an edge to produce an inclusion constraint
     * and an exclusion constraint. The results are stored in constraints1 and
     * constraints2, which the main function can then handle.
     * @param t
     */
    protected void split(Tour t) {
        constraints1 = deepCopy(t.constraints);
        constraints2 = deepCopy(t.constraints);
        // Look for edge to split on
        for(int i = 0; i < constraints1.length; i++)
            for(int j = 0; j < constraints1.length; j++)
                if (constraints1[i][j] == 0) {  // Found an edge to use
                    constraints1[i][j] = 1;  // Must include this edge
                    constraints1[j][i] = 1;

                    constraints2[i][j] = -1;  // Must exclude this edge
                    constraints2[j][i] = -1;

                    // Fill in constraints
                    fillConstraints(constraints1);
                    fillConstraints(constraints2);

                    return;
                }
    }


    /**
     * This is a recursive method that takes a constraint matrix and fills it based on the following logic:
     * If a row contains 2 inclusions, the rest of the row must be exclusions.
     * If a row contains (dimension - 2) exclusions, the rest must be inclusions.
     * Anytime a change is made to constraints, execute the method recursively.
     * @param constraints
     */
    private void fillConstraints(int[][] constraints) {
        for(int i = 0; i < constraints.length; i++) {
            int include_count = 0;
            int exclude_count = 0;
            int fill_count = 0;
            for(int j = 0; j < constraints.length; j++) {  // Sift through row
                if (constraints[i][j] == 1) {
                    include_count++;
                    fill_count++;
                }
                if (constraints[i][j] == -1) {
                    exclude_count++;
                    fill_count++;
                }
            }

            if (!(fill_count == constraints.length)) {
                if (include_count == 2) {
                    // If there are 2 necessary inclusions, the rest of the row elements must be exclusions
                    for(int j = 0; j < constraints.length; j++) {
                        if (constraints[i][j] != 1) {
                            constraints[i][j] = -1;
                            constraints[j][i] = -1;
                        }
                    }
                    fillConstraints(constraints);  // When a change is made, recheck
                } else if (exclude_count == (constraints.length - 2)) {
                    // If there are (dimension - 2) exclusions, then the remaining 2 must be inclusions
                    for(int j = 0; j < constraints.length; j++) {
                        if (constraints[i][j] != -1) {
                            constraints[i][j] = 1;
                            constraints[j][i] = 1;
                        }
                    }
                    fillConstraints(constraints);  // When a change is made, recheck
                }
            }
        }
    }


    /**
     * Removes the elements of the priority queue which have a lower cost than the
     * lowest cost completed tour.
     */
    private void prune() {
        Tour[] pq_array = pq.toArray(new Tour[pq.size()]);
        for(Tour t : pq_array) {
            try {
                if (t.cost >= lowest_tour.cost) {
                    pq.remove(t);  // Prune the tour
                    if (verbose)
                        System.out.println("Pruned..." + t.cost);
                }
            } catch(NullPointerException e) {
            }
        }
    }


    /**
     * Helper function to easily print a 2-D matrix.
     * @param input_map
     */
    public static void printMap(int[][] input_map) {
        for(int i = 0; i < input_map.length; i++) {
            for (int j = 0; j < input_map.length; j++)
                System.out.print(input_map[i][j] + " ");
            System.out.println();
        }
    }


    /**
     * Helper function to create deep copies of square 2-D matrices.
     * @param input: Must be a square 2-D matrix
     * @return: Returns a copy of the input
     */
    public static int[][] deepCopy(int[][] input) {
        int[][] copy = new int[input.length][input.length];
        for(int i = 0; i < input.length; i++)
            for(int j = 0; j < input.length; j++)
                copy[i][j] = input[i][j];
        return copy;
    }


    /**
     * Helper function to generate a size x size constraint matrix where the diagonals are -1.
     * @param size
     * @return
     */
    public static int[][] initializeConstraints(int size) {
        // Initialize a new tour with no constraints
        int[][] constraints = new int[size][size];
        for(int i = 0; i < size; i++) {
            constraints[i][i] = -1;  // Set constraint matrix to -1 on diagonals
        }
        return constraints;
    }
}