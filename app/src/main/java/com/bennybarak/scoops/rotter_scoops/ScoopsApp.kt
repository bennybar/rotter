package com.bennybarak.scoops.rotter_scoops

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.bennybarak.scoops.rotter_scoops.data.AIStore
import com.bennybarak.scoops.rotter_scoops.data.AuthService
import com.bennybarak.scoops.rotter_scoops.data.DiskCache
import com.bennybarak.scoops.rotter_scoops.data.DraftStore
import com.bennybarak.scoops.rotter_scoops.data.MyRepliesStore
import com.bennybarak.scoops.rotter_scoops.data.Prefs
import com.bennybarak.scoops.rotter_scoops.data.ReadStore
import com.bennybarak.scoops.rotter_scoops.data.ReadingStore
import com.bennybarak.scoops.rotter_scoops.data.SavedStore
import com.bennybarak.scoops.rotter_scoops.data.ScoopMetaCache
import com.bennybarak.scoops.rotter_scoops.data.SecureStore
import com.bennybarak.scoops.rotter_scoops.data.SettingsController
import com.bennybarak.scoops.rotter_scoops.net.Http
import com.bennybarak.scoops.rotter_scoops.net.PageCache
import com.bennybarak.scoops.rotter_scoops.net.RotterGated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import com.bennybarak.scoops.rotter_scoops.net.RotterService
import kotlinx.coroutines.launch

class ScoopsApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        SecureStore.init(this)
        DiskCache.dir = cacheDir
        PageCache.dir = java.io.File(cacheDir, "pages")
        RotterGated.appContext = this
        // Everything the first frame reads, loaded before it (as the Flutter
        // build did before runApp) — small, local reads.
        // A store whose saved data doesn't parse starts empty rather than
        // crashing every launch.
        fun guarded(name: String, load: () -> Unit) = try {
            load()
        } catch (e: Exception) {
            android.util.Log.e("ScoopsApp", "couldn't load $name", e)
        }
        guarded("settings") { SettingsController.load() }
        guarded("auth") { AuthService.load() }
        guarded("read") { ReadStore.load() }
        guarded("replies") { MyRepliesStore.load() }
        guarded("saved") { SavedStore.saved.load() }
        guarded("followed") { SavedStore.followed.load() }
        guarded("reading") { ReadingStore.load() }
        guarded("drafts") { DraftStore.load() }
        guarded("ai") { AIStore.load() }
        // Card metadata from the last session, so cards paint complete at once.
        ScoopMetaCache.instance.load()
        MainScope().launch(Dispatchers.IO) { AIStore.loadKeyState() }
        // Put the feed request on the wire now; the list picks it up when it
        // first loads, instead of starting it after the first frame.
        RotterService.launchFeed = MainScope().async(Dispatchers.IO) { RotterService.fetchScoops() }
    }

    // Images share the app's HTTP client (connection reuse) and play GIFs.
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(Http.client)
        .components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }
        .build()
}
