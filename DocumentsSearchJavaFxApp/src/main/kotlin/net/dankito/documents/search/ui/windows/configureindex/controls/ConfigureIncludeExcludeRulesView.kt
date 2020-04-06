package net.dankito.documents.search.ui.windows.configureindex.controls

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import net.dankito.utils.javafx.ui.controls.addButton
import net.dankito.utils.javafx.ui.controls.removeButton
import net.dankito.utils.javafx.ui.extensions.bindIsAnItemSelectedTo
import net.dankito.utils.javafx.ui.extensions.bindSelectedItemTo
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.fixedWidth
import tornadofx.*


class ConfigureIncludeExcludeRulesView(label: String, currentRules: List<String>, private val updateRulesPreview: (List<String>) -> Unit) : View() {

    companion object {
        private val AddRemoveButtonsHeight = 34.0

        private val AddRemoveButtonsWidth = 40.0

        private val SpaceBetweenAddAndRemoveButtons = 6.0
    }


    private val isARuleSelected = SimpleBooleanProperty(false)

    private val rules = FXCollections.observableArrayList<String>(currentRules)

    private val selectedRule = SimpleObjectProperty<String>(null)


    private val enteredRule = SimpleStringProperty("")


    init {
        enteredRule.addListener { _, _, _ -> updateRulesPreview() }
    }


    override val root = vbox {

        anchorpane {
            alignment = Pos.CENTER_LEFT

            label(messages[label]) {
                useMaxHeight = true
            }

            vboxConstraints {
                marginTop = 6.0
            }
        }

        hbox {
            fixedHeight = AddRemoveButtonsHeight
            alignment = Pos.CENTER_LEFT

            label(messages["configure.index.window.configure.rule.label"]) {
                useMaxHeight = true
            }

            textfield(enteredRule) {
                fixedHeight = 30.0
                useMaxWidth = true

                promptText = messages["configure.index.window.configuring.includes.and.excludes.prompt"]

                action { addRule() }

                hboxConstraints {
                    hGrow = Priority.ALWAYS

                    marginLeft = 6.0
                    marginRight = 12.0
                }
            }

            removeButton {
                fixedWidth = AddRemoveButtonsWidth

                enableWhen(isARuleSelected)

                action { removeSelectedRule() }

                hboxConstraints {
                    marginRight = SpaceBetweenAddAndRemoveButtons
                }
            }

            addButton {
                fixedWidth = AddRemoveButtonsWidth

                action { addRule() }
            }

            vboxConstraints {
                marginTop = 12.0
                marginBottom = 6.0
            }
        }

        listview<String>(rules) {
            selectionModel.bindSelectedItemTo(selectedRule) { selectedRuleChanged(it) }

            selectionModel.bindIsAnItemSelectedTo(isARuleSelected)

            vboxConstraints {
                vGrow = Priority.ALWAYS
            }
        }
    }


    private fun addRule() {
        if (enteredRule.value.isNullOrBlank() == false) {
            rules.add(enteredRule.value)
        }

        updateRulesPreview()
    }

    private fun removeSelectedRule() {
        selectedRule.value?.let {
            rules.remove(it)
        }

        updateRulesPreview()
    }

    private fun updateRulesPreview() {
        val rules = ArrayList(rules)

        enteredRule.value?.let { enteredRule ->
            if (enteredRule.isBlank() == false && rules.contains(enteredRule) == false) {
                rules.add(enteredRule)
            }
        }

        updateRulesPreview(rules)
    }


    private fun selectedRuleChanged(newValue: String?) {
        newValue?.let {
            enteredRule.value = it
        }
    }

}