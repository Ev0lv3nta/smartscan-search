package com.fpf.smartscan.search

interface MediaTag{
    val prototypeId: Long
    val name: String
    val createdAt: Long?
    val lastUsedAt: Long?
    val cohesionScore: Float?
    val nPrototype: Int
}