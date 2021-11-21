package com.fengyuhe.music

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.fragment.app.FragmentActivity
import com.fengyuhe.music.databinding.MainFragmentBinding
import android.content.Context
import android.widget.AdapterView
import java.io.IOException

class MainActivity : FragmentActivity() {

    private val mediaPlayer = MediaPlayer()
    var mBinding: MainFragmentBinding? = null
    var isSeekBarChange = false
    var nowPlaying = ""
    private val data = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MainFragmentBinding.inflate(layoutInflater)
        setContentView(mBinding!!.root)

        initListView()
        initSeekBarView()

        getMusicsFromAssets(this, "")

        mBinding!!.play.setOnClickListener {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                updateSeekBar()
                mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.pause))
            } else if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.play))
            }
        }

        mBinding!!.stop.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.reset()
                initMediaPlayer()
            }
        }
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val data: Bundle = msg.data
            var currentPosition = data.getInt("cur rentPosition")

            mBinding!!.tvStart.setText(calculateTime(currentPosition / 1000))
            mBinding!!.seekbar.progress = (currentPosition.toDouble() / mediaPlayer.duration.toDouble() * 100).toInt()
        }
    }

    private fun initListView() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, data)
        mBinding!!.musicList.adapter = adapter
        mBinding!!.musicList.setOnItemClickListener { _, _, position, _ ->
            nowPlaying = data[position]
            initMediaPlayer()
        }
    }

    private fun initMediaPlayer() {
        var assetManager = assets
        val fd = assetManager.openFd(nowPlaying)
        mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        mediaPlayer.prepare()

        var duration = mediaPlayer.duration
        var position = mediaPlayer.currentPosition
        mBinding!!.tvStart.setText(calculateTime(position / 1000))
        mBinding!!.tvEnd.setText(calculateTime(duration / 1000))
    }

    private fun initSeekBarView() {
        mBinding!!.seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val duration = mediaPlayer.duration / 1000
                val position = mediaPlayer.currentPosition
                mBinding!!.tvStart.setText(calculateTime(position / 1000))
                mBinding!!.tvEnd.setText(calculateTime(duration))
                mBinding!!.tvStart.setText(calculateTime(mediaPlayer.duration / 100 * p0!!.progress / 1000))
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                isSeekBarChange = true

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                isSeekBarChange = false
                mediaPlayer.seekTo(mediaPlayer.duration / 100 * p0!!.progress)
                mBinding!!.tvStart.setText(calculateTime(mediaPlayer.currentPosition / 1000))
            }
        })
    }

    private fun getMusicsFromAssets(context: Context, path: String) {
        val assetManager = context.assets
        var musics: Array<String>? = null
        try {
            musics = assetManager.list(path)
            println(musics)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (musics != null) {
            for (indices: String in musics) {
                data.add(indices)
            }
            println(data)
        }
    }

    private fun updateSeekBar() {
        Thread {
            while (true) {
                try {
                    Thread.sleep(1000)
                } catch (error: InterruptedException) {
                    error.printStackTrace()
                }

                var currentPosition = mediaPlayer.currentPosition
                var message = Message.obtain()
                var bundle = Bundle()
                bundle.putInt("currentPosition", currentPosition)
                message.data = bundle
                this.handler.sendMessage(message)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    private fun calculateTime(time: Int): String {
        var minute = 0
        var second = 0
        if (time >= 60) {
            minute = time / 60
            second = time % 60
            return if (minute < 10) {
                if (second < 10) {
                    "0$minute:0$second"
                } else {
                    "0$minute:$second"
                }
            } else {
                if (second < 10) {
                    "$minute:0$second"
                } else {
                    "$minute:$second"
                }
            }
        } else {
            second = time
            return if (second in 0..9) {
                "00:0$second"
            } else {
                "00:$second"
            }
        }
    }
}