package com.jeremy.router.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

import groovy.json.JsonSlurper

class RouterPlugin implements Plugin<Project> {

    // 当我们采用apply去引用我们的插件时，apply方法将被执行。因此我们可以在apply方法里面注入插件的逻辑
    @Override
    void apply(Project project) {
        println("I am from RouterPlugin,apply from ${project.name}")
        // 1. 自动帮助用户传递路径参数到注解处理器中
        //     // 声明参数
        //    kapt {
        //        arguments {
        //            arg("root_project_dir", rootProject.projectDir.absolutePath)
        //        }
        //    }
        if (project.extensions.findByName("kapt") != null) {
            project.extensions.findByName("kapt").arguments {
                arg("root_project_dir", project.rootProject.projectDir.absolutePath)
            }
        }

        // 2. 实现旧的构建产物的自动清理
        // TODO: 这里实际没有删除掉
        project.clean.doFirst {
            File routerMappingDir = new File(project.rootProject.projectDir, "router-mapping")
            boolean exists = routerMappingDir.exists()
            println("routerMappingDir path: " + routerMappingDir.absolutePath + " \n isExists: " + exists)
            if (exists) {
                routerMappingDir.deleteDir()
            }
        }
        // 3. 在javac任务后，汇总生成文档


        // 注册Extension
        project.getExtensions().create("router", RouterExtension)

        // 在gradle里面，首先是初始化阶段，然后是配置阶段。在配置阶段，才会把gradle脚本，也就是build.gradle
        // 的所有配置代码集成一遍，插件只有在gradle的配置阶段结束以后才能拿到相关的配置。
        // 当执行到afterEvaluate这个方法时，就意味着当前工程的配置阶段已经结束了。
        RouterExtension extension1 = project["router"]
        println("wikiDir path: " + extension1.wikiDir) // wikiDir path: null

        project.afterEvaluate {
            RouterExtension extension = project["router"]
            println("wikiDir path: " + extension.wikiDir)
            // wikiDir path: D:\Projects\LearningProjects\JSeries\JAppBuild

            // 3. 在javac任务(compileDebugJavaWithJavac)后，汇总生成文档
            project.tasks.findAll { task ->
                task.name.startsWith("compile") && task.name.endsWith("JavaWithJavac")
            }.each { task ->
                task.doLast {
                    File routerMappingDir = new File(project.rootProject.projectDir, "router_mapping")
                    if (!routerMappingDir.exists()) {
                        return
                    }
                    File[] allChildFiles = routerMappingDir.listFiles()
                    if (allChildFiles.length < 1) {
                        return
                    }
                    StringBuilder markdownBuilder = new StringBuilder();
                    markdownBuilder.append("# 页面文档\n\n")

                    allChildFiles.each { child ->
                        if (child.name.endsWith(".json")) {
                            // 接下来需要解析json文件，需要引入JsonSluter
                            JsonSlurper jsonSlurper = new JsonSlurper()
                            def content = jsonSlurper.parse(child)
                            content.each { innerContent ->
                                def url = innerContent['url']
                                def description = innerContent['description']
                                def realPath = innerContent['realPath']

                                markdownBuilder.append("## $description\n")
                                markdownBuilder.append("- url: $url\n")
                                markdownBuilder.append("-realPath $realPath\n\n")
                            }
                        }
                    }
                    def wikiFileDir = new File(extension.wikiDir)
                    if (!wikiFileDir.exists()) {
                        wikiFileDir.mkdirs()
                    }
                    File wikiFile = new File(wikiFileDir, "页面文档.md")
                    if ((wikiFile.exists())) {
                        wikiFile.delete()
                    }
                    wikiFile.write(markdownBuilder.toString())
                }
            }
        }
    }
}