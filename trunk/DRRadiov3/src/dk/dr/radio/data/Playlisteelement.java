package dk.dr.radio.data;

import java.util.Date;

/**
 * Repræsenterer en udsendelse
 * Created by j on 28-01-14.
 */
public class Playlisteelement {
  public String titel;
  public String kunstner;
  public String billedeUrl;

  public Date startTid;
  public String startTidKl;

  @Override
  public String toString() {
    return startTidKl + "/" + kunstner + "/" + titel;
  }
}
