# Changelog (English)

The same as CHANGELOG.md (Spanish, the original), in English: the app shows it when it's in English.

## 1.7.1

- **Quick menu:** holding her now opens a short menu with what you use most: talk, look at your screen, teleport,
  satellite, put her away, a few moves (hop, spin, stretch…) and her modes as switches (live, ghost, icon, no
  gravity, gamer). From there, *All settings*.
- **Clearer settings:** the full menu is sorted into sections (her AI, her, your phone, more) and the screen behind
  dims (and blurs on Android 12 or newer), so it reads over any app.
- **AI model in two steps:** first you pick the service (Gemini, Groq, NVIDIA…), then its model inside it, without
  the long mixed-up list.
- **Her panel no longer shows up without a background on older phones:** on some (with an Android WebView older than
  version 111) her panels and cards came out transparent, with black text over your wallpaper. Now her colors show
  on all of them.
- **With a Gemini key she answers much faster:** Gemini’s Flash models “think” before answering, sometimes so long
  that she sat on “Let me think…” and her chat gave up. Now she asks them to think less, like with the free AI.

## 1.7.0

- **Icon mode:** only her green mask stays on screen, eyes open, and she keeps commenting from there (what she sees
  on your screen, cheers in gamer mode, her chat, a line now and then). Drag it with your finger wherever it bothers
  you least; tap it to open her menu. So you can keep her big and, whenever you want, have her take up almost
  nothing. Turn it on in *Settings*, in her menu or by saying “Capri, icon mode” (and “Capri, icon mode off”).
- **No gravity:** she stays right where you put her, even in the air. She doesn’t wander off on her own, doesn’t jump
  onto your notifications, and if you fling her she doesn’t fly away, so she doesn’t get mad anymore. Great for
  leaving her in a corner or next to your chats. In *Settings* or her menu; she remembers where you left her.
- **She remembers you:** after a chat, Capri notes down what matters about you (what you like, the people you tell
  her about, what’s going on with you, how things went) and remembers it next time, so you pick up where you left
  off instead of starting from scratch. See it and delete it in *Settings → What she remembers about you* (one thing
  or everything), and turn it off there too. It stays only on your phone; to note it down she asks her AI once per
  chat, once you’re no longer talking with her (with the free AI she waits her turn so you don’t have to wait).
- **NVIDIA AI:** you can now give her a free NVIDIA key (build.nvidia.com), with lots of models to pick from. She
  picks a fast one to chat and, to see your screen, an NVIDIA one that can see pictures. If a model is gone, she
  moves on to the next one by herself.
- **More languages:** in *Settings → Idioma · Language* pick Automatic, Español, English, Français, Português or
  Русский, for the app and for what she says. Voice commands (“Capri, …”) work in those languages too.
- **New Community tab** in the app’s bottom bar: the Discord and the TikTok are there now (they used to be at the
  end of *Settings*), and that’s where new community things will go.
- **She comments on what you watch:** with “Watch my screen live” on, when you switch videos on TikTok, Reels or
  YouTube she gives her opinion on what she sees (at most once every 50 seconds).
- **She can see your screen with more AIs:** if your key is from a service whose chat model can’t see pictures (Groq,
  for example), she looks with one from the same service that can.
- **She no longer vanishes when a notification arrives:** she jumps to where the card ends up, not where its
  animation starts.
- **The keyboard no longer closes on its own** while you type to her: she doesn’t move while her chat is open.
- **She connects on older phones:** on some with Android 8 (like the Sony Xperia XA1) setting up her AI failed with
  “Can't create handler inside thread that has not called Looper.prepare()”. Not anymore.
- **Errors you can understand:** instead of technical messages like “messages content must be a string” or “signal
  lost”, she tells you in plain words what happened.
- **Voice chat:** if the AI takes a while to answer, she says out loud that she’s thinking, instead of going silent.

## 1.6.2

- **Join the community:** the community’s Discord and Emma’s TikTok, at the end of *Settings* and at the end of
  Capri’s menu. The first time you open the app it shows them to you, just once.

## 1.6.1

- **Automatic gamer mode now really notices when you leave the game:** it used to wait 20 seconds and sometimes
  missed it (for example if the phone didn’t say which app was in front after you left, or with Samsung’s game
  tools). Now it also notices when the game goes away, ignores floating panels like the notification shade or Game
  Booster, and turns off 10 seconds after you leave.
- *Settings → Log* now notes which app she sees in front and when gamer mode turns on or off, in case some game
  isn’t detected properly.

## 1.6.0

- **Gamer mode (Capri’s own idea):** for when you play. She gets tiny in a corner, can’t be touched (your taps reach
  the game), her notices and music bar stay away, she stops listening and doesn’t wander. Now and then she cheers
  you on. Turn it on in *Settings → Gamer mode*, in her menu, by saying “Capri, gamer mode” or typing it in her
  chat. It turns off the same way (in her chat: “gamer mode off”) or when you restart her, and when you’re done she
  asks how it went.
