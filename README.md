## CraftMusic

CraftMusic is a client-side local music player for Minecraft (NeoForge). It plays audio files from your computer inside the game and shows synchronized lyrics. 

### What it does
- Plays local audio files placed in the game folder `CraftMusic/`.
- Supports ogg, mp3, flac, wav. Fully supports Chinese file names/paths on Windows.
- Provides an in-game UI for browsing and controlling playback, plus optional floating (on-screen) lyrics.

### Key features
- Playback: play/pause/resume, previous/next, seek by dragging, volume control, and modes: Sequential / Repeat All / Shuffle / Repeat One.
- UI: QuickPlay screen with track list (double-click to play), seekbar, time text, volume bar, mode switch, refresh, open folder, settings.
- Lyrics: auto-detects same-name `.lrc` files (UTF-8/GBK); centered panel with current line highlight and gentle scaling; shows “No lyrics / Instrumental” when absent.
- Floating lyrics: toggle on/off; render scope Global or In-World only; position Top-Left/Top-Right/Bottom-Left/Bottom-Right (left edges left-aligned, right edges right-aligned).
- Keybinding: default M opens the UI. Also a title-screen button.
- Commands: `/craftmusic play <path>`, `/craftmusic stop`, `/craftmusic ui`, `/craftmusic tone`.
- Persistence: settings saved to `config/craftmusic-client.json`.

### Supported Languages
CraftMusic now supports **12 languages** for a truly global experience:
- **English** (en_us) 🇬🇧
- **Simplified Chinese** (zh_cn) 🇨🇳  
- **Traditional Chinese - Hong Kong** (zh_hk) 🇭🇰
- **Traditional Chinese - Taiwan** (zh_tw) 🇹🇼
- **Classical Chinese** (lzh) - Literary style
- **Korean** (ko_kr) 🇰🇷
- **Japanese** (ja_jp) 🇯🇵
- **Spanish** (es_es) 🇪🇸
- **French** (fr_fr) 🇫🇷
- **German** (de_de) 🇩🇪
- **Russian** (ru_ru) 🇷🇺
- **LOLCAT** (lol_us) - I can has mewsic!

### How to use
1) Place your audio files into `<Minecraft Game Directory>/CraftMusic/`.
2) Launch the game. Press M or click the title-screen button “CraftMusic” to open the player.
3) Double-click a song to play. Use bottom controls to seek, switch tracks, adjust volume, and change mode.
4) Lyrics: put a same-named `.lrc` next to the audio file. If found, lyrics sync automatically.
5) Floating lyrics: open Settings to control on/off, render scope (Global / In-World), and position.

### Configuration (config/craftmusic-client.json)
- `volume`: float 0.0–1.0
- `mode`: `SEQUENTIAL` | `REPEAT_ALL` | `SHUFFLE` | `REPEAT_ONE`
- `lyricEffects`: boolean, enables lyric scrolling/scale effects
- `floatingLyrics`: boolean, floating lyrics on/off
- `floatingLyricsRender`: `GLOBAL` (all screens) | `WORLD` (only in-game)
- `floatingLyricsPosition`: `TOP_LEFT` | `TOP_RIGHT` | `BOTTOM_LEFT` | `BOTTOM_RIGHT`

### Requirements
- Client-side, NeoForge-based Minecraft.
- Windows only. The native audio backend (miniaudio) is distributed as a Windows DLL.

### Limitations
- Windows platform only.

### Credits
- Audio backend: [miniaudio](https://miniaud.io/).

### Troubleshooting
- “Audio output not ready”: ensure the native DLL is properly loaded (Windows) and restart the game.
- Chinese filenames not playing: the mod uses wide-character APIs on Windows; ensure the file exists and is readable.
