package com.stremlopo.app

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(markerClass=[androidx.media3.common.util.UnstableApi::class])
@Composable fun PlayerScreen(film: Film,onClose:()->Unit) {
    val context=LocalContext.current
    val activity=context as ComponentActivity
    val prefs=remember {context.getSharedPreferences("stremlopo",0)}
    var error by remember {mutableStateOf("")}
    var ready by remember {mutableStateOf(false)}
    var sourceIndex by remember {mutableIntStateOf(0)}
    var retry by remember {mutableIntStateOf(0)}
    val player=remember(film.id,sourceIndex,retry) {ExoPlayer.Builder(context).build().apply {setMediaItem(MediaItem.fromUri(film.sources[sourceIndex].url));seekTo(prefs.getLong("progress:${film.id}",0));prepare();playWhenReady=true}}
    BackHandler(onBack=onClose)
    LaunchedEffect(player) {while(true) {delay(5000);prefs.edit().putLong("progress:${film.id}",player.currentPosition).apply()}}
    DisposableEffect(player) {
        val oldOrientation=activity.requestedOrientation
        activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val bars=WindowCompat.getInsetsController(activity.window,activity.window.decorView)
        bars.hide(WindowInsetsCompat.Type.systemBars());bars.systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ready=false
        val listener=object:Player.Listener {
            override fun onRenderedFirstFrame(){ready=true}
            override fun onPlayerError(e: PlaybackException){error="Não foi possível reproduzir esta fonte. Verifique a internet e tente novamente."}
            override fun onPlaybackStateChanged(state: Int){if(state==Player.STATE_ENDED)prefs.edit().putLong("progress:${film.id}",0).apply()}
        }
        val observer=LifecycleEventObserver {_,event -> if(event==Lifecycle.Event.ON_STOP){prefs.edit().putLong("progress:${film.id}",player.currentPosition).apply();player.pause()}}
        player.addListener(listener);activity.lifecycle.addObserver(observer)
        onDispose {prefs.edit().putLong("progress:${film.id}",if(player.playbackState==Player.STATE_ENDED) 0 else player.currentPosition).apply();player.removeListener(listener);activity.lifecycle.removeObserver(observer);player.release();bars.show(WindowInsetsCompat.Type.systemBars());activity.requestedOrientation=oldOrientation}
    }
    Box(Modifier.fillMaxSize().background(Color.Black).semantics {contentDescription=if(ready) "Vídeo em reprodução" else "Preparando reprodução"}) {
        AndroidView(factory={PlayerView(it).apply {this.player=player;useController=true;setShowSubtitleButton(true)}},update={it.player=player},modifier=Modifier.fillMaxSize())
        if(!ready && error.isBlank()) CircularProgressIndicator(Modifier.align(Alignment.Center),color=Color.White)
        IconButton(onClick=onClose,modifier=Modifier.padding(12.dp)) {Icon(Icons.Outlined.ArrowBack,contentDescription="Fechar reprodução",tint=Color.White)}
        if(error.isNotBlank()) AlertDialog(onDismissRequest={error=""},title={Text("Falha na reprodução")},text={Text(error)},confirmButton={TextButton(onClick={error="";if(film.sources.size>1)sourceIndex=(sourceIndex+1)%film.sources.size else retry++}){Text(if(film.sources.size>1) "Outra fonte" else "Tentar novamente")}},dismissButton={TextButton(onClick=onClose){Text("Voltar")}})
    }
}
