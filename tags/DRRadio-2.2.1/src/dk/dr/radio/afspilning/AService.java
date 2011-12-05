/**
DR Radio 2 is developed by Jacob Nordfalk, Hanafi Mughrabi and Frederik Aagaard.
Some parts of the code are loosely based on Sveriges Radio Play for Android.

DR Radio 2 for Android is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2 as published by
the Free Software Foundation.

DR Radio 2 for Android is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
DR Radio 2 for Android.  If not, see <http://www.gnu.org/licenses/>.

*/

package dk.dr.radio.afspilning;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import dk.dr.radio.util.Log;

/**
 * Tidligere AfspillerService - service-del der sørger for at app'en
 * bliver i hukommelsen mens der spilles lyd
 * @author j
 */
public class AService extends Service {
  /**
   * Service-mekanik: Servicens binder. Ligegyldig da vi kører i samme proces.
   */
  public class AfspillerBinder extends Binder {
    public AService getService() {
      return AService.this;
    }
  }
  /** Service-mekanik. Ligegyldig da vi kører i samme proces. */
  private final IBinder mBinder = new AfspillerBinder();

  /** Service-mekanik. Ligegyldig da vi kører i samme proces.  */
  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }


  @Override
  public void onCreate() {
    Log.d("AfspillerService onCreate!");
  }
/*
  // For Android 1.6-kompetibilitet bruger vi ikke onStartCommand()
  // Se http://developer.android.com/reference/android/app/Service.html#onStartCommand%28android.content.Intent,%20int,%20int%29
  // This is the old onStart method that will be called on the pre-2.0
  // platform.  On 2.0 or later we override onStartCommand() so this
  // method will not be called.
  @Override
  public void onStart(Intent intent, int startId) {
      handleCommand(intent);
  }
*/
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
      Log.d("AfspillerService onStartCommand("+intent+" "+flags+" "+startId);
      return START_STICKY;
  }


  @Override
  public void onDestroy() {
    Log.d("AfspillerService onDestroy!");
    super.onDestroy();
  }

}
