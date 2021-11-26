package com.fengyuhe.music

import androidx.appcompat.app.AppCompatActivity
import android.media.MediaPlayer
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.fragment.app.FragmentActivity
import com.fengyuhe.music.databinding.MainFragmentBinding
import androidx.appcompat.widget.Toolbar
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.jar.Manifest
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {

    private var mediaPlayer = MediaPlayer()
    var mBinding: MainFragmentBinding? = null
    var isSeekBarChange = false
    var nowPlaying = ""
    private val data = ArrayList<String>()
    private val mode = arrayOf(R.drawable.line, R.drawable.loop, R.drawable.random)
    private val modeName = arrayOf("line", "loop", "random")
    private var isKeepingPlaying = false
    private var modeId = 0
    private var SDlist = ArrayList<String>()
    private var sumMusic = ArrayList<String>()
    private var musicName: File? = null
    var adapter: ArrayAdapter<String>? = null

    private val PERMISSION_STORAGE = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MainFragmentBinding.inflate(layoutInflater)
        setContentView(mBinding!!.root)

        initListView()
        initSeekBarView()
        getMusicsFromAssets(this, "")
        setSupportActionBar(findViewById(R.id.toolBar))
        mBinding!!.toolBar.inflateMenu(R.menu.actionbar_menu)

        mBinding!!.previous.setOnClickListener {
            if (isKeepingPlaying) {
                stopMediaPlayer()
                isKeepingPlaying = false
                println("Now Playing: $nowPlaying")
                mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.play))
            }
            var index = 0
            if (sumMusic.indexOf(nowPlaying) - 1 < 0) {
                index = 0
            } else {
                index = sumMusic.indexOf(nowPlaying) - 1
            }
            nowPlaying = sumMusic[index]
            println("Now Playing: $nowPlaying")
            initMediaPlayer()

            println(modeName[modeId])

            if (modeName[modeId] == "random") {
                val num = (0 until sumMusic.size).random()
                nowPlaying = sumMusic[num]
                println("isRandom to $nowPlaying")
            } else if (modeName[modeId] == "line") {
                println(sumMusic.indexOf(nowPlaying))
                val num = if (sumMusic.indexOf(nowPlaying) - 1 < 0) 0 else (sumMusic.indexOf(nowPlaying) - 1) % (sumMusic.size - 1)
                println(num)
                nowPlaying = sumMusic[num]
            }

            println("Now Playing: $nowPlaying")
            initMediaPlayer()
            if (isKeepingPlaying) {
                playMusic()
            }

        }

        mBinding!!.play.setOnClickListener {
            if (nowPlaying == "") {
                nowPlaying = sumMusic.first()
                initMediaPlayer()
            }

            playMusic()
        }

        mBinding!!.next.setOnClickListener {
            if (isKeepingPlaying) {
                stopMediaPlayer()
                isKeepingPlaying = false
                println("Now Playing: $nowPlaying")
                mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.play))
            }

            println(modeName[modeId])

            if (modeName[modeId] == "random") {
                val num = (0 until sumMusic.size).random()
                nowPlaying = sumMusic[num]
                println("isRandom to $nowPlaying")
            } else if (modeName[modeId] == "line") {
                println(sumMusic.indexOf(nowPlaying))
                val num = (sumMusic.indexOf(nowPlaying) + 1) % (sumMusic.size - 1)
                println(num)
                nowPlaying = sumMusic[num]
            }

            println("Now Playing: $nowPlaying")
            initMediaPlayer()
            if (isKeepingPlaying) {
                playMusic()
            }
        }

        mBinding!!.mode.setImageDrawable(resources.getDrawable(mode.first()))
        mBinding!!.mode.setOnClickListener {
            modeId = (modeId + 1) % 3
            mBinding!!.mode.setImageDrawable(resources.getDrawable(mode[modeId]))
        }

        mediaPlayer.setOnCompletionListener {
            if (isKeepingPlaying) {
                stopMediaPlayer()
                println("Now Playing: $nowPlaying")
                mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.play))
            }
            if (modeName[modeId] == "random") {
                val num = (0 until sumMusic.size).random()
                nowPlaying = sumMusic[num]
                println("isRandom to $nowPlaying")
            } else if (modeName[modeId] == "line") {
                println(sumMusic.indexOf(nowPlaying))
                val num = (sumMusic.indexOf(nowPlaying) + 1) % (sumMusic.size - 1)
                println(num)
                nowPlaying = sumMusic[num]
            }
            initMediaPlayer()
            playMusic()
        }
    }

    private fun playMusic() {
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
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sumMusic)
        mBinding!!.musicList.adapter = adapter
        mBinding!!.musicList.setOnItemClickListener { _, _, position, _ ->
            if (sumMusic[position] != nowPlaying) {
                if (isKeepingPlaying) {
                    stopMediaPlayer()
                    println("Now Playing: $nowPlaying")
                    mBinding!!.play.setImageDrawable(resources.getDrawable(R.drawable.play))
                }
                nowPlaying = sumMusic[position]
                println("Now Playing: $nowPlaying")
                initMediaPlayer()
            }
        }
    }

    private fun initMediaPlayer() {
        var assetManager = assets
        if (data.contains(nowPlaying)) {
            var fd = assetManager.openFd(nowPlaying)
            try {
                mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            } catch (e: IllegalStateException) {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
        } else if (SDlist.contains(nowPlaying)) {
            try {
                mediaPlayer.setDataSource("$musicName/$nowPlaying")
            } catch (e: IllegalStateException) {
                mediaPlayer.reset()
                mediaPlayer.setDataSource("$musicName/$nowPlaying")
            }
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
                    sumMusic.add(indices)
                }
            }
            println(sumMusic)
        }
    }

    private fun getMusicFromDisk() {
        var permission = ActivityCompat.checkSelfPermission(this, PERMISSION_STORAGE[0])
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSION_STORAGE, 0)
        }
        musicName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        println(musicName)
        try {
            for (music in musicName!!.list()) {
                if (music.takeLast(3) == "mp3") {
                    SDlist.add(music)
                    sumMusic.add(music)
                }
            }
            println(sumMusic)
            adapter!!.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.i("TAG", "读取文件异常 $e")
        }
    }

    private fun updateListView() {

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.actionbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.scan -> {
            getMusicFromDisk()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }
}