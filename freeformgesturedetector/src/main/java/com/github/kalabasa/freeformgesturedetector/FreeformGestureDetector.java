package com.github.kalabasa.freeformgesturedetector;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Detects freeform transformation gestures using the supplied {@link MotionEvent}s. The
 * {@link OnFreeformGestureListener} callback will notify users when a transform gesture event has
 * occurred.
 * <p>
 * Freeform transformation gestures range from single-finger dragging, two-finger scaling and
 * rotation, to full multi-touch distortion (skew and perspective).
 * <p>
 * Note that excess pointers may be ignored if the resultant transformation cannot be expressed in
 * a standard {@link Matrix} (ie, no warping).
 * <p>
 * To use this class:
 * <ul>
 * <li>Create an instance of the {@code FreeformGestureDetector} for your {@link View}
 * <li>In the {@link View#onTouchEvent(MotionEvent)} method ensure you call
 * {@link #onTouchEvent(MotionEvent)}. The methods defined in your callback will be executed when
 * the events occur.
 * </ul>
 */
public class FreeformGestureDetector {

	/**
	 * The listener for receiving notifications when gestures occur.
	 */
	public interface OnFreeformGestureListener {
		/**
		 * Notified of transformation eents during a gesture.
		 *
		 * @param ev        the current move {@link MotionEvent} that triggered the current transform.
		 * @param transform the transformation {@link Matrix} delta from the previous accepted event
		 *                  to the current event.
		 * @return Whether or not the detector should consider this event as handled. If an event was
		 * not handled, the detector will continue to accumulate movement until an event is handled.
		 */
		boolean onTransform(MotionEvent ev, Matrix transform);
	}

	private final OnFreeformGestureListener listener;

	private int maxPointers = 4;
	private float touchSlop;
	private boolean pastSlop;

	private SparseArray<PointF> pointerStartLocations = new SparseArray<>();
	private SparseArray<PointF> pointerLocations = new SparseArray<>();

	private float[] record = null;

	private int[] pointerIdMap = null;
	private Matrix tmpMatrix = new Matrix();

	private Matrix matrix = new Matrix();

	/**
	 * Creates a FreeformGestureDetector with the supplied listener.
	 *
	 * @param context  the application's context
	 * @param listener the listener for callbacks
	 */
	public FreeformGestureDetector(Context context, OnFreeformGestureListener listener) {
		this(ViewConfiguration.get(context).getScaledTouchSlop(), listener);
	}

	/**
	 * Creates a FreeformGestureDetector with the supplied listener and a custom touch slop.
	 *
	 * @param touchSlop touch slop before recognizing a gesture
	 * @param listener  the listener for callbacks
	 */
	public FreeformGestureDetector(float touchSlop, OnFreeformGestureListener listener) {
		if (listener == null) throw new NullPointerException("listener must not be null.");
		this.listener = listener;
		this.touchSlop = touchSlop;
	}

	/**
	 * Set the touch slop, the movement allowance before recognizing a gesture
	 *
	 * @param touchSlop new touch slop in pixels
	 */
	public void setTouchSlop(float touchSlop) {
		this.touchSlop = touchSlop;
	}

	/**
	 * @return the touch slop in pixels
	 */
	public float getTouchSlop() {
		return touchSlop;
	}

	/**
	 * Set the maximum number of pointers simultaneously processed. This will determine the degrees
	 * of freedom the transformation may have. For example, limiting it to 2 max pointers will allow
	 * only translation, rotation, and scaling transformations, preventing skew and distortions.
	 *
	 * @param maxPointers the maximum number of pointers, from 0 to 4.
	 */
	public void setMaxPointers(int maxPointers) {
		if (maxPointers < 0 || maxPointers > 4) throw new IllegalArgumentException("maxPointers must be in the range 0 to 4");
		this.maxPointers = maxPointers;
	}

	/**
	 * @return the maximum number of pointers simultaneously recognized
	 */
	public int getMaxPointers() {
		return maxPointers;
	}

	/**
	 * Accepts MotionEvents and dispatches events to the
	 * {@link FreeformGestureDetector.OnFreeformGestureListener} when appropriate.
	 * <p>
	 * Applications should pass a complete and consistent event stream to this method.
	 * A complete and consistent event stream involves all MotionEvents from the initial
	 * ACTION_DOWN to the final ACTION_UP or ACTION_CANCEL
	 *
	 * @param event the event to process
	 */
	public void onTouchEvent(MotionEvent ev) {
		int actionMasked = ev.getActionMasked();
		switch (actionMasked) {
			case MotionEvent.ACTION_DOWN: {
				pastSlop = false;
				startPointer(ev.getPointerId(0), ev.getX(), ev.getY());
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				recordUpdate();
				for (int i = 0; i < ev.getPointerCount(); i++) {
					movePointer(ev.getPointerId(i), ev.getX(i), ev.getY(i));
				}
				applyUpdate(ev);
				break;
			}
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL: {
				endPointer(ev.getPointerId(0), ev.getX(), ev.getY());
				break;
			}
			case MotionEvent.ACTION_POINTER_DOWN: {
				int index = ev.getActionIndex();
				startPointer(ev.getPointerId(index), ev.getX(index), ev.getY(index));
				break;
			}
			case MotionEvent.ACTION_POINTER_UP: {
				int index = ev.getActionIndex();
				endPointer(ev.getPointerId(index), ev.getX(index), ev.getY(index));
				break;
			}
		}
	}

	private void startPointer(int pointerId, float x, float y) {
		if (!pastSlop) pointerStartLocations.put(pointerId, new PointF(x, y));
		pointerLocations.put(pointerId, new PointF(x, y));
	}

	private void movePointer(int pointerId, float x, float y) {
		if (!pastSlop) {
			PointF startLoc = pointerStartLocations.get(pointerId);
			pastSlop |= PointF.length(x - startLoc.x, y - startLoc.y) >= touchSlop;
		}
		pointerLocations.get(pointerId).set(x, y);
	}

	private void endPointer(int pointerId, float x, float y) {
		pointerLocations.get(pointerId).set(x, y);
		pointerLocations.remove(pointerId);
		pointerStartLocations.remove(pointerId);
	}

	private void recordUpdate() {
		int n = pointerLocations.size();
		record = new float[n * 4];
		pointerIdMap = new int[n];
		for (int i = 0; i < n; i++) {
			PointF point = pointerLocations.valueAt(i);
			record[i * 2] = point.x;
			record[i * 2 + 1] = point.y;
			pointerIdMap[i] = pointerLocations.keyAt(i);
		}
	}

	private void applyUpdate(MotionEvent ev) {
		int n = pointerLocations.size();
		for (int i = 0; i < n; i++) {
			int pointerId = pointerIdMap[i];
			PointF point = pointerLocations.get(pointerId);
			record[n * 2 + i * 2] = point.x;
			record[n * 2 + i * 2 + 1] = point.y;
		}

		for (int i = Math.min(maxPointers, n); i > 0; i--) {
			if (tmpMatrix.setPolyToPoly(record, 0, record, n * 2, i)) {
				matrix.postConcat(tmpMatrix);
				if (pastSlop) sendTransform(ev);
				break;
			}
		}
	}

	private void sendTransform(MotionEvent ev) {
		if (listener.onTransform(ev, new Matrix(matrix))) {
			matrix.reset();
		}
	}
}
