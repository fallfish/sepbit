library(ggplot2);
library(grid);
library(testit);
options(scipen=999)

simplifiedTheme <- function(legend.position = c(0.2, 0.78), axis.text.size = rel(1), 
  legend.direction = "vertical", legend.text.size = 11, angle = 0,
  hjust = 0.5, vjust = 0.5, axis.title.x.vjust = 0.5,
  axis.title.y.vjust = 0.5, axis.title.y.hjust = 0.5, 
  axis.title.x.size = 0) {
  if (axis.title.x.size == 0) {
    axis.title.x.size = axis.text.size;
  }

  theme_classic() +  
    theme(panel.grid.major = element_blank(), 
        panel.grid.minor = element_blank(),
        panel.background = element_blank(), axis.line = element_line(colour = "black", size = 0.1),
        axis.text.x = element_text(angle = angle, hjust = hjust, vjust = vjust, colour = "black", size = axis.text.size),
        axis.title.y = element_text(size = axis.text.size, hjust = axis.title.y.hjust, vjust = axis.title.y.vjust),
        axis.text.y = element_text(colour = "black",size = axis.text.size),
        axis.title.x = element_text(size = axis.title.x.size, vjust = axis.title.x.vjust),
        legend.title = element_blank(),
        legend.position = legend.position,
        legend.key.size = unit(0.3, "cm"),
        legend.text = element_text(size = legend.text.size),
        legend.background = element_rect(size = 5, fill = alpha(NA, 0.5)),
        legend.direction = legend.direction,
        plot.margin = unit(c(0.15,0.15,0.15,0.15), "cm"))
}

simplePdf <- function(output_name, mywidth = 4, myheight = 3, font = "Times") {
  pdf(file = paste0(output_name, ".pdf"), family = font, width = mywidth, height = myheight)
}

simpleBar <- function(width = 0, alpha = 1) {
  if (width == 0) { 
    tmp <- geom_bar(position = "dodge", stat = "identity", colour = "black", alpha = alpha)
  } else {
    tmp <- geom_bar(position = "dodge", stat = "identity", colour = "black", width = width, alpha = alpha)
  }

  tmp
}

cropPdf <- function(pdf_prefix) {
  if (.Platform$OS.type == "unix") {
    ret <- system(paste0("echo \"cropping: ", pdf_prefix, ".pdf\""));
    assert("echo failed", ret == 0);
    ret <- system(paste0("pdfcrop ", pdf_prefix, ".pdf > /dev/null"));
    if (ret != 0) {
      ret <- system(paste0("pdfcrop.exe ", pdf_prefix, ".pdf > /dev/null"));
      assert("pdfcrop failed", ret == 0);
    }
    ret <- system(paste0("mv ", pdf_prefix, "-crop.pdf ", pdf_prefix, ".pdf"));
    assert("mv failed", ret == 0);
  } else {
    print(paste0("please crop manually: ", pdf_prefix, ".pdf"));
  }
}

gglegend <- function(x){
  tmp <- ggplot_gtable(ggplot_build(x))
  leg <- which(sapply(tmp$grobs, function(y) y$name) == "guide-box")
  tmp$grobs[[leg]]
}

plot2pdf <- function(pdf_prefix, pdf_width, pdf_height, graph) {
  for (i in 1) {
    simplePdf(pdf_prefix, pdf_width, pdf_height);
    print(graph);
    dev.off();
    cropPdf(pdf_prefix);
  }
}

plotLegend2pdf <- function(pdf_prefix, graph_with_legend) {
  leg <- gglegend(graph_with_legend);
  for (i in 1) {
    simplePdf(pdf_prefix, 10, 6);
    grid.draw(leg);
    dev.off()
    cropPdf(pdf_prefix);
  }
}

toCdfFormat <- function(values, types) {
  df_x <- c();
  df_y <- c();
  df_type <- c();
  for (tp in unique(types)) {
    subs <- values[types == tp];
    df_x <- c(df_x, subs[order(subs)]);
    df_y <- c(df_y, (1:length(subs)) / length(subs));
    df_type <- c(df_type, rep(tp, length(subs)));
  }

  types_new <- unique(types);
  df <- data.frame(x = df_x, y = df_y, type = df_type);
  df$type <- factor(df$type, types_new);

  df
}

d1_line_color <- "#ff8888";
d2_line_color <- "#66897F";
d3_line_color <- "#160184"
d4_line_color <- "#910C00";
d5_line_color <- "#7611E4";
