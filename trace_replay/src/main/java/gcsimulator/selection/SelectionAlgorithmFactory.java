package gcsimulator.selection;

import gcsimulator.BlockContainer;

public class SelectionAlgorithmFactory {
  public static <T extends BlockContainer> SelectionAlgorithm<T> getSelectionAlgorithm(String algorithm) {
    switch (algorithm) {
      case "Basic":
        return new Basic<>();
      case "Lru":
        return new Lru<>();
      case "Random":
        return new Random<>();
      case "Greedy":
        return new Greedy<>();
      case "RandomGreedy":
        return new RandomGreedy<>();
      case "WindowGreedy":
        return new WindowGreedy<>();
      case "CostBenefit":
        return new CostBenefit<>();
      case "MultiLog":
        return new MultiLog<>();
      case "CostHotness":
        return new CostHotness<>();
    }
    return null;
  }
}
