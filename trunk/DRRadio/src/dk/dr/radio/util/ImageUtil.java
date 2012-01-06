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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageUtil {

	private static final int DEFAULT_NUM_RETRIES = 3;
	private static int numRetries = DEFAULT_NUM_RETRIES;

	public static Bitmap downloadImage(String url) {
		int timesTried = 1;

		while (timesTried <= numRetries) {
			try {
				byte[] imageData = retrieveImageData(url);

				return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
			} catch (Throwable e) {
				timesTried++;
			}
		}
		return null;
	}

	private static byte[] retrieveImageData(String imageUrl) throws IOException {
		URL url = new URL(imageUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// determine the image size and allocate a buffer
		int fileSize = connection.getContentLength();
		if (fileSize < 0) {
			return null;
		}
		byte[] imageData = new byte[fileSize];

		BufferedInputStream istream = new BufferedInputStream(connection.getInputStream());
		int bytesRead = 0;
		int offset = 0;
		while (bytesRead != -1 && offset < fileSize) {
			bytesRead = istream.read(imageData, offset, fileSize - offset);
			offset += bytesRead;
		}

		// clean up
		istream.close();
		connection.disconnect();

		return imageData;
	}
}
