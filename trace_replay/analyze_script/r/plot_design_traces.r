library(ggplot2)
source("common_graph.r");
source("common.r");

filenames <- paste0("../result/", prefices, "_design_uw.data");
pdf_width <- 3.5;
pdf_height <- 1.23;
legend.position <- c(0.5, 0.97);
axis.text.size <- 8.5;
legend.text.size <- 8.5;

outlier.shape <- 4;
outlier.color <- "#ff8888";
outlier.size <- 0.8;

for (i in 1:length(filenames)) {
  filename <- filenames[i];
  if (!file.exists(filename)) {
    next
  }

  df <- read.table(filename, header = T, stringsAsFactors = F);
  df <- subset(df, v0 %in% c(0.025, 0.05, 0.1, 0.2, 0.4) & u0 %in% c(0.025, 0.1, 0.4));

  legend_colors <- c(d1_line_color, d2_line_color, d3_line_color) 

  df$u0 <- as.character(df$u0);
  df$v0 <- as.character(df$v0);

  types <- unique(df$u0);
  df$u0 <- factor(df$u0, level = types);
  xscale <- unique(df$v0);
  df$v0 <- factor(df$v0, level = xscale);

  legend_breaks <- types;
  legend_labels <- paste0(as.character(as.numeric(legend_breaks) * 100), "%");
  legend_labels[1] <- expression("u"[0]~"= 2.5% write WSS");

  t <- ggplot(df, aes(x = v0, y = prob, color = u0)) + 
    geom_boxplot(outlier.shape = outlier.shape, outlier.size = outlier.size) + 
    scale_y_continuous(breaks = seq(0,1,0.25), labels = seq(0, 100, 25)) +
    scale_colour_manual(breaks = legend_breaks, labels = legend_labels, values = legend_colors) +
    xlab("v"[0]~"(% of write WSS)") + ylab("Probability (%)") +
    coord_cartesian(ylim = c(0, 1.15)) + 
    simplifiedTheme(legend.position, legend.direction = "horizontal", axis.text.size = axis.text.size, legend.text.size = legend.text.size)
  plot2pdf(paste0("../figure/", prefices[i], "_design_uw"), pdf_width, pdf_height, t);
}

#################### Cold
filenames <- paste0("../result/", prefices, "_design_gw.data");
legend_colors <- c(d1_line_color, d2_line_color, d3_line_color, d4_line_color); 

for (i in 1:length(filenames)) {
  filename <- filenames[i];
  if (!file.exists(filename)) {
    next
  }

  df <- read.table(filename, header = T, stringsAsFactors = F);
  df <- subset(df, g0 %in% c(0.8, 1.6, 3.2, 6.4) & r0 %in% c(0.4, 0.8, 1.6));

  df$g0 <- as.character(df$g0);
  df$r0 <- as.character(df$r0);
  df$prob[is.nan(df$prob)] <- 0;

  types <- unique(df$g0);
  xscale <- unique(df$r0);
  df$g0 <- factor(df$g0, level = types);
  df$r0 <- factor(df$r0, level = xscale);

  legend_breaks <- types;
  legend_labels <- paste0(as.character(as.numeric(legend_breaks)), "x");
  legend_labels[1] <- expression("g"[0]~" = 0.8x write WSS");

  t <- ggplot(df, aes(x = r0, y = prob, color = g0)) + 
    geom_boxplot(outlier.shape = outlier.shape, outlier.size = outlier.size, width = 0.65) + 
    scale_y_continuous(breaks = seq(0, 1, 0.25), labels = seq(0, 100, 25)) +
    scale_colour_manual(breaks = legend_breaks, labels = legend_labels, values = legend_colors) +
    xlab(expression("r"[0]~" (times of write WSS)")) + ylab("Probability (%)") +
    coord_cartesian(ylim = c(0, 1.15)) + 
    simplifiedTheme(legend.position, legend.direction = "horizontal", axis.text.size = axis.text.size, legend.text.size = legend.text.size)
  plot2pdf(paste0("../figure/", prefices[i], "_design_gw"), pdf_width, pdf_height, t);
}
