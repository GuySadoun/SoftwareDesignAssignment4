package main.kotlin

class PairSerializerImpl : Serializer<Pair<String, String>> {
    companion object {
        const val separatorKey = "^|"
    }
    override fun serialize(decoded: Pair<String, String>): ByteArray {
        val from = decoded.first
        val msg = decoded.second
        return (from + separatorKey + msg).toByteArray()
    }

    override fun deserialize(encoded: ByteArray): Pair<String, String> {
        val fromMsg = encoded.toString().split(separatorKey)
        if (fromMsg.size > 2) println("BAD LUCK - WHY CHOOSE \"^|\"?!?!")
        return Pair(fromMsg[0], fromMsg[1])
    }
}