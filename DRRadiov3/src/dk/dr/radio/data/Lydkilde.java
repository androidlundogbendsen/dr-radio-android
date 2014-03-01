package dk.dr.radio.data;

import java.util.ArrayList;

/**
 * Created by j on 01-03-14.
 */
public abstract class Lydkilde {
  public String urn;   // Bemærk - kan være tom!
  public String slug;  // Bemærk - kan være tom!
  public ArrayList<Lydstream> streams;
}
