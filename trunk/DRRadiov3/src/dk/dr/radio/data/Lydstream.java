package dk.dr.radio.data;

/**
 * Created by j on 08-02-14.
 */
public class Lydstream {
  public String url;
  public DRJson.StreamType type;
  //public DRJson.StreamKind kind;
  public DRJson.StreamQuality kvalitet;
  public boolean foretrukken;

  @Override
  public String toString() {
    return type + "/" + kvalitet + "/" + url;
  }
}
