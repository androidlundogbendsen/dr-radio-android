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

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * @author j
 */
public class MedieafspillerInfo {


  public String lavTelefoninfo(Activity a) {
    String ret = "";

    PackageManager pm = a.getPackageManager();
    String version;
    try {
      PackageInfo pi = pm.getPackageInfo(a.getPackageName(), 0);
      version = pi.versionName;
    } catch (Exception e) {
      version = e.toString();
      e.printStackTrace();
    }

    ret += a.getPackageName() + " (v " + version + ")" + "\nTelefonmodel: " + Build.MODEL + " " + Build.PRODUCT + "\nAndroid v" + Build.VERSION.RELEASE + " (sdk: " + Build.VERSION.SDK + ")";

    return ret;
  }
}
