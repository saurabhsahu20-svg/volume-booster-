# Crystal Volume Booster v1.5

This Android app boosts **audio played in its own local music player only**. It does not modify system-wide audio playback from YouTube, Spotify or other applications, and cannot bypass hardware volume ceilings or guarantee louder sound on already normalized tracks.

## UI fix / clearer testing
- Fixed critical UI defect: controls added to vertical LinearLayout used a 0-pixel width, making Choose Music File, Enhancement ON, Extra Loudness and Output Mode buttons invisible.
- Step 1: choose a local music file and play it. Step 2: compare Normal versus Boost.
- Sound Settings shortcut lets the user find OEM built-in audio enhancements if their phone supports them.
- Requested signal amplitude boost of 100%-200% on the built-in speaker profile, with a 160% cap for headphone mode. This is **not** a 200% actual loudness guarantee.
- Optional multiband compression to bring up quieter passages, limiter at -2 dBFS on compatible devices, gradual gain ramp, voice EQ.
- Keep listening at comfortable levels. At high input/output levels, digital limiting or hardware speaker distortion may remove any audible benefit.

APK build: GitHub Actions successful Build Crystal Volume Booster APK workflow → artifact CrystalVolumeBooster-v1.5-APK → app-debug.apk.
