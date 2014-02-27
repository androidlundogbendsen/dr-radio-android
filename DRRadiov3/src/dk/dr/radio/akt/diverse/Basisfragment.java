/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dr.radio.akt.diverse;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.net.URL;
import java.net.URLEncoder;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

/**
 * @author j
 */
//public class BasisFragment extends DialogFragment {
public class Basisfragment extends Fragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d("onCreate " + this);
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d("onStart " + this);
  }

  @Override
  public void onResume() {
    Log.d("onResume " + this);
    super.onResume();
  }

  @Override
  public void onPause() {
    Log.d("onPause " + this);
    super.onPause();
  }

  @Override
  public void onDestroy() {
    Log.d("onDestroy " + this);
    super.onDestroy();
  }

  @Override
  public void onAttach(Activity activity) {
    Log.d("onAttach " + this + " til " + activity);
    super.onAttach(activity);
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d("onStop " + this);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Log.d("onDestroyView " + this);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    Log.d("onActivityCreated " + this);
    super.onActivityCreated(savedInstanceState);
  }

  public static final int LINKFARVE = 0xff00458f;

  /*
SKALERINGSFORHOLD
Skalering af billeder - 16/9/3
Forhold 16:9 for de store billeder
Forhold 1:1 for playlistebilleder - og de skal være 1/3-del i højden af de store billeder
 */
  public static final int bredde16 = 16;
  public static final int højde9 = 9;


  /**
   * Finder bredden som et et velskaleret billede forventes at have
   *
   * @param rod         listen eller rod-viewet hvor billedet skal vises
   * @param paddingView containeren der har polstring/padding
   */
  protected int bestemBilledebredde(View rod, View paddingView) {
    int br = rod.getWidth();
    if (rod.getHeight() < br / 2) br = br / 2; // Halvbreddebilleder ved liggende visning
    br = br - paddingView.getPaddingRight() - paddingView.getPaddingLeft();
    //Log.d("QQQQQ listView.getWidth()=" + rod.getWidth() + " getHeight()=" + rod.getHeight());
    //Log.d("QQQQQ billedeContainer.getPaddingRight()=" + paddingView.getPaddingRight() + "   .... så br=" + br);
    return br;
  }

