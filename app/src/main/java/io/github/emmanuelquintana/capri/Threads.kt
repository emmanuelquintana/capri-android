package io.github.emmanuelquintana.capri

import android.os.Looper
import java.util.concurrent.ThreadFactory

/**
 * Her background threads, each with a Looper of its own. On some phones (Android 8, a Sony Xperia
 * XA1) the system's own code makes a Handler on whatever thread calls it, and on a plain thread
 * that fails with "Can't create handler inside thread that has not called Looper.prepare()": her
 * setup said so instead of connecting.
 */
object Threads {
    val factory = ThreadFactory { r ->
        Thread {
            if (Looper.myLooper() == null) Looper.prepare()
            r.run()
        }
    }

    /** Like `Thread { … }`, not started yet. */
    fun make(block: () -> Unit): Thread = factory.newThread(block)
}
