package com.arsvechkarev.opengldrawing.drawing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.view.MotionEvent
import android.view.TextureView
import com.arsvechkarev.opengldrawing.Brush
import com.arsvechkarev.opengldrawing.UndoStore
import com.arsvechkarev.opengldrawing.multiplyMatrices
import com.arsvechkarev.opengldrawing.orthoM
import com.arsvechkarev.opengldrawing.throwEx
import com.arsvechkarev.opengldrawing.to4x4Matrix

@SuppressLint("ViewConstructor")
class OpenGLDrawingView(
  context: Context,
  paintingSize: Size,
  var currentBrush: Brush,
  private val undoStore: UndoStore,
  private var backgroundBitmap: Bitmap
) : TextureView(context) {
  
  private val transformedBitmap = false
  private val inputProcessor = InputProcessor(this)
  private var eglDrawer: EGLDrawer? = null
  private var shuttingDown = false
  
  var onDown: () -> Unit = {}
  var onUp: () -> Unit = {}
  
  var currentWeight = 0f
    private set
  var currentColor = 0
    private set
  var painting: Painting? = null
    private set
  
  init {
    val painter = object : Painter {
      override fun onContentChanged(rect: RectF?) {
        eglDrawer?.scheduleRedraw()
      }
      
      override val undoStore = this@OpenGLDrawingView.undoStore
    }
    painting = Painting(paintingSize, painter, this, currentBrush)
    surfaceTextureListener = object : SurfaceTextureListener {
      
      override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture,
        width: Int,
        height: Int
      ) {
        val painting = painting ?: throwEx()
        eglDrawer = EGLDrawer(surface, backgroundBitmap, painting)
        val eglDrawer = eglDrawer ?: throwEx()
        eglDrawer.setBufferSize(width, height)
        updateTransform()
        eglDrawer.requestRender()
        if (painting.isPaused()) {
          painting.onResume()
        }
      }
      
      override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int
      ) {
        val drawer = eglDrawer ?: throwEx()
        drawer.setBufferSize(width, height)
        updateTransform()
        drawer.requestRender()
      }
      
      override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        eglDrawer ?: return true
        if (!shuttingDown) {
          painting!!.onPause(completionAction = {
            eglDrawer!!.shutdown()
            eglDrawer = null
          })
        }
        return true
      }
      
      override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
    }
  }
  
  fun updateColor(value: Int) {
    currentColor = value
  }
  
  fun updateBrushSize(size: Float) {
    currentWeight = size
  }
  
  fun updateBrush(brush: Brush) {
    currentBrush = brush
    painting!!.setBrush(currentBrush)
  }
  
  fun performInEGLContext(action: () -> Unit) {
    val eglDrawer = eglDrawer ?: return
    eglDrawer.postRunnable {
      eglDrawer.setCurrentContext()
      action()
    }
  }
  
  fun getResultBitmap(): Bitmap = eglDrawer!!.getTexture()
  
  fun shutdown() {
    shuttingDown = true
    performInEGLContext {
      painting?.cleanResources(transformedBitmap)
      painting = null
      eglDrawer?.shutdown()
      eglDrawer = null
    }
  }
  
  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> onDown()
      MotionEvent.ACTION_UP -> onUp()
    }
    val eglDrawer = eglDrawer ?: throwEx()
    if (event.pointerCount > 1) {
      return false
    }
    if (eglDrawer.isInitialized && eglDrawer.isReady) {
      inputProcessor.process(event)
      return true
    }
    return false
  }
  
  private fun updateTransform() {
    val matrix = Matrix()
    val paintingSize = painting!!.size
    val scale = width / paintingSize.width
    matrix.preTranslate(width / 2.0f, height / 2.0f)
    matrix.preScale(scale, -scale)
    matrix.preTranslate(-paintingSize.width / 2.0f, -paintingSize.height / 2.0f)
    inputProcessor.setMatrix(matrix)
    val projection = FloatArray(16)
    val right = eglDrawer!!.bufferWidth.toFloat()
    val top = eglDrawer!!.bufferHeight.toFloat()
    projection.orthoM(0.0f, right, 0.0f, top, -1.0f, 1.0f)
    val effectiveProjection = matrix.to4x4Matrix()
    val finalProjection = multiplyMatrices(projection, effectiveProjection)
    painting!!.setRenderProjection(finalProjection)
  }
}