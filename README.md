# Traveling Salesman MPI

## Summary
This project contains a multi-threaded implementation of the traveling salesman problem using the branch and bound method with both static and dynamic execution. During each step, tours are recursively split according to edge inclusion and exclusion. The cost of each new tour is then calculated. When a tour is completed, the priority queue of threads is pruned of unfinished higher cost tours since child tours can only result in a higher cost.

## Prerequisites
* Java 9.0

## Usage
The console offers 3 options:

![alt text](../media/media/console.PNG?raw=true)

### Option 1 & Option 2
**Whether running the static or dynamic implementation of TSP, you will be prompted to input:**

![alt text](../media/media/1.PNG?raw=true)

  1. The name of a text file containing the exact format:
      * The first line contains the dimensions of the adjacency matrix (ex: '5 5' for a 5x5)
      * The following lines contain the adjacency matrix with diagonal values of -1
      * Example files are included: class.txt, test16.txt, test20.txt, test22.txt
  2. The number of threads to utilize
  3. Whether to display each step (t/f)

**The best tour configuration, cost, and total running time are returned:**

![alt text](../media/media/2.PNG?raw=true)

## Results
**Execution time in seconds when using 16 threads:**

|Size|Static|Dynamic|
|-|-|-|
|5x5|0.003869|0.002688|
|16x16|0.546536|0.125095|
|20x20|2.398549|0.561734|
|22x22|8.075040|4.305108|

The dynamic implementation has reduced execution time as long as overhead communication and execution costs aren't significant.
