## TODOs
- unroll for loops
  - create artificial map storing 'loaded' R data
- parse R data


- check whether the for `for (i in 1:N)` `N` can be allocated dynamically or has to be loaded from R data file

This may be allowed:
```
for (i in 1:N) {
  a[i] ~ dnorm(0, 1)
}
N <- 2
```
This
```
for (i in 1:N) {
  a[i] ~ dnorm(0, 1)
}
N ~ dbern(0.5)
```
or this
```
N ~ dbern(0.5)
for (i in 1:N) {
  a[i] ~ dnorm(0, 1)
}
```
should not be allowed.

- change the existing code so that it would generate the appropriate Clojure data structure



## Big picture
1. Load R data
2. Unroll loops
3. Create dependency graph
4. Create topological ordering of the relations
5. Translate relations one by one in appropriate order


## Miscellanea
- unrolled forloop will be treated just like normal relations so there will be no need for separate forloop consideration in the moving around or translation part

[Differences between distributions in BUGS and R](https://stats.stackexchange.com/questions/5543/for-which-distributions-are-the-parameterizations-in-bugs-and-r-different)
