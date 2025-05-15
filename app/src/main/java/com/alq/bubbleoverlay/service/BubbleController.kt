package com.alq.bubbleoverlay.service

interface BubbleController {
    fun renameBubble(id: Long, newName: String)
    fun changeShape(id: Long)
    fun changeSize(id: Long, sizeDp: Int)
    fun changeImage(id: Long)
    fun deleteBubble(id: Long)
    fun addBubble()
    fun closeApp()
}
