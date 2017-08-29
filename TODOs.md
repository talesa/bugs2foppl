## TODOs
- unroll for loops
  - single indeces first
  - then expressions in single indeces
  - then multiple indeces and full-length vectors, e.g. `x[,i]`
  - create artificial map storing 'loaded' R data
  - create a simple example to evaluate algebraic expressions in it, e.g.
    ```
    for (i in 1:2) {
      for (j in 1:2) {
        a[2*(i-1)+(j-1)] ~ dnorm(0, 1)
      }
    }
    ```
    to
    ```
    a_0 ~ dnorm(0, 1)
    a_1 ~ dnorm(0, 1)
    a_2 ~ dnorm(0, 1)
    a_3 ~ dnorm(0, 1)
    ```
- parse R data



## Big picture
1. Create a map of variables (string -> variable object), one of the keys in the variable object would be whether the variables is observed (comes from data)

2. Load R data
  - put the array-like objects into appropriate clojure data structures
3. Create dependency graph
  - including for loops and what's inside them
  - what about the nested loops?
    - recursively create dependency graph for each of the for loops
4. Create topological ordering of the relations
  - First take into account the variables that has been loaded from the data
5. Unroll loops
  - Some expressions will need to be evaluated in order to unroll the loop, there will be such expressions in the indeces of the array-like objects
  - Evaluate the algebraic expressions to be able to be able to unroll the variables in the loop
  - this can be
6. Translate relations one by one in appropriate order
  -



### Pseudocode for loop unrolling

traverse the tree
enter the forloop
  initialize a list of relations you will append to, output-relations
  add the variable to be iterated over to the current list of vars-ranges: name of the variable and range of values
  for each :relation : [will need updating for nested forloops]
    do (for vars-ranges):
      for each :varIndexed : [need to traverse the node with some kind of walk, separate function]
        substitute :varIndexed to :varID with appropriate name [seperate function inputed into the walk]
      add relation to output-relations

I can extend this logic later to also serve higher dimensional arrays



scrap:
check which of the variables in (keys vars-ranges) are in this relation and store them in list iter-vars-in-this-rel
if (< 0 (count iter-vars-in-this-rel)):

## Miscellanea
- unrolled forloop will be treated just like normal relations so there will be no need for separate forloop consideration in the moving around or translation part

- take into account various effects mentioned on the page of model specification of OpenBUGS

[Differences between distributions in BUGS and R](https://stats.stackexchange.com/questions/5543/for-which-distributions-are-the-parameterizations-in-bugs-and-r-different)
[BUGS functions and distributions](http://www.mas.ncl.ac.uk/~nmf16/teaching/mas8303/functions.pdf)
[OpenBUGS model specificaiton](http://www.openbugs.net/Manuals/ModelSpecification.html)


## Things to discuss with Frank
- DLRC
  - do you think is it going to be beneficial for me?
  - when are you away?
- Tutoring on B papers?
-
