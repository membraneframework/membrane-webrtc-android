package org.membraneframework.rtc.dagger

import android.content.Context
import dagger.Module
import dagger.Provides
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import timber.log.Timber
import javax.inject.Named
import javax.inject.Singleton

@Module
object RTCModule {
    @Singleton
    @Provides
    fun eglBase(): EglBase {
        return EglBase.create()
    }

    @Provides
    fun eglContext(eglBase: EglBase): EglBase.Context = eglBase.eglBaseContext

    @Singleton
    @Provides
    fun audioDeviceModule(appContext: Context): AudioDeviceModule {
        val audioRecordErrorCallback = object : JavaAudioDeviceModule.AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String?) {
                Timber.e ( "onWebRtcAudioRecordInitError: $errorMessage" )
            }

            override fun onWebRtcAudioRecordStartError(
                errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                errorMessage: String?
            ) {
                Timber.e ( "onWebRtcAudioRecordStartError: $errorCode. $errorMessage" )
            }

            override fun onWebRtcAudioRecordError(errorMessage: String?) {
                Timber.e ( "onWebRtcAudioRecordError: $errorMessage" )
            }
        }

        val audioTrackErrorCallback = object : JavaAudioDeviceModule.AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String?) {
                Timber.e ( "onWebRtcAudioTrackInitError: $errorMessage" )
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                errorMessage: String?
            ) {
                Timber.e ( "onWebRtcAudioTrackStartError: $errorCode. $errorMessage" )
            }

            override fun onWebRtcAudioTrackError(errorMessage: String?) {
                Timber.e ( "onWebRtcAudioTrackError: $errorMessage" )
            }

        }
        val audioRecordStateCallback: JavaAudioDeviceModule.AudioRecordStateCallback = object :
            JavaAudioDeviceModule.AudioRecordStateCallback {
            override fun onWebRtcAudioRecordStart() {
                Timber.i ( "Audio recording starts" )
            }

            override fun onWebRtcAudioRecordStop() {
                Timber.i ( "Audio recording stops" )
            }
        }

        // Set audio track state callbacks.
        val audioTrackStateCallback: JavaAudioDeviceModule.AudioTrackStateCallback = object :
            JavaAudioDeviceModule.AudioTrackStateCallback {
            override fun onWebRtcAudioTrackStart() {
                Timber.i ( "Audio playout starts" )
            }

            override fun onWebRtcAudioTrackStop() {
                Timber.i ( "Audio playout stops" )
            }
        }

        return JavaAudioDeviceModule.builder(appContext)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .setAudioRecordStateCallback(audioRecordStateCallback)
            .setAudioTrackStateCallback(audioTrackStateCallback)
            .createAudioDeviceModule()
    }

    @Provides
    fun videoEncoderFactory(
        @Named(InjectedNames.OPTIONS_VIDEO_HW_ACCEL)
        videoHwAccel: Boolean,
        eglContext: EglBase.Context
    ): VideoEncoderFactory {
        return if (videoHwAccel) {
            DefaultVideoEncoderFactory(eglContext, true, false)
        } else {
            SoftwareVideoEncoderFactory()
        }
    }

    @Provides
    fun videoDecoderFactory(
        @Named(InjectedNames.OPTIONS_VIDEO_HW_ACCEL)
        videoHwAccel: Boolean,
        eglContext: EglBase.Context,
    ): VideoDecoderFactory {
        return if (videoHwAccel) {
            DefaultVideoDecoderFactory(eglContext)
        } else {
            SoftwareVideoDecoderFactory()
        }
    }

    @Singleton
    @Provides
    fun peerConnectionFactory(
        appContext: Context,
        audioDeviceModule: AudioDeviceModule,
        videoEncoderFactory: VideoEncoderFactory,
        videoDecoderFactory: VideoDecoderFactory
    ): PeerConnectionFactory {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(appContext)
                .createInitializationOptions()
        )

        return PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .createPeerConnectionFactory()
    }

    @Provides
    @Named(InjectedNames.OPTIONS_VIDEO_HW_ACCEL)
    fun videoHwAccel() = true
}