/****************************************************************\
*                Android Background Timelapse                    *
* Copyright (c) 2009-10 by Florian Echtler <floe@butterbrot.org> *
*  Licensed under GNU General Public License (GPL) 3 or later    *
\****************************************************************/

package floe.timelapse;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.text.format.Time;


public class Timelapse extends Activity {

	private boolean mIsBound = false;
	private TimelapseService mBoundService;
	private Intent myIntent;

	private SurfaceView sv;

	private ProgressDialog pd;
	private Handler ph = new Handler();

	@Override protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Watch for button clicks.
		Button button = (Button)findViewById( R.id.start );
		button.setOnClickListener(mBindListener);

		button = (Button)findViewById( R.id.stop );
		button.setOnClickListener(mUnbindListener);

		button = (Button)findViewById(R.id.convert);
		button.setOnClickListener(mConvertListener);

		button = (Button)findViewById(R.id.about);
		button.setOnClickListener(mAboutListener);

		// setup the preview surface
		sv = (SurfaceView)findViewById(R.id.view);
		SurfaceHolder sh = sv.getHolder();
		sh.setType( SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS );
		//sh.setFormat( PixelFormat.JPEG );

		myIntent = new Intent( Timelapse.this, TimelapseService.class );
		bindService( myIntent, mConnection, Context.BIND_AUTO_CREATE );
	}

	@Override protected void onDestroy() {
		try {
			unbindService(mConnection);
		} catch (Exception e) { }
		super.onDestroy();
	}


	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((TimelapseService.TimelapseBinder)service).getService();

			// Tell the user about this for our demo.
			Toast.makeText( Timelapse.this, "Connected to timelapse service.", Toast.LENGTH_SHORT ).show();
			//Toast.makeText(Timelapse.this, getFilesDir().getAbsolutePath(), Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
			Toast.makeText(Timelapse.this, "Disconnected from timelapse service.", Toast.LENGTH_SHORT).show();
		}
	};


	// get delay value in ms from input element
	private int getDelay() {
		int mydelay = 1000;
		String delaytext = ((EditText)findViewById(R.id.delay)).getText().toString();
		try {
			mydelay = Integer.decode(delaytext);
		} catch (Exception e) { }
		if (mydelay < 1000) mydelay = 1000;
		return mydelay;
	}

	private OnClickListener mBindListener = new OnClickListener() {
		public void onClick(View v) {

			// Establish a connection with the service.  We use an explicit
			// class name because we want a specific service implementation that
			// we know will be running in our own process (and thus won't be
			// supporting component replacement by other applications).
			startService( myIntent );
			mBoundService.launch( sv, getDelay() );
		}
	};

	private OnClickListener mUnbindListener = new OnClickListener() {
		public void onClick(View v) {
			try {
				// Detach our existing connection.
				//unbindService( mConnection );
				mBoundService.cleanup();
				stopService( myIntent );
			} catch (IllegalArgumentException e) { }
		}
	};

	private OnClickListener mConvertListener = new OnClickListener() {
		public void onClick( View v ) {

			if (mIsBound) {
				Toast.makeText( Timelapse.this, "Please stop image service first", Toast.LENGTH_SHORT).show();
				return;
			}

			File outdir = new File("/sdcard/floe.timelapse/");
			final File[] subdirs = outdir.listFiles();
			final FilenameFilter filter = new FilenameFilter() {
				public boolean accept( File dir, String name ) {
					return name.endsWith(".yuv");
				}
			};

			if (subdirs == null) {
				Toast.makeText( Timelapse.this, "No images found - SD card not mounted?", Toast.LENGTH_SHORT).show();
				return;
			}

			pd = ProgressDialog.show( Timelapse.this, "Converting..", "", true, false );

			new Thread() {
				public void run() {
					try {

						Log.v( "TL", "got " + subdirs.length + " directories" );

						int curdirnum = 0;
						int[] outbuf = new int[640*480];
						byte[] inbuf = new byte[640*480*3/2];

						for (File curdir: subdirs) {

							File[] contents = curdir.listFiles(filter);
							int curfilenum = contents.length;
							curdirnum++;

							Log.v( "TL", "got " + curfilenum + " files" );

							for (File curfile: contents) {

								String msg = "" + curfilenum-- + " images left in folder " + curdirnum + " of " + subdirs.length;
								updateProgress(msg);

								File outpath = new File( curfile.getAbsolutePath().replace(".yuv",".png") );
								if (outpath.exists()) continue;

								FileInputStream infile = new FileInputStream( curfile );
								infile.read( inbuf );
								infile.close();

								decodeYUV420SP( outbuf, inbuf, 640, 480 );
								Bitmap result = Bitmap.createBitmap( outbuf, 640, 480, Bitmap.Config.ARGB_8888 );

								FileOutputStream outfile = new FileOutputStream( outpath );
								result.compress( Bitmap.CompressFormat.PNG, 100, outfile );
								outfile.close();
							}
						}
					} catch (Exception e) {
						Log.e( "TL", "convert: " + e.toString() );
					}
					pd.dismiss();
				}
			}.start();
		}
	};

	// no idea what this is supposed to do, but needed for the PD
	public void updateProgress( final String msg ) {
		ph.post( new Runnable() { public void run() {
			pd.setMessage( msg );
		}});
	}

	private OnClickListener mAboutListener = new OnClickListener() {
		public void onClick( View v ) {
			String msg = "Timelapse 0.2\n(c) 2011 by floe@butterbrot.org\n\nContinously captures images in the background for timelapse videos.\n\nStart: start the capture service\nStop: shut down the service\nConvert All: transform temporary images to PNGs (warning: slow!)\nAbout: this cruft\nDelay: milliseconds between captures";
			new AlertDialog.Builder( Timelapse.this )
				.setMessage(msg)
				.setPositiveButton( "Close", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { dialog.cancel(); }})
				.show();
		}
	};

	// decode Y, U, and V values on the YUV 420 buffer described as YCbCr_422_SP by Android
	// David Manpearl 081201 - 1:27 for 20 images
	public static void decodeYUV(int[] out, byte[] fg, int width, int height) throws NullPointerException, IllegalArgumentException {

		final int sz = width * height;

		if (out == null) throw new NullPointerException("buffer 'out' is null");
		if (out.length < sz) throw new IllegalArgumentException("buffer 'out' size " + out.length + " < minimum " + sz);
		if (fg == null) throw new NullPointerException("buffer 'fg' is null");
		if (fg.length < sz) throw new IllegalArgumentException("buffer 'fg' size " + fg.length + " < minimum " + sz * 3/ 2);

		int i, j;
		int Y, Cr = 0, Cb = 0;

		for (j = 0; j < height; j++) {

			int pixPtr = j * width;
			final int jDiv2 = j >> 1;

			for (i = 0; i < width; i++) {

				Y = fg[pixPtr]; if(Y < 0) Y += 255;

				if ((i & 0x1) != 1) {
					final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
					Cb = fg[cOff];
					if(Cb < 0) Cb += 127; else Cb -= 128;
					Cr = fg[cOff + 1];
					if(Cr < 0) Cr += 127; else Cr -= 128;
				}

				int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
				if (R < 0) R = 0; else if(R > 255) R = 255;
				int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
				if (G < 0) G = 0; else if(G > 255) G = 255;
				int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
				if (B < 0) B = 0; else if(B > 255) B = 255;
				out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
			}
		}
	}

	static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) { // 1:25 for 20 img.

		final int frameSize = width * height;
		
		for (int j = 0, yp = 0; j < height; j++) {

			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;

			for (int i = 0; i < width; i++, yp++) {

				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0) y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}
				
				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);
				
				if (r < 0) r = 0; else if (r > 262143) r = 262143;
				if (g < 0) g = 0; else if (g > 262143) g = 262143;
				if (b < 0) b = 0; else if (b > 262143) b = 262143;
				
				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}

	static public void fastDecodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) { // 1:29 for 20 img.

		final int frameSize = width * height;
		int yp = 0;
		int uvp;

		int y0p,y1p,y2p,y3p;
		int y0,y1,y2,y3;
		int u,v,r,g,b;
		int y1192;
		
		for (int y = 0; y < height; y+=2, yp+=width) {

			uvp = frameSize + (y >> 1) * width;

			for (int x = 0; x < width; x+=2, yp+=2) {
				
				y0p = yp;
				y1p = yp+1;
				y2p = yp+width;
				y3p = yp+width+1;

				y0 = (0xff & ((int) yuv420sp[y0p])) - 16; if (y0 < 0) y0 = 0;
				y1 = (0xff & ((int) yuv420sp[y1p])) - 16; if (y1 < 0) y1 = 0;
				y2 = (0xff & ((int) yuv420sp[y2p])) - 16; if (y2 < 0) y2 = 0;
				y3 = (0xff & ((int) yuv420sp[y3p])) - 16; if (y3 < 0) y3 = 0;

				v = (0xff & yuv420sp[uvp++]) - 128;
				u = (0xff & yuv420sp[uvp++]) - 128;
				
				y1192 = 1192 * y0;

				r = (y1192 + 1634 * v);
				g = (y1192 - 833 * v - 400 * u);
				b = (y1192 + 2066 * u);
				
				if (r < 0) r = 0; else if (r > 262143) r = 262143;
				if (g < 0) g = 0; else if (g > 262143) g = 262143;
				if (b < 0) b = 0; else if (b > 262143) b = 262143;
				
				rgb[y0p] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);

				y1192 = 1192 * y1;

				r = (y1192 + 1634 * v);
				g = (y1192 - 833 * v - 400 * u);
				b = (y1192 + 2066 * u);
				
				if (r < 0) r = 0; else if (r > 262143) r = 262143;
				if (g < 0) g = 0; else if (g > 262143) g = 262143;
				if (b < 0) b = 0; else if (b > 262143) b = 262143;
				
				rgb[y1p] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);

				y1192 = 1192 * y2;

				r = (y1192 + 1634 * v);
				g = (y1192 - 833 * v - 400 * u);
				b = (y1192 + 2066 * u);
				
				if (r < 0) r = 0; else if (r > 262143) r = 262143;
				if (g < 0) g = 0; else if (g > 262143) g = 262143;
				if (b < 0) b = 0; else if (b > 262143) b = 262143;
				
				rgb[y2p] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);

				y1192 = 1192 * y3;

				r = (y1192 + 1634 * v);
				g = (y1192 - 833 * v - 400 * u);
				b = (y1192 + 2066 * u);
				
				if (r < 0) r = 0; else if (r > 262143) r = 262143;
				if (g < 0) g = 0; else if (g > 262143) g = 262143;
				if (b < 0) b = 0; else if (b > 262143) b = 262143;
				
				rgb[y3p] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}
}

