library(rjags, quietly=TRUE)

check.fun <- function() {
   checkstats <- summary(x)$statistics
   if (is.matrix(benchstats)) {
     rnb <- order(rownames(benchstats))
     rnc <- order(rownames(checkstats))
     z <- (benchstats[rnb,1] - checkstats[rnc,1])
     z <- ifelse(z==0, 0, z/benchstats[rnb,2])
   } else {
     z <- (benchstats[1] - checkstats[1])
     z <- ifelse(z==0, 0, z/benchstats[2])
   }
   print(z)
   if (any(abs(z) > 0.15)) {
     stop("FAIL")
     quit(save="no", status=1)
   } else {
     cat("OK\n")
   }
}

check.data <- function(m, data, skip)
{
    mdata <- m$data()
    if (!missing(skip)) {
        mdata[skip] <- NULL
        data[skip] <- NULL
    }
    ok <- isTRUE(all.equal(mdata[sort(names(mdata))], data[sort(names(data))]))
    if (!ok) stop("Data mismatch")
}

