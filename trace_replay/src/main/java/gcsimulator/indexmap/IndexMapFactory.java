package gcsimulator.indexmap;

public class IndexMapFactory {
  public static IndexMap getInstance(String type, String cache) {
    IndexMap map = null;
    switch (type) {
      case "Persistent":
        map = new PersistentIndexMap();
        break;
      case "PageTable":
        map = new PageTableIndexMap();
        break;
      case "PureInMem":
        map = new PureInMemIndexMap();
        break;
      default:
        System.out.println("Wrong type for IndexMap");
        break;
    }

    switch (cache) {
      case "Null":
        return map;
      case "GlobalPageCache":
        return new IndexMapWithGlobalPageCache(map);
      default:
        return map;
    }
  }
}