/* Doku fra Nicolai
Alle billeder der ligger på dr.dk skal igennem "asset.dr.dk/imagescaler<http://asset.dr.dk/imagescaler>".

Artist billeder fra playlister i APIet skal igennem asset.dr.dk/discoImages,
da den bruger en whitelisted IP hos LastFM og discogs (de har normalt max requests).

Det er begge simple services, og du har ikke brug for andre parametre end dem der kan ses
her (w = width, h = height -- scaleAfter skal du ikke pille ved):

http://asset.dr.dk/imagescaler/?file=%2Fmu%2Fbar%2F52f8d952a11f9d0c90f8b3e3&w=300&h=169&scaleAfter=crop&server=www.dr.dk
( bliver til
http://asset.dr.dk/imagescaler/?file=/mu/programcard/imageuri/radioavis-24907&w=300&h=169&scaleAfter=crop  )

http://asset.dr.dk/discoImages/?discoserver=api.discogs.com&file=%2fimage%2fA-885103-1222266056.jpeg&h=400&w=400&scaleafter=crop&quality=85

Jeg bruger selv følgende macro'er i C til generering af URIs:

#define DRIMAGE(path, width, height) \
            FORMAT( \
                @"http://asset.dr.dk/drdkimagescale/?server=www.dr.dk&amp;w=%0.f&amp;h=%0.f&amp;file=%@&amp;scaleAfter=crop", \
                width * ScreenScale, \
                height * ScreenScale, \
                URLENCODE(path) \
            )

#define DISCOIMAGE(host, path, width, height) \
            FORMAT( \
                @"http://asset.dr.dk/discoImages/?discoserver=%@&amp;w=%0.f&amp;h=%0.f&amp;file=%@&amp;scaleAfter=crop&amp;quality=85", \
                host \
                width * ScreenScale, \
                height * ScreenScale, \
                URLENCODE(path) \
            )
 */

  /**
   * Billedeskalering af billeder på DRs servere.
   */
  public static String skalérDrDkBilledeUrl(String url, int bredde, int højde) {
    if (url == null || url.length() == 0 || "null".equals(url)) return null;
    try {
      URL u = new URL(url);
      String skaleretUrl = "http://asset.dr.dk/drdkimagescale/?server=www.dr.dk&amp;w=" + bredde + "&amp;h=" + højde +
          "&amp;file=" + URLEncoder.encode(u.getPath(), "UTF-8") + "&amp;scaleAfter=crop";

      Log.d("skalérDrDkBilledeUrl url1 = " + url);
      Log.d("skalérDrDkBilledeUrl url2 = " + u);
      Log.d("skalérDrDkBilledeUrl url3 = " + skaleretUrl);
      return skaleretUrl;
    } catch (Exception e) {
      Log.e("url=" + url, e);
      return null;
    }
  }


  /**
   * Billedeskalering af billeder på DRs servere.
   */
  public static String skalérSlugBilledeUrl(String slug, int bredde, int højde) {
    return "http://asset.dr.dk/imagescaler/?file=/mu/programcard/imageuri/" + slug + "&w=" + bredde + "&h=" + højde + "&scaleAfter=crop";
  }


  /**
   * Billedeskalering til LastFM og discogs til playlister.
   *
   * @see dk.dr.radio.data.DRJson#parsePlayliste(org.json.JSONArray)
   * Image: "http://api.discogs.com/image/A-4970-1339439274-8053.jpeg",
   * ScaledImage: "http://asset.dr.dk/discoImages/?discoserver=api.discogs.com&file=%2fimage%2fA-4970-1339439274-8053.jpeg&h=400&w=400&scaleafter=crop&quality=85",
   */
  public static String skalérDiscoBilledeUrl(String url, int bredde, int højde) {
    Log.d("skalérDiscoBilledeUrl url1 = " + url);
    if (url == null || url.length() == 0 || "null".equals(url)) return null;
    try {
      URL u = new URL(url);
      //String skaleretUrl = "http://asset.dr.dk/discoImages/?discoserver=" + u.getHost() + ";w=" + bredde16 + "&amp;h=" + højde9 +
      //    "&amp;file=" + URLEncoder.encode(u.getPath(), "UTF-8") + "&amp;scaleAfter=crop&amp;quality=85";
      String skaleretUrl = "http://asset.dr.dk/discoImages/?discoserver=" + u.getHost() + "&w=" + bredde + "&h=" + højde +
          "&file=" + u.getPath() + "&scaleAfter=crop&quality=85";

      //Log.d("skalérDiscoBilledeUrl url2 = " + u);
      //Log.d("skalérDiscoBilledeUrl url3 = " + skaleretUrl);
      return skaleretUrl;
    } catch (Exception e) {
      Log.e("url=" + url, e);
      return null;
    }
  }

  /**
   * Skalering af andre billeder. Denne her virker til begge ovenstående, samt til skalering af andre biller, så den gemmer vi, selvom den ikke bliver brugt p.t.
   */
  public static String skalérAndenBilledeUrl(String url, int bredde, int højde) {
    if (url == null || url.length() == 0 || "null".equals(url)) return null;
    try {
      URL u = new URL(url);
      String skaleretUrl = "http://dr.dk/drdkimagescale/imagescale.drxml?server=" + u.getAuthority() + "&file=" + u.getPath() + "&w=" + bredde + "&h=" + højde + "&scaleafter=crop";

      Log.d("skalérAndenBilledeUrl url1 = " + url);
      Log.d("skalérAndenBilledeUrl url2 = " + u);
      Log.d("skalérAndenBilledeUrl url3 = " + skaleretUrl);
      return skaleretUrl;
    } catch (Exception e) {
      Log.e("url=" + url, e);
      return null;
    }
  }


  protected static void udvikling_checkDrSkrifter(View view, String beskrivelse) {
    if (view instanceof ViewGroup) {
      ViewGroup vg = (ViewGroup) view;
      for (int i = 0; i < vg.getChildCount(); i++) {
        udvikling_checkDrSkrifter(vg.getChildAt(i), beskrivelse);
      }
    } else if (view instanceof TextView) {
      TextView tv = ((TextView) view);
      Typeface tf = tv.getTypeface();
      if (tv.getVisibility() == View.VISIBLE && tv.getText().length() > 0 && tf != App.skrift_normal && tf != App.skrift_fed) {
        String resId = tv.getId() > 0 ? App.instans.getResources().getResourceEntryName(tv.getId()) : "(MANGLER ID)";
        Log.e("udvikling_checkDrSkrifter: TextView " + resId + " har forkert skrift: " + tf + " for " + beskrivelse, null);
        tv.setTypeface(App.skrift_normal);
      }
    }
  }
}
