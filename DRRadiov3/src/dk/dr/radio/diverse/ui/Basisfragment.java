/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dr.radio.diverse.ui;

import android.support.v4.app.Fragment;

import java.net.URL;

import dk.dr.radio.diverse.Log;

/**
 * @author j
 */
//public class BasisFragment extends DialogFragment {
public class Basisfragment extends Fragment {


  public static final int LINKFARVE = 0xff00458f;

  /*
//a.id(R.id.billede).image("http://asset.dr.dk/imagescaler/?file=/mu/programcard/imageuri/radioavis-24907&w=300&h=169&scaleAfter=crop");

Forhold
16/9/3

bredde=16*x
højde=9*x
firkant=3*x
 */ int x = 20;
  public int bredde = 16 * x;
  public int højde = 9 * x;
  public int firkant = 3 * x;


  public static String skalérBilledeUrl(String u, int bredde, int højde) {
    if (u == null || u.length() == 0) return null;
    try {
      URL url = new URL(u);
      //String skaleretUrl = "http://asset.dr.dk/imagescaler/?host="+url.getAuthority()+"&path="+url.getPath()+"&h=" + firkant + "&w=" + firkant + "&scaleafter=crop";
      //"http://dr.dk/drdkimagescale/imagescale.drxml?server=api.discogs.com&file=/image/A-3062379-1371611467-2166.jpeg&w=100&h=100&&scaleAfter=crop"
      String skaleretUrl = "http://dr.dk/drdkimagescale/imagescale.drxml?server=" + url.getAuthority() + "&file=" + url.getPath() + "&w=" + bredde + "&h=" + højde + "&scaleafter=crop";

      Log.d("ZZZ url1 = " + u);
      Log.d("ZZZ url2 = " + url);
      Log.d("ZZZ url3 = " + skaleretUrl);
      // "http://asset.dr.dk/imagescaler/?host=api.discogs.com&path=/image/A-455304-1340627060-2526.jpeg&h=" + firkant + "&w=" + firkant + "&scaleafter=crop"
      return skaleretUrl;
    } catch (Exception e) {
      Log.e("url=" + u, e);
      return null;
    }
  }

}
