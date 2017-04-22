/*___________________________________________________________________
|
| File: OpenGLRenderer.java
|
| Description: Rendering class for slotmachine app.  
|
| TO DO: 1) Reload OpenGL resources (textures) on a context switch.
|__________________________________________________________________*/
package com.example.hm5_gokani_jyotishi;

import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.opengl.GLU;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

/*___________________________________________________________________
|
| Class: OpenGLRenderer
|__________________________________________________________________*/
public class OpenGLRenderer implements Renderer
{
	int counter =0;
	// Used for runtime debug messages
	private static final String DEBUG_TAG="Project Logging";
	public float screenWidth;
	public float screenHeight;

	// App context
	Context mContext;
	// Handler to run code on UI thread
	Handler mHandler;

	// 3D models
	private Spinner spinner;
	private Rectangle rectangle;

	// State variables
	private long mLastTime;
	private long spinTime = 0;
	private Random rgen;
	//float fraction1=
	private int rot1, rot2, rot3;					// current spinner rotations
	private int final_rot1, final_rot2, final_rot3;	// final (after a spin)

	// Sound variables, constants
	private soundmanager smgr = null;
	private int  soundhandle_ring1 = 0;
	private int  soundhandle_ring2 = 0;
	private int  soundhandle_ring3 = 0;

	public static final int SFX_PULL	= 1;
	public static final int SFX_RING1	= 2;
	public static final int SFX_RING2	= 3;
	public static final int SFX_RING3 	= 4;
	public static final int SFX_WIN 	= 5;


