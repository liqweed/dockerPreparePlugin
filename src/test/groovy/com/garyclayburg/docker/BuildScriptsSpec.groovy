/*
 * Copyright (c) 2017 Gary Clayburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.garyclayburg.docker

import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

/**
 * <br><br>
 * Created 2017-09-04 16:49
 *
 * @author Gary Clayburg
 */
@Slf4j
class BuildScriptsSpec extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    @Rule TestName name = new TestName()
    private srcdir

    def cleanup() {
        def root = testProjectDir.getRoot()
        println "build dir after test: ${name.methodName}"
        root.traverse {
            println "builddir ${it}"
        }
    }

    def "setup"() {
        println "running test: ${name.methodName}"
        buildFile = testProjectDir.newFile('build.gradle')
        srcdir = testProjectDir.newFolder('src', 'main', 'groovy', 'dummypackage')
        createclass('SimpleMain.groovy',"""
package dummypackage

class DockerplugindemoApplication {

	static void main(String[] args) {
	    println('this is not the main you are looking for')
	}
}

""")
    }

    private void createclass(String filename,String contents) {
        File mainclass = new File(this.srcdir, filename)
        mainclass.createNewFile()
        mainclass << contents
    }


    def 'bootRepackage by itself works'() {
        given:
        buildFile << """
buildscript {
	ext {
		springBootVersion = '1.5.6.RELEASE'
	}
	repositories {
		jcenter()
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:1.5.6.RELEASE")
	}
}

apply plugin: 'groovy'
apply plugin: 'eclipse'
apply plugin: 'org.springframework.boot'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}


dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace')
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
        result.task(':build').outcome == SUCCESS
    }

    def 'apply dockerprepare to vanilla build'() {
        given:
        buildFile << """
plugins {
    id 'com.garyclayburg.dockerprepare'
    id 'org.springframework.boot' version '1.5.6.RELEASE'
}

apply plugin: 'groovy'
apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dockerprepare{
  	dockerBuildDirectory "\${project.buildDir}/docker"
	dockerSrcDirectory "\${project.rootDir}/src/main/docker"

}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('dockerLayerPrepare', '--stacktrace', '--info')
                .withPluginClasspath()
                .build()
        File bootrunner = new File(testProjectDir.root.toString() +"/build/docker/bootrunner.sh")

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS
        bootrunner.exists()
    }

    def 'apply dockerprepare after spring boot'() {
        given:
        buildFile << """
plugins {
    id 'org.springframework.boot' version '1.5.6.RELEASE'
    id 'com.garyclayburg.dockerprepare'
}

apply plugin: 'groovy'
apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace','--info')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS
    }
    def 'apply dockerprepare before spring boot'() {
        given:
        buildFile << """
plugins {
    id 'com.garyclayburg.dockerprepare'
    id 'org.springframework.boot' version '1.5.6.RELEASE'
}

apply plugin: 'groovy'
apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace','--info')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS
    }
    def 'apply dockerprepare before spring boot and groovy'() {
        given:
        buildFile << """
plugins {
    id 'com.garyclayburg.dockerprepare'
    id 'org.springframework.boot' version '1.5.6.RELEASE'
    id 'groovy'
}

apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace','--info')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output
        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS
    }

    def 'apply dockerprepare with spring boot 2'() {
        given:
        buildFile << """
buildscript {
	ext {
		springBootVersion = '2.0.0.M3'
	}
	repositories {
	    jcenter()
		maven { url "https://repo.spring.io/snapshot" }
		maven { url "https://repo.spring.io/milestone" }
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:\${springBootVersion}")
	}
}

plugins {
    id 'com.garyclayburg.dockerprepare'
}

apply plugin: 'groovy'
apply plugin: 'eclipse'
apply plugin: 'org.springframework.boot'
apply plugin: 'io.spring.dependency-management'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
	maven { url "https://repo.spring.io/snapshot" }
	maven { url "https://repo.spring.io/milestone" }
}


dependencies {
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace','--info')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output
        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS
    }

    def 'apply dockerprepare with war file'(){
        given:
        buildFile << """
plugins {
    id 'com.garyclayburg.dockerprepare'
    id 'org.springframework.boot' version '1.5.6.RELEASE'
}

apply plugin: 'groovy'
apply plugin: 'eclipse-wtp'
apply plugin: 'war'
apply plugin: 'com.garyclayburg.dockerprepare'

group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

configurations {
	providedRuntime
}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	providedRuntime('org.springframework.boot:spring-boot-starter-tomcat')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output
        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS

        when:
        result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--info')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output
        then: "2nd build processes new war"
        result.task(':dockerLayerPrepare').outcome == SUCCESS

        when:
        createclass('DockerpluginStuff.groovy',"""
package dummypackage

class DockerpluginStuff {

	static void justamethod(String[] args) {
	    println('this is not even a main')
	}
}

""")
        result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--info')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output
        then: "new class triggers prepare"
        result.task(':dockerLayerPrepare').outcome == SUCCESS

    }

    def 'apply dockerprepare with executable war file'(){
        given:
        buildFile << """
plugins {
    id 'com.garyclayburg.dockerprepare'
    id 'org.springframework.boot' version '1.5.6.RELEASE'
}

apply plugin: 'groovy'
apply plugin: 'eclipse-wtp'
apply plugin: 'war'
apply plugin: 'com.garyclayburg.dockerprepare'

group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

configurations {
	providedRuntime
}

springBoot {
  executable = true
}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	providedRuntime('org.springframework.boot:spring-boot-starter-tomcat')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output

        then: 'fail the build instead of letting zipTree silently fail'
        thrown UnexpectedBuildFailure

    }

    def 'apply dockerprepare with com.palantir.docker'() {
        given:
        buildFile << """
plugins {
    id 'com.garyclayburg.dockerprepare'
    id 'org.springframework.boot' version '1.5.6.RELEASE'
    id 'com.palantir.docker' version '0.13.0'
}

apply plugin: 'groovy'
apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
docker {
  name 'hellodock'
  tags 'v123'
  dockerfile file("\${dockerprepare.dockerBuildDirectory}/Dockerfile")
  dependsOn build
  files "\${dockerprepare.dockerBuildDirectory}"
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace','--info')
                .withPluginClasspath()
                .build()
        println result.getOutput()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS
    }

    def 'apply dockerprepare with DockerBuildImage'() {
        given:
        buildFile << """
plugins {
    id 'com.garyclayburg.dockerprepare'
    id 'org.springframework.boot' version '1.5.6.RELEASE'
    id "com.bmuschko.docker-remote-api" version "3.1.0"
}

apply plugin: 'groovy'
apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}

//task buildImage(type: DockerBuildImage, dependsOn: 'dockerLayerPrepare') {
//    description = "build and tag a Docker Image"
//    inputDir = project.file(dockerprepare.dockerBuildDirectory)
//    tags = [ 'ignoreme:latest']
//}

"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace','--info')
                .withPluginClasspath()
                .build()
        println result.getOutput()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS
    }

    def 'apply dockerprepare without springboot'() {
        given:
        buildFile << """
plugins {
    id 'com.garyclayburg.dockerprepare'
}

apply plugin: 'groovy'
apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dockerprepare{
  	dockerBuildDirectory "\${project.buildDir}/docker"
	dockerSrcDirectory "\${project.rootDir}/src/main/docker"

}

dependencies {
	compile('org.codehaus.groovy:groovy')
}
"""
        when:
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('dockerLayerPrepare', '--info')
                .withPluginClasspath()
                .build()

        then:
        thrown UnexpectedBuildFailure
    }

}
