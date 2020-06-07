package com.exmple.musicplayer.adapters

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.exmple.musicplayer.R
import com.exmple.musicplayer.activities.SimpleActivity
import com.exmple.musicplayer.fragments.SongsFragment

class ViewPagerAdapter(val activity: SimpleActivity) : PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layout = R.layout.fragment_songs
        val view = activity.layoutInflater.inflate(layout, container, false)
        container.addView(view)

        (view as? SongsFragment)?.apply {
            setupFragment(activity)
        }

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        container.removeView(item as View)
    }

    override fun getCount() = 1

    override fun isViewFromObject(view: View, item: Any) = view == item
}
