library(ggplot2);
library(grid);

source("common.r")

######################### Analysis for short-lived

myplot <- function(filename, output_file, fixed_value, x_range, type_range,
  fix_col, x_col, type_col, xlab_name, yscale_interval, ylimits_up, legend_prefix, guide_nrow,
  linetypes, shapes, colors, sizes, axis.text.size, legend.text.size,
  legend.position) {

  x1 <- read.csv(filename, header = T, stringsAsFactors = F);
  x1[, x_col] <- as.character(x1[, x_col]);
  x_range <- as.character(x_range);
  x1[, type_col] <- as.character(x1[, type_col]);
  type_range <- as.character(type_range);
  x1[, fix_col] <- as.character(x1[, fix_col]);
  fixed_value <- as.character(fixed_value);

  x1 <- x1[x1[, x_col] %in% x_range & (x1[, type_col] %in% type_range) & (x1[, fix_col] == fixed_value), ];

  types <- type_range;
  xscale <- x_range;  #unique(x1[, x_col]);
  yscale <- seq(0, 1, yscale_interval);
  ylabels <- yscale * 100;
  ylimits <- c(0, ylimits_up);
  
  x1[, x_col] <- factor(x1[, x_col], x_range);
  x1[, type_col] <- factor(x1[, type_col], types);
  legend_labels <- c(legend_prefix, types[2:length(types)]); 

  part1 <- ggplot(x1, aes(x = x1[, x_col], y = x1[, "value"], 
    color = x1[, type_col], linetype = x1[, type_col], shape = x1[, type_col], size = x1[, type_col], group = x1[, type_col])) + 
    geom_point(size = 2.5, stroke = 0.8) + 
    geom_line(stat = "identity") + 
    scale_x_discrete(breaks = xscale, labels = xscale, expand = c(0.08, 0.08)) + 
    scale_y_continuous(breaks = yscale, labels = ylabels, expand = c(0, 0.02)) +
    scale_colour_manual(breaks = types, labels = legend_labels, values = colors) + 
    scale_linetype_manual(breaks = types, labels = legend_labels, values = linetypes) +
    scale_shape_manual(breaks = types, labels = legend_labels, values = shapes) +
    scale_size_manual(breaks = types, labels = legend_labels, values = sizes) +
    guides(color = guide_legend(nrow = guide_nrow, keywidth = 1.4, keyheight = 0.8), 
      linetype = guide_legend(nrow = guide_nrow, keywidth = 1.4, keyheight = 0.8), 
      shape = guide_legend(nrow = guide_nrow, keywidth = 1.4, keyheight = 0.8)) +
    coord_cartesian(ylim = ylimits) +
    xlab(xlab_name) + ylab("Probablity (%)");
  t <- part1 +
    simplifiedTheme("", legend.direction = "horizontal", hjust = 0.5,
        legend.text.size = legend.text.size, axis.text.size = axis.text.size, 
        axis.title.x.size = axis.title.x.size)

  plot2pdf(output_file, pdf_width, pdf_height, t);

  part1 + simplifiedTheme(legend.position, legend.direction = "horizontal", hjust = 0.5, 
        legend.text.size = legend.text.size, axis.text.size = axis.text.size)
}

linetypes <- c(42, 22, 23, 24, 25);
shapes <- 1:10; 
colors <- c(d1_line_color, d2_line_color, d3_line_color, d4_line_color, d5_line_color);
sizes <- rep(0.7, 5);
axis.text.size <- 15;
legend.text.size <- 15;

legend.position <- c(0.5, 0.35);
filename <- "../result/zipf_hot_wss10gb.csv";
output_file <- "../figures/design_fig7a";
s_range <- 1;
u0_range <- c(0.25, 1, 4);
v0_range <- c(0.25, 0.5, 1, 2, 4);
pdf_width <- 3;
pdf_height <- 1.8;
axis.title.x.size <- 15;

graph_with_legend <- myplot(filename, output_file, 1, v0_range, u0_range, "s", "v0", "u0",
  expression("v"[0]~"(GiB)"), 0.25, 1.05, expression("u"[0]~"= 0.25"), 1, linetypes,
  shapes,
  colors, sizes, axis.text.size, legend.text.size, legend.position);
plotLegend2pdf(paste0(output_file, "_legend"), graph_with_legend);


legend.position <- c(0.35, 0.85);
filename <- "../result/zipf_hot_wss10gb.csv";
output_file <- "../figures/design_fig7b";
v0_range <- c(0.25, 1, 4);
s_range <- seq(0, 1, 0.2);
axis.title.x.size <- 20;

graph_with_legend <- myplot(filename, output_file, 1, s_range, v0_range, "u0", "s", "v0",
  "\u03b1", 0.25, 1.05, expression("v"[0]~"= 0.25"), 1, linetypes, shapes,
  colors, sizes, axis.text.size, legend.text.size, legend.position);

plotLegend2pdf(paste0(output_file, "_legend"), graph_with_legend);

######################### Analysis for long-lived

legend.position <- c(0.5, 0.9);
filename <- "../result/zipf_cold_wss10gb.csv";
output_file <- "../figures/design_fig9a";
s_range <- 1;
r_range <- c(2, 4, 8);
v_range <- c(2, 4, 8, 16, 32);
axis.title.x.size <- 15;

graph_with_legend <- myplot(filename, output_file, 1, v_range, r_range, "s", "v", "r",  
  expression("g"[0]~" (GiB)"), 0.25, 1.05, expression("r"[0]~"= 2"), 1, linetypes, shapes,
  colors, sizes, axis.text.size, legend.text.size, legend.position);
plotLegend2pdf(paste0(output_file, "_legend"), graph_with_legend);

legend.position <- c(0.3, 0.4);
filename <- "../result/zipf_cold_wss10gb.csv";
output_file <- "../figures/design_fig9b";
s_range <- seq(0, 1, 0.2);
v_range <- c(2, 8, 32);
r_range <- c(2, 4, 8, 16, 32);
axis.title.x.size <- 20;

graph_with_legend <- myplot(filename, output_file, 8, s_range, v_range, "r", "s", "v", 
  "\u03b1", 0.25, 1.05, expression("g"[0]~"= 2"), 1, linetypes, shapes,
  colors, sizes, axis.text.size, legend.text.size, legend.position);
plotLegend2pdf(paste0(output_file, "_legend"), graph_with_legend);
