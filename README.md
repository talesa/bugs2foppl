# BUGS to FOPPL transpiler

[BUGS](https://www.mrc-bsu.cam.ac.uk/software/bugs) (Bayesian inference Using Gibbs Sampling) is a software for the Bayesian analysis of statistical models using Markov chain Monte Carlo (MCMC) methods. It allows to describe graphical models using a declarative langauge.

FOPPL (First Order Probabilistic Programming Language) is a stripped down version of [Anglican](http://www.robots.ox.ac.uk/~fwood/anglican), a probabilistic programming language based on Clojure.

The ANLTRv4 specification of BUGS grammar was obtained heavily inspired with the FLEX+BISON lexing and parsing grammar incorporated in [JAGS](http://mcmc-jags.sourceforge.net/) 4.3.0 [source code](https://sourceforge.net/projects/mcmc-jags/files/JAGS/4.x/Source/), in files `src/lib/compiler/parser.yy` and `src/lib/compiler/scanner.ll`.

I use [my 'custom' version](https://github.com/talesa/clj-antlr) of [`clj-antlr`](https://github.com/aphyr/clj-antlr/).
