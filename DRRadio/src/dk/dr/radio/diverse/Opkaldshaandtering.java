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

package dk.dr.radio.diverse;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import dk.dr.radio.afspilning.Afspiller;
import dk.dr.radio.util.Log;


/*
 * Denne klasse sørger for at stoppe afspilning hvis telefonen ringer
 */
public class Opkaldshaandtering extends PhoneStateListener {

  private Afspiller service;
  private boolean venterPåKaldetAfsluttes;

  public Opkaldshaandtering(Afspiller service) {
    this.service = service;
  }

  @Override
  public void onCallStateChanged(int state, String incomingNumber) {
    int afspilningsstatus = service.getAfspillerstatus();
    switch (state) {
      case TelephonyManager.CALL_STATE_OFFHOOK:
        Log.d("Offhook state detected");
        if (afspilningsstatus != Afspiller.STATUS_STOPPET) {
          venterPåKaldetAfsluttes = true;
          service.stopAfspilning();
        }
        break;
      case TelephonyManager.CALL_STATE_RINGING:
        Log.d("Ringing detected");
        if (afspilningsstatus != Afspiller.STATUS_STOPPET) {
          venterPåKaldetAfsluttes = true;
          service.stopAfspilning();
        }
        break;
      case TelephonyManager.CALL_STATE_IDLE:
        Log.d("Idle state detected");
        if (venterPåKaldetAfsluttes) {
          try {
            service.startAfspilning();
          } catch (Exception e) {
            Log.e(e);
          }
          venterPåKaldetAfsluttes = false;
        }
        break;
      default:
        Log.d("Unknown phone state=" + state);
    }
  }
}
