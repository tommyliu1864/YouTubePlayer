package com.my.customplayer

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.my.customplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater);
        setContentView(mBinding.root)


        mBinding.listView.apply {
            adapter = ArrayAdapter.createFromResource(
                this@MainActivity,
                R.array.movie_list,
                android.R.layout.simple_list_item_1
            )
            cacheColorHint = Color.TRANSPARENT
            setOnItemClickListener { parent, view, position, id ->
                mBinding.videoDetailView.apply {
                    mBinding.videoDetailView.open()
                }
            }
        }

    }

    override fun onBackPressed() {
        mBinding.videoDetailView.apply {
            if (state != VideoDetailView.State.COLLAPSED) {
                collapse()
            } else {
                super.onBackPressed()
            }
        }
    }


}