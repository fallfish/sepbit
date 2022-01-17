source("common.r");

filenames <- paste0("../result/", prefices, "_obsv2.data");
for (i in 1:length(filenames)) {
  filename <- filenames[i];
  if (!file.exists(filename)) {
    next
  }

  df <- read.table(filename, header = T, stringsAsFactors = F);

  types <- unique(df$type);
  type_labels <- c("<1%", "1-5%", "5-10%", "10-20%");
  i <- 1;

  print(paste0("In ", display_names[i], ":"));
  for (tp in unique(df$type)) {
    subs <- subset(df, type == tp);
    print(paste0("75th percentile of ", type_labels[i], ": ", quantile(subs$value, c(0.75))));
    i <- i + 1;
  }
}

args <- commandArgs(trailingOnly = T);
if (length(args) < 1 || args[1] != "plot") {
  q()
}

print("Drawing Figure 4. Will stop if necessary packages are not installed");
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
cdf_colors <- c(d1_line_color, d2_line_color, d3_line_color, d4_line_color);

for (i in 1:length(filenames)) {
  filename <- filenames[i];
  if (!file.exists(filename)) {
    next
  }
  print(paste0("Figure in ", display_names[i]));
  df <- read.table(filename, header = T, stringsAsFactors = F);

  types <- unique(df$type);
  xbreaks <- types;
  df$type <- factor(df$type, levels = types);
  type_labels <- c("< 1%", "1-5%", "5-10%", "10-20%");
  breaks <- seq(0, 8, 1);

  df_cdf <- toCdfFormat(df$value, df$type);
  xscale <- breaks;
  xlabels <- xscale;
  xlimits <- c(min(xscale), max(xscale));

  t <- ggplot(df_cdf, aes(x = x, y = y, color = type)) + 
    geom_line(stat = "identity") + 
    scale_x_continuous(breaks = xscale, labels = xlabels) + 
    scale_y_continuous(breaks = cdf_scale, labels = cdf_labels, expand = c(0.02, 0.02)) +
    scale_colour_manual(breaks = types, labels = type_labels, values = cdf_colors) +
    coord_cartesian(xlim = xlimits, ylim = cdf_limits) +
    xlab("CVs") + ylab("Cumulative (%)") +
    simplifiedTheme(c(0.75, 0.35), legend.direction = "vertical", hjust = 0.5,
                  legend.text.size = legend.text.size, axis.text.size = axis.text.size);
  plot2pdf(paste0("../figure/", prefices[i], "_obsv2"), pdf_width, pdf_height, t);
}
