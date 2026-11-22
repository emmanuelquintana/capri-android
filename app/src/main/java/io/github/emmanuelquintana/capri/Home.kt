package io.github.emmanuelquintana.capri

/**
 * (1.11.5) The app's screen, as Android starts it. All of it is MainActivity; this name only exists so that
 * ".MainActivity" can be the launcher entry (an activity-alias, see AndroidManifest.xml) that's switched off when
 * you pick another icon (AppIcon.kt), while the app itself (notifications, widget, share) always opens here.
 * The launcher entry keeps its old name, so your home screen icon stays where it was after updating.
 */
class Home : MainActivity()