- **Automatic gamer mode (optional):** in *Settings → Gamer mode → On its own when a game opens*, it turns on when you
  open a game and off when you leave (if you turn it off mid-game, she respects that). It needs Android’s “Usage
  access” permission, which only shows which app is open. It’s off by default: some banking and antivirus apps
  distrust that permission.

## 1.5.1

- **Your videos and music no longer pause:** with “Talk to her” on, every time Capri started listening again the
  phone gave her the sound and paused what you were watching. Now she doesn’t listen while a video or music plays
  (*Settings → Talk to her* says so) and comes back on her own when it ends. For music, use her bar or type to her.
- **“Paste” button for your AI key:** in her panel, long-pressing doesn’t let you paste. Now there’s a *Paste* button
  that puts in what you copied and checks it right away. The app also has *Paste key*.
- **A key set from the app no longer keeps asking for another:** if her panel was asking for the key and you set it
  from the app, the panel goes away on its own.
- **Music bar your way:** in *Settings → Music bar* choose *Always* (stays while music plays), *A while* (as before)
  or *Never*.
- **Notices higher up:** your notification cards no longer show up mid-screen, but at the top, right where Capri
  can still climb on them.
- **Capri no longer gets lost:** sometimes she peeked from the top edge, which on a phone is covered by the status
  bar, and it looked like she’d disappeared. Now she only peeks from the sides, and never stays hidden.
- **Cleaner notification:** it no longer repeats every notification or song you get (Android already shows them),
  it says “On your screen. Tap here for my menu.”, and the last thing she said goes away after 10 minutes.

## 1.5.0

- **Capri no longer shoots off:** when you tapped her or let go after dragging, she sometimes flew off as if you’d
  thrown her. Now she’s only thrown if you really fling her with a quick move; if you just let go, she drops softly.
  And when you throw her, she goes with less force.
- **Pick her size:** in *Settings → Capri on your screen*, from mini to huge.
- **Ghost mode:** Capri stays on your screen but can’t be touched and doesn’t get in the way: your taps go through to
  the app below, like a ghost shimeji. Turn it on in *Settings* or her menu.
- **More movement:** she no longer stands still. Choose how restless she is in *Settings → Movement*: calm (as
  before), normal or restless. She walks, jumps, spins, stretches and looks around on her own.
- **Capri sees your screen:** tell her “Capri, look at my screen” (or tap *Look at my screen now* in Settings, or in
  her menu) and she comments on what she sees. With *Watch live* she keeps looking and reacts on her own now and
  then, or when you switch apps, until you turn it off. Android asks you each time she starts looking, and you’ll see
  the cast icon while she does. Screenshots aren’t saved: they only go to her AI. Protected apps (banks, passwords)
  look black to her. It needs an AI that sees pictures, like Gemini (its key is free); the free AI can’t see, and
  Capri tells you so.
- **The chat no longer freezes:** we found out why it was slow: the free AI only allows one answer every 45 seconds
  per phone, and if you wrote before that, it waited or failed. Now Capri waits her turn and asks again on her own,
  asks the AI to think less so it answers faster, and meanwhile tells you she’s thinking instead of going silent. If
  you want instant answers, give her a free Gemini key.
- **She picks a better model on her own:** she prefers fast models that really are chat models, and remembers the
  list for a few minutes so she doesn’t ask every time.
- **Your AI key no longer gets erased:** sometimes, if the AI was slow or failed once, Capri dropped the key and said
  she had no AI. Now she only removes it if it’s really invalid.
- **She talks more like Capri:** in the chat she answers like someone you’re texting, not like an assistant: she
  reacts, jokes, gives opinions and answers what you actually said. In Spanish she talks like a Latina, with some
  Mexican slang.

## 1.4.1

- **No more dead zones next to Capri:** after she teleported, tapping near her (not on her) did nothing, neither
  Capri nor the app below, until you moved her. Her touch area now follows the outline of her body instead of a big
  box around her, and while she teleports it blocks nothing.
- **Updating from the app works on more phones:** on some (like Samsung with Android 16) installing failed with
  “Self update is blocked”. Now the new version opens on Android’s install screen, just as if you opened it from the
  browser. If that still fails, its page opens so you can download it.

## 1.4.0

- **Animated cards:** your notification cards slide in with a glitch and leave the same way, instead of popping in
  and out. A little red bar at the bottom shows how long they’ll stay.
- **The music bar isn’t always there anymore:** it shows up when a song starts, changes, or you pause or resume it,
  and leaves on its own after 15 seconds. If you touch it, it stays a bit longer. Paused, it leaves after 12 seconds.
- **New “Commands” tab:** everything you can ask Capri, out loud or typed in her chat, with examples.
- **Update from the app again:** *Update* downloads and installs the new version right there, without going through
  the browser (Android asks you to confirm). The first time it asks you to let Capri install apps.
- **System notice when there’s a new version:** the app checks every 3 hours, even with Capri off, and sends you a
  notification. Tapping it opens the app and starts the update.
- **Folded news:** each version shows closed, with how many changes it brings; tap it to read it.
- **Smoother “Talk to her”:** when you only say “Capri”, she answers in her panel without speaking, so she doesn’t
  cover what you say next, and she waits a bit longer for you to finish your sentence.
