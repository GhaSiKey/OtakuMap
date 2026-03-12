package com.gaoshiqi.otakumap

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.gaoshiqi.room.AnimeMarkRepository
import com.gaoshiqi.room.RecentViewRepository
import com.gaoshiqi.room.SavedPointRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics

class BangumiApplication: Application() {
    lateinit var animeMarkRepository: AnimeMarkRepository
    lateinit var savedPointRepository: SavedPointRepository
    lateinit var recentViewRepository: RecentViewRepository

    override fun onCreate() {
        super.onCreate()
        animeMarkRepository = AnimeMarkRepository(this)
        savedPointRepository = SavedPointRepository(this)
        recentViewRepository = RecentViewRepository(this)
        registerCrashlyticsScreenTracker()
    }

    private fun registerCrashlyticsScreenTracker() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                FirebaseCrashlytics.getInstance()
                    .setCustomKey("current_screen", activity.javaClass.simpleName)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}