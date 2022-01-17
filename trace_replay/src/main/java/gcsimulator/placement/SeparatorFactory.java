package gcsimulator.placement;

import gcsimulator.Log;

public class SeparatorFactory {
  public static Separator getInstance(String name) {
    System.out.println("Sep: " + name);
    switch (name) {
      case "NoSep":
        return new NoSep();
      case "SepGC":
        return new SepGC();
      case "FADaC": // fadac.systor19
        return new FADaC();
      case "WARCIP": // warcip.systor19
        return new Warcip();
      case "MultiQueue": // autostream.systor19 (mq)
        return new MultiQueue();
      case "SFR": // autostream.systor19 (sfr)
        return new SFR();
      case "UW":
        return new UW();
      case "GW":
        return new GW();
      case "SepBIT":
        return new SepBIT();
      case "DAC":
        return new DAC();
      case "ETI":
        return new ETI();
      case "SFS":
        return new SFS();
      case "MultiLog":
        return new MultiLog();
      case "FK":
        return new FK();
      case "Method1":
        return new Method1();
      case "Method2":
        return new Method2();
      case "BITGW":
        return new BITGW();
      case "BITHalf":
        return new BITHalf();
      case "BITDouble":
        return new BITDouble();
    }

    return null;
  }
}
