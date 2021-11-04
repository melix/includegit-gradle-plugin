package me.champeau.gradle.igp

import me.champeau.includegit.AbstractFunctionalTest

class WellBehavedPluginFunctionalTest extends AbstractFunctionalTest {
    def "can apply the plugin without any configuration"() {
        withSample 'zeroconf'

        when:
        run 'help'

        then:
        tasks {
            succeeded ':help'
        }
    }

}
