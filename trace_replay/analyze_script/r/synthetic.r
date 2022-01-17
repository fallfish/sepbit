sequence_gen_zipf <- function(probs, n, m, permutation_interval, filename) {
  if (!file.exists(filename)) {
    i <- 0;
    ans <- c();

    hot_length <- n %/% 5;
    probs_indices_hot <- 1:hot_length;
    probs_indices_cold <- (hot_length+1):n;

    while (i < m) {
      m_tmp <- min(permutation_interval, m - i); 
      seqs <- sample(c(probs_indices_hot, probs_indices_cold), size = m_tmp, prob = probs, replace = T);
      ans <- c(ans, seqs);
      print(paste0("Processed ", i, " to ", i + m_tmp, " (", i / m * 100, " %)"));
      i <- i + permutation_interval;
      
      if (alpha > 0) {
        probs_indices_hot <- sample(1:hot_length, size = hot_length, replace = F);
      }

      if (length(ans) >= 500 * 1024 * 256) { # Flush to disk for every 500 GiB writes
        write.table(ans, file = filename, quote = F, row.names = F, col.names = F, append = T);
        ans <- c();
      }
    }

    if (length(ans) > 0) { # Flush to disk for the rest
      write.table(ans, file = filename, quote = F, row.names = F, col.names = F, append = T);
      ans <- c();
    }

  }
  print(paste0("finished ", filename));
}

n <- 50 * 1024 * 256; # 50 GiB write WSS
interval <- 0.5; # Change the permutation for every 512 MiB writes
args <- commandArgs(trailingOnly = T)
if (length(args) < 2) {
  print("Usage: Rscript synthetic.r <alpha> <output_path>");
  q()
}

print(args);
alpha <- as.numeric(args[1]);

print("Zipf - hotness change - 20 %");
print(paste0("alpha = ", alpha));
sequence_gen_zipf(1/(1:n)^alpha, n, n * 60, interval * 1024 * 256, paste0(args[2], "/alpha_", alpha, ".csv"));
