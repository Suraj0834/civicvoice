package com.example.civicvoice.utils

object CommentMapper {
    fun toDataComment(networkComment: com.example.civicvoice.network.Comment): com.example.civicvoice.data.Comment {
        return com.example.civicvoice.data.Comment(
            id = networkComment._id,
            content = networkComment.content,
            date = networkComment.date,
            username = networkComment.username, // Now safe since username is String? in both classes
            isOfficial = networkComment.isOfficial
        )
    }

    fun toDataComments(networkComments: List<com.example.civicvoice.network.Comment>): List<com.example.civicvoice.data.Comment> {
        return networkComments.map { toDataComment(it) }
    }
}