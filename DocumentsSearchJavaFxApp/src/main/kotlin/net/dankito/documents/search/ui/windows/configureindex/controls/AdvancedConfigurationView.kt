package net.dankito.documents.search.ui.windows.configureindex.controls

import net.dankito.documents.search.model.IndexConfig
import tornadofx.*


class AdvancedConfigurationView(indexConfig: IndexConfig, private val updateRulesPreview: () -> Unit) : View() {

    var includeRules: List<String> = ArrayList(indexConfig.includeRules)
        private set

    var excludeRules: List<String> = ArrayList(indexConfig.excludeRules)
        private set


    override val root = vbox {

        add(ConfigureIncludeExcludeRulesView("configure.index.window.determining.includes.label", includeRules) { includeRules ->
            updateIncludeRules(includeRules)
        })

        add(ConfigureIncludeExcludeRulesView("configure.index.window.determining.excludes.label", excludeRules) { excludeRules ->
            updateExcludeRules(excludeRules)
        })

    }


    private fun updateIncludeRules(includeRules: List<String>) {
        this.includeRules = includeRules

        updateRulesPreview()
    }

    private fun updateExcludeRules(excludeRules: List<String>) {
        this.excludeRules = excludeRules

        updateRulesPreview()
    }

}