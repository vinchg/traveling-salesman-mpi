/**
 * Tour Object - On creation, the tour object utilizes an input map and constraints to
 * compute a tour's cost and whether or not the tour is a "complete" tour. The Tour object
 * also implements Comparable to allow it to function in a priority queue.
 *
 * @author Vincent Cheong
 */


public class Tour implements Comparable<Tour>, Runnable {
    private TSP tsp;
    double cost = 0;
    int[][] map;
    int[][] constraints;
    boolean complete = false;  // Is this a complete tour?
    boolean invalid = false;  // Is this an invalid tour?


    /**
     * Default Constructor
     */
    public Tour() {
        cost = Double.MAX_VALUE;
        complete = false;
        invalid = false;
    }


    /**
     * Creates hardcopies of input and stores them for cost calculation.
     * @param input_map: Adjacency matrix containing edge costs
     * @param input_constraints: Constraints matrix where:
     *                         1 - Included
     *                         0 - Undetermined
     *                        -1 - Excluded
     */
    public Tour(int[][] input_map, int[][] input_constraints, TSP input_tsp) {
        // Create deep copies so that modification and storing is easy
        map = TSP.deepCopy(input_map);
        constraints = TSP.deepCopy(input_constraints);
        tsp = input_tsp;
    }


    /**
     * Utilizes adjacency matrix and constraints to find cost of the tour.
     * Then, determines whether or not the tour is a completed tour (the constraints
     * contain no elements that equal 0).
     */
    public void run() {
        // Go through adjacency matrix and compute cost of tour
        for(int i = 0; i < map.length; i++) {
            int count = 2;  // Count of lowest edges for each row: by default we want the 2 lowest edges

            // Check constraints first
            for(int j = 0; j < map.length; j++) {
                if (constraints[i][j] == 1) {  // Check if there's a required edge
                    cost += map[i][j];  // Add edge to cost
                    map[i][j] = -1;  // Set edge to an invalid value so it can't be counted again
                    count--;  // Remove one from the count since this edge has been included in the cost
                }
                if (constraints[i][j] == -1)  // Check if there's an excluded edge
                    map[i][j] = -1;  // Set the edge to an invalid value
            }

            // Add lowest edges for each count
            for(int k = 0; k < count; k++) {  // For each edge we need to count left
                int lowest = Integer.MAX_VALUE;
                int[] position = new int[2];
                for(int j = 0; j < map.length; j++) {
                    if (map[i][j] > 0 && map[i][j] < lowest) {  // If we find a new lowest value, save it.
                        // Ignore negative or zero values
                        lowest = map[i][j];
                        position[0] = i;  // Save position of lowest
                        position[1] = j;
                    }
                    if (j == map.length - 1) {  // Reached end of loop
                        cost += lowest;  // Add lowest edge to cost
                        map[position[0]][position[1]] = -1;  // Set edge to an invalid value so it can't be counted again
                    }
                }
                if (lowest == Integer.MAX_VALUE) {  // EDGE CASE: Could not find another lowest edge
                    cost = -1;  // Set cost to invalid value
                    invalid = true;
                    return;
                }
            }
        }
        cost /= 2;  // Divide final cost by 2
        complete = isComplete();  // Determine if this tour is complete

        try {
            if (tsp.verbose)
                tsp.c_mutex.acquire();
            tsp.handle(this);
            if (tsp.verbose) {
                tsp.printMap(constraints);
                System.out.println("C: " + cost + "\n");
                tsp.c_mutex.release();
            }
        } catch (InterruptedException e) {
            System.out.println("Error: Interrupted");
        }
        if (tsp.isDynamic)
            tsp.es_mutex.release();
    }


    /**
     * Based on the constraints matrix, determines if the tour is a "complete" tour:
     * If ALL elements are non-zero, then the tour is complete/finished.
     * @return
     */
    public boolean isComplete() {
        for(int i = 0; i < constraints.length; i++)
            for(int j = 0; j < constraints.length; j++)
                if (constraints[i][j] == 0)
                    return false;
        return true;
    }


    /**
     * Required implementation for Comparable. Allows Tours to be by default sorted in
     * the priority queue with the lowest cost tour having the highest priority.
     * @param t
     * @return
     */
    public int compareTo(Tour t) {
        if (this.cost == t.cost)
            return 0;
        else
            return this.cost > t.cost ? 1 : -1;
    }
}
