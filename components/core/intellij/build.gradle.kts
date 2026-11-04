import ru.astrainteractive.klibs.rules.core.unit.model.UnitKind

plugins {
    id("kaleidos.kotlin.jvm")
    id("kaleidos.intellij.module")
    alias(libs.plugins.klibs.rules.linter)
}

linter {
    unitKind(UnitKind.CORE_PLATFORM)
    rootPackage.set("com.makeevrserg.kaleidos.core")
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
    }
}
