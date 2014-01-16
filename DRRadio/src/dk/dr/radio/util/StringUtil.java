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

package dk.dr.radio.util;

public class StringUtil {

  public static String getTimeFromDate(String date) {
    String time = "";

    if (date != null) {
      int timeStart = date.indexOf("T");
      int timeEnd = date.lastIndexOf(":");
      if ((timeStart != -1) && (timeEnd != -1)) {
        time = date.substring(timeStart + 1, timeEnd);
        System.out.println("Time Striped is: " + time);
      }
    }
    return time;
  }

  public static String limitString(String stringToLimit, String maxStringLimit) {
    String resultString = stringToLimit;

    try {
      Integer maxLimit = Integer.parseInt(maxStringLimit);
      if (stringToLimit.length() > maxLimit) {
        resultString = stringToLimit.substring(0, maxLimit) + "...";
      }
    } catch (NumberFormatException nfe) {
    }
    return resultString;
  }
}
