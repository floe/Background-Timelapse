/****************************************************************\
*                Android Background Timelapse                    *
* Copyright (c) 2009-10 by Florian Echtler <floe@butterbrot.org> *
*  Licensed under GNU General Public License (GPL) 3 or later    *
\****************************************************************/

package floe.timelapse;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.TextureView;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;


public class Timelapse extends Activity {

	private TimelapseService mBoundService;
	private Intent myIntent;

	private TextureView tv;

	// --Commented out by Inspection (11/02/21 12:39):private final String TAG = "Timelapse";
	private final int PERMISSION_REQUEST_CODE = 0x100;

	@Override protected void onCreate( Bundle savedInstanceState ) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Watch for button clicks.
		Button button = findViewById( R.id.start );
		button.setOnClickListener(mBindListener);

		button = findViewById( R.id.stop );
		button.setOnClickListener(mUnbindListener);

		button = findViewById(R.id.about);
		button.setOnClickListener(mAboutListener);

		// setup the preview surface
		tv = findViewById(R.id.view);

		// check for camera/storage permission
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.CAMERA)	                != PackageManager.PERMISSION_GRANTED ||
			    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{
					Manifest.permission.CAMERA,
					Manifest.permission.WRITE_EXTERNAL_STORAGE
				}, PERMISSION_REQUEST_CODE);
			}
		}

		// bind to the service, starting it when not yet running
		myIntent = new Intent( this, TimelapseService.class );
		bindService( myIntent, mConnection, Context.BIND_AUTO_CREATE );
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == PERMISSION_REQUEST_CODE) {
			// If request is cancelled, the result arrays are empty.
			if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(Timelapse.this, "Unable to run without camera/storage permission.", Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override protected void onDestroy() {
		try {
			unbindService(mConnection);
		} catch (Exception ignored) { }
		super.onDestroy();
	}

	// catch & ignore config changes: screen rotation/keyboard slide
	// FIXME: this is a hack, the proper solution is MUCH more complicated - see:
	// https://github.com/commonsguy/cw-android/blob/master/Rotation/RotationAsync/src/com/commonsware/android/rotation/async/RotationAsync.java
	@Override public void onConfigurationChanged( Configuration newConfig ) {
		// just call the default method
		super.onConfigurationChanged( newConfig );
	}


	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundService = ((TimelapseService.TimelapseBinder)service).getService();
			//Toast.makeText( Timelapse.this, "Connected to timelapse service.", Toast.LENGTH_SHORT ).show();
			((TextView)findViewById( R.id.status )).setText( mBoundService.isRunning() ? "running" : "stopped" );
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundService = null;
			Toast.makeText(Timelapse.this, "Disconnected from timelapse service.", Toast.LENGTH_SHORT).show();
			((TextView)findViewById( R.id.status )).setText( "terminated" );
		}
	};


	// get delay value in ms from input element
	private int getDelay() {
		int mydelay = 1000;
		String delaytext = ((EditText)findViewById(R.id.delay)).getText().toString();
		try {
			mydelay = Integer.decode(delaytext);
		} catch (NumberFormatException ignored) { }
		if (mydelay < 1000) mydelay = 1000;
		return mydelay;
	}

	private OnClickListener mBindListener = new OnClickListener() {
		public void onClick(View v) {
			// mark the service as started - will not be
			// killed now, even if the connection is closed
			startService( myIntent );
			mBoundService.launch( tv, getDelay() );
			((TextView)findViewById( R.id.status )).setText( "running" );
		}
	};

	private OnClickListener mUnbindListener = new OnClickListener() {
		public void onClick(View v) {
			try {
				// stop the service again - now will be
				// terminated once the connection closes.
				mBoundService.cleanup();
				stopService( myIntent );
				((TextView)findViewById( R.id.status )).setText( "stopped" );
			} catch (IllegalArgumentException ignored) { }
		}
	};

	private OnClickListener mAboutListener = new OnClickListener() {
		public void onClick( View v ) {
			String msg = "Background Timelapse 0.3\n(c) 2021 by floe@butterbrot.org\n\nhttps://github.com/floe/background-timelapse\nContinously captures HD images in the background for timelapse videos.\n\nStart: start the capture service\nStop: shut down the service\nAbout: this cruft\nDelay: milliseconds between captures";
			new AlertDialog.Builder( Timelapse.this )
				.setMessage(msg)
				.setPositiveButton( "Close", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) { dialog.cancel(); }})
				.show();
		}
	};
}
