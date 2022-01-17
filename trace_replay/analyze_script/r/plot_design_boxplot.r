library(ggplot2)
source("common.r")

df <- read.table("../result/design_uw.data", header = T, stringsAsFactors = F);

df <- subset(df, v0 %in% c(0.25, 0.5, 1, 2, 4) & u0 %in% c(0.25, 1, 4));

legend_colors <- c(d1_line_color, d2_line_color, d3_line_color) #c("#ff8888", "#7611E4", "#160184");

pdf_width <- 3.5;
pdf_height <- 1.35;

df$u0 <- as.character(df$u0);
df$v0 <- as.character(df$v0);

types <- unique(df$u0);
df$u0 <- factor(df$u0, level = types);
xscale <- unique(df$v0);
df$v0 <- factor(df$v0, level = xscale);
print(types);

for (u00 in types) {
  subs <- subset(df, u00 == u0);
  for (v00 in xscale) {
    subsubs <- subset(subs, v00 == v0);
    print(unname(c(v00, u00, quantile(subsubs$prob, c(0.25, 0.5, 0.75)))));
  }
}

legend_breaks <- types;
legend_labels <- paste0(legend_breaks, "");
legend_labels[1] <- expression("u"[0]~"= 0.25");

legend.position <- c(0.5, 0.97);
axis.text.size <- 10;
legend.text.size <- 10;

t <- ggplot(df, aes(x = v0, y = prob, color = u0)) + 
  geom_boxplot(outlier.shape = outlier.shape, outlier.size = outlier.size) + 
  scale_y_continuous(breaks = seq(0,1,0.25), labels = seq(0, 100, 25)) +
  scale_colour_manual(breaks = legend_breaks, labels = legend_labels, values = legend_colors) +
  xlab("v"[0]~"(GiB)") + ylab("Probability (%)") +
  coord_cartesian(ylim = c(0, 1.08)) + 
  simplifiedTheme(legend.position, legend.direction = "horizontal", axis.text.size = axis.text.size, legend.text.size = legend.text.size)
plot2pdf("../figure/design_uw", pdf_width, pdf_height, t);

#################### Cold
filename <- "../result/design_gw.data";
df <- read.table(filename, header = T, stringsAsFactors = F);

legend_colors <- c(d1_line_color, d2_line_color, d3_line_color, d4_line_color); #c("#ff8888", "#7611E4", "#160184");

df$r <- as.character(df$l);
df$v <- as.character(df$t);

df <- subset(df, v %in% c("1", "4", "16", "64") & r %in% c("2", "4", "8"));
types <- unique(df$v);
xscale <- unique(df$r);
df$r <- factor(df$r, level = xscale);
df$v <- factor(df$v, level = types);
print(types);

legend_breaks <- types;
legend_labels <- paste0(legend_breaks, "");
legend_labels[1] <- expression("g"[0]~" = 1");

for (u00 in xscale) {
  subs <- subset(df, u00 == r);
  for (v00 in types) {
    subsubs <- subset(subs, v00 == v);
    print(unname(c(v00, u00, round(quantile(subsubs$value, c(0.25, 0.5, 0.75)), digits = 4))));
  }
}

t <- ggplot(df, aes(x = r, y = value, color = v)) + 
  geom_boxplot(outlier.shape = outlier.shape, outlier.size = outlier.size, width = 0.7) + 
  scale_y_continuous(breaks = seq(0, 1, 0.25), labels = seq(0, 100, 25)) +
  scale_colour_manual(breaks = legend_breaks, labels = legend_labels, values = legend_colors) +
  xlab(expression("r"[0]~" (GiB)")) + ylab("Probability (%)") +
  coord_cartesian(ylim = c(0, 1.10)) + 
  simplifiedTheme(legend.position, legend.direction = "horizontal", axis.text.size = axis.text.size, legend.text.size = legend.text.size)
plot2pdf("../figure/design_gw", pdf_width, pdf_height, t);
