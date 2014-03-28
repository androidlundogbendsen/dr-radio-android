package dk.dr.radio.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by j on 01-03-14.
 */
public class Programserie { //implements Serializable {
//  private static final long serialVersionUID = 1L;
  public String titel;
  public String beskrivelse;
  public String slug;
  public int antalUdsendelser;
  public String urn;
  public ArrayList<Udsendelse> udsendelser;
  public Date senesteUdsendelseTid;
}
