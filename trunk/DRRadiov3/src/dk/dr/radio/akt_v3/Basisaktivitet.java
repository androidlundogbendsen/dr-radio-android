package dk.dr.radio.akt_v3;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ScrollView;
import android.widget.TextView;

import com.androidquery.AQuery;

import dk.dr.radio.diverse.App;
import dk.dr.radio.diverse.Log;

public class Basisaktivitet extends ActionBarActivity {
  protected final AQuery aq = new AQuery(this);


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    //getActionBarSetDisplayHomeAsUpEnabledKompat(this, true);
  }

  @SuppressWarnings("UnusedDeclaration")
  public static void invalidateOptionsMenuKompat(final FragmentActivity akt) {
    if (akt != null && android.os.Build.VERSION.SDK_INT >= 11) { // separat klasse, for at undgå crash på tidl. versioner
      new Runnable() {
        @SuppressLint("NewApi")
        public void run() {
          akt.invalidateOptionsMenu();
        }
      }.run();
    }
  }

  /**
   * Tillader brugeren at navigere 'op' v.hj.a. actionbaren
   */
  public void getActionBarSetDisplayHomeAsUpEnabledKompat(final boolean b) {
    if (android.os.Build.VERSION.SDK_INT >= 11) { // separat klasse, for at undgå crash på tidl. versioner
      new Runnable() {
        @SuppressLint("NewApi")
        public void run() {
          ActionBar ab = getActionBar();
          if (ab == null) return;
          ab.setDisplayHomeAsUpEnabled(b);
        }
      }.run();
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Log.d("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    if (App.udvikling) {
      menu.add(0, 642, 0, "Udvikler");
      menu.add(0, 643, 0, "Log");
      menu.add(0, 644, 0, "Hent nyeste udvikler-version");
      menu.add(0, 646, 0, "Send fejlrapport");
    }
    return super.onCreateOptionsMenu(menu);
  }

  protected static Bundle putString(Bundle args, String key, String value) {
    args = new Bundle(args);
    args.putString(key, value);
    return args;
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      /*
      case android.R.id.home:
        //NavUtils.navigateUpTo(this, new Intent(this, HjemAkt.class));
        finish();
        return true;
        */
      case 642:
        App.udvikling = !App.udvikling;
        App.kortToast("Log.udvikling = " + App.udvikling);
        return true;
      case 644:
        // scp /home/j/android/dr-radio-android/DRRadiov3/out/production/DRRadiov3/DRRadiov3.apk j:javabog.dk/privat/
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://javabog.dk/privat/DRRadiov3.apk")));
        return true;
      case 645:
      case 643:
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        TextView tv = new TextView(this);
        tv.setText(Log.getLog());
        android.util.Log.i("", Log.getLog());
        tv.setTextSize(10f);
        tv.setBackgroundColor(0xFF000000);
        tv.setTextColor(0xFFFFFFFF);
        final ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        dialog.setView(sv);
        dialog.show();
        sv.post(new Runnable() {
          public void run() {
            sv.fullScroll(View.FOCUS_DOWN);
          }
        });
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(Log.getLog());
        App.kortToast("Log kopieret til udklipsholder");
        return true;
      case 646:
        Log.rapporterFejl(new Exception("Fejlrapport for enhed sendes"));
        return true;
    }
    return super.onOptionsItemSelected(item);
  }


  @Override
  protected void onResume() {
    super.onResume();
    if (App.udvikling) Log.d(this + " onResume()");
    App.instans.onResume(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (App.udvikling) Log.d(this + " onPause()");
    App.instans.onPause();
  }
}
