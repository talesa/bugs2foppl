model in "gauss.bug"
data in "gauss-data.R"
load glm
compile, nchains(1)
inits in "gauss-init.R"
initialize
update 2500
monitor mu
update 100000
coda *
