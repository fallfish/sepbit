options(scipen=999)

prob_calculation <- function(probs, n, t0, t1) {
  probs <- probs / sum(probs)
  denominator <- sum(probs * (1-(1-probs)^t0));

  left <- 1 - (1 - probs) ^ t1;
  right <- (1 - (1 - probs) ^ t0) * probs;

  numerator <- sum(left * right)
  numerator / denominator
}

get_prob_results <- function(n, v0s, u0s, probs, zipf_factor = 1) {
  v0_values <- c();
  u0_values <- c();
  prob_values <- c(); 
  for (v0_value in v0s) {  # v0
    for (u0_value in u0s) { # u0
      v0_values <- c(v0_values, v0_value);
      u0_values <- c(u0_values, u0_value);
      prob <- prob_calculation(probs, n, v0_value, u0_value);
      prob_values <- c(prob_values, prob);
    }
  }

  data.frame(
    s = zipf_factor, 
    v0 = v0_values / 1024 / 256, 
    u0 = u0_values / 1024 / 256, 
    prob = prob_values 
  )
}

n <- 2^20 * 2.5;
v0_values <- n / 2.5 * (2^((-4):(0)));
u0_values <- n / 2.5 * (2^c(-4, -2, 0));

filename <- "../result/zipf_hot_wss10gb.csv"

run_func <- function() {
  t <- NULL;
  for (alpha in seq(0, 1, 0.2)) {
    print(paste0("User-Written Blocks: Zipf ", alpha, ":"))
    t <- rbind(t, get_prob_results(n, v0_values, u0_values, 1/((1:n)^alpha), alpha));
  }
  write.table(t, file = filename, quote = F, row.names = F, col.names = T, sep = ',');
}

if (!file.exists(filename)) {
  run_func();
}

################################################
# Second Part: P(u <= t | u >= t0)

prob_calculation <- function(probs, n, t0, t1) { # t0 = a, t1 = l
  t1 <- t0 + t1;

  probs <- probs / sum(probs)
  denominator <- sum(probs * (1-probs)^t0);
  numerator <- sum(probs * (1-probs)^t0 - probs * (1-probs)^t1)
  numerator / denominator
}

get_prob_results <- function(n, v0_cuts, l_cuts, probs, zipf_factor) {
  value <- c(); 
  for (t0 in v0_cuts) { # v0
    for (t1 in l_cuts) { # l
      value <- c(value, prob_calculation(probs, n, t0, t1));
      print(c(t0, t1, value[length(value)]))
    }
  }
  df <- expand.grid(r = l_cuts / 1024 / 256, v = v0_cuts / 1024 / 256);
  s <- rep(zipf_factor, length(l_cuts) * length(v0_cuts))
  df <- cbind(s, df, value);
  print(df)
}

g0_values <- n / 2.5 * (2^c(-1, 0, 1, 2, 3)); # 2, 4, 8, 16, 32 GiB
r0_values <- n / 2.5 * (2^c(-1, 0, 1)); # 2, 4, 8 GiB
filename <- "../result/zipf_cold_wss10gb.csv"

run_func <- function() {
  t <- NULL;
  for (zf in seq(0, 1, 0.2)) {
    print(paste0("GC-rewritten Blocks: Zipf ", zf, ":"))
    t <- rbind(t, get_prob_results(n, g0_values, r0_values, 1/((1:n)^zf), zf));
  }
  write.table(t, file = filename, quote = F, row.names = F, col.names = T, sep = ',');
}

if (!file.exists(filename)) {
  run_func();
}
