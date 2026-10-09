package top.roomio.data.party

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeerGenerationFenceTest {
    @Test
    fun offerAnswerAndIceAreBoundToCurrentPeerEpochs() {
        val fence = PeerGenerationFence(localGeneration = 4)
        assertTrue(fence.acceptOffer(senderGeneration = 9, targetGeneration = 0))
        assertTrue(fence.acceptCandidate(senderGeneration = 9, targetGeneration = 0))
        assertTrue(fence.acceptAnswer(senderGeneration = 9, targetGeneration = 4))
        assertTrue(fence.acceptCandidate(senderGeneration = 9, targetGeneration = 4))
        assertFalse(fence.acceptCandidate(senderGeneration = 8, targetGeneration = 4))
        assertFalse(fence.acceptAnswer(senderGeneration = 10, targetGeneration = 4))
        assertFalse(fence.acceptAnswer(senderGeneration = 9, targetGeneration = 3))
    }

    @Test
    fun restartAndNewOfferRejectStalePeerGenerations() {
        val fence = PeerGenerationFence(localGeneration = 7)
        assertTrue(fence.acceptOffer(senderGeneration = 2, targetGeneration = 0))
        assertTrue(fence.acceptRestart(senderGeneration = 2, targetGeneration = 7))
        val replacementPeer = PeerGenerationFence(localGeneration = 8)
        assertFalse(replacementPeer.acceptOffer(senderGeneration = 2, targetGeneration = 7))
        assertTrue(replacementPeer.acceptOffer(senderGeneration = 3, targetGeneration = 0))
        assertFalse(replacementPeer.acceptRestart(senderGeneration = 2, targetGeneration = 7))
    }
}
