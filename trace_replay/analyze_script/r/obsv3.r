source("common.r");

filenames <- paste0("../result/", prefices, "_obsv3.data");
for (i in 1:length(filenames)) { 
  filename <- filenames[i];
  if (!file.exists(filename)) {
    next
  }
  df <- read.table(filename, header = T, stringsAsFactors = F);

  for (lg in unique(df$log)) {
    subs <- subset(df, log == lg);
    df$pct[df$log == lg & df$type == "p1"] <- sum(subs$pct[subs$type %in% paste0("p", 1:3)]);
    df$pct[df$log == lg & df$type == "p2"] <- sum(subs$pct[subs$type %in% paste0("p", 4)]);
    df$pct[df$log == lg & df$type == "p3"] <- sum(subs$pct[subs$type %in% paste0("p", 5:6)]);
    df$pct[df$log == lg & df$type == "p4"] <- sum(subs$pct[subs$type %in% paste0("p", 7:8)]);
    df$pct[df$log == lg & df$type == "p5"] <- 1 - sum(subs$pct[subs$type %in% paste0("p", 1:8)]);
  }
  df <- subset(df, type %in% paste0("p", 1:5));

  subs <- subset(df, type == "p1");
  print(paste0("In 25% of the volumes of ", display_names[i], ", more than ", quantile(subs$pct, 0.75) * 100, 
        "% of the rarely updated blocks have their lifespans shorter than 0.5x write WSS"));

  str <- "In the remaining 4 groups, the medians are ";
  for (tp in paste0("p", 2:5)) {
    subs <- subset(df, type == tp);
    str <- paste0(str, median(subs$pct) * 100, ifelse(tp == "p5", "%", "%, "));
  }
  print(str);
}

args <- commandArgs(trailingOnly = T);
if (length(args) < 1 || args[1] != "plot") {
  q()
}

print("Drawing Figure 5. Will stop if necessary packages are not installed");
print("---------------");

library(scales);
source("common_graph.r");
options(scipen=999);

pdf_width <- 3;
pdf_height <- 1.4;
axis.text.size <- 10;
legend.text.size <- 10;

cdf_scale <- seq(0, 1, 0.25);
cdf_labels <- cdf_scale * 100;
cdf_limits <- c(0, 1.02);
cdf_colors <- c(d1_line_color, d2_line_color, d3_line_color, d4_line_color, d5_line_color);

for (i in 1:length(filenames)) {
  filename <- filenames[i];
  if (!file.exists(filename)) {
    next
  }
  print(paste0("Figure in ", display_names[i]));
  df <- read.table(filename, header = T, stringsAsFactors = F);
  for (lg in unique(df$log)) {
    subs <- subset(df, log == lg);
    df$pct[df$log == lg & df$type == "p1"] <- sum(subs$pct[subs$type %in% paste0("p", 1:3)]);
    df$pct[df$log == lg & df$type == "p2"] <- sum(subs$pct[subs$type %in% paste0("p", 4)]);
    df$pct[df$log == lg & df$type == "p3"] <- sum(subs$pct[subs$type %in% paste0("p", 5:6)]);
    df$pct[df$log == lg & df$type == "p4"] <- sum(subs$pct[subs$type %in% paste0("p", 7:8)]);
    df$pct[df$log == lg & df$type == "p5"] <- 1 - sum(subs$pct[subs$type %in% paste0("p", 1:8)]);
  }
  df <- subset(df, type %in% paste0("p", 1:5));
  labels <- c(paste0(c("< 0.5", "0.5-1", "1-1.5", "1.5-2", ">2"), "x"));

  xscale <- seq(0, 1, 0.25);
  xlabels <- xscale * 100;
  xlimits <- c(0, 1.02);

  xlab_name <- "Percentage (%)";
  ylab_name <- "Cumulative (%)";

  types <- unique(df$type);
  df$type <- factor(df$type, types);

  df_cdf <- toCdfFormat(df$pct, df$type);

  t <- ggplot(data = df_cdf, aes(x = x, y = y, color = type)) +
    geom_line(stat = "identity") +
    coord_cartesian(xlim = xlimits, ylim = cdf_limits) +           
    scale_x_continuous(breaks = xscale, labels = xlabels) +
    scale_y_continuous(breaks = cdf_scale, labels = cdf_labels) +
    scale_colour_manual(breaks = types, labels = labels, values = cdf_colors) + 
    ylab(ylab_name) + xlab(xlab_name) +
    simplifiedTheme(c(0.82, 0.37), axis.text.size = axis.text.size,
        legend.text.size = legend.text.size,
        legend.direction = "vertical");

  plot2pdf(paste0("../figure/", prefices[i], "_obsv3"), pdf_width, pdf_height, t);
}
