package dk.dr.radio.diverse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by j on 06-03-14.
 */
public class Netvaerksstatus extends BroadcastReceiver {
  public enum Status {
    WIFI, MOBIL, INGEN
  }

  public Status status;
  public List<Runnable> observatører = new ArrayList<Runnable>();

  public boolean erOnline() {
    return status != Status.INGEN;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    NetworkInfo networkInfo = App.connectivityManager.getActiveNetworkInfo();
    if (App.udvikling) App.kortToast("Netvaerksstatus\n" + intent + "\n" + networkInfo);

    Status nyStatus;

    if (networkInfo == null || !networkInfo.isConnected()) {
      nyStatus = Status.INGEN;
    } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
      nyStatus = Status.WIFI;
    } else {
      nyStatus = Status.MOBIL;
    }

    if (status != nyStatus) {
      status = nyStatus;
      if (App.udvikling) App.kortToast("Netvaerksstatus\n" + status);
      for (Runnable o : observatører) o.run();
    }
  }
}