	/*___________________________________________________________________
	|
	| Function: OpenGLRenderer (constructor)
	|
	| Description: Called when surface is created.  Do first-time initializations
	|	here.
	|__________________________________________________________________*/
	public OpenGLRenderer(Context context, Handler handler)
	{
		mContext = context;
		mHandler = handler;

		// Init random number generator using the system time
		rgen = new Random(System.currentTimeMillis());

		// Calculate starting spinner positions randomly
		rot1 = rgen.nextInt(6) * 60;
		rot2 = rgen.nextInt(6) * 60;
		rot3 = rgen.nextInt(6) * 60;

		// Initialize a spinner
		spinner = new Spinner (1.1f, 1.1f, 1.1f);
		spinner.loadBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.slot_4pics));

		// Initialize the rectangle
		rectangle = new Rectangle (3, 1);
		rectangle.loadBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.slot_pull));

		// Init sound manager
		smgr = new soundmanager ();
		smgr.Init (context, 6, false, 0);
		// Load sounds
		smgr.AddSound (SFX_PULL,  R.raw.pull);
		smgr.AddSound (SFX_RING1, R.raw.ring1);
		smgr.AddSound (SFX_RING2, R.raw.ring2);
		smgr.AddSound (SFX_RING3, R.raw.ring3);
		smgr.AddSound (SFX_WIN,   R.raw.win);

		// Set timer
		mLastTime = System.currentTimeMillis() + 100;

	}

	/*___________________________________________________________________
	|
	| Function: onSurfaceCreated
	|
	| Description: Called when surface is created.  Do first-time initializations
	|	here.
	|__________________________________________________________________*/
	public void onSurfaceCreated (GL10 gl, EGLConfig config)
	{
//		Log.i(DEBUG_TAG, "onsurfaceCreated()");
		// Set the background color to black ( rgba )
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);
		// Enable Smooth Shading, default not really needed
		gl.glShadeModel(GL10.GL_SMOOTH);
		// Depth buffer setup
		gl.glClearDepthf(1.0f);
		// Enables depth testing
		gl.glEnable(GL10.GL_DEPTH_TEST);
		// The type of depth testing to do
		gl.glDepthFunc(GL10.GL_LEQUAL);
		// Really nice perspective calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);


	}

	/*___________________________________________________________________
	|
	| Function: onSurfaceChanged
	|
	| Description: Called when device is changing between portrait and landscape
	|	mode.
	|__________________________________________________________________*/
	public void onSurfaceChanged (GL10 gl, int width, int height)
	{
//		Log.i(DEBUG_TAG, "onsurfaceChanged()");
		// Sets the current view port to the new size
		gl.glViewport (0, 0, width, height);
		// Select the projection matrix
		gl.glMatrixMode (GL10.GL_PROJECTION);
		// Reset the projection matrix
		gl.glLoadIdentity ();
		// Calculate the aspect ratio of the window
		GLU.gluPerspective (gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);
		// Select the modelview matrix
		gl.glMatrixMode (GL10.GL_MODELVIEW);
		// Reset the modelview matrix
		gl.glLoadIdentity ();
		screenWidth = width;
		screenHeight = height;
//		System.out.println("Screen x: "+screenx);
//		System.out.println("Screen y: "+screeny);


		Log.i("Project Logging", gl.glGetString(GL10.GL_VERSION));
		String extensions = gl.glGetString(GL10.GL_EXTENSIONS);
		if (extensions.contains ("vertex_buffer_object"))
			Log.i("Project Logging", "VBOs are supported");
		else
			Log.i("Project Logging", "VBOs are not supported");
	}

	/*___________________________________________________________________
	|
	| Function: onDrawFrame
	|
	| Description: Called each frame of drawing.  Can be called at varying
	|	frequencies so good idea to compute elapse time between calls and
	|	scale any animation accordingly.
	|__________________________________________________________________*/
	public void onDrawFrame(GL10 gl)
	{
		long now = System.currentTimeMillis();
		double elapsed_time = now - mLastTime;
		boolean pullVisible = false;


		synchronized (this) {

			// Spin the spinners?
			if (spinTime != 0) {

				// First time spinning?
				if (spinTime == 5000) {
					smgr.PlaySound (SFX_PULL);
					soundhandle_ring1 = smgr.PlayLoopedSound(SFX_RING1);
					soundhandle_ring2 = smgr.PlayLoopedSound(SFX_RING2);
					soundhandle_ring3 = smgr.PlayLoopedSound(SFX_RING3);
					pullVisible=true;

				}

				pullVisible=true;
				// Decrease spin time
				spinTime -= elapsed_time;
				if (spinTime < 0)
					spinTime = 0;
				// Add to the rotation of each spinner
				rot1 += 24; // scale this based on elapsed time so works same on different speed processors
				rot2 += 24;
				rot3 += 24;
				if (rot1 >= 360) rot1 -= 360;
				if (rot2 >= 360) rot2 -= 360;
				if (rot3 >= 360) rot3 -= 360;

				// Stop left spinnner?
				if (spinTime <= 2000) {
					rot1 = final_rot1;
					smgr.StopSound(soundhandle_ring1);

					//spin = false;
				}
				// Stop middle spinner?
				if (spinTime <= 1000) {
					rot2 = final_rot2;
					smgr.StopSound(soundhandle_ring2);
					//spin=false;
				}
				// Stop right spinner?
				if (spinTime == 0) {
					rot3 = final_rot3;

					pullVisible = false;
					smgr.StopSound(soundhandle_ring3);

					if (((final_rot1 == final_rot2) && (final_rot1 == final_rot3))) {
						smgr.PlaySound(SFX_WIN);
						// Display a simple toast
						//		mHandler.post(new Runnable() {
						//   		 public void run() {
						// 				Toast.makeText(mContext, "YOU WIN!", Toast.LENGTH_SHORT).show();
						// 		 }
						//		});

						// Toast that displays a custom view
						mHandler.post(new Runnable() {
							public void run() {
								// Display a toast
								Toast ImageToast = new Toast(mContext);
								LinearLayout toastLayout = new LinearLayout(mContext);
								toastLayout.setOrientation(LinearLayout.VERTICAL);
								ImageView image = new ImageView(mContext);
								//	TextView text = new TextView(mContext);
								image.setImageResource(R.drawable.jackpotgirl);
								//image.setBackgroundColor(Color.RED);
								//	text.setText("You Win!");
								//	toastLayout.addView(text);
								toastLayout.addView(image);
								ImageToast.setView(toastLayout);
								ImageToast.setDuration(Toast.LENGTH_LONG);
								ImageToast.setGravity(Gravity.BOTTOM, 0, 0);
								ImageToast.show();

								// Display a notification
								String svcName = Context.NOTIFICATION_SERVICE;
								NotificationManager nmgr = (NotificationManager)mContext.getSystemService(svcName);

								long when = System.currentTimeMillis();
								Notification note = new Notification (R.drawable.potgold, "You hit the JACKPOT!", when);

								// Use the next 2 lines so the extended notification does nothing when clicked
								Intent intent = new Intent(mContext, main_activity.class);
								PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

								note.setLatestEventInfo(mContext, "hm5_gokani_jyotishi", "You win Jackpot!", pendingIntent);
								note.flags = Notification.FLAG_AUTO_CANCEL;
								nmgr.notify(1, note);

							}
						});

					}
				}
			}
		}
		mLastTime = now;


		// Clears the screen and depth buffer.
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);



		// Draw left spinner
		gl.glLoadIdentity();
		gl.glTranslatef(-1.15f, 0, -8);
		gl.glRotatef(rot1, 1, 0, 0);
		spinner.draw(gl);

		// Draw middle spinner
		gl.glLoadIdentity();
		gl.glTranslatef(0, 0, -8);
		gl.glRotatef(rot2, 1, 0, 0);
		spinner.draw(gl);

		// Draw right spinner
		gl.glLoadIdentity();
		gl.glTranslatef(1.15f, 0, -8);
		gl.glRotatef(rot3, 1, 0, 0);
		spinner.draw(gl);

		// Draw the rectangle
		if(!pullVisible) {
			gl.glLoadIdentity();
			gl.glTranslatef(0, -2, -8);
			rectangle.draw(gl);
		}

	}

	/*___________________________________________________________________
      |
      | Function: doTouchEvent
      |
      | Description: Handles a touch event.  Returns true if the event was
      |		handled and consumed, else false.
      |
      | CAUTION: This method executes on the UI thread.  If changing any
      |	data used by the drawing thread, you may need to provide mutual
      |	exclusion so both threads don't try to make changes at the same
      |	time.
      |__________________________________________________________________*/
	public boolean doTouchEvent (MotionEvent event)
	{
		// Only interested in down press - not checking screen press location currently, this should be fixed
		if (event.getAction() == MotionEvent.ACTION_DOWN) {

			// Look at where the actual touch is in the LogCat window when running this program in Android Studio
			Log.i("Project Logging", "Touch at " + (int) (event.getX()) + "," + (int) (event.getY()));
			float touchx, touchy;
			touchx = event.getX();
			touchy = event.getY();
			float x1= 0.13f*screenWidth;
			float x2= 0.843f*screenWidth;
			float y1= 0.719f*screenHeight;
			float y2= 0.864f*screenHeight;
			if ((touchx >= x1 && touchx <= x2) && (touchy >= y1 && touchy <= y2)){

				synchronized (this) {
					// If not spinning then start
					if (spinTime == 0) {
						counter++;
						spinTime = 5000;
						// Calculate the final spinner positions randomly
						final_rot1 = rgen.nextInt(6) * 60;
						final_rot2 = rgen.nextInt(6) * 60;
						final_rot3 = rgen.nextInt(6) * 60;
						if(counter==4) {
							final_rot2 = final_rot1; // force a win
							final_rot3 = final_rot1;
							counter=0;
						}
					}
				}
			}
		}
		return true;
	}

	/*___________________________________________________________________
    |
    | Function: onPause
    |
       | CAUTION: This method executes on the UI thread.  If changing any
      |	data used by the drawing thread, you may need to provide mutual
      |	exclusion so both threads don't try to make changes at the same
      |	time.
    |__________________________________________________________________*/
	public void onPause ()
	{
		synchronized (this) {
			// Stop spinner sounds if pausing
			if (smgr != null) {
				smgr.StopSound(soundhandle_ring1);
				smgr.StopSound(soundhandle_ring2);
				smgr.StopSound(soundhandle_ring3);
			}
		}
	}

	/*___________________________________________________________________
    |
    | Function: onResume
    |
      | CAUTION: This method executes on the UI thread.  If changing any
      |	data used by the drawing thread, you may need to provide mutual
      |	exclusion so both threads don't try to make changes at the same
      |	time.
    |__________________________________________________________________*/
	public void onResume ()
	{

	}

}
