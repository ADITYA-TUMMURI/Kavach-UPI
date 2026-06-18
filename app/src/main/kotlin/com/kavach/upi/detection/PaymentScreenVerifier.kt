package com.kavach.upi.detection

import android.view.accessibility.AccessibilityNodeInfo

object PaymentScreenVerifier {

    private val paymentKeywords = setOf(
        "enter upi pin",
        "pay ₹",
        "send money",
        "scan & pay",
        "payment",
        "transfer",
        "upi pin",
        "amount"
    )

    private val paymentIdFragments = setOf(
        "pin_entry",
        "amount_field",
        "pay_button",
        "upi_transfer",
        "payment_container"
    )

    private const val MAX_DEPTH = 15
    private const val MAX_NODE_LIMIT = 200

    /**
     * Traverses the AccessibilityNodeInfo tree recursively to identify payment signals.
     */
    fun isPaymentScreenActive(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        val counter = intArrayOf(0)
        return traverseNode(rootNode, 0, counter)
    }

    private fun traverseNode(node: AccessibilityNodeInfo, depth: Int, counter: intArrayOf): Boolean {
        if (depth > MAX_DEPTH) return false
        counter[0]++
        if (counter[0] > MAX_NODE_LIMIT) return false

        // 1. Check current node characteristics
        if (matchesPaymentIndicators(node)) {
            return true
        }

        // 2. Traversal of children
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = traverseNode(child, depth + 1, counter)
            child.recycle() // Recycle child node immediately after processing
            if (found) {
                return true
            }
        }

        return false
    }

    private fun matchesPaymentIndicators(node: AccessibilityNodeInfo): Boolean {
        // Check text content
        val text = node.text?.toString()?.lowercase()
        if (text != null) {
            if (paymentKeywords.any { text.contains(it) }) {
                return true
            }
        }

        // Check content description
        val contentDescription = node.contentDescription?.toString()?.lowercase()
        if (contentDescription != null) {
            if (paymentKeywords.any { contentDescription.contains(it) }) {
                return true
            }
        }

        // Check resource ID name
        val viewId = node.viewIdResourceName?.lowercase()
        if (viewId != null) {
            if (paymentIdFragments.any { viewId.contains(it) }) {
                return true
            }
        }

        // Check class name / context relationship
        val className = node.className?.toString()
        if (className != null && className.contains("EditText", ignoreCase = true)) {
            // If it is an input field, check if it resides near currency markers
            val parent = node.parent
            if (parent != null) {
                val hasCurrencyIndicator = searchSiblingForCurrency(parent)
                parent.recycle()
                if (hasCurrencyIndicator) {
                    return true
                }
            }
        }

        return false
    }

    private fun searchSiblingForCurrency(parent: AccessibilityNodeInfo): Boolean {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChild(i) ?: continue
            val text = child.text?.toString()
            child.recycle()
            if (text != null && (text.contains("₹") || text.contains("Rs") || text.contains("INR", ignoreCase = true))) {
                return true
            }
        }
        return false
    }
}
