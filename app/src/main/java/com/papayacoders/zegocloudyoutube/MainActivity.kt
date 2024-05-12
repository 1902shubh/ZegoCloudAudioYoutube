package com.papayacoders.zegocloudyoutube

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.papayacoders.zegocloudyoutube.databinding.ActivityMainBinding
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.constants.ZegoPlayerState
import im.zego.zegoexpress.constants.ZegoPublisherState
import im.zego.zegoexpress.constants.ZegoRoomStateChangedReason
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoUpdateType
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoStream
import im.zego.zegoexpress.entity.ZegoUser
import org.json.JSONObject
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var userId: String
    private lateinit var userName: String
    private val roomId: String = "test_call_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        userId = generateUserId()

        userName = "${userId}_name"





        binding.button.setOnClickListener {

            createEngine()
        }

        binding.button2.setOnClickListener {
            stopCall()
        }


    }


    private fun createEngine() {
        val profile = ZegoEngineProfile()

        profile.appID = Utils.APP_ID
        profile.appSign = Utils.SIGN_KEY
        profile.scenario = ZegoScenario.DEFAULT
        profile.application = application


        ZegoExpressEngine.createEngine(profile, null)

        loginRoom()

        startEventListener()

    }

    private fun destroyEngine() {
        ZegoExpressEngine.destroyEngine(null)
    }

    private fun startEventListener() {
        ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler() {
            override fun onRoomStreamUpdate(
                roomID: String?,
                updateType: ZegoUpdateType?,
                streamList: ArrayList<ZegoStream>?,
                extendedData: JSONObject?
            ) {
                super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData)


                if (updateType == ZegoUpdateType.ADD) {
                    startPlayStream(streamList!![0].streamID)
                } else stopPlayStream(streamList!![0].streamID)


            }

            override fun onRoomUserUpdate(
                roomID: String?,
                updateType: ZegoUpdateType?,
                userList: ArrayList<ZegoUser>?
            ) {
                super.onRoomUserUpdate(roomID, updateType, userList)

                if (updateType == ZegoUpdateType.ADD) {

                    for (user in userList!!) {
                        Toast.makeText(this@MainActivity, "Logged in to room", Toast.LENGTH_SHORT)
                            .show()
                    }

                } else if (updateType == ZegoUpdateType.DELETE) {

                    for (user in userList!!) {
                        Toast.makeText(
                            this@MainActivity,
                            "Logged out from room",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


            }

            override fun onRoomStateChanged(
                roomID: String?,
                reason: ZegoRoomStateChangedReason?,
                errorCode: Int,
                extendedData: JSONObject?
            ) {
                super.onRoomStateChanged(roomID, reason, errorCode, extendedData)

                if (reason == ZegoRoomStateChangedReason.LOGIN_FAILED) {
                    Toast.makeText(
                        this@MainActivity,
                        "ZegoRoomStateChangedReason>LoginFailed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onPublisherStateUpdate(
                streamID: String?,
                state: ZegoPublisherState?,
                errorCode: Int,
                extendedData: JSONObject?
            ) {
                super.onPublisherStateUpdate(streamID, state, errorCode, extendedData)
                if (errorCode == 0) {

                }

                if (state == ZegoPublisherState.PUBLISHING) {

                } else if (state == ZegoPublisherState.NO_PUBLISH) {
                    Toast.makeText(
                        this@MainActivity,
                        "ZegoPublisherState.NO_PUBLISH",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            }

            override fun onPlayerStateUpdate(
                streamID: String?,
                state: ZegoPlayerState?,
                errorCode: Int,
                extendedData: JSONObject?
            ) {
                super.onPlayerStateUpdate(streamID, state, errorCode, extendedData)


                if (errorCode != 0) {
                    Toast.makeText(
                        this@MainActivity,
                        "onPlayerStateUpdate state : $errorCode", Toast.LENGTH_SHORT
                    ).show()
                }

                if (state == ZegoPlayerState.NO_PLAY) {
                    Toast.makeText(this@MainActivity, "ZegoPlayerState.NO_PLAY", Toast.LENGTH_SHORT)
                        .show()
                }

            }
        })


    }


    private fun loginRoom() {
        val user = ZegoUser(userId, userName)
        val roomConfig = ZegoRoomConfig()

        roomConfig.isUserStatusNotify = true
        ZegoExpressEngine.getEngine().loginRoom(
            roomId, user, roomConfig
        ) { error: Int, extendedDate: JSONObject? ->
            if (error == 0) {
                Toast.makeText(this, "User login", Toast.LENGTH_SHORT).show()
                startPublish()
            } else {
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
            }

        }
    }


    private fun startPublish() {
        val streamId = "room_${userId}_$roomId"

        ZegoExpressEngine.getEngine().enableCamera(false)
        ZegoExpressEngine.getEngine().startPublishingStream(streamId)
    }

    private fun stopPublish() {
        ZegoExpressEngine.getEngine().stopPublishingStream()
    }

    private fun startPlayStream(streamId: String) {
        ZegoExpressEngine.getEngine().startPlayingStream(streamId)
    }

    private fun stopPlayStream(streamId: String) {
        ZegoExpressEngine.getEngine().stopPlayingStream(streamId)
    }

    private fun stopCall() {
        stopPublish()
        logoutRoom()
        destroyEngine()
    }

    private fun logoutRoom() {

        ZegoExpressEngine.getEngine().logoutRoom()
    }


    private fun generateUserId(): String {
        val builder = StringBuilder()
        val random = Random()

        while (builder.length < 5) {
            val nextInt = random.nextInt(10)
            if (builder.isEmpty() && nextInt == 0) {
                continue
            }
            builder.append(nextInt)
        }

        return builder.toString()
    }


    override fun onDestroy() {
        super.onDestroy()
        destroyEngine()
    }

}