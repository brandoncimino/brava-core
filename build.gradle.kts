// 📎 All of these "blocks" are actually method calls with a single lambda parameter.
plugins {
    // 📎 These lambdas have a secret parameter, which can be referred to as `this`.
    //    Everything inside of lambda is actually called as though it were `this.{x}`.
    id("java-library")
    id("java-test-fixtures")
    id("maven-publish")
    // TODO: Inclusion of `jreleaser` seems to cause the https://docs.gradle.org/8.5/userguide/upgrading_version_8.html#deprecated_access_to_conventions warning. Need to look up and see if this is a known issue, which it better be if `jrleaser` is a real thing.
    id("org.jreleaser").version("1.15.0")
    id("signing")
}


val githubUsername = "brandoncimino"

/**
 * 📎 On the "Maven Central" website _(which is actually called "sonatype", or sometimes "nexus")_,
 *    the `groupId` is referred to as the "[namespace](https://central.sonatype.com/publishing/namespaces)". 
 */
val mavenGroupId = "io.github.$githubUsername"
val mavenArtifactId = "brava-core"
val mavenDescription = "Brandon's generic Java utilities."

val repoName = mavenArtifactId
val githubProfile = "https://github.com/$githubUsername"
val githubRepo = "https://github.com/$githubUsername/$repoName"

group = "brava"
// ⚠️ The version number secretly controls build logic if it ends in `-SNAPSHOT`: 
//     - https://github.com/jreleaser/jreleaser/discussions/1685
//     - https://github.com/jreleaser/jreleaser/discussions/1565
version = "2.0.0"

repositories {
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(libs.jetbrains.annotations)
    compileOnly(libs.jackson.annotations)
    api(libs.guava)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(platform(libs.assertj.bom))
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testFixturesImplementation(platform(libs.assertj.bom))
    testFixturesImplementation("org.assertj:assertj-core")
}

tasks.jar {
    enabled = true
    // Remove `plain` postfix from jar file name
    archiveClassifier = ""
}

tasks.test {
    useJUnitPlatform()
}

tasks.javadoc {
    // This tells the "javadoclet" to allow the tags introduced in https://openjdk.org/jeps/8068562. 
    // Source: https://gist.github.com/claudioaltamura/aba1f6506a53b9f5499fd507abd572df
    (options as StandardJavadocDocletOptions).tags(
        "apiNote:a:API Note:",
        "implSpec:a:Implementation Requirements:",
        "implNote:a:Implementation Note:"
    )

    // This tells the "javadoclet" to stop failing my build if it detects and warnings.
    // Source: https://stackoverflow.com/a/73930431/18494923
    // 📎 In a perfect world, we'd be able to just fix these issues. 
    //    However, some of these warnings are just objectively incorrect, the most notable being
    //    the `reference not found` warning issued when you use a type parameter in a `@link`, such as `{@link Function#apply(IN)}`.
    // 📎 Some people reference another option, `quiet` / `-quiet`, but it doesn't seem to do anything.
    (options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true);
}

publishing {
    publications {
        create<MavenPublication>("Maven") {
            // Inspired by: https://stackoverflow.com/a/69891314/18494923
            val javaComponent = components["java"] as AdhocComponentWithVariants
            from(javaComponent)

            groupId = mavenGroupId
            artifactId = mavenArtifactId
            // 📎 `description` and `url` are *required* by Maven Central: https://central.sonatype.org/publish/requirements/#project-name-description-and-url
            // That may or may not actually refer to the `pom` object below...
            // Either way, there is no `url` property here, so...I guess I won't set it 🤷‍♀️
            description = mavenDescription

            // The existence of `testFixtures` breaks JReleaser. See: https://stackoverflow.com/a/69891314/18494923
            // TODO: Test this on newer versions of JReleaser.
            javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) {
                skip()
            }
            javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) {
                skip()
            }

            // This looks to basically be a 1:1 representation of a maven `pom.xml` file, but using Kotlin object initializers. 
            pom {
                packaging = "jar"
                name = mavenArtifactId
                // 📎 `description` and `url` are *required* by Maven Central: https://central.sonatype.org/publish/requirements/#project-name-description-and-url
                description = mavenDescription
                url = githubRepo
                licenses {
                    license {
                        name = "MIT license"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = githubUsername
                        name = "Brandon Cimino"
                        email = "brandon.cimino@gmail.com"
                        // The example on Maven Central's website also includes "organization", but that seems suspect
                        organizationUrl = githubProfile
                    }
                }
                scm {
                    connection = "scm:git:${githubRepo}.git"
                    developerConnection = "scm:git:ssh://github.com/$githubUsername/$repoName.git"
                    url = githubRepo
                }
            }
        }
    }
    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    project.copyright = "Brandon Cimino"
    dryrun = false
    signing {
        active = org.jreleaser.model.Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            active = org.jreleaser.model.Active.ALWAYS
            mavenCentral {
                create("sonatype" /* I think this is an arbitrary name 🤔 */) {
                    active = org.jreleaser.model.Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    // ⚠️ This is incorrectly shown in tutorials as `target/staging-deploy`, but `target` is the output folder of maven, NOT gradle!
                    //    Supporting evidence: https://github.com/jreleaser/jreleaser/discussions/1752
                    stagingRepository("build/staging-deploy")

                    applyMavenCentralRules = true
                    namespace = mavenGroupId
                }
            }
        }
    }
}
