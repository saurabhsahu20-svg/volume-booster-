# Crystal Volume Booster v1.4
A native Android local-file music player with 100–200% input-signal boost (up to +6.02 dB before limiting), an optional **Extra Loudness** mode using DynamicProcessing multiband compressor, EQ voice clarity controls, a final limiter and slow gain ramp.

**Extra Loudness:** raises softer parts of selected local audio to sound fuller; it does not raise hardware speaker output above the device limit or guarantee louder sound on tracks already near maximum. Compression runs only where supported, and will not affect calls/Spotify/YouTube or other apps. Headphone signal boost is limited to 160%.

The limiter targets -2 dBFS digital peak reduction on supported devices, but cannot prevent hardware distortion or guarantee safe hearing levels. Keep system volume comfortable.

Build on GitHub Actions from the repository's main branch, open successful run, download `CrystalVolumeBooster-v1.4-APK` ZIP, and extract `app-debug.apk`. Debug builds can differ in signing keys between runs; an older debug build might need to be removed before installing.
