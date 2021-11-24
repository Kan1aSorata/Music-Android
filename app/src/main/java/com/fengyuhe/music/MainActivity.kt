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
import androidx.appcompat.widget.Toolbar
import android.content.Context
import java.io.IOException
import java.lang.IllegalStateException

class MainActivity : FragmentActivity() {

    private var mediaPlayer = MediaPlayer()
    var mBinding: MainFragmentBinding? = null
    var isSeekBarChange = false
    var nowPlaying = ""
    private val data = ArrayList<String>()
    private var isKeepingPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MainFragmentBinding.inflate(layoutInflater)
        setContentView(mBinding!!.root)

        initListView()
        initSeekBarView()
        getMusicsFromAssets(this, "")

        mBinding!!.previous.setOnClickListener {
            if (isKeepingPlaying) {
                stopMediaPlayer()
                println("Now Playing: $nowPlaying")
                mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.play))
            }
            var index = 0
            if (data.indexOf(nowPlaying) - 1 < 0) {
                index = 0
            } else {
                index = data.indexOf(nowPlaying) - 1
            }
            nowPlaying = data[index]
            println("Now Playing: $nowPlaying")
            initMediaPlayer()
        }

        mBinding!!.play.setOnClickListener {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                isKeepingPlaying = true
                updateSeekBar()
                mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.pause))
            } else if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.play))
            }
        }

        mBinding!!.next.setOnClickListener {
            if (isKeepingPlaying) {
                stopMediaPlayer()
                println("Now Playing: $nowPlaying")
                mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.play))
            }
            var index = 0
            if (data.indexOf(nowPlaying) + 1 >= data.size) {
                index = data.lastIndex
            } else {
                index = data.indexOf(nowPlaying) + 1
            }
            nowPlaying = data[index]
            println("Now Playing: $nowPlaying")
            initMediaPlayer()
        }

        mBinding!!.stop.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                isKeepingPlaying = false
                stopMediaPlayer()
            }
        }
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val data: Bundle = msg.data
            val currentPosition = data.getInt("currentPosition")

            mBinding!!.tvStart.setText(calculateTime(currentPosition / 1000))
            mBinding!!.seekbar.progress = (currentPosition.toDouble() / mediaPlayer.duration.toDouble() * 100).toInt()
        }
    }

    private fun initListView() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, data)
        mBinding!!.musicList.adapter = adapter
        mBinding!!.musicList.setOnItemClickListener { _, _, position, _ ->
            if (data[position] != nowPlaying) {
                if (isKeepingPlaying) {
                    stopMediaPlayer()
                    println("Now Playing: $nowPlaying")
                    mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.play))
                }
                nowPlaying = data[position]
                println("Now Playing: $nowPlaying")
                initMediaPlayer()
            }
        }
    }

    private fun initMediaPlayer() {
        var assetManager = assets
        val fd = assetManager.openFd(nowPlaying)
        try {
            mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        } catch (e: IllegalStateException) {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        }
        mediaPlayer.prepare()
        mBinding!!.musicName.text = nowPlaying

        var duration = mediaPlayer.duration
        var position = mediaPlayer.currentPosition
        mBinding!!.tvStart.setText(calculateTime(position / 1000))
        mBinding!!.tvEnd.setText(calculateTime(duration / 1000))
    }

    private fun initSeekBarView() {
        mBinding!!.seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                var duration = 0
                var position = 0
                duration = mediaPlayer.duration / 1000
                position = mediaPlayer.currentPosition
                mBinding!!.tvStart.setText(calculateTime(position / 1000))
                mBinding!!.tvEnd.setText(calculateTime(duration))
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
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (musics != null) {
            for (indices: String in musics) {
                if (indices.takeLast(3) == "mp3") {
                    data.add(indices)
                }
            }
            println(data)
        }
    }

    private fun updateSeekBar() {
        Thread {
            while (isKeepingPlaying) {
                try {
                    Thread.sleep(1000)
                } catch (error: InterruptedException) {
                    error.printStackTrace()
                }
                var currentPosition = 0
                currentPosition = try {
                    mediaPlayer.currentPosition
                } catch (e: IllegalStateException) {
                    println("Go next line")
                    mediaPlayer.reset()
                    mediaPlayer.currentPosition
                }
                println(currentPosition)
                val message = Message.obtain()
                val bundle = Bundle()
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

    private fun stopMediaPlayer() {
        mediaPlayer.stop()
        mediaPlayer.reset()
    }
}