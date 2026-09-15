# Changelog

## [1.2.0] - 2026-09-16

### Fixed
- **Playback no longer fails to start on machines without Visual Studio.** The bundled
  native audio library was compiled in MSVC's Debug configuration and linked against
  `VCRUNTIME140D.dll` / `ucrtbased.dll` — debug-only runtimes that ship with Visual Studio
  and are not redistributable. On any other machine the library failed to load
  (`UnsatisfiedLinkError: Can't find dependent libraries`), leaving the play button disabled
  and double-clicks doing nothing. Rebuilt statically with MinGW: the DLL now depends only on
  `KERNEL32.dll` and `msvcrt.dll`, both present on every Windows install.
  **If you could not get any sound out of 1.0.x / 1.1.x, this release fixes it.**
- Dragging the floating lyrics position no longer writes the config file on every frame.

### Changed
- **Volume slider now follows a perceptual curve** (output gain = slider value squared).
  Linear gain made the lower half of the slider nearly useless — 50% still sounded about
  70% as loud. Stored config values and the UI still show the slider position; only the
  gain handed to the audio backend is mapped.
- Requires **NeoForge 21.1.200** or newer for Minecraft 1.21.1.

### Internal
- Removed the dead `NullSafetyUtils` wrapper (it null-checked APIs that never return null).
- Merged three copies of the lyrics-loading logic and two copies of the lyric binary search.
- Lyrics lookup by track is now a map lookup instead of scanning the whole library.
- Dropped the unused `FontCore` repository entry and leftover MDK template files.

## [1.1.0] - 2024-11-21

### What's New
- **More features**: Improved UI layout, better text rendering, and enhanced lyrics display
- **More languages**: Now supports 12 languages including Traditional Chinese, Korean, Japanese, Spanish, French, German, Russian, and LOLCAT!
