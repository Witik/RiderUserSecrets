package eu.gillissen.rider.usersecrets

import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private const val propertyGroupNodeName = "PropertyGroup"
private const val conditionAttributeName = "Condition"

internal fun Document.getOrCreatePropertyGroup(indentOptions: CommonCodeStyleSettings.IndentOptions): Node {
    val propertyGroupNodes = getElementsByTagName(propertyGroupNodeName)
    return propertyGroupNodes.first { !it.hasAttribute(conditionAttributeName) }
        ?: insertPropertyGroup(indentOptions)
}

internal fun Document.insertUserSecrets(propertyGroup: Node, value: String, indentOptions: CommonCodeStyleSettings.IndentOptions): Node {
    val newUserSecretsId = createElement(SharedConstants.UserSecretsIdMsBuildProperty)
    newUserSecretsId.textContent = value

    return propertyGroup.insertLastIndented(newUserSecretsId, indentOptions)
}

internal fun Document.insertPropertyGroup(indentOptions: CommonCodeStyleSettings.IndentOptions): Node {
    val newPropertyGroup = createElement(propertyGroupNodeName)

    return documentElement.insertFirstIndented(newPropertyGroup, indentOptions)
}

internal fun Document.saveToFile(path: String) {
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    transformer.setOutputProperty(OutputKeys.METHOD, "xml")
    transformer.setOutputProperty(OutputKeys.INDENT, "no")
    val source = DOMSource(this)
    val writer = OutputStreamWriter(FileOutputStream(path))
    val result = StreamResult(writer)

    transformer.transform(source, result)

    writer.close()
}

internal fun Node.insertFirstIndented(child: Node, indentOptions: CommonCodeStyleSettings.IndentOptions): Node {
    val parentIndent = this.getOuterIndent()
    val childIndent = indentOptions.createIndent(1)
    val childIndentNode = ownerDocument.createTextNode("$parentIndent$childIndent")

    val insertedIndentNode = insertFirst(childIndentNode)

    return insertChildAfterIndent(ownerDocument, child, insertedIndentNode, parentIndent)
}

internal fun Node.insertLastIndented(child: Node, indentOptions: CommonCodeStyleSettings.IndentOptions): Node {
    val parentIndent = this.getOuterIndent()
    val childIndent = indentOptions.createIndent(1)
    val childIndentNode = ownerDocument.createTextNode("$parentIndent$childIndent")

    val insertedIndentNode = insertBeforeLastText(childIndentNode)

    return insertChildAfterIndent(ownerDocument, child, insertedIndentNode, parentIndent)
}

internal fun NodeList.first(predicate: (Node) -> Boolean): Node? {
    for (i in 0 until this.length) {
        val currentNode = this.item(i)
        if (predicate(currentNode)) {
            return currentNode
        }
    }

    return null
}

internal fun Node.hasAttribute(attributeName: String): Boolean {
    val attributes = this.attributes
    for (i in 0 until attributes.length) {
        val currentAttribute = attributes.item(i)
        if (currentAttribute.nodeName == attributeName) {
            return true
        }
    }

    return false
}

internal fun Node.insertAfter(node: Node, afterNode: Node): Node =
    insertBefore(node, afterNode.nextSibling)

internal fun Node.insertFirst(otherNode: Node): Node =
    insertBefore(otherNode, firstChild)

internal fun Node.insertBeforeLastText(node: Node): Node {
    var candidate = lastChild ?: return appendChild(node)

    while (candidate.previousSibling is Text && candidate.previousSibling.parentNode == parentNode) {
        candidate = candidate.previousSibling
    }

    return insertBefore(node, candidate)
}

internal fun Node.getOuterIndent(): String {
    val indentNode = previousSibling as Text?
    return "\n${indentNode?.textContent?.split("\n")?.last() ?: ""}"
}

private fun Node.insertChildAfterIndent(doc: Document, child: Node, insertedIndentNode: Node, parentIndent: String): Node {
    val insertedChildNode = insertAfter(child, insertedIndentNode)
    val potentialIndentNode = insertedChildNode.nextSibling as Text?
    if (potentialIndentNode !is Text || !potentialIndentNode.isIndent() || !potentialIndentNode.textContent.startsWith(parentIndent)) {
        insertAfter(doc.createTextNode(parentIndent), insertedChildNode)
    }

    return insertedChildNode
}

private fun Text.isIndent(): Boolean =
    textContent.contains("\n") && textContent.trim().isEmpty()