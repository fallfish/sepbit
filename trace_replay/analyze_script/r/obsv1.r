source("common.r");

filenames <- paste0("../result/", prefices, "_obsv1.data");
for (i in 1:length(filenames)) {
  filename <- filenames[i];
  if (!file.exists(filename)) {
    next
  }
  df <- read.table(filename, header = T, stringsAsFactors = F);
  df <- df[!is.na(df$pct), ];

  subs <- subset(df, wss == 0.8);
  subs <- subs[order(subs$pct), ];
  print(paste0("50th percentile of 0-80% WSS in ", display_names[i], " is ", median(subs$pct)));
  subs <- subset(df, wss == 0.1);
  subs <- subs[order(subs$pct), ];
  print(paste0("50th percentile of 0-10% WSS in ", display_names[i], " is ", median(subs$pct)));
}

args <- commandArgs(trailingOnly = T);
if (length(args) < 1 || args[1] != "plot") {
  q()
}

print("Drawing Figure 3. Will stop if necessary packages are not installed");
print("---------------");

library(scales);
source("common_graph.r");
options(scipen=999);

pdf_width <- 3;
pdf_height <- 1.4;
axis.text.size <- 10;
legend.text.size <- 10;

breaks <- seq(0, 100, 25);
xscale <- seq(0, 100, 20);
xlabels <- xscale;
xlimits <- c(0, 105);

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
  df <- df[!is.na(df$pct), ];
  
  wsses <- unique(df$wss);
  wsses_labels <- paste0("<", wsses * 100, "%");
  wsses <- as.character(wsses[order(wsses)]);
  df$wss <- as.character(df$wss);
  df$wss <- factor(df$wss, levels = wsses);

  df_cdf <- toCdfFormat(df$pct, df$wss);

  t <- ggplot(df_cdf, aes(x = x, y = y, color = type)) +
    geom_line(stat = "identity") +
    scale_x_continuous(breaks = xscale, labels = xlabels) +
    scale_y_continuous(breaks = cdf_scale, labels = cdf_labels, expand = c(0.02, 0.02)) +
    scale_colour_manual(breaks = wsses, labels = wsses_labels, values = cdf_colors) + 
    coord_cartesian(xlim = xlimits, ylim = cdf_limits) +
    xlab("% of User-Writes") + ylab("Cumulative (%)") +
      simplifiedTheme(c(0.23, 0.8), legend.direction = "vertical", hjust = 0.5,
          legend.text.size = legend.text.size, axis.text.size = axis.text.size);

  plot2pdf(paste0("../figure/", prefices[i], "_obsv1"), pdf_width, pdf_height, t);
}
