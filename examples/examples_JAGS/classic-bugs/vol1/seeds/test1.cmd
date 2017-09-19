model in "seeds.bug" 
data in "seeds-data.R"
load glm
compile, nchains(2)
inits in "seeds-init.R"
initialize
samplers to foo-samplers.txt
update 1000 
monitor alpha0 , thin(10)
monitor alpha1 , thin(10)
monitor alpha2 , thin(10)
monitor alpha12 , thin(10)
monitor sigma , thin(10)
update 10000 
coda *
