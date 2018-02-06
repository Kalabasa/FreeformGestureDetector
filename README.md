# FreeformGestureDetector

:raised_hand: **Multi-touch transform gesture detector for Android.**

### Sample

![sample animation](https://media.giphy.com/media/3ohs4glUsYjZA6zUDC/giphy.gif)

[sample repo](https://github.com/Kalabasa/FreeformGestureSample)

### Usage

The FreeformGestureDetector class acts like a regular gesture detector. It receives a stream of MotionEvents and outputs a stream of transformation matrices.

Create an instance for your View. The callback `implements FreeformGestureDetector.OnGestureListener`.

```
FreeformGestureDetector freeformGestureDetector = new FreeformGestureDetector(getContext(), callback);
```

In the View's touch event handler, unconditionally call `freeformGestureDetector.onTouchEvent(MotionEvent)`.

```
@Override
public boolean onTouch(View v, MotionEvent e) {
	freeformGestureDetector.onTouchEvent(e);
	// ...
}
```

The callback will be executed for each resulting transform event. The transformation is based on touch movements.

The `transform` parameter contains the accumulated transformation since the last call that the callback has returned true (Similar behavior as ScaleGestureDetector).

```
@Override
public boolean onTransform(MotionEvent ev, Matrix transform) {
	// Handle transform events here
	myMatrix.postConcat(transform);
	return true;
}
```

### Installation

Add JitPack to your root `build.gradle`'s repositories.

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

In your app's `build.gradle` dependencies:

```
dependencies {
	implementation 'com.github.Kalabasa:FreeformGestureDetector:1.0.0'
}
```

You can check the latest version in the Releases tab.

### License

MIT.

See [LICENSE file](LICENSE)
