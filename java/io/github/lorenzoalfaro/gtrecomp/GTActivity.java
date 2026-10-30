package io.github.lorenzoalfaro.gtrecomp;

import android.util.Log;
import android.view.KeyEvent;

import org.libsdl.app.SDLActivity;

// SDL's activity runs SDL_main() from libmain.so (the recompiled game and its runtime).
public class GTActivity extends SDLActivity {
    @Override
    protected String[] getLibraries() {
        return new String[] {"SDL2", "main"};
    }

    // Command-line options for testing, e.g.
    //   adb shell am start -n <package>/.GTActivity -e args "--frames 600 --keys 300:start"
    @Override
    protected String[] getArguments() {
        String args = getIntent() != null ? getIntent().getStringExtra("args") : null;
        if (args == null || args.trim().isEmpty()) return new String[0];
        return args.trim().split("\\s+");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // A controller's guide button arriving as BUTTON_MODE would be a system shortcut that
        // leaves the game: swallow it.
        if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_MODE) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0)
                Log.i("GT", "guide button ignored (" + event.getDevice() + ")");
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
