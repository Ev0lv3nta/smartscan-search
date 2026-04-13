package com.fpf.smartscan.data

interface MediaClusterMetadata{
    val clusterId: Long
    val prototypeSize: Int
    val meanSimilarity: Float
    val stdSimilarity: Float
    val label: String?
